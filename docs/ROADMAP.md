# 구현 로드맵

전체 구현 순서와 단계별 완료 기준은 `docs/IMPLEMENTATION_SEQUENCE.md`를 따른다.

## M1 현지 거래소 백엔드 하네스
- Spring Boot API server scaffold
- 영어권 사용자와 USD 기준 서비스 계약 명시
- DB schema 초안: Flyway 기반 user/account/cash ledger/refresh session schema 구현 완료
- Hana-OmniLens-API 단건 quote REST client
- 환경별 secret 관리
- Gradle Wrapper와 CI

## M2 시장 데이터와 모의 주문
- 종목 검색/상세 proxy API: Hana stock search/detail client와 FE용 영어/USD 응답 구현 완료
- 단건 종목 실시간 시세 REST snapshot API
- 전체 종목 실시간 시세 REST API: Hana all quote client와 시장 필터 구현 완료
- 시장별/다건/watchlist/보유종목 실시간 시세 REST API: Hana bulk quote client와 시장별/다건/watchlist/보유종목 view 구현 완료
- 전체/시장별/watchlist/보유종목 실시간 시세 WebSocket stream: FE topic publish와 Hana stream client/reconnect/replay/backpressure buffer 구현 완료
- KRW 가격과 USD 환산 가격 동시 제공
- 환율 기준시각/출처/stale flag FE 전달
- Hana-OmniLens-API KRX 기반 과거 시세 API client: BE client/proxy 계약 구현 완료, Hana KRX history API 완성 예정
- FE용 과거 시세 차트 REST API: KRW/현지통화 OHLCV 응답 구현 완료, 실제 Hana history 연동 smoke 예정
- Hana-OmniLens-API quote snapshot short-cache와 stale data 정책: 기본 3초 fresh cache, 30초 stale fallback, 응답 cache metadata 구현 완료
- Hana-OmniLens-API market quote stream 구독, reconnect, replay, backpressure 정책: 기본 비활성화 설정과 upstream WebSocket client 구현 완료, 실제 Hana endpoint 연동 smoke 예정
- 보유종목과 watchlist 모델: DB holding과 DB watchlist 구현 완료
- 아이디/비밀번호 회원가입, 로그인, mock USD 계좌 생성: JDBC/Flyway 기반 user/account 저장, local JWT 발급/검증, refresh token/session rotation, Spring Security bearer filter 구현 완료
- 실제 결제 없는 mock 달러 충전: DB cash ledger 구현 완료
- KIS 모의투자 API가 아닌 자체 mock ledger 매수·매도: DB trade ledger와 holding 구현 완료
- 평균단가, 평가손익, 매도 실현손익 계산: 평균단가, 현재가 기반 평가금액, 미실현손익, 매도 실현손익 구현 완료
- 외국인 한도, VI, 상·하한가 기반 주문 가능 여부 계산: Hana orderability client, mock 주문 전 경고 API, mock 주문 실행 전 차단 검증 구현 완료, 실제 Hana 연동 smoke 예정
- 평가금액과 주문 경고 contract test: portfolio valuation과 주문 경고 API contract test 구현 완료

## M3 뉴스·공시 알림
- Hana-OmniLens-API WebSocket client: 기본 비활성화 설정, reconnect, replay, backpressure buffer 구현 완료
- 이벤트 저장소와 idempotency key: REST ingest와 WebSocket 수신 기반 DB 저장 구현 완료
- 보유종목/watchlist 대상자 매칭: REST ingest와 WebSocket 수신 기반 DB match result 저장과 notification push worker 구현 완료
- push provider 연동: `LOCAL_NOOP_PUSH` provider abstraction, delivery 상태, 실패/미발송 retry worker 구현 완료, FCM/APNS/web push provider 예정
- 알림함 API: DB 조회/읽음 처리와 push delivery 상태 저장 구현 완료, FCM/APNS/web push provider 예정
- 종목별 인텔리전스 피드 API: REST ingest와 WebSocket 이벤트 수신 기반 DB 조회 구현 완료, FE 연동 smoke 예정

## M4 세무 전산화
- 세무 서류 upload endpoint, 로컬 object storage adapter, DB metadata와 tax refund case 연결 구현 완료
- mock 거래원장/sub-ledger와 매도 실현손익 DB 매칭 구현 완료
- Hana-OmniLens-API 세무 상태 동기화 구현 완료
- 환급/선지급 상태 API: mock 세무 케이스 상태, 예상 환급액/선지급 가능 여부, Hana 상태 sync 구현 완료
- audit log와 개인정보 마스킹/보존 정책 구현 완료

## M5 운영 하드닝
- auth context 기반 계좌 API 보호: `/api/v1/accounts/**` bearer token 검증과 accountId path 매칭 구현 완료
- refresh token/session rotation과 refresh session DB 영속화 구현 완료
- refresh session IP/User-Agent anomaly audit 구현 완료
- rate limit 구현 완료
- Hana-OmniLens-API REST client retry/backoff 구현 완료
- Hana-OmniLens-API market quote/news·disclosure WebSocket reconnect/replay 구현 완료
- push provider retry/backoff hardening과 외부 provider delivery retry policy
- 세무/금융 데이터 접근 로그: 주문 체결, notification 읽음, tax refund case 변경 감사 이벤트 DB 저장과 조회 API, 개인정보 마스킹/보존 정책 구현 완료
