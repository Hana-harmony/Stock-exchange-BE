# API Standard

모든 비즈니스 REST API는 공통 응답 envelope를 사용한다. Actuator health처럼 인프라가 직접 소비하는 운영 endpoint는 예외로 둔다.

## Success Response

```json
{
  "success": true,
  "status": 200,
  "code": "COMMON_000",
  "message": "OK",
  "data": {},
  "timestamp": "2026-06-18T00:00:00Z"
}
```

## Error Response

```json
{
  "success": false,
  "status": 409,
  "code": "TRADE_001",
  "message": "Mock USD account has insufficient balance",
  "timestamp": "2026-06-18T00:00:00Z"
}
```

## Error Codes

| Code | HTTP | Meaning |
| --- | --- | --- |
| `COMMON_000` | 200 | Success |
| `COMMON_001` | 400 | Invalid request |
| `COMMON_002` | 400 | Validation failed |
| `COMMON_003` | 404 | Resource not found |
| `COMMON_004` | 429 | Rate limit exceeded |
| `COMMON_999` | 500 | Internal server error |
| `AUTH_001` | 409 | Username already exists |
| `AUTH_002` | 401 | Invalid username or password |
| `AUTH_003` | 401 | Invalid auth token |
| `AUTH_004` | 403 | Authenticated account cannot access this account resource |
| `AUTH_005` | 401 | Invalid refresh token |
| `ACCOUNT_001` | 404 | Mock USD account not found |
| `WATCHLIST_001` | 404 | Watchlist item not found |
| `ALERT_001` | 404 | Alert event not found |
| `NOTIFICATION_001` | 404 | Notification not found |
| `MARKET_001` | 502 | Hana OmniLens market upstream unavailable |
| `TRADE_001` | 409 | Mock USD account has insufficient balance |
| `TRADE_002` | 409 | Mock holding has insufficient quantity |
| `TAX_001` | 404 | Tax refund case not found |
| `TAX_002` | 502 | Tax refund status sync failed |
| `TAX_003` | 400 | Tax document is invalid |
| `TAX_004` | 404 | Tax document not found |
| `TAX_005` | 500 | Tax document storage failed |

## Swagger

- Swagger UI: `/swagger-ui/index.html`
- OpenAPI JSON: `/v3/api-docs`

## Rate Limit

- Rate limit is enabled with `EXCHANGE_RATE_LIMIT_ENABLED=true`.
- Applies to `/api/v1/**` requests.
- Account paths use accountId as the rate limit key, and other API paths use the client IP or first `X-Forwarded-For` value.
- Configure with `EXCHANGE_RATE_LIMIT_MAX_REQUESTS` and `EXCHANGE_RATE_LIMIT_WINDOW`.
- Responses include `X-RateLimit-Limit`, `X-RateLimit-Remaining`, and, when blocked, `Retry-After`.
- Exceeded requests return common response code `COMMON_004` with HTTP 429.

## Hana REST Retry

- Hana-OmniLens-API REST clients retry transport failures from Spring `RestClientException`.
- Common response envelope failures such as `success=false` are treated as business failures and are not retried.
- Defaults: `HANA_OMNILENS_REST_RETRY_ENABLED=true`, `HANA_OMNILENS_REST_RETRY_MAX_ATTEMPTS=3`, `HANA_OMNILENS_REST_RETRY_INITIAL_DELAY=100ms`, `HANA_OMNILENS_REST_RETRY_MAX_DELAY=1s`.
- Covered clients: stock search/detail, single/bulk/all quote snapshot, chart history, orderability, and tax status sync.

## Auth

- `POST /api/v1/auth/signup`
  - Creates a local user and a mock USD account.
- `POST /api/v1/auth/login`
  - Issues a local HMAC-signed JWT-like bearer token and refresh token for a username/password pair.
  - Token claims include `sub`, `username`, `accountId`, `iat`, and `exp`.
  - Signing key and TTLs are configured by `EXCHANGE_AUTH_TOKEN_SIGNING_KEY`, `EXCHANGE_AUTH_ACCESS_TOKEN_TTL`, and `EXCHANGE_AUTH_REFRESH_TOKEN_TTL`.
