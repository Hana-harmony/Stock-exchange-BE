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
| `COMMON_999` | 500 | Internal server error |
| `AUTH_001` | 409 | Username already exists |
| `ACCOUNT_001` | 404 | Mock USD account not found |
| `WATCHLIST_001` | 404 | Watchlist item not found |
| `ALERT_001` | 404 | Alert event not found |
| `NOTIFICATION_001` | 404 | Notification not found |
| `MARKET_001` | 502 | Hana OmniLens market upstream unavailable |
| `TRADE_001` | 409 | Mock USD account has insufficient balance |
| `TRADE_002` | 409 | Mock holding has insufficient quantity |

## Swagger

- Swagger UI: `/swagger-ui/index.html`
- OpenAPI JSON: `/v3/api-docs`

## Market Quote Snapshot

- `GET /api/v1/market/quotes`
  - `stockCodes`: optional repeated 6-digit stock code. Omit it to use `HANA_OMNILENS_DEFAULT_STOCK_CODES`.
  - `market`: optional `KOSPI`, `KOSDAQ`, `KONEX`, `OTHER` filter.
  - `currency`: optional ISO 4217 display currency. Defaults to `USD`.
- `GET /api/v1/market/quotes/{stockCode}`
  - Single stock snapshot for stock detail and mock trade price lookup.
- `GET /api/v1/accounts/{accountId}/market/quotes/watchlist`
  - Account watchlist quote snapshot. Empty watchlists return `quoteCount: 0` and do not fall back to the default universe.
- `GET /api/v1/accounts/{accountId}/market/quotes/portfolio`
  - Account portfolio holding quote snapshot. Empty holdings return `quoteCount: 0`.
- Quote payload includes KRW price, requested local currency price, derived FX rate, market, change rate, volume, stale flag, and REST/WebSocket transport metadata.
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
  - Warnings include VI activity and buy/sell at upper/lower price limit.
- `GET /api/v1/accounts/{accountId}/portfolio`
  - Returns mock USD cash, holdings, recent trades, realized PnL, total market value, total asset value, and unrealized PnL.
  - Holding rows include average price, cost basis, current Hana USD quote price, market value, unrealized PnL, unrealized PnL rate, and market data time.

## Market Chart

- `GET /api/v1/market/stocks/{stockCode}/chart`
  - `from`: required ISO date.
  - `to`: required ISO date.
  - `interval`: optional `1d`, `1w`, `1mo`. Defaults to `1d`.
  - `currency`: optional ISO 4217 display currency. Defaults to `USD`.
- Stock-exchange-BE calls Hana-OmniLens-API `/api/v1/market/stocks/{stockCode}/history` and reformats KRX history for the Flutter chart.
- Chart points include KRW OHLCV, trading value, adjusted flag, and requested local currency OHLC.

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
  - The request contains `stockCode`, `stockName`, `market`, `currentPriceKrw`, `changeRate`, `volume`, `localCurrency`, `localCurrencyPrice`, `fxRate`, `fxRateTime`, `fxStale`, `marketDataTime`, and `source`.
- The publisher sends the same tick to global, market, stock, matching watchlist account, and matching portfolio account topics.
- WebSocket reconnect, replay, and backpressure policies are planned hardening work; FE must use REST snapshot endpoints for initial load and recovery.
