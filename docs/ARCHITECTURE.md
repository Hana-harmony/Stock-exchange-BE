# 아키텍처

## 목적
- 영어권 현지 거래소 사용자가 모든 한국 상장주식 정보를 조회하고, USD 기준 자체 모의 주문을 실행하고, 관심/보유 종목의 뉴스·공시 알림을 받을 수 있도록 백엔드 기능을 제공한다.
- 최종투자자별 세무 서류 업로드, mock 거래원장/매도 실현손익 매칭, 환급/선지급 상태 표시를 위한 현지 데이터 계층을 제공한다.

## 서비스 구성
- `market/api`: FE용 단건, 다건, 전체, 시장별, 계좌별 watchlist/보유종목 실시간 시세 REST API, 과거 차트 API, quote stream ingest API
- `market/application`: Hana-OmniLens-API snapshot을 현지 사용자 컨텍스트, 계좌별 watchlist/보유종목, market filter에 맞게 조합하고 short-cache/stale fallback과 WebSocket topic 재배포를 수행하는 application service
- `market/stream`: Hana-OmniLens-API market quote WebSocket client, reconnect, replay, backpressure buffer worker
- `market/domain`: quote snapshot, quote tick, chart point, transport, KRW/USD 표시 field 등 market 계약 record
- `market/client`: Hana-OmniLens-API 단건 실시간 시세 REST client, 다건 조합 adapter, KRX history REST client, 차트 FX metadata용 quote client
- `stock/api`: FE용 종목 검색과 종목 상세 REST API
- `stock/application`: Hana-OmniLens-API 종목 검색/상세 응답을 영어권/USD 화면 계약으로 재가공하는 service
- `stock/domain`: stock search result와 stock detail response 계약 record
- `stock/client`: Hana-OmniLens-API 종목 검색/상세 REST client
- `account/api`: 아이디/비밀번호 회원가입, 로그인/token verify, mock USD 계좌 조회, 실제 결제 없는 달러 충전 REST API
- `account/application`: password hash/verify, local JWT 발급/검증, 사용자 생성, mock USD cash ledger 조합 service
- `account/domain`: user, mock USD account, cash ledger, account response 계약 record
- `auth/api`: Spring Security bearer token filter와 계좌 path 접근 검증
- `auth/application`: Spring Security authentication token adapter
- `auth/domain`: 인증 context에 보관되는 user/account principal 계약 record
- `trade/api`: KIS 모의투자 API를 쓰지 않는 mock 매수·매도, 체결 원장 조회, 주문 가능 여부 경고, portfolio REST API
- `trade/application`: Hana-OmniLens-API quote 가격을 이용한 내부 mock ledger, orderability warning, 평균단가, 실현손익, 현재 평가금액 계산 service
- `trade/domain`: holding, trade ledger, orderability response, portfolio response, valuation history 계약 record
- `watchlist/api`: 계좌별 watchlist 조회, 추가, 삭제 REST API
- `watchlist/application`: Hana-OmniLens-API quote metadata 확인과 watchlist alert target 저장 service
- `watchlist/domain`: watchlist item과 response 계약 record
- `alert/api`: Hana-OmniLens-API 뉴스·공시 분석 이벤트 REST ingest, target 조회, 종목별 인텔리전스 피드 API
- `alert/application`: idempotency key 기반 이벤트 저장, watchlist/holder 대상자 매칭, 종목별 분석 피드 조합 service
- `alert/domain`: alert event, matched target, source link, AI 분석 metadata, 금융용어 glossary, translation quality flag 계약 record
- `alert/stream`: Hana-OmniLens-API 뉴스·공시 분석 이벤트 WebSocket client, replay, reconnect, backpressure buffer, ingest retry/drop worker
- `notification/api`: 계좌별 인앱 알림함 조회와 읽음 처리 REST API
- `notification/application`: matched alert target와 tax recapture risk 기반 notification 저장, push provider 경계, delivery 상태 기록, 중복 방지 service, 실패/미발송 retry worker
- `notification/domain`: notification inbox, subject, original URL, match reason, alert translation quality metadata, delivery state, read state 계약 record
- `tax/api`: 계좌별 tax refund case 생성, 최신 환급 상태 조회, Hana 세무 상태 sync REST API
- `tax/application`: mock SELL 원장 기반 실현손익 매칭, 예상 환급액/선지급 가능 여부 계산, 세무 케이스 저장, Hana status sync, 사후 환수 리스크 notification 연동 service
- `tax/client`: Hana-OmniLens-API 세무 상태 sync REST client
- `tax/domain`: 세무 서류 metadata, matched trade, refund status, estimated tax/refund 계약 record
- `audit/api`: 계좌별 최근 감사 이벤트 조회 REST API
- `audit/application`: 주문 체결, notification 읽음 처리, tax refund case 생성/갱신 이벤트 저장, 개인정보 마스킹, 보존기간 정리 service
- `audit/domain`: audit event type, subject, summary, occurredAt 계약 record
- `config`: Hana-OmniLens-API client 설정, WebSocket broker 설정, Spring Security 설정, API rate limit 설정, profile별 runtime 설정
- `auth`: refresh session rotation, session context anomaly audit
- `account/persistence`: Flyway schema와 JDBC repository 기반 user, mock USD account, cash ledger, refresh session 영속화
- `market/client`: Hana-OmniLens-API 호가 API client
- `trade/persistence`: Flyway schema와 JDBC repository 기반 mock holding, mock trade ledger, portfolio valuation snapshot 영속화
- `notification`: FCM/APNS/web push provider routing, encrypted token vault, FCM HTTP v1 send client, APNS HTTP send client, Web Push gateway send client, provider별 retry worker 연동
- `tax`: object storage 파일 업로드, 세무 문서 metadata, tax refund case 연결
- `tax`: Hana status sync 기반 사후 환수 리스크 notification
- `audit/persistence`: Flyway schema와 JDBC repository 기반 사용자별 알림/주문/세무 상태 변경 이력 영속화와 retention purge

