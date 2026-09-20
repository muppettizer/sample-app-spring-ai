#!/bin/sh
set -e

SOLR_HOST="${SOLR_HOST:-solr}"
SOLR_PORT="${SOLR_PORT:-8983}"
COLLECTION_NAME="${COLLECTION_NAME:-security-screener}"
BASE_URL="http://${SOLR_HOST}:${SOLR_PORT}/solr"

log() {
  printf '\n========================================\n%s\n========================================\n\n' "$1"
}

log "1. Waiting for SolrCloud cluster state..."
until curl -s "${BASE_URL}/admin/collections?action=CLUSTERSTATUS" | grep -q '"status":0'; do
  echo "SolrCloud not ready yet. Retrying in 2s..."
  sleep 2
done

log "2. Resetting collection if it exists..."
curl -s -X POST \
  "${BASE_URL}/admin/collections?action=DELETE&name=${COLLECTION_NAME}" \
  > /dev/null || true

log "3. Creating collection..."
curl -f -X POST \
  "${BASE_URL}/admin/collections?action=CREATE&name=${COLLECTION_NAME}&numShards=1&replicationFactor=1&collection.configName=_default"

log "4. Waiting for collection schema endpoint to activate..."
until curl -s -f "${BASE_URL}/${COLLECTION_NAME}/schema" > /dev/null; do
  echo "Collection ${COLLECTION_NAME} endpoint not ready yet. Retrying in 2s..."
  sleep 2
done

log "5. Disabling auto-create fields..."
curl -f -X POST \
  -H "Content-Type: application/json" \
  "${BASE_URL}/${COLLECTION_NAME}/config" \
  -d '{"set-user-property": {"update.autoCreateFields":"false"}}'

log "6. Ensuring marginRate exists before schema replace..."
curl -f -X POST \
  -H "Content-Type: application/json" \
  "${BASE_URL}/${COLLECTION_NAME}/schema" \
  -d '{"add-field":{"name":"marginRate","type":"pfloat","stored":true,"indexed":true}}'

log "7. Uploading schema..."
curl -f -v -X POST \
  -H "Content-Type: application/json" \
  "${BASE_URL}/${COLLECTION_NAME}/schema" \
  -d @/data/security-screener-schema.json

log "8. Verifying schema fields..."
curl -s \
  "${BASE_URL}/${COLLECTION_NAME}/schema/fields/assetClass,country?wt=json"

log "9. Seeding initial data..."
curl -f -X POST \
  -H "Content-Type: application/json" \
  "${BASE_URL}/${COLLECTION_NAME}/update?commit=true" \
  -d @/data/securities.json

log "10. Testing collection queries..."
curl -f \
  "${BASE_URL}/${COLLECTION_NAME}/select?q=*:*&rows=2&wt=json"

log "=== Solr Initialization Complete ==="

# Block script completion so container remains running for Docker Compose checks
exec tail -f /dev/null