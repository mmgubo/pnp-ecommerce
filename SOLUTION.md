# Solution Notes — PnP Ecommerce Service

This document explains the key design decisions made during implementation.

---

## Technology Choices

| Concern | Choice | Reason |
|---|---|---|
| Language | Java 17 | LTS release; records, sealed classes, pattern matching available |
| Framework | Spring Boot 3.2 | Production-grade, well-understood, integrates all required concerns |
| Database | H2 in-memory (default) / PostgreSQL (Docker) | Zero-setup locally; Docker profile uses real Postgres for production parity |
| ORM | Spring Data JPA | Clean CRUD repositories with zero boilerplate |
| Reporting SQL | `NamedParameterJdbcTemplate` | Reporting queries are aggregation-heavy; keeping them explicit makes them auditable and performant |
| Validation | Jakarta Bean Validation (`@Valid`, `@Min`, `@Max`) | Declarative, enforced at the controller boundary |
| Security | Spring Security — HTTP Basic, stateless | Simplest scheme that satisfies the auth requirements without session state |
| API Docs | Springdoc OpenAPI / Swagger UI | Auto-generated from annotations; live at `/swagger-ui.html` |
| Testing | JUnit 5 + Mockito + MockMvc + `@DataJpaTest` + `@SpringBootTest` | Full pyramid: unit → slice → integration |
| Build | Maven 3.8+ | Standard for Spring Boot; reproducible with wrapper |
| Containerisation | Multi-stage Docker + Docker Compose | Separate build and runtime images; Compose adds Postgres + pgAdmin |
| CI | GitHub Actions | `mvn -B verify` runs on every push and pull request |

---

## API Design

Base path: `http://localhost:8080/api/v1`

### Endpoints

| Method | Path | Auth | Description |
|---|---|---|---|
| `POST` | `/products` | Basic Auth | Create a product |
| `GET` | `/products` | Public | Paginated + filtered product list (`name`, `minPrice`, `maxPrice`, `page`, `size`, `sort`) |
| `GET` | `/products/{id}` | Public | Fetch a single product |
| `POST` | `/orders` | Basic Auth | Place an order (locks stock, decrements atomically) |
| `GET` | `/orders/{id}` | Basic Auth | Retrieve order with line items |
| `PATCH` | `/orders/{id}/cancel` | Basic Auth | Cancel a CONFIRMED order and restore stock |
| `GET` | `/reports/top-products` | Basic Auth | Top N products by units sold; optional ISO date range |
| `GET` | `/reports/order-summary` | Basic Auth | Order counts, revenue, and average by status |

### Why this auth split?

- **`GET /products` is public** — catalogue browsing is a pre-login, shopper-facing action.
- **`GET /orders/{id}` is protected** — order records contain customer name and email. With sequential `BIGINT` IDs an unauthenticated endpoint would expose PII to trivial enumeration attacks (a POPIA / GDPR concern).
- **`GET /reports/**` is protected** — sales aggregates are commercially sensitive.
- **All writes are protected** for obvious reasons.

---

## Domain Model

Three tables. Schema is declared explicitly in `schema.sql` with `ddl-auto=none` — no Hibernate schema generation in any environment.

```
products
  id, name, description, price (cents), stock_quantity, sku (unique), created_at, updated_at

orders
  id, customer_name, customer_email, status (CONFIRMED|CANCELLED), total_amount (cents), created_at, updated_at

order_items
  id, order_id (FK), product_id (FK), quantity, unit_price (cents snapshot)
```

### Key constraints (enforced in the DB)

- `uq_products_sku` — SKU is the business identifier; duplicates are rejected with `409 CONFLICT`.
- `chk_products_price` / `chk_products_stock` — price and stock cannot go negative at the DB level.
- `chk_orders_status` — status is limited to the two defined transitions.
- `chk_order_items_qty` / `chk_order_items_price` — line-item quantity must be > 0; unit price ≥ 0.

### Indexes

- `idx_order_items_order_id`, `idx_order_items_product_id` — FK columns; make JOIN-heavy reporting queries fast.
- `idx_orders_status` — `WHERE status = 'CONFIRMED'` filter used by top-products report.
- `idx_orders_customer_email` — future support for customer order lookup.

---

## Notable Implementation Decisions

### Prices in cents (integer)

`R39.99` is stored as `3999` (`INT`). This eliminates IEEE 754 floating-point rounding errors in all arithmetic. Prices are formatted as `R%.2f` only at the API boundary (response DTOs).

### Pessimistic write lock on stock

`ProductRepository.findByIdWithLock` issues `SELECT ... FOR UPDATE`. This ensures that two concurrent orders cannot both observe sufficient stock and race to oversell — the second transaction blocks until the first commits or rolls back.

