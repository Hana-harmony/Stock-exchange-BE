# 아키텍처

## 목적
- 영어권 현지 거래소 사용자가 모든 한국 상장주식 정보를 조회하고, USD 기준 자체 모의 주문을 실행하고, 관심/보유 종목의 뉴스·공시 알림을 받을 수 있도록 백엔드 기능을 제공한다.
- 최종투자자별 세무 서류 업로드, mock 거래원장/매도 실현손익 매칭, 환급/선지급 상태 표시를 위한 현지 데이터 계층을 제공한다.

## 서비스 구성
- `market/api`: FE용 단건, 다건, 설정 universe, 시장별, 계좌별 watchlist/보유종목 실시간 시세 REST API와 quote stream ingest API
- `market/application`: Hana-OmniLens-API snapshot을 현지 사용자 컨텍스트, 계좌별 watchlist/보유종목, market filter에 맞게 조합하고 WebSocket topic으로 재배포하는 application service
- `market/domain`: quote snapshot, quote tick, transport, KRW/USD 표시 field 등 market 계약 record
- `market/client`: Hana-OmniLens-API 단건 실시간 시세 REST client와 다건 조합 adapter
- `account/api`: 아이디/비밀번호 회원가입, mock USD 계좌 조회, 실제 결제 없는 달러 충전 REST API
- `account/application`: password hash, 사용자 생성, mock USD cash ledger 조합 service
- `account/domain`: user, mock USD account, cash ledger, account response 계약 record
- `trade/api`: KIS 모의투자 API를 쓰지 않는 mock 매수·매도와 portfolio REST API
- `trade/application`: Hana-OmniLens-API quote 가격을 이용한 내부 mock ledger, 평균단가, 실현손익 계산 service
- `trade/domain`: holding, trade ledger, portfolio response 계약 record
- `watchlist/api`: 계좌별 watchlist 조회, 추가, 삭제 REST API
- `watchlist/application`: Hana-OmniLens-API quote metadata 확인과 watchlist alert target 저장 service
- `watchlist/domain`: watchlist item과 response 계약 record
- `alert/api`: Hana-OmniLens-API 뉴스·공시 분석 이벤트 REST ingest, target 조회, 종목별 인텔리전스 피드 API
- `alert/application`: idempotency key 기반 이벤트 저장, watchlist/holder 대상자 매칭, 종목별 분석 피드 조합 service
- `alert/domain`: alert event, matched target, source link, AI 분석 metadata 계약 record
- `notification/api`: 계좌별 인앱 알림함 조회와 읽음 처리 REST API
- `notification/application`: matched alert target 기반 notification 저장과 중복 방지 service
- `notification/domain`: notification inbox, original URL, match reason, read state 계약 record
- `config`: Hana-OmniLens-API client 설정, WebSocket broker 설정, profile별 runtime 설정
- Planned `auth`: 로그인, 세션/JWT, 인증 context
- Planned `account`: 영속 DB 기반 USD cash account와 잔고 이력
- Planned `market/client`: Hana-OmniLens-API 종목 검색, bulk/all 실시간 시세 snapshot, KRX 기반 과거 시세, 호가, orderability API client
- Planned `market/stream`: Hana-OmniLens-API market quote WebSocket client, reconnect, replay, backpressure worker
- Planned `market/cache`: Hana-OmniLens-API snapshot을 현지 거래소 화면 요구사항에 맞게 짧게 캐시하는 layer
- Planned `portfolio`: 사용자 보유종목, 평가금액, 자체 mock ledger 주문 상태
- Planned `trade`: 영속 DB 기반 거래원장, 주문 가능 여부 경고, 외국인 한도/VI/상·하한가 검증
- Planned `alert`: Hana-OmniLens-API WebSocket client, 영속 이벤트 저장소, replay/retry worker
- Planned `notification`: push provider 발송, 웹 푸시, delivery retry worker
- Planned `tax`: 세무 서류 업로드 metadata, 거래원장/sub-ledger 매칭, 환급 상태 동기화
- Planned `audit`: 사용자별 알림/주문/세무 상태 변경 이력

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
- 현재 REST quote 목록은 `HANA_OMNILENS_DEFAULT_STOCK_CODES` 설정 universe 또는 요청 `stockCodes`를 기준으로 Hana 단건 quote를 조합하고, `market` query로 KOSPI/KOSDAQ/KONEX/OTHER를 필터링한다.
- 계좌별 REST quote view는 watchlist와 mock portfolio holding의 stockCode만 사용하며, 빈 watchlist/보유종목은 기본 universe로 대체하지 않고 빈 snapshot을 반환한다.
- WebSocket stream은 장중 가격, 호가, 등락률, VI/상·하한가 상태 변화처럼 화면에서 즉시 움직여야 하는 데이터에 사용한다.
- FE quote WebSocket topic은 `/topic/market/quotes`, `/topic/market/markets/{market}`, `/topic/market/stocks/{stockCode}`, `/topic/accounts/{accountId}/market/quotes/watchlist`, `/topic/accounts/{accountId}/market/quotes/portfolio`로 고정한다.
- 현재 quote stream publisher는 REST ingest로 받은 tick을 전체/시장/종목 topic과 해당 종목을 watchlist 또는 holding으로 가진 계좌 topic에 재배포한다. Hana-OmniLens-API WebSocket client는 다음 단계에서 같은 publisher를 호출한다.
- KIS 원천 WebSocket은 Hana-OmniLens-API가 구독하고, Stock-exchange-BE는 Hana의 quote snapshot/stream을 받아 FE용 REST와 WebSocket으로 재배포한다.
- Hana quote payload는 KRW 가격과 실시간 또는 최신 환율이 적용된 USD 가격을 모두 포함해야 하며, Stock-exchange-BE는 단건 snapshot에서 이를 FE 표시 형식으로 전달한다.
- 환율 stale flag가 내려오면 Stock-exchange-BE는 FE가 지연 환율 상태를 표시할 수 있도록 그대로 전달한다. 현재 단건 snapshot은 Hana 가격을 기준으로 환율 값을 산출하고 `fxStale=false`로 응답한다.
- 과거 시세는 Hana-OmniLens-API가 KRX 데이터를 수집·정규화·DB 저장한 결과를 REST로 조회한다.
- Stock-exchange-BE는 KRX를 직접 호출하지 않고, Hana의 과거 시세 API를 FE 차트 응답 형식으로 재가공한다.
- 종목 상세 화면에 필요한 외국인 보유율, 당일 예측 지분율 boundary, VI 발동, 상·하한가 상태를 Hana-OmniLens-API에서 조회해 FE에 전달한다.
- 거래 기능은 실제 주문 또는 KIS 모의투자 주문이 아니다. Stock-exchange-BE가 자체 mock ledger에서 USD 잔고, 가짜 매수·매도, 평균단가, 매도 실현손익을 계산한다. 현재 체결 가격은 Hana-OmniLens-API 단건 quote의 USD 환산 가격을 사용한다.
- 회원가입은 아이디/비밀번호만 받고, 가입 즉시 mock USD 계좌를 생성한다. 현재 API는 비밀번호를 PBKDF2로 해시하고 로컬 개발용 인메모리 저장소에 계좌를 생성한다.
- 달러 충전은 실제 결제 없이 입력 금액만큼 mock USD cash ledger를 증가시킨다. 현재 API는 재시작 시 사라지는 인메모리 ledger entry를 사용한다.
- 매도 내역과 실현손익은 세무 환급/선지급 화면과 Hana-OmniLens-API 세무 상태 계약에 연결되는 거래원장 입력 데이터로 사용한다.
- watchlist는 뉴스·공시 WebSocket 이벤트의 `watchlistTarget` 대상자 매칭 입력 데이터로 사용한다. 현재 API는 로컬 개발용 인메모리 저장소를 사용한다.
- WebSocket 이벤트를 수신한 뒤 보유종목과 watchlist를 기준으로 푸시 대상자를 매칭한다. 현재 구현은 REST ingest를 통해 동일한 payload를 저장·매칭한다.
- 종목 상세 화면은 저장된 뉴스·공시 분석 이벤트를 `stockCode`와 `relatedStocks` 기준으로 조회해 원문 URL, AI 요약, sentiment, importance, risk flag를 함께 표시한다. 현재 구현은 REST ingest로 저장된 인메모리 이벤트를 조회한다.
- 매칭된 alert target은 계좌별 인앱 알림함에 저장하고, FE가 읽음 상태를 갱신할 수 있다. 현재 구현은 로컬 개발용 인메모리 저장소를 사용한다.
- 세무 기능은 거주자증명서, 제한세율신청서, 거래원장, 조세조약 케이스, 환급금 선지급 상태를 사용자별로 연결한다.