- `POST /api/v1/auth/token/verify`
  - Verifies token signature and expiry, then returns the token claims used by the FE session context.
- `POST /api/v1/auth/token/refresh`
  - Validates an active refresh token, revokes the previous refresh session, then issues a new access token and rotated refresh token.
  - Reusing an old, revoked, expired, or unknown refresh token returns `AUTH_005`.
  - Refresh sessions store issue-time IP/User-Agent context. If a refresh request arrives from a different IP or User-Agent, the API still rotates the token but records `AUTH_SESSION_ANOMALY_DETECTED` in the account audit log.
- `POST /api/v1/auth/logout`
  - Revokes the active refresh session. A revoked refresh token cannot be used again.
- `GET/POST/DELETE /api/v1/accounts/**`
  - Requires `Authorization: Bearer <accessToken>`.
  - Spring Security verifies the local HMAC token and stores `userId`, `username`, `accountId`, `iat`, and `exp` in the authentication context.
  - Account-scoped paths must match the token `accountId`; mismatches return `AUTH_004`.
- Public endpoints include signup, login, token verify, stock/market public snapshots, chart, WebSocket ingest smoke endpoints, alert ingest/target lookup, Swagger, and actuator health.

## Stock Search And Detail

- `GET /api/v1/stocks/search`
  - `query`: required search text.
  - `market`: optional `KOSPI`, `KOSDAQ`, `KONEX`, `OTHER` filter.
  - `currency`: optional ISO 4217 display currency. Defaults to `USD`.
  - `limit`: optional result limit from 1 to 50. Defaults to 20.
- `GET /api/v1/stocks/{stockCode}`
  - Stock detail for the FE stock detail screen.
  - Returns English display name, KRW price, requested local currency price, foreign ownership metrics, VI state, price limit state, trading halt, and orderable flag.
- Stock search/detail calls Hana-OmniLens-API and returns the common response envelope.

## Market Quote Snapshot

- `GET /api/v1/market/quotes`
  - `stockCodes`: optional repeated 6-digit stock code. Omit it to call Hana-OmniLens-API all Korean stock quote endpoint.
  - `market`: optional `KOSPI`, `KOSDAQ`, `KONEX`, `OTHER` filter.
  - `currency`: optional ISO 4217 display currency. Defaults to `USD`.
  - Calls Hana bulk quote endpoint when `stockCodes` is present and Hana all quote endpoint when it is omitted.
- `GET /api/v1/market/quotes/{stockCode}`
  - Single stock snapshot for stock detail and mock trade price lookup.
- `GET /api/v1/accounts/{accountId}/market/quotes/watchlist`
  - Account watchlist quote snapshot. Empty watchlists return `quoteCount: 0` and do not fall back to the default universe.
- `GET /api/v1/accounts/{accountId}/market/quotes/portfolio`
  - Account portfolio holding quote snapshot. Empty holdings return `quoteCount: 0`.
- Quote payload includes KRW price, requested local currency price, FX rate, FX rate time, FX rate source, market, change rate, volume, stale flag, and REST/WebSocket transport metadata.
- Quote snapshot payload includes `cache.status`, `cache.cachedAt`, `cache.expiresAt`, and `cache.staleUntil`.
- Cache status values:
  - `LIVE`: Stock-exchange-BE called Hana-OmniLens-API and refreshed the short-cache.
  - `FRESH_CACHE`: Stock-exchange-BE served a snapshot inside `HANA_OMNILENS_QUOTE_CACHE_TTL`.
  - `STALE_CACHE`: Hana-OmniLens-API was unavailable and Stock-exchange-BE served a snapshot inside `HANA_OMNILENS_QUOTE_CACHE_STALE_TTL`; quote `fxStale` is `true`.
  - `EMPTY`: Account watchlist or portfolio had no stock codes, so no upstream call or cache entry was used.