## 패키지 원칙
- `Hana-OmniLens-API`와 동일하게 기능별 package 안에 `api`, `application`, `domain`을 둔다.
- 외부 시스템 또는 runtime 설정은 `config`와 planned `*/client` 경계로 분리한다.
- controller는 request/response routing만 담당하고, 조합 로직은 application service에 둔다.
- domain record는 FE와 API 계약에서 안정적으로 재사용할 수 있는 값 객체로 유지한다.

## 외부 연동
- Hana-OmniLens-API: 한국 주식 시장 데이터, orderability, 뉴스·공시 이벤트, 세무 환급 상태
- Stock-exchange-FE: Flutter 기반 iOS/Android MTS 앱
- Push provider: 앱 푸시와 웹 푸시 발송
- Object storage: 세무 서류 파일 저장
- Local ledger: 사용자별 mock 거래원장, sub-ledger, USD cash ledger, 실현손익 ledger

## 최신 기능정의 반영
- 현지 거래소 FE는 과거 시세뿐 아니라 모든 종목의 실시간 시세를 REST snapshot과 WebSocket stream으로 조회할 수 있어야 한다.
- 현지 거래소 사용자는 영어 UI와 USD 계좌/표시 금액을 기본으로 사용한다.
- REST snapshot은 초기 로딩, 전체 목록, 검색, 새로고침, WebSocket 재연결 복구에 사용한다.
- 종목 검색/상세 API는 Hana-OmniLens-API의 `/api/v1/market/stocks/search`, `/api/v1/market/stocks/{stockCode}/detail` 경계를 호출하고 영어명, USD 가격, 외국인 보유율, 당일 예상 외국인 보유율/한도소진율 min/max boundary, VI, 상/하한가, orderable flag를 FE 계약으로 제공한다.
- 현재 REST quote 목록은 요청 `stockCodes`가 있으면 Hana bulk quote endpoint를, 없으면 Hana all quote endpoint를 호출하고, `market` query로 KOSPI/KOSDAQ/KONEX/OTHER를 필터링한다.
- 계좌별 REST quote view는 watchlist와 mock portfolio holding의 stockCode만 사용하며, 빈 watchlist/보유종목은 기본 universe로 대체하지 않고 빈 snapshot을 반환한다.
- WebSocket stream은 장중 가격, 호가, 등락률, VI/상·하한가 상태 변화처럼 화면에서 즉시 움직여야 하는 데이터에 사용한다.
- FE quote WebSocket topic은 `/topic/market/quotes`, `/topic/market/markets/{market}`, `/topic/market/stocks/{stockCode}`, `/topic/accounts/{accountId}/market/quotes/watchlist`, `/topic/accounts/{accountId}/market/quotes/portfolio`로 고정한다.
- quote stream publisher는 REST ingest 또는 Hana-OmniLens-API WebSocket client로 받은 tick을 전체/시장/종목 topic과 해당 종목을 watchlist 또는 holding으로 가진 계좌 topic에 재배포한다. Hana stream client는 기본 비활성화이며, `HANA_OMNILENS_QUOTE_STREAM_ENABLED=true`에서 기본 `/ws/market/quotes` upstream WebSocket에 연결된다.
- Hana stream client는 validated tick을 bounded buffer에 넣고 `HANA_OMNILENS_QUOTE_STREAM_DRAIN_INTERVAL` 간격으로 publisher에 전달한다. buffer 초과 tick은 drop 처리하고, 연결 종료/오류 시 exponential backoff로 재연결한다.
- replay가 활성화된 경우 reconnect 후 마지막 published `marketDataTime` 이후 tick replay 요청을 Hana-OmniLens-API로 전송한다.
- KIS 원천 WebSocket은 Hana-OmniLens-API가 구독하고, Stock-exchange-BE는 Hana의 quote snapshot/stream을 받아 FE용 REST와 WebSocket으로 재배포한다.
- quote REST snapshot은 동일 요청 stockCode/currency 조합을 짧게 캐시한다. 기본 fresh TTL은 3초, stale fallback TTL은 30초이며 환경변수 `HANA_OMNILENS_QUOTE_CACHE_TTL`, `HANA_OMNILENS_QUOTE_CACHE_STALE_TTL`로 조정한다.
- Hana-OmniLens-API 장애 시 stale fallback 구간 안의 snapshot은 `cache.status=STALE_CACHE`, `fxStale=true`로 내려 FE가 지연 데이터를 명확히 표시할 수 있게 한다.
- Hana quote payload는 KRW 가격과 실시간 또는 최신 환율이 적용된 현지통화 가격을 모두 포함해야 하며, Stock-exchange-BE는 snapshot 응답에서 `fxRate`, `fxRateTime`, `fxRateSource`, `fxStale`을 FE 표시 형식으로 전달한다.
- 환율 stale flag가 내려오면 Stock-exchange-BE는 FE가 지연 환율 상태를 표시할 수 있도록 그대로 전달한다. Hana가 명시적 `fxRate`를 주지 않는 경우에만 KRW/현지통화 가격으로 fallback 산출한다.
- 과거 시세는 Hana-OmniLens-API가 KRX 데이터를 수집·정규화·DB 저장한 결과를 REST로 조회한다.
- Stock-exchange-BE는 KRX를 직접 호출하지 않고, Hana의 `/api/v1/market/stocks/{stockCode}/history` 과거 시세 API와 단건 quote FX metadata를 FE 차트 응답 형식으로 재가공한다. `1d`는 Hana 일봉을 그대로 사용하고, `1w`/`1mo`는 BE가 OHLCV·거래량·거래대금을 집계한다. 모든 국내주식 KRX history 수집/정규화/DB/API 완성은 Hana-OmniLens-API 레포 책임이다.
- 종목 상세 화면에 필요한 외국인 보유율, 당일 예측 지분율 boundary, VI 발동, 상·하한가 상태를 Hana-OmniLens-API에서 조회해 FE에 전달한다.
- 주문 가능 여부 API는 Hana-OmniLens-API orderability boundary를 호출해 외국인 한도, 거래정지, VI, 상/하한가 상태를 mock 주문 전 경고/차단 사유로 제공한다.
- mock 주문 실행 API도 같은 orderability boundary를 다시 확인하며, 차단 사유가 있으면 자체 ledger 기록 전에 `TRADE_003`으로 거절한다.
- 거래 기능은 실제 주문 또는 KIS 모의투자 주문이 아니다. Stock-exchange-BE가 자체 mock ledger에서 USD 잔고, 가짜 매수·매도, 평균단가, 매도 실현손익을 계산한다. 현재 체결 가격은 Hana-OmniLens-API 단건 quote의 USD 환산 가격을 사용한다.
- 회원가입은 아이디/비밀번호만 받고, 가입 즉시 mock USD 계좌를 생성한다. 현재 API는 비밀번호를 PBKDF2로 해시하고 Flyway/JDBC 기반 DB 저장소에 사용자와 계좌를 저장한다.
- 로그인 API는 저장된 PBKDF2 hash를 검증하고 HMAC 기반 local JWT와 refresh token을 발급한다. token verify API는 FE session context가 사용할 userId, username, accountId, expiry를 반환한다.
- refresh token API는 active refresh session을 검증한 뒤 이전 session을 revoke하고 새 access token과 refresh token으로 rotation한다. logout API는 refresh session을 revoke한다. 현재 refresh session 저장소는 DB 구현이며 token 원문 대신 SHA-256 hash를 저장한다.
- `/api/v1/accounts/**`는 Spring Security bearer filter가 token을 검증하고 token accountId와 path accountId 일치를 강제한다. signup/login/token verify와 공개 시장 데이터, alert ingest, Swagger, actuator는 익명 접근을 허용한다.
- API rate limit은 기본 비활성화이며, `EXCHANGE_RATE_LIMIT_ENABLED=true`에서 `/api/v1/**` 요청에 적용된다. 계좌 path는 accountId 기준, 그 외 API path는 client IP 기준으로 고정 window 제한을 수행한다.
- 달러 충전은 실제 결제 없이 입력 금액만큼 mock USD cash ledger를 증가시킨다. 현재 API는 DB account balance와 cash ledger entry를 갱신한다.
- 매도 내역과 실현손익은 세무 환급/선지급 화면과 Hana-OmniLens-API 세무 상태 계약에 연결되는 거래원장 입력 데이터로 사용한다.
- mock holding과 mock trade ledger는 DB에 영속화되며, market quote stream과 tax refund case는 DB holding/trade ledger를 기준으로 계좌별 topic과 매도 실현손익을 계산한다.
- watchlist는 DB에 영속화되며 뉴스·공시 WebSocket 이벤트의 `watchlistTarget` 대상자 매칭 입력 데이터로 사용한다.
- WebSocket 이벤트를 수신한 뒤 보유종목과 watchlist를 기준으로 푸시 대상자를 매칭한다. 현재 구현은 REST ingest와 Hana alert stream client가 동일한 payload를 `AlertEventService`로 전달해 DB에 이벤트와 매칭 결과를 저장한다.
- 종목 상세 화면은 DB에 저장된 뉴스·공시 분석 이벤트를 `stockCode`와 `relatedStocks` 기준으로 조회해 원문 URL, AI 요약, sentiment, importance, risk flag, 금융용어 glossary, translation quality flag를 함께 표시한다.
- 매칭된 alert target과 Hana 세무 sync에서 반환된 사후 환수 리스크는 계좌별 DB 인앱 알림함에 저장하고, alert notification은 AI 번역 품질 메타데이터를 함께 보존하며, FE가 읽음 상태를 갱신할 수 있다.
- notification은 provider 추상화와 delivery 상태, 계좌별 device token 등록 상태를 보관한다. 기본 provider는 외부 발송 없는 `LOCAL_NOOP_PUSH`이며, `EXCHANGE_NOTIFICATION_PUSH_ENABLED_PROVIDERS`로 FCM/APNS/web push routing을 켤 수 있다. 현지 거래소 앱은 iOS/Android/web device token을 등록·조회·비활성화할 수 있고, API 응답은 원문 token 대신 hash와 masked token만 노출한다. FCM provider는 AES-GCM encrypted token vault와 HTTP v1 send client를 사용하고, APNS provider는 bearer token/topic 기반 Apple push endpoint를 호출하며, Web Push provider는 VAPID credential과 subscription endpoint를 configured gateway로 전달한다. provider credential이 없으면 `SKIPPED`로 기록한다. 실패/미발송 notification은 `EXCHANGE_NOTIFICATION_PUSH_WORKER_ENABLED=true`일 때 retry worker가 batch size와 max attempt 설정 기준으로 재전송한다.
- 세무 기능은 거주자증명서/제한세율신청서 metadata, 거래원장, 조세조약 케이스, 환급금 선지급 상태를 사용자별 DB tax refund case로 연결한다. 현재 구현은 mock SELL 원장의 실현손익을 tax refund case에 매칭해 예상 환급액과 선지급 가능 여부를 제공하고, 최신 tax case를 Hana-OmniLens-API 세무 상태 sync boundary로 전송해 반환 status를 DB에 반영한다. Hana sync 결과가 `RECAPTURE_RISK`이면 tax case subject 기반 인앱 notification을 한 번 저장한다.