## 현재 구현 상태
- Spring Boot 하네스와 health/market quote 계약용 REST endpoint가 존재한다.
- `POST /api/v1/auth/signup`은 아이디/비밀번호 가입과 mock USD 계좌 생성을 공통 응답 형식으로 제공한다.
- `GET /api/v1/accounts/{accountId}`와 `POST /api/v1/accounts/{accountId}/deposits`는 mock USD 잔고 조회와 실제 결제 없는 달러 충전을 제공한다.
- `POST /api/v1/accounts/{accountId}/trades`와 `GET /api/v1/accounts/{accountId}/portfolio`는 자체 mock ledger 기반 매수·매도, 보유수량, 평균단가, 매도 실현손익을 제공한다.
- `GET/POST/DELETE /api/v1/accounts/{accountId}/watchlist`는 계좌별 관심종목과 alert target 입력 데이터를 제공한다.
- `POST /api/v1/alerts/events`와 `GET /api/v1/alerts/events/{eventId}/targets`는 뉴스·공시 분석 이벤트 저장, idempotency 처리, watchlist/holder target 매칭 결과를 제공한다.
- `GET /api/v1/stocks/{stockCode}/intelligence`는 종목코드와 관련종목 기준으로 저장된 뉴스·공시 AI 분석 결과와 원문 링크를 최신순으로 제공한다.
- `GET /api/v1/accounts/{accountId}/notifications`와 `POST /api/v1/accounts/{accountId}/notifications/{notificationId}/read`는 알림함 조회와 읽음 처리를 제공한다.
- `GET /api/v1/market/quotes?stockCodes=...&market=...&currency=USD`는 설정 universe, 요청 종목코드, 시장 필터 기준으로 KRW/USD 시세 목록 snapshot을 제공한다.
- `GET /api/v1/market/quotes/{stockCode}?currency=USD`는 Hana-OmniLens-API 단건 quote REST snapshot을 호출해 KRW 가격, USD 환산 가격, 기준시각을 공통 응답 형식으로 제공한다.
- `GET /api/v1/accounts/{accountId}/market/quotes/watchlist`와 `/portfolio`는 계좌별 관심종목/보유종목 기준 KRW/USD 시세 목록 snapshot을 제공한다.
- `POST /api/v1/market/stream/quotes`는 local/Hana adapter가 quote tick을 FE WebSocket topic으로 publish하는 ingest 계약을 제공한다.
- 로그인/JWT, 영속 DB schema, orderability 경고, Hana bulk/all quote client, Hana market WebSocket client, replay/backpressure, alert WebSocket client, push worker, 웹 푸시는 미구현이다.
