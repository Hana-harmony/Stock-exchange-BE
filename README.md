# Stock-exchange-BE

현지 거래소·브로커 백엔드 예시 서비스다. Java 17, Spring Boot 기반으로 구현하며, Hana-OmniLens-API를 통해 모든 한국 상장주식 데이터, 매매제한 판단 데이터, 뉴스·공시 인텔리전스, 세무 환급 상태를 수신하고 영어권/USD 기준 현지 사용자 데이터와 매칭한다.

## 빠른 시작
```bash
./gradlew test --no-daemon
./gradlew bootRun --no-daemon
```

로컬 Docker 실행:
```bash
docker compose -f compose.local.yml up --build
curl http://localhost:3000/actuator/health
curl http://localhost:3000/api/v1/market/quotes
curl "http://localhost:3000/api/v1/market/quotes?stockCodes=005930&stockCodes=000660&market=KOSPI&currency=USD"
curl "http://localhost:3000/api/v1/market/quotes/005930?currency=USD"
curl "http://localhost:3000/api/v1/market/stocks/005930/chart?from=2026-06-01&to=2026-06-18&interval=1d&currency=USD"
curl -X POST http://localhost:3000/api/v1/market/stream/quotes \
  -H 'Content-Type: application/json' \
  -d '{"stockCode":"005930","stockName":"Samsung Electronics","market":"KOSPI","currentPriceKrw":75000,"changeRate":1.25,"volume":1000000,"localCurrency":"USD","localCurrencyPrice":54.00,"fxRate":0.00072,"fxRateTime":"2026-06-18T06:00:00Z","fxStale":false,"marketDataTime":"2026-06-18T06:00:01Z","source":"HANA_OMNILENS_API_STREAM"}'
curl -X POST http://localhost:3000/api/v1/auth/signup \
  -H 'Content-Type: application/json' \
  -d '{"username":"local_trader","password":"localPass123!"}'
```

기본 포트는 `3000`이다. Hana-OmniLens-API를 로컬 Docker 또는 호스트에서 `8080`으로 먼저 띄우면 `HANA_OMNILENS_API_BASE_URL=http://host.docker.internal:8080` 기준으로 연동 테스트할 수 있다.

## 범위
- 아이디/비밀번호 기반 현지 사용자 가입, mock USD 계좌, 보유종목, watchlist 관리
- Hana-OmniLens-API REST 호출과 WebSocket 구독
- 전체/다건/단건 한국 주식 실시간 시세 REST API 제공
- 전체/시장별/watchlist/보유종목 한국 주식 실시간 시세 WebSocket 제공
- KRW 가격과 실시간 환율 적용 USD 가격을 함께 FE에 제공
- Hana-OmniLens-API의 KRX 기반 과거 시세 DB를 조회해 FE용 과거 시세 API 제공
- KIS 모의투자 API를 사용하지 않는 자체 mock ledger 기반 가짜 매수·매도/자산 평가 로직
- 실제 결제 없이 금액 입력만으로 mock USD 잔고를 증가시키는 달러 충전 기능
- 외국인 한도, VI, 상·하한가 상태 기반 주문 가능 여부 안내
- 뉴스·공시 이벤트 수신, 저장, 사용자별 푸시 대상자 매칭
- 앱 푸시/웹 알림/알림함 저장
- 세무 서류 업로드 수신, mock 거래원장/sub-ledger와 매도 실현손익 매칭, 환급 상태 동기화

## 책임 경계
- 실제 한국 시장 데이터 원천 수집은 Hana-OmniLens-API 책임이다.
- 뉴스·공시 AI 분석은 Hannah-Montana-AI 책임이다.
- 한국 주식 실제 주문 실행, 체결, 정산, 환전은 현재 프로젝트 범위 밖이다.
- 이 레포는 현지 거래소 관점의 사용자 데이터, 자체 mock 주문 원장, 알림, 세무 신청 상태를 관리한다.
- KIS 모의투자 계좌/API를 주문 기능에 사용하지 않는다. KIS는 Hana-OmniLens-API의 실제 시세 원천으로만 사용한다.

## 현재 하네스
- Java 17
- Spring Boot 3.5.x
- Gradle Wrapper
- Hana-OmniLens-API와 동일한 `api / application / domain / config` 패키지 구조
- `GET /actuator/health`
- `POST /api/v1/auth/signup`
- `GET /api/v1/accounts/{accountId}`
- `POST /api/v1/accounts/{accountId}/deposits`
- `POST /api/v1/accounts/{accountId}/trades`
- `GET /api/v1/accounts/{accountId}/portfolio`
- `GET /api/v1/accounts/{accountId}/watchlist`
- `POST /api/v1/accounts/{accountId}/watchlist`
- `DELETE /api/v1/accounts/{accountId}/watchlist/{stockCode}`
- `POST /api/v1/alerts/events`
- `GET /api/v1/alerts/events/{eventId}/targets`
- `GET /api/v1/stocks/{stockCode}/intelligence`
- `GET /api/v1/accounts/{accountId}/notifications`
- `POST /api/v1/accounts/{accountId}/notifications/{notificationId}/read`
- `GET /api/v1/market/quotes?stockCodes=005930&market=KOSPI&currency=USD`
- `GET /api/v1/market/quotes/{stockCode}?currency=USD`
- `GET /api/v1/market/stocks/{stockCode}/chart?from=2026-06-01&to=2026-06-18&interval=1d&currency=USD`
- `POST /api/v1/market/stream/quotes`
- `GET /api/v1/accounts/{accountId}/market/quotes/watchlist?currency=USD`
- `GET /api/v1/accounts/{accountId}/market/quotes/portfolio?currency=USD`
- STOMP `/ws/market`
- GitHub Actions CI: `./gradlew test`, `./gradlew bootJar`
- 현재 mock 사용자와 mock USD 계좌 저장소는 로컬 개발용 인메모리 구현이며, 영속 DB schema와 마이그레이션은 별도 단계에서 추가한다.

