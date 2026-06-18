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
| `MARKET_001` | 502 | Hana OmniLens market upstream unavailable |
| `TRADE_001` | 409 | Mock USD account has insufficient balance |

## Swagger

- Swagger UI: `/swagger-ui/index.html`
- OpenAPI JSON: `/v3/api-docs`
