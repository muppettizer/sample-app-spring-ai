#!/bin/sh
set -e

SOLR_HOST="${SOLR_HOST:-solr}"
SOLR_PORT="${SOLR_PORT:-8983}"
COLLECTION_NAME="${COLLECTION_NAME:-security-screener}"
ORIGIN="${ORIGIN:-http://localhost:4200}"

BASE_URL="http://${SOLR_HOST}:${SOLR_PORT}/solr"

log() {
  printf '\n========================================\n%s\n========================================\n\n' "$1"
}

log "1. Testing CORS Preflight OPTIONS Request"
curl -i -s -X OPTIONS \
  -H "Origin: ${ORIGIN}" \
  -H "Access-Control-Request-Method: GET" \
  -H "Access-Control-Request-Headers: Content-Type" \
  "${BASE_URL}/${COLLECTION_NAME}/select?q=type%3ALOANS"

log "2. Testing CORS GET Request"
curl -i -s -X GET \
  -H "Origin: ${ORIGIN}" \
  "${BASE_URL}/${COLLECTION_NAME}/select?q=type%3ALOANS&start=0&rows=100&wt=json"

log "3. Testing Basic Admin Endpoint"
curl -i -s -X GET \
  -H "Origin: ${ORIGIN}" \
  "${BASE_URL}/admin/collections?action=LIST"

log "=== CORS Testing Complete ==="

# Block script completion so container remains running for Docker Compose checks
exec tail -f /dev/null