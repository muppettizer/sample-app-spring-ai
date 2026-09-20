# Security Screening Grid Features

This document explains each major feature of the AG Grid Security Screening app and how it works end-to-end.

## Architecture Overview

1. Angular AG Grid renders the screening table and sends filter/sort requests.
2. Solr (`security-screener` collection) serves row data and facet values.
3. Spring Boot + Spring AI handles:
   - Grid state AI instructions (`/api/ai/grid-query`)
   - Screening chat assistant with per-portfolio-manager memory (`/api/ai/screening-chat`)

## Core Grid Features

## 1) Server-Side Row Model

### What it does
- Loads rows from Solr on demand (paging/sorting/filtering at server side).

### How it works
- `frontend/src/app/grid/grid.ts` configures `rowModelType: 'serverSide'`.
- Grid datasource calls `SolrGridService.fetchSolrRows(...)`.
- `frontend/src/app/grid/solr-grid.service.ts` maps AG Grid request model to Solr query params:
  - `start`, `rows`
  - `sort`
  - `fq` filters

## 2) Solr-Backed Set Filters (Asset Class / Country / CCY)

### What it does
- Filter dropdown options for `Asset Class`, `Country`, and `CCY` are populated from live Solr facets.

### How it works
- Columns use `agSetColumnFilter`.
- Filter values are loaded via `SolrGridService.fetchFacetValues(field)`.
- Service queries:
  - `facet=true`
  - `facet.field=assetClass|country|currency`
  - `facet.mincount=1`
- Returned buckets are converted into AG Grid set-filter values.

## 3) Numeric / Text / Date Filtering

### What it does
- Supports:
  - text filters (`issuer`, `rating`, etc.)
  - numeric filters (`riskScore`, `marginRate`)
  - date-like values (`lastReviewDate`)

### How it works
- AG Grid filter models are converted into Solr filter queries (`fq`) in `SolrGridService`.
- Number filters map to Solr range/equality expressions.
- Set filters map to OR expressions:
  - `field:("A" OR "B" OR "C")`

## 4) Sorting

### What it does
- Column sorting in grid maps directly to Solr `sort`.

### How it works
- Grid sort model is read server-side and mapped to Solr sort string.
- Solr schema uses sortable field types (primarily `string`, numeric types).
- Collection init enforces schema and disables auto-create fields to avoid type drift.

## AI Features

## 5) Toolbar Prompt: "Run Security Screening AI"

### What it does
- User enters natural language, AI returns AG Grid state updates.

### How it works
- Frontend posts query, current grid state, and structured schema to:
  - `POST /api/ai/grid-query`
- `GridAiService` (Spring) prompts the model to return strict JSON:
  - `filter`
  - `sort`
  - `columnVisibility`
  - `columnSizing`
- Frontend applies returned state to AG Grid.

## 6) Preset Prompt Buttons

### What it does
- One-click common screening actions:
  - High Risk
  - Sanctions Flagged
  - Pending Review
  - Better Margin

### How it works
- Buttons call `runPreset(...)` in `grid.ts`.
- Preset text is assigned to query input and sent through the same AI endpoint as manual prompts.

## 7) Row Action: "Find Similar (Better Margin)"

### What it does
- On a selected security row, asks AI for similar securities with lower margin rate.

### How it works
- Row action builds a contextual prompt from row JSON.
- Prompt requests lower `marginRate` while keeping comparable profile.
- Returned AI grid state is applied to filters/sorting.

## 8) Screening AI Chat Assistant

### What it does
- Side-panel conversational assistant for PMs.
- Maintains conversation history per portfolio manager.

### How it works
- Frontend chat panel calls:
  - `POST /api/ai/screening-chat`
  - `GET /api/ai/screening-chat/{portfolioManagerId}/history`
  - `DELETE /api/ai/screening-chat/{portfolioManagerId}/history`
- Backend `ScreeningChatService` stores memory by `portfolioManagerId` (currently in-memory).
- Prompt includes:
  - current grid state
  - structured schema
  - recent chat history window

## 11) Spring AI ChatMemory (InMemory + Redis-Ready)

### What it does
- Uses Spring AI `ChatMemory` abstraction (instead of a custom map) for chat history.
- Keeps chat memory scoped per Portfolio Manager using conversation IDs.
- Supports switching memory backend by config.

### How it works
- Config class: `src/main/java/com/sample/app/ai/screeningchat`
- Service class: `src/main/java/com/sample/app/ai/screeningchat`
- `portfolioManagerId` is used as `ChatMemory.CONVERSATION_ID`.
- `MessageChatMemoryAdvisor` automatically:
  - reads memory for conversation before model call
  - appends latest user/assistant messages after call

### Default behavior
- Property in `src/main/resources/application.yml`:
  - `app.screening.chat-memory.type: in-memory`
- Uses:
  - `InMemoryChatMemoryRepository`
  - `MessageWindowChatMemory` (max 50 messages)

### Redis option
- Set:
  - `app.screening.chat-memory.type: redis`
- Then provide a Redis-backed `ChatMemoryRepository` bean/starter configuration.
- If Redis mode is selected but no non-in-memory repository is found, startup fails fast with a clear error.

### API flow for chat memory
- `POST /api/ai/screening-chat`
  - sends PM id + message + grid context
  - model response is generated with memory advisor
- `GET /api/ai/screening-chat/{portfolioManagerId}/history`
  - returns stored chat history from `ChatMemory`
- `DELETE /api/ai/screening-chat/{portfolioManagerId}/history`
  - clears history for that PM conversation

### Why this is better than manual memory
- Standard Spring AI integration point
- Backend swap flexibility (in-memory now, Redis/JDBC later)
- Cleaner conversation lifecycle handling via advisor + repository abstractions

## Solr/Data Features

## 9) Security Screening Sample Dataset

### What it does
- Seeds realistic synthetic securities for UI/testing.

### How it works
- Data file: `data/securities.json`
- Includes fields like:
  - `securityId`, `issuer`, `assetClass`, `country`, `currency`
  - `riskScore`, `marginRate`, `sanctionsFlag`, `screeningStatus`

## 10) Externalized Solr Schema

### What it does
- Keeps schema definition versioned and readable.

### How it works
- Schema file: `data/security-screener-schema.json`
- `compose.yml` `solr-init`:
  1. resets collection
  2. creates `security-screener`
  3. disables auto-create fields
  4. applies schema file
  5. seeds sample data

## Portfolio Manager History Behavior

- Current chat memory is per-PM and works while backend process is alive.
- For durable PM history across restarts, use persistent storage (Redis or relational DB).

## Suggested Next Enhancements

- Add PM authentication and derive `portfolioManagerId` from auth token.
- Persist chat memory in DB for audit/compliance.
- Add "Apply to Grid" actions from chat replies (structured action buttons).
- Add explainability metadata (why a filter/sort was chosen) for governance.

# Spring AI Tools
Flow:
Frontend Portfolio holdings → POST /api/ai/portfolio-holdings-assistant.
PortfolioHoldingsAssistantService uses Spring AI ChatClient + @Tool so the model can call getPortfolioHoldingsForSecurity(instrumentId).
SecurityHoldingsApiClient performs GET {base}/api/v1/securities/{id}/portfolio-holdings, which hits WireMock on localhost:18089 when configured as above.
