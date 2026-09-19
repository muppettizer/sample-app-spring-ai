#!/bin/sh

log() {
  printf '\n%s\n\n' "$1"
}

log 'Waiting for Solr...'
until curl -s http://solr:8983/solr/admin/info/system > /dev/null; do
  sleep 5
done

log 'Resetting collection if it exists...'
curl -s -X POST \
  "http://solr:8983/solr/admin/collections?action=DELETE&name=security-screener" \
  > /dev/null || true

log 'Creating collection...'
curl -X POST \
  "http://solr:8983/solr/admin/collections?action=CREATE&name=security-screener&numShards=1&replicationFactor=1&collection.configName=_default"

log 'Disabling auto-create fields...'
curl -X POST \
  -H "Content-Type: application/json" \
  "http://solr:8983/solr/security-screener/config" \
  -d '{"set-user-property": {"update.autoCreateFields":"false"}}'

log 'Ensuring marginRate exists before replace...'
curl -X POST \
  -H "Content-Type: application/json" \
  "http://solr:8983/solr/security-screener/schema" \
  -d '{"add-field":{"name":"marginRate","type":"pfloat","stored":true,"indexed":true}}'

log 'Adding schema...'
curl -v -X POST \
  -H "Content-Type: application/json" \
  "http://solr:8983/solr/security-screener/schema" \
  -d @/data/security-screener-schema.json

log 'Schema after update...'
curl -s \
  "http://solr:8983/solr/security-screener/schema/fields/assetClass,country?wt=json"

log 'Seeding data...'
curl -X POST \
  -H "Content-Type: application/json" \
  "http://solr:8983/solr/security-screener/update?commit=true" \
  -d @/data/securities.json

log 'Done'

log 'Testing collection...'
curl \
  "http://solr:8983/solr/security-screener/select?q=*:*&rows=2&wt=json"

log 'Testing assetClass facet...'
curl -s \
  "http://solr:8983/solr/security-screener/select?q=*:*&rows=0&facet=true&facet.field=assetClass&wt=json"

printf '\n'