## Trade

- `POST /api/v1/accounts/{accountId}/trades`
  - Executes an internal mock buy/sell ledger entry.
  - Uses Hana-OmniLens-API USD quote as the mock execution price.
  - Does not call KIS real order or KIS mock trading.
- `GET /api/v1/accounts/{accountId}/trades/orderability`
  - Query params: `stockCode`, `side`, `quantity`.
  - Calls Hana-OmniLens-API orderability boundary before a mock order.
  - Returns `canPlaceMockOrder`, `blockingReasons`, and `warnings`.
  - Blocking reasons include foreign ownership limit exhaustion, trading halt, or upstream order blocked reason.
  - Warnings include VI activity, single-price trading, and buy/sell at upper/lower price limit.
- `POST /api/v1/accounts/{accountId}/trades`
  - Re-checks the same Hana-OmniLens-API orderability boundary before writing the mock ledger.
  - Returns common error code `TRADE_003` when blocking reasons exist, and no mock holding/trade ledger row is written.
- `GET /api/v1/accounts/{accountId}/portfolio`
  - Returns mock USD cash, holdings, recent trades, realized PnL, total market value, total asset value, and unrealized PnL.
  - Holding rows include average price, cost basis, current Hana USD quote price, market value, unrealized PnL, unrealized PnL rate, and market data time.

## Market Chart

- `GET /api/v1/market/stocks/{stockCode}/chart`
  - `from`: required ISO date.
  - `to`: required ISO date.
  - `interval`: optional `1d`, `1w`, `1mo`. Defaults to `1d`.
  - `currency`: optional ISO 4217 display currency. Defaults to `USD`.
- Stock-exchange-BE calls Hana-OmniLens-API `/api/v1/market/stocks/{stockCode}/history` for KRX daily history and the single quote API for FX metadata.
- Chart points include KRW OHLCV, trading value, adjusted flag, and requested local currency OHLC. `1d` returns daily rows, while `1w` and `1mo` aggregate OHLCV and trading value in Stock-exchange-BE.

## Market Order Book

- `GET /api/v1/market/stocks/{stockCode}/orderbook`
  - `currency`: optional ISO 4217 display currency. Defaults to `USD`.
  - Calls Hana-OmniLens-API `/api/v1/market/stocks/{stockCode}/orderbook`.
  - Returns ask and bid levels with KRW price, requested local currency price, quantity, order count, market data time, and source.

## Tax Refund

- `POST /api/v1/accounts/{accountId}/tax/documents`
  - Multipart upload endpoint for residence certificate and reduced tax application files.
  - Request fields: `documentType` (`RESIDENCE_CERTIFICATE` or `REDUCED_TAX_APPLICATION`) and `file`.
  - Stores the file through the configured tax document storage adapter and persists metadata in `tax_documents`.
  - Configure local storage with `EXCHANGE_TAX_DOCUMENT_STORAGE_ROOT` and `EXCHANGE_TAX_DOCUMENT_MAX_FILE_SIZE_BYTES`.
  - Returns `documentId`, original file name, content type, size, SHA-256, storage key, and created time.
- `POST /api/v1/accounts/{accountId}/tax/refund-cases`
  - Creates or replaces a tax refund case for a mock USD account and tax year.
  - Request fields: `taxYear`, `treatyCountry`, `residenceCertificateFileName`, `reducedTaxApplicationFileName`, optional `residenceCertificateDocumentId`, optional `reducedTaxApplicationDocumentId`, and `advancePaymentRequested`.
  - When document IDs are provided, the service verifies that the uploaded documents belong to the account and match the expected document type.
  - The service matches internal mock `SELL` ledger entries for the requested tax year.
  - It returns total sell amount, realized profit, realized loss, net realized PnL, taxable realized PnL, estimated local withholding tax, estimated treaty tax, estimated refund, matched trades, and advance payment eligibility.
