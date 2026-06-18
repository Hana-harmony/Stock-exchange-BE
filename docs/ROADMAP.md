# 구현 로드맵

전체 구현 순서와 단계별 완료 기준은 `docs/IMPLEMENTATION_SEQUENCE.md`를 따른다.

## M1 현지 거래소 백엔드 하네스
- Spring Boot API server scaffold
- 영어권 사용자와 USD 기준 서비스 계약 명시
- DB schema 초안
- Hana-OmniLens-API 단건 quote REST client
- 환경별 secret 관리
- Gradle Wrapper와 CI

## M2 시장 데이터와 모의 주문
- 종목 검색/상세 proxy API
- 단건 종목 실시간 시세 REST snapshot API
- 전체 종목 실시간 시세 REST API: 설정 universe 기반 구현 완료, Hana bulk/all quote client 예정
- 시장별/다건/watchlist/보유종목 실시간 시세 REST API: 시장별/다건/watchlist/보유종목 view 구현 완료, Hana bulk/all quote client 예정
- 전체/시장별/watchlist/보유종목 실시간 시세 WebSocket stream: FE topic publish 구현 완료, Hana stream client/replay/backpressure 예정
- KRW 가격과 USD 환산 가격 동시 제공
- 환율 기준시각/출처/stale flag FE 전달
- Hana-OmniLens-API KRX 기반 과거 시세 API client: BE client/proxy 계약 구현 완료, Hana KRX history API 완성 예정
- FE용 과거 시세 차트 REST API: KRW/현지통화 OHLCV 응답 구현 완료, 실제 Hana history 연동 smoke 예정
- Hana-OmniLens-API quote snapshot short-cache와 stale data 정책
- Hana-OmniLens-API market quote stream 구독, reconnect, replay, backpressure 정책: FE publisher 계약 구현 완료, upstream 구독/운영정책 예정
- 보유종목과 watchlist 모델: watchlist 인메모리 API 구현 완료, 보유종목은 mock 거래 portfolio로 부분 구현
- 아이디/비밀번호 회원가입과 mock USD 계좌 생성: 인메모리 API 구현 완료, 로그인/영속화 예정
- 실제 결제 없는 mock 달러 충전: 인메모리 cash ledger 구현 완료, 영속 ledger 예정
- KIS 모의투자 API가 아닌 자체 mock ledger 매수·매도: 인메모리 API 구현 완료, 영속 ledger 예정
- 평균단가, 평가손익, 매도 실현손익 계산: 평균단가/매도 실현손익 구현 완료, 실시간 평가손익 예정
- 외국인 한도, VI, 상·하한가 기반 주문 가능 여부 계산
- 평가금액과 주문 경고 contract test

## M3 뉴스·공시 알림
- Hana-OmniLens-API WebSocket client
- 이벤트 저장소와 idempotency key: REST ingest 기반 인메모리 구현 완료, WebSocket 수신/영속화 예정
- 보유종목/watchlist 대상자 매칭: REST ingest 기반 매칭 구현 완료, 이벤트 worker 예정
- push provider 연동
- 알림함 API: 인메모리 조회/읽음 처리 구현 완료, push delivery 상태/영속화 예정
- 종목별 인텔리전스 피드 API: REST ingest 기반 인메모리 조회 구현 완료, WebSocket 이벤트 수신/영속화와 FE 연동 예정

## M4 세무 전산화
- 세무 서류 업로드와 object storage 연동
- mock 거래원장/sub-ledger와 매도 실현손익 매칭
- Hana-OmniLens-API 세무 상태 동기화
- 환급/선지급 상태 API
- audit log와 개인정보 마스킹

## M5 운영 하드닝
- rate limit
- retry/backoff
- WebSocket reconnect/replay
- push 중복 방지
- 세무/금융 데이터 접근 로그