## 현재 구현 상태
- Spring Boot 하네스와 health/market quote 계약용 REST endpoint가 존재한다.
- `POST /api/v1/auth/signup`은 아이디/비밀번호 가입과 mock USD 계좌 생성을 공통 응답 형식으로 제공한다.
- `GET /api/v1/accounts/{accountId}`와 `POST /api/v1/accounts/{accountId}/deposits`는 mock USD 잔고 조회와 실제 결제 없는 달러 충전을 제공한다.
- `POST /api/v1/accounts/{accountId}/trades`, `GET /api/v1/accounts/{accountId}/trades`, `GET /api/v1/accounts/{accountId}/portfolio`는 orderability 강제 검증, 자체 mock ledger 기반 매수·매도, 체결 원장 조회, 보유수량, 평균단가, 현재가 기반 평가금액, 미실현손익, 매도 실현손익을 제공한다. portfolio 조회 성공 시 평가 snapshot을 저장하며 `GET /api/v1/accounts/{accountId}/portfolio/history`에서 최근 이력을 조회한다.
- `GET /api/v1/accounts/{accountId}/trades/orderability`는 Hana-OmniLens-API orderability 결과를 이용해 mock 주문 전 차단 사유와 경고를 제공한다.
- `GET/POST/DELETE /api/v1/accounts/{accountId}/watchlist`는 계좌별 관심종목과 alert target 입력 데이터를 제공한다.
- `POST /api/v1/alerts/events`와 `GET /api/v1/alerts/events/{eventId}/targets`는 뉴스·공시 분석 이벤트 저장, AI 번역 품질 메타데이터 보존, idempotency 처리, watchlist/holder target 매칭 결과를 제공한다.
- Hana alert WebSocket client는 기본 비활성화 설정, reconnect, replay request, backpressure buffer를 제공하고 수신 payload를 동일한 alert ingest service로 전달한다. ingest 실패 event는 다음 drain에서 최대 3회까지 재시도하고, 이후 dropped로 집계해 poison message가 worker를 막지 않게 한다.
- `POST /api/v1/accounts/{accountId}/tax/refund-cases`, `GET /api/v1/accounts/{accountId}/tax/refund-status`, `POST /api/v1/accounts/{accountId}/tax/refund-status/sync`는 mock 매도 실현손익 기반 세무 케이스, 문서 metadata, 예상 환급액, 선지급 가능 여부, Hana status sync를 제공한다.
- `GET /api/v1/stocks/{stockCode}/intelligence`는 종목코드와 관련종목 기준으로 저장된 뉴스·공시 AI 분석 결과, 원문 링크, glossary, translation quality flag를 최신순으로 제공한다.
- `GET /api/v1/accounts/{accountId}/notifications`와 `POST /api/v1/accounts/{accountId}/notifications/{notificationId}/read`는 알림함 조회와 읽음 처리를 제공한다.
- notification 응답은 push `deliveryStatus`, `deliveryProvider`, `deliveryAttemptCount`, `deliveredAt`, `lastDeliveryError`를 포함한다.
- `GET/POST/DELETE /api/v1/accounts/{accountId}/notifications/devices`는 계좌별 iOS/Android/web push device token 조회, 등록/refresh, 비활성화를 제공한다.
- `GET /api/v1/accounts/{accountId}/audit/events`는 계좌별 최근 주문 체결, notification 읽음 처리, tax refund case 생성/갱신 감사 이벤트를 최신순으로 제공한다.
- 감사 이벤트의 `subjectId`와 `summary`는 저장 전 이메일, 전화번호, 주민등록번호 형식, 긴 secret/token 형식을 마스킹한다. retention worker는 기본 비활성화이며, `EXCHANGE_AUDIT_RETENTION_WORKER_ENABLED=true`에서 `EXCHANGE_AUDIT_RETENTION_DAYS` 이전 이벤트를 정리한다.
- `GET /api/v1/stocks/search`와 `GET /api/v1/stocks/{stockCode}`는 Hana-OmniLens-API 종목 검색/상세 결과를 영어권/USD 화면 계약으로 제공한다.
- `GET /api/v1/market/quotes?stockCodes=...&market=...&currency=USD`는 Hana all/bulk quote endpoint와 시장 필터 기준으로 KRW/USD 시세 목록 snapshot을 제공한다.
- `GET /api/v1/market/quotes/{stockCode}?currency=USD`는 Hana-OmniLens-API 단건 quote REST snapshot을 호출해 KRW 가격, USD 환산 가격, 기준시각을 공통 응답 형식으로 제공한다.
- `GET /api/v1/market/stocks/{stockCode}/orderbook?currency=USD`는 Hana-OmniLens-API 호가 REST snapshot을 호출해 매도/매수 호가별 KRW 가격, 현지통화 가격, 잔량, 주문 건수를 제공한다.
- Hana-OmniLens-API REST client는 transport 실패에 대해 설정 기반 retry/backoff를 적용한다.
- quote REST snapshot 응답은 `cache.status` metadata를 포함하고, upstream 장애 시 stale fallback 가능 구간의 snapshot을 반환한다.
- `GET /api/v1/market/stocks/{stockCode}/chart?from=...&to=...&interval=1d&currency=USD`는 Hana-OmniLens-API KRX history API와 quote FX metadata를 호출해 Flutter chart용 KRW/현지통화 OHLCV를 제공한다.
- `GET /api/v1/accounts/{accountId}/market/quotes/watchlist`와 `/portfolio`는 계좌별 관심종목/보유종목 기준 KRW/USD 시세 목록 snapshot을 제공한다.
- `POST /api/v1/market/stream/quotes`는 local adapter가 quote tick을 FE WebSocket topic으로 publish하는 ingest 계약을 제공한다.
- Hana market WebSocket client는 기본 비활성화 설정, reconnect, replay request, backpressure buffer를 제공한다.
- FCM HTTP v1, APNS HTTP, Web Push gateway provider 실발송 client는 구현되어 있다. 세무 파일은 로컬 object storage adapter로 저장하며, Hana REST retry/backoff, notification provider routing/retry worker, audit retention worker는 기본 설정으로 구현되어 통합 환경에서 활성화할 수 있다.