- `GET /api/v1/accounts/{accountId}/tax/refund-status`
  - Returns the latest tax refund case, or `NOT_SUBMITTED` when no case exists.
- `POST /api/v1/accounts/{accountId}/tax/refund-status/sync`
  - Sends the latest tax refund case to Hana-OmniLens-API tax status sync boundary and persists the returned status.
  - When Hana returns `RECAPTURE_RISK`, the service stores one in-app notification for the tax refund case subject and records local push delivery state.
  - Returns `TAX_001` when no local tax case exists and `TAX_002` when Hana sync fails or returns an unsupported status.
- Current statuses:
  - `NOT_SUBMITTED`: no tax refund case has been created.
  - `READY_FOR_HANA_SYNC`: taxable realized profit exists and the case can be sent to Hana tax status sync.
  - `NO_REFUNDABLE_PROFIT`: no positive taxable realized PnL exists for the requested year.
  - `SYNCED_WITH_HANA`, `REFUND_APPROVED`, `ADVANCE_PAID`, `RECAPTURE_RISK`: Hana tax status synchronization and post-payment controls.
- Current tax estimates are local mock calculations for the demo flow. They are not tax advice and do not submit a filing.

## Market Quote WebSocket

- STOMP endpoints:
  - `/ws/market`
- FE subscription topics:
  - `/topic/market/quotes`
  - `/topic/market/markets/{market}`
  - `/topic/market/stocks/{stockCode}`
  - `/topic/accounts/{accountId}/market/quotes/watchlist`
  - `/topic/accounts/{accountId}/market/quotes/portfolio`
- Local/Hana adapter ingest:
  - `POST /api/v1/market/stream/quotes`
  - The request contains `stockCode`, `stockName`, `market`, `currentPriceKrw`, `changeRate`, `volume`, `localCurrency`, `localCurrencyPrice`, `fxRate`, `fxRateTime`, `fxRateSource`, `fxStale`, `marketDataTime`, and `source`.
- The publisher sends the same tick to global, market, stock, matching watchlist account, and matching portfolio account topics.
- Hana-OmniLens-API quote stream client is controlled by `HANA_OMNILENS_QUOTE_STREAM_ENABLED`. It is disabled by default for local tests and connects to `HANA_OMNILENS_QUOTE_STREAM_PATH`, default `/ws/market/quotes`, when enabled.
- Reconnect policy uses exponential backoff from `HANA_OMNILENS_STREAM_RECONNECT_INITIAL_DELAY` to `HANA_OMNILENS_STREAM_RECONNECT_MAX_DELAY`.
- Replay policy sends `QUOTE_STREAM_REPLAY` with `currency` and last published `marketDataTime` when `HANA_OMNILENS_QUOTE_STREAM_REPLAY_ENABLED=true`.
- Backpressure policy buffers validated ticks up to `HANA_OMNILENS_STREAM_BACKPRESSURE_BUFFER_SIZE` and drops excess ticks with an internal `DROPPED` processing result.
- FE must still use REST snapshot endpoints for initial load and recovery.

## News And Disclosure Alert WebSocket

- Hana-OmniLens-API alert stream client is controlled by `HANA_OMNILENS_ALERT_STREAM_ENABLED`. It is disabled by default for local tests and connects to `HANA_OMNILENS_ALERT_STREAM_PATH`, default `/ws/alerts/events`, when enabled.
- Incoming alert event payload uses the same contract as `POST /api/v1/alerts/events`: `eventId`, `idempotencyKey`, `sourceType`, `title`, `summary`, `originalUrl`, `stockCode`, `relatedStocks`, optional `glossaryTerms`, optional `translationQualityFlags`, `sentiment`, `importance`, `riskLevel`, `watchlistTarget`, `holderTarget`, and `publishedAt`.
- `glossaryTerms` contains AI translation normalization metadata with `sourceTerm`, `normalizedTerm`, `englishTerm`, and `category`; `translationQualityFlags` contains quality markers such as glossary matching or fallback translation.
- Stored alert target responses and stock intelligence feed items return the same glossary and translation quality metadata so the FE can explain translated news or disclosure wording with the original link.
- Reconnect policy uses the shared stream backoff settings, and replay policy sends `ALERT_STREAM_REPLAY` with the last ingested `publishedAt` when `HANA_OMNILENS_ALERT_STREAM_REPLAY_ENABLED=true`.
- Backpressure policy buffers validated alert events up to `HANA_OMNILENS_STREAM_BACKPRESSURE_BUFFER_SIZE`, then drains them into `AlertEventService.ingest`.
- REST ingest remains available for local smoke tests and adapter verification.

