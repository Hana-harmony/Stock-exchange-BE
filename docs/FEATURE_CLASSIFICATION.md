# 기능 분류와 레포 책임

## 1. 한국 주식 주문 지원

| 기능 | 책임 | 상태 |
| --- | --- | --- |
| Spring Boot API 하네스 | Stock-exchange-BE | Partial |
| 영어권 현지 사용자와 USD 기준 서비스 목표 | Stock-exchange-BE | Planned |
| 아이디/비밀번호 기반 간편 회원가입 | Stock-exchange-BE | Partial |
| 회원가입 시 mock USD 계좌 자동 생성 | Stock-exchange-BE | Done |
| 실제 결제 없는 mock 달러 충전 | Stock-exchange-BE | Done |
| Hana-OmniLens-API 단건 시세 조회 | Stock-exchange-BE | Done |
| Hana-OmniLens-API 종목/orderability 조회 | Stock-exchange-BE | Planned |
| 단건 실시간 시세 REST API 제공 | Stock-exchange-BE | Done |
| 전체/시장별/다건/watchlist/보유종목 실시간 시세 REST API 제공 | Stock-exchange-BE | Partial |
| 전체/시장별/watchlist/보유종목 실시간 시세 WebSocket 제공 | Stock-exchange-BE | Planned |
| KRW 가격과 USD 환산 가격을 FE에 함께 전달 | Stock-exchange-BE | Partial |
| 환율 기준시각/출처/stale flag 전달 | Stock-exchange-BE | Partial |
| Hana-OmniLens-API의 KRX 기반 과거 시세 API 조회 | Stock-exchange-BE | Planned |
| FE용 과거 시세 차트 REST API 제공 | Stock-exchange-BE | Planned |
| Hana-OmniLens-API 실시간 quote snapshot short-cache | Stock-exchange-BE | Planned |
| Hana-OmniLens-API market quote stream 구독과 FE 재배포 | Stock-exchange-BE | Planned |
| 사용자 보유종목, watchlist, 자체 mock 주문 상태 관리 | Stock-exchange-BE | Partial |
| watchlist 관심종목 API와 알림 대상 입력 데이터 | Stock-exchange-BE | Done |
| KIS 모의투자 API가 아닌 BE 내부 원장 기반 가짜 매수·매도 | Stock-exchange-BE | Done |
| 평균단가, 평가손익, 매도 실현손익 계산 | Stock-exchange-BE | Partial |
| 외국인 한도, VI, 상·하한가 기반 주문 가능 여부 계산 | Stock-exchange-BE | Planned |
| 실시간 평가금액과 주문 경고 API 제공 | Stock-exchange-BE | Planned |
| 실제 주문 실행, 체결, 정산, 환전 | Out of scope | Out of scope |

## 2. 뉴스·공시 인텔리전스

| 기능 | 책임 | 상태 |
| --- | --- | --- |
| Hana-OmniLens-API WebSocket 구독 | Stock-exchange-BE | Planned |
| 이벤트 저장과 중복 제거 | Stock-exchange-BE | Partial |
| `holderTarget`, `watchlistTarget` 기반 사용자 매칭 | Stock-exchange-BE | Partial |
| 앱 푸시/웹 알림/알림함 저장 | Stock-exchange-BE | Partial |
| 종목별 인텔리전스 피드 API | Stock-exchange-BE | Done |

## 3. 세무 전산화 및 환급금 선지급

| 기능 | 책임 | 상태 |
| --- | --- | --- |
| 거주자증명서, 제한세율신청서 업로드 접수 | Stock-exchange-BE | Planned |
| 파일 metadata와 거래원장/sub-ledger 매칭 | Stock-exchange-BE | Planned |
| 매도 실현손익 기반 세무 환급/선지급 입력 데이터 연결 | Stock-exchange-BE | Planned |
| Hana-OmniLens-API 세무 상태 조회/동기화 | Stock-exchange-BE | Planned |
| 환급/선지급 상태와 사후 환수 리스크 알림 | Stock-exchange-BE | Planned |
| 국세청 경정청구 실제 제출 | Hana/업무 운영 시스템 | Out of scope until compliance approval |