## Hana-OmniLens-API 연동
- REST: 단건 실시간 시세 snapshot 구현, 설정된 국내주식 universe/다건/시장별 실시간 시세 snapshot 구현, quote short-cache/stale fallback 구현, KRX 기반 과거 차트 client/proxy 구현, 종목 검색, 호가, orderability, tax refund status 조회 예정
- WebSocket: 뉴스·공시 알림과 market quote stream 구독/재배포
- 구독 topic:
  - `/topic/partners/{partnerId}/alerts`
  - `/topic/stocks/{stockCode}/alerts`
  - `/topic/market/quotes`
  - `/topic/market/markets/{market}`
  - `/topic/market/stocks/{stockCode}`
  - `/topic/accounts/{accountId}/market/quotes/watchlist`
  - `/topic/accounts/{accountId}/market/quotes/portfolio`
- 인증: 서버 간 요청에서만 `X-HANA-OMNILENS-API-KEY`를 사용하고 프론트엔드에는 노출하지 않는다.

## 주요 흐름
1. Hana-OmniLens-API의 KIS 기반 단건 실시간 시세 snapshot을 조회해 FE에 공통 응답 형식으로 전달한다.
2. Hana-OmniLens-API의 market quote WebSocket stream을 구독해 현지 거래소 cache에 반영한다.
3. Stock-exchange-BE는 Hana가 내려준 `currentPriceKrw`, `localCurrencyPrice`, `localCurrency`, `fxRate`, `fxRateTime`, `fxRateSource`를 보존해 FE에 전달한다.
4. FE가 설정된 전체 universe, 시장별 종목, 다건 종목, watchlist, 보유종목, 단건 상세 시세를 REST로 요청하면 Stock-exchange-BE가 초기 snapshot을 응답한다. 현재 전체 universe는 `HANA_OMNILENS_DEFAULT_STOCK_CODES` 설정값을 사용하고, watchlist/보유종목 view는 계좌별 저장 데이터를 기준으로 stockCode를 조합한다. Hana-OmniLens-API bulk/all quote가 추가되면 client 경계에서 교체한다.
5. quote REST snapshot은 `HANA_OMNILENS_QUOTE_CACHE_TTL` 동안 short-cache를 사용하고, upstream 장애 시 `HANA_OMNILENS_QUOTE_CACHE_STALE_TTL` 안의 snapshot을 `cache.status=STALE_CACHE`, `fxStale=true`로 내려준다.
6. FE가 quote WebSocket을 구독하면 Stock-exchange-BE가 전체, 시장별, 종목별, watchlist, portfolio 컨텍스트에 맞는 KRW/USD 실시간 tick을 송신한다. 현재 구현은 `POST /api/v1/market/stream/quotes`로 동일 publisher를 검증하며, Hana-OmniLens-API stream client가 붙으면 같은 publisher를 호출한다.
7. FE가 과거 차트를 요청하면 Stock-exchange-BE는 Hana-OmniLens-API의 KRX 기반 과거 시세 DB 조회 API를 호출해 차트 응답으로 재가공한다.
8. 사용자가 종목을 검색하거나 상세 화면에 진입하면 Hana-OmniLens-API에서 시세, 외국인 보유율, VI, 상·하한가 상태를 조회한다.
9. 사용자가 가입하면 아이디/비밀번호 계정과 mock USD cash account를 생성한다. 현재 구현은 PBKDF2 password hash와 인메모리 계좌 저장소를 사용한다.
10. 사용자가 달러 충전 금액을 입력하면 실제 결제 없이 mock USD 잔고를 증가시키고 mock cash ledger entry를 남긴다.
11. 사용자가 모의 주문을 입력하면 Hana-OmniLens-API의 USD 환산 quote 가격을 기준으로 Stock-exchange-BE 내부 원장에 가짜 매수·매도를 기록한다. 실제 한국 주식 주문이나 KIS 모의투자 주문은 실행하지 않는다.
12. 매도 체결로 계산된 실현손익과 거래원장 항목은 포트폴리오 API에 반영되며, 이후 세무 환급/선지급 기능의 입력 데이터로 연결한다.
13. 사용자가 watchlist에 종목을 추가하면 Hana-OmniLens-API의 quote metadata를 확인해 종목명/시장과 함께 알림 대상 입력 데이터로 저장한다.
14. Hana-OmniLens-API의 뉴스·공시 분석 이벤트를 저장하고 idempotency key로 중복 처리를 수행한다. 현재 구현은 WebSocket client 전 단계의 REST ingest 계약이다.
15. 이벤트의 `holderTarget`, `watchlistTarget`, `stockCode`, `relatedStocks`를 사용자 보유종목/watchlist와 매칭한다.
16. 종목 상세 화면은 `stockCode`와 `relatedStocks` 기준으로 저장된 뉴스·공시 AI 분석 결과, sentiment, importance, risk flag, 원문 URL을 인텔리전스 피드로 조회한다.
17. 매칭된 사용자에게 인앱 알림함 notification을 저장하고 읽음 상태를 관리한다. 실제 push provider 발송은 다음 단계에서 수행한다.
18. 세무 서류 업로드와 거래원장 데이터를 Hana-OmniLens-API의 세무 상태 계약과 동기화하고 환급/선지급 상태를 사용자에게 제공한다.

## 문서
- [아키텍처](docs/ARCHITECTURE.md)
- [기능 분류와 레포 책임](docs/FEATURE_CLASSIFICATION.md)
- [API 표준](docs/API_STANDARD.md)
- [로드맵](docs/ROADMAP.md)