The same lock is re-acquired during cancellation (`cancelOrder`) before restoring stock, maintaining the same invariant in both directions.

### Price snapshot on order items

`order_items.unit_price` captures the product's price at the moment of order placement. Subsequent catalogue price changes do not alter historical order totals or revenue reports.

### JPA + JdbcTemplate split

- **JPA repositories** handle all CRUD operations (products, orders, order items).
- **`NamedParameterJdbcTemplate`** handles the two reporting queries. Keeping aggregation SQL explicit makes it readable, optimisable, and safe from N+1 traps that JPA can introduce for complex joins.

### Order state machine

Only one transition is currently supported: `CONFIRMED → CANCELLED`. Attempting to cancel an order that is already `CANCELLED` returns `409 CONFLICT` with error code `INVALID_ORDER_STATE`.

### Error responses

All errors follow a consistent envelope via `GlobalExceptionHandler`:

```json
{
  "status": 409,
  "error": "INSUFFICIENT_STOCK",
  "message": "Insufficient stock for SKU APPLE-1KG: requested 5, available 2",
  "path": "/api/v1/orders",
  "timestamp": "2024-01-15T10:30:00Z",
  "fieldErrors": null
}
```

Validation errors populate `fieldErrors` with a map of `{ field: message }`.

---

## Security Configuration

- **Stateless sessions** (`STATELESS`) — no server-side session; every request carries credentials.
- **BCrypt** password encoding.
- Credentials are externalised via `app.security.username` / `app.security.password` (env vars `APP_SECURITY_USERNAME` / `APP_SECURITY_PASSWORD`). Default: `admin` / `secret`.
- CSRF is disabled — the API is stateless and consumed by machine clients, not browsers submitting forms.
- H2 console, Swagger UI, and OpenAPI docs are whitelisted for development convenience.

---

## Testing Strategy

| Layer | Class | What it covers |
|---|---|---|
| Unit | `ProductServiceTest` | Service logic: duplicate SKU rejection, not-found handling |
| Unit | `OrderServiceTest` | Place order (happy path + insufficient stock), cancel (happy + wrong state), product not found |
| Slice | `ProductControllerTest` | MockMvc: create, list (filters, pagination), get-by-id, auth guard |
| Slice | `OrderControllerTest` | MockMvc: place order, get order, cancel, auth guard |
| Slice | `ProductRepositoryTest` | `@DataJpaTest`: SKU uniqueness constraint, price/stock constraints |
| Integration | `ReportControllerIntegrationTest` | `@SpringBootTest` + real H2: seeds data, exercises both report endpoints end-to-end |

Run all tests:

```bash
mvn test
```

---

## Running the Service

### Option 1 — Maven (local JDK 17, H2 in-memory)

```bash
mvn spring-boot:run
```

No external dependencies. Database is wiped on restart.

### Option 2 — Docker Compose (PostgreSQL)

```bash
docker compose up --build
```

Starts three containers:

| Container | Port | Notes |
|---|---|---|
| `pnp-ecommerce` | `8080` | Spring Boot app (waits for DB health check) |
| `pnp-db` | `5432` | PostgreSQL 16 with persistent volume |
| `pnp-pgadmin` | `5050` | pgAdmin 4 — `admin@pnp.local` / `admin` |

Override credentials via environment variables:

```bash
APP_SECURITY_USERNAME=myuser APP_SECURITY_PASSWORD=mypass docker compose up --build
```

### Dockerfile

Multi-stage build:

1. **Build stage** (`maven:3.9-eclipse-temurin-17`) — resolves dependencies, then compiles sources. Dependency layer is cached separately for fast rebuilds on source-only changes.
2. **Runtime stage** (`eclipse-temurin:17-jre-jammy`) — copies only the JAR; runs as a non-root `spring` user.

---

## Useful URLs (local)

| Resource | URL |
|---|---|
| Swagger UI | `http://localhost:8080/swagger-ui.html` |
| OpenAPI JSON | `http://localhost:8080/api-docs` |
| H2 Console | `http://localhost:8080/h2-console` (JDBC: `jdbc:h2:mem:pnpdb`, user: `sa`, blank password) |
| pgAdmin (Docker) | `http://localhost:5050` |

---

## Postman Collection

A ready-to-import Postman v2.1 collection lives at [`postman/pnp-ecommerce.postman_collection.json`](postman/pnp-ecommerce.postman_collection.json).

- Collection-level Basic Auth uses the default `admin` / `secret`.
- Public `GET` endpoints override auth to `No Auth`.
- Post-response scripts capture `productId` and `orderId` from create/place responses so follow-up requests work without manual copy-paste.
