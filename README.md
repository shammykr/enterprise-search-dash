# Enterprise Search Dashboard

A full-stack enterprise search application. Users type into a search bar, get
auto-complete suggestions as they type, and narrow results with faceted filters
(category, tags, date range). Documents are stored in **MongoDB** and searched with
**Elasticsearch**, behind a **Spring Boot** REST API and an **Angular Material** UI.

![Search results with category facet selected](docs/screenshot.png)

## Stack

| Layer     | Technology                                            |
|-----------|-------------------------------------------------------|
| Frontend  | Angular 20, Angular Material, RxJS, served by nginx   |
| Backend   | Java 17, Spring Boot 3.3, elasticsearch-java client   |
| Search    | Elasticsearch 8.15 (full-text, aggregations, completion suggester) |
| Storage   | MongoDB 7 (system of record)                          |
| Packaging | Docker Compose                                        |

## Architecture

```
                 ┌──────────────────────────── docker compose ─────────────────────────────┐
 Browser ──────► │ frontend (nginx :80 → host :4200)                                        │
                 │   • Angular SPA (lazy-loaded search + document routes)                   │
                 │   • /api/* proxied ───────┐                                              │
                 │                           ▼                                              │
                 │ backend (Spring Boot :8080)                                              │
                 │   SearchController ──► SearchService ────────► elasticsearch (:9200)     │
                 │     /api/search          multi_match + post_filter      index "documents"│
                 │     /api/autocomplete    terms aggs (facets)            completion field │
                 │                          completion suggester                ▲           │
                 │   DocumentController ──► DocumentService ──► SearchIndexService (sync)   │
                 │     /api/documents CRUD      │                                           │
                 │                              └──────────► mongodb (:27017)               │
                 │                                           collection "documents"         │
                 └──────────────────────────────────────────────────────────────────────────┘
```

**Write path:** a create/update/delete goes to MongoDB first, then the same change is
mirrored into Elasticsearch (with `refresh=wait_for`, so it is searchable as soon as the
API responds). MongoDB is the source of truth: on startup the API seeds 48 sample
documents if MongoDB is empty and rebuilds the index if its document count has drifted from MongoDB's.

**Read path:** search and auto-complete hit Elasticsearch only; the document detail
page reads the full document from MongoDB via the CRUD API.

## Features

### Search bar with auto-complete
- `mat-autocomplete` on the search input; keystrokes are debounced (200 ms) and
  stale requests are cancelled with `switchMap`.
- Backed by an Elasticsearch **completion suggester** with fuzzy matching (`fuzziness: AUTO`
  from 4 chars, so `kubernets` still suggests *Kubernetes Rollout Guide…*).
- Every word-suffix of a title is indexed as a suggestion input, so typing a word from
  the middle of a title (`rollout`, `policy`) also completes it.

### Faceted search
- `GET /api/search` returns `category` and `tags` facets built from Elasticsearch
  **terms aggregations**, and the filter sidebar renders them as checkboxes with counts.
- Selected facets go into `post_filter`; each facet's aggregation is wrapped in a filter
  containing the *other* facet's selection. Selecting a category narrows the results
  and the tag counts, while the category list keeps its sibling counts, so you can tick
  several categories at once (OR within a facet, AND across facets).
- A Material **date range picker** filters on `createdAt`.
- Matches are highlighted (`<mark>`) in titles and snippets.

### Angular frontend
- Standalone components with **lazy-loaded route modules**: `search` and `documents`
  each build into their own chunk (`loadChildren`).
- Search state (query, facets, dates, page) lives in the URL, so results can be linked
  and back/forward works.
- Responsive: filter sidebar beside the results on wide screens, stacked above them
  below 900 px. Results are paginated with `mat-paginator`.

### REST API

| Method | Path                       | Description |
|--------|----------------------------|-------------|
| GET    | `/api/search`              | Full-text search. Params: `q`, `category` (repeatable), `tag` (repeatable), `from`, `to` (`yyyy-MM-dd`), `page`, `size` |
| GET    | `/api/autocomplete`        | Title suggestions. Params: `q`, `limit` (default 8) |
| GET    | `/api/documents`           | List documents from MongoDB (`page`, `size`) |
| GET    | `/api/documents/{id}`      | Get one document |
| POST   | `/api/documents`           | Create (MongoDB + index) |
| PUT    | `/api/documents/{id}`      | Update (MongoDB + reindex) |
| DELETE | `/api/documents/{id}`      | Delete (MongoDB + index) |

Example:

```bash
curl 'http://localhost:4200/api/search?q=policy&category=Security&category=HR&from=2025-01-01'
curl 'http://localhost:4200/api/autocomplete?q=kubern'
curl -X POST http://localhost:4200/api/documents -H 'Content-Type: application/json' \
  -d '{"title":"VPN Setup Guide","content":"How to connect to the corporate VPN.","category":"Operations","tags":["vpn","onboarding"],"author":"IT"}'
```

## Running it

### Everything with Docker Compose (one command)

```bash
docker compose up --build
```

Then open **http://localhost:4200**. The first start builds both images and seeds the
sample data, which takes about a minute. Services:

| Service        | URL / port               |
|----------------|--------------------------|
| UI (nginx)     | http://localhost:4200    |
| API            | http://localhost:8080    |
| Elasticsearch  | http://localhost:9200    |
| MongoDB        | mongodb://localhost:27017 |

`docker compose down -v` stops everything and deletes the data volumes.

### Local development

Requires JDK 17+, Maven, Node 20.19+ (or 22+).

```bash
docker compose up -d elasticsearch mongodb   # just the data stores

cd backend && mvn spring-boot:run            # API on :8080

cd frontend && npm install && npm start      # UI on :4200, /api proxied to :8080
```

Backend configuration (environment variables): `MONGODB_URI`, `ELASTICSEARCH_URIS`,
`SEED_SAMPLE_DATA` (default `true`), `CORS_ALLOWED_ORIGINS`.

## Project layout

```
backend/                      Spring Boot API
  src/main/java/com/shammy/search/
    web/                      SearchController, DocumentController
    service/                  SearchService (queries, facets, suggest),
                              SearchIndexService (mapping + ES sync),
                              DocumentService (CRUD), DataInitializer (seed/reindex)
    model/ repository/ dto/ config/
  src/main/resources/         application.yml, sample-documents.json
frontend/                     Angular app
  src/app/core/               API service + models
  src/app/features/search/    search page, search bar, facet sidebar, results list
  src/app/features/documents/ document detail view
  nginx.conf                  SPA hosting + /api reverse proxy
docker-compose.yml
```

## Results

Checked against the running Compose stack with the 48 bundled sample documents:

- `docker compose up --build` from empty volumes brings up all four services, creates
  the index and seeds and indexes the 48 documents with no manual steps.
- Auto-complete: `roll` → *Kubernetes Rollout Guide for Production Services*;
  `pol` → four `…Policy` titles; the typo `kubernets` still completes to the Kubernetes guide.
- Facets: `q=search` returns 11 hits across 5 categories. Adding `category=Engineering` narrows the results to 3
  and the tag facet to Engineering's tags, while the category facet still lists all 5 (multi-select);
  adding `tag=frontend` narrows the results to 1.
- CRUD: a document created through `POST /api/documents` is searchable right away,
  an update shows up in auto-complete, and after `DELETE` it is gone from both stores.

No performance benchmarks were run. The API reports per-query Elasticsearch time
(`tookMs`), and the UI shows it next to the result count.
