# Overview

Spring Boot app with Spring AI

See SETUP.md for instructions on how this project was setup.

## Running the App

### 1) Start Solr + Seed Data

```bash
# from project root
docker compose up -d solr
docker compose run --rm solr-init
```

Solr runs on: `http://localhost:8983`

### 2) Start Spring Boot API

```bash
# from project root
mvn spring-boot:run
```

Spring Boot runs on: `http://localhost:8081`

### 3) Start Angular Frontend

```bash
# from project root
cd frontend
npm install
npm start
```

Frontend runs on: `http://localhost:4200`

## Testing / Verification

### Verify Solr is working

```bash
# Check seeded record count
curl -s "http://localhost:8983/solr/security-screener/select?q=*:*&rows=0&wt=json"

# Check facet values used by AG Grid set filters
curl -s "http://localhost:8983/solr/security-screener/select?q=*:*&rows=0&facet=true&facet.field=assetClass&facet.field=country&facet.field=currency&facet.mincount=1&wt=json"
```

Expected:
- `numFound` is greater than `0` (seed currently loads ~400 records)
- `facet_fields.assetClass`, `facet_fields.country`, and `facet_fields.currency` contain values

### Verify Spring Boot API is working

```bash
curl -s -X POST "http://localhost:8081/api/ai/grid-query" \
  -H "Content-Type: application/json" \
  -d '{
    "userQuery": "Show high risk securities and sort by risk score descending",
    "gridState": {},
    "structuredSchema": {
      "columns": [
        {"name":"assetClass","type":"string"},
        {"name":"country","type":"string"},
        {"name":"currency","type":"string"},
        {"name":"riskScore","type":"number"}
      ]
    }
  }'
```

Expected:
- JSON response with top-level fields: `filter`, `sort`, `columnVisibility`, `columnSizing`

### Verify Frontend is working

1. Open `http://localhost:4200`
2. Confirm rows are visible in AG Grid
3. Open filters for `Asset Class`, `Country`, and `CCY` and confirm filter options are populated
4. In a grid prompt box, try:
   - `Show securities with risk score above 70 and sort by risk score descending`
   - `Show only sanctions flagged securities`
   - `Show pending review securities and hide currency column`
5. Click **Run Security Screening AI** and confirm the grid updates