## Notification Delivery

- `GET /api/v1/accounts/{accountId}/notifications`
  - Returns in-app notification items and push delivery metadata.
  - Each notification includes `eventId`, `subjectType`, `subjectId`, `sourceType`, `deliveryStatus`, `deliveryProvider`, `deliveryAttemptCount`, `deliveredAt`, `lastDeliveryError`, and alert translation quality metadata when the subject is an AI-analyzed alert.
  - Alert notifications use subject `ALERT_EVENT`; tax recapture risk notifications use subject `TAX_REFUND_CASE` and source type `TAX_RECAPTURE_RISK`.
- `GET /api/v1/accounts/{accountId}/notifications/devices`
  - Returns registered iOS/Android/web push device tokens for the account with `activeCount`, `totalCount`, and masked token metadata.
- `POST /api/v1/accounts/{accountId}/notifications/devices`
  - Registers or refreshes a device token with `platform`, `provider`, `deviceToken`, optional `appVersion`, and optional `locale`.
  - Responses expose `tokenHash` and `maskedToken`; the original `deviceToken` is not returned.
- `DELETE /api/v1/accounts/{accountId}/notifications/devices/{deviceTokenId}`
  - Disables a registered device token without deleting its registration audit state.
- Delivery statuses:
  - `PENDING`: notification was created but no provider result has been recorded yet.
  - `DELIVERED`: provider accepted or locally confirmed the delivery.
  - `FAILED`: provider returned a failure result.
  - `SKIPPED`: delivery was intentionally skipped.
- Default provider is `LOCAL_NOOP_PUSH`, which records a successful local delivery without calling FCM/APNS or a web push gateway.
- Configure provider routing with `EXCHANGE_NOTIFICATION_PUSH_ENABLED_PROVIDERS`, for example `LOCAL_NOOP_PUSH`, `FCM_PUSH`, `APNS_PUSH`, or `WEB_PUSH`.
- FCM/APNS/web push providers record `SKIPPED` until external credentials and provider-specific send clients are connected.
- Retry worker is enabled with `EXCHANGE_NOTIFICATION_PUSH_WORKER_ENABLED=true` and retries `PENDING`/`FAILED` notifications under `EXCHANGE_NOTIFICATION_PUSH_MAX_ATTEMPT_COUNT` in `EXCHANGE_NOTIFICATION_PUSH_BATCH_SIZE` batches.
- External mobile/web push credentials and provider delivery integration remain planned hardening work.

## Audit Events

- `GET /api/v1/accounts/{accountId}/audit/events`
  - Returns recent account audit events in newest-first order.
  - Current event types are `TRADE_EXECUTED`, `NOTIFICATION_READ`, and `TAX_REFUND_CASE_UPSERTED`.
  - Each item includes `auditEventId`, `eventType`, `subjectType`, `subjectId`, `summary`, and `occurredAt`.
  - The endpoint is protected by the same bearer account ownership rule as other `/api/v1/accounts/**` APIs.
  - `summary` and `subjectId` are masked before persistence for email, phone number, Korean resident registration number format, and long secret/token-like values.
  - Retention worker is enabled with `EXCHANGE_AUDIT_RETENTION_WORKER_ENABLED=true` and deletes events older than `EXCHANGE_AUDIT_RETENTION_DAYS` on `EXCHANGE_AUDIT_RETENTION_FIXED_DELAY`.
