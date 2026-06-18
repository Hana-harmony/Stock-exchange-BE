# 아키텍처

## 목적
- 영어권 현지 거래소 사용자가 모든 한국 상장주식 정보를 조회하고, USD 기준 자체 모의 주문을 실행하고, 관심/보유 종목의 뉴스·공시 알림을 받을 수 있도록 백엔드 기능을 제공한다.
- 최종투자자별 세무 서류 업로드, mock 거래원장/매도 실현손익 매칭, 환급/선지급 상태 표시를 위한 현지 데이터 계층을 제공한다.

## 서비스 구성
- `market/api`: FE용 단건 실시간 시세 REST API와 전체/시장별/watchlist/보유종목 시세 계약
- `market/application`: Hana-OmniLens-API snapshot을 현지 사용자 컨텍스트에 맞게 조합하는 application service
- `market/domain`: quote snapshot, transport, KRW/USD 표시 field 등 market 계약 record
- `market/client`: Hana-OmniLens-API 단건 실시간 시세 REST client
- `config`: Hana-OmniLens-API client 설정, WebSocket broker 설정, profile별 runtime 설정
- Planned `auth`: 아이디/비밀번호 기반 간편 회원가입과 로그인
- Planned `account`: 회원가입 시 mock USD cash account 생성, 달러 충전, 잔고 이력
- Planned `market/client`: Hana-OmniLens-API 종목 검색, 다건/전체 실시간 시세 snapshot, KRX 기반 과거 시세, 호가, orderability API client
- Planned `market/stream`: FE용 전체/시장별/watchlist/보유종목/단건 실시간 시세 WebSocket stream
- Planned `market/cache`: Hana-OmniLens-API snapshot을 현지 거래소 화면 요구사항에 맞게 짧게 캐시하는 layer
- Planned `portfolio`: 사용자 보유종목, 평가금액, watchlist, 자체 mock ledger 주문 상태
- Planned `trade`: KIS 모의투자 API가 아닌 내부 원장 기반 가짜 매수·매도, 평균단가, 실현손익 계산
- Planned `alert`: Hana-OmniLens-API WebSocket client, 이벤트 저장, 대상자 매칭
- Planned `notification`: 푸시 대상자 매칭, 알림함 저장, 푸시 발송
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
- WebSocket stream은 장중 가격, 호가, 등락률, VI/상·하한가 상태 변화처럼 화면에서 즉시 움직여야 하는 데이터에 사용한다.
- KIS 원천 WebSocket은 Hana-OmniLens-API가 구독하고, Stock-exchange-BE는 Hana의 quote snapshot/stream을 받아 FE용 REST와 WebSocket으로 재배포한다.
- Hana quote payload는 KRW 가격과 실시간 또는 최신 환율이 적용된 USD 가격을 모두 포함해야 하며, Stock-exchange-BE는 단건 snapshot에서 이를 FE 표시 형식으로 전달한다.
- 환율 stale flag가 내려오면 Stock-exchange-BE는 FE가 지연 환율 상태를 표시할 수 있도록 그대로 전달한다. 현재 단건 snapshot은 Hana 가격을 기준으로 환율 값을 산출하고 `fxStale=false`로 응답한다.
- 과거 시세는 Hana-OmniLens-API가 KRX 데이터를 수집·정규화·DB 저장한 결과를 REST로 조회한다.
- Stock-exchange-BE는 KRX를 직접 호출하지 않고, Hana의 과거 시세 API를 FE 차트 응답 형식으로 재가공한다.
- 종목 상세 화면에 필요한 외국인 보유율, 당일 예측 지분율 boundary, VI 발동, 상·하한가 상태를 Hana-OmniLens-API에서 조회해 FE에 전달한다.
- 거래 기능은 실제 주문 또는 KIS 모의투자 주문이 아니다. Stock-exchange-BE가 자체 mock ledger에서 USD 잔고, 가짜 매수·매도, 평균단가, 매도 실현손익을 계산한다.
- 회원가입은 아이디/비밀번호만 받고, 가입 즉시 mock USD 계좌를 생성한다. 달러 충전은 실제 결제 없이 입력 금액만큼 mock USD cash ledger를 증가시킨다.
- 매도 내역과 실현손익은 세무 환급/선지급 화면과 Hana-OmniLens-API 세무 상태 계약에 연결되는 거래원장 입력 데이터로 사용한다.
- WebSocket 이벤트를 수신한 뒤 보유종목과 watchlist를 기준으로 푸시 대상자를 매칭한다.
- 세무 기능은 거주자증명서, 제한세율신청서, 거래원장, 조세조약 케이스, 환급금 선지급 상태를 사용자별로 연결한다.

## 현재 구현 상태
- Spring Boot 하네스와 health/market quote 계약용 REST endpoint가 존재한다.
- `GET /api/v1/market/quotes/{stockCode}?currency=USD`는 Hana-OmniLens-API 단건 quote REST snapshot을 호출해 KRW 가격, USD 환산 가격, 기준시각을 공통 응답 형식으로 제공한다.
- 다건/전체 quote REST, DB schema, market WebSocket stream, alert WebSocket client, push worker는 미구현이다.
