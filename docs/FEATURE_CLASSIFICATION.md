# 기능 분류와 레포 책임

## 1. 한국 주식 주문 지원

| 기능 | 책임 | 상태 |
| --- | --- | --- |
| Spring Boot API 하네스 | Stock-exchange-BE | Done |
| 영어권 현지 사용자와 USD 기준 서비스 목표 | Stock-exchange-BE | Done |
| 아이디/비밀번호 기반 간편 회원가입/로그인 | Stock-exchange-BE | Done |
| 계좌 API bearer token 인증과 accountId 접근 제어 | Stock-exchange-BE | Done |
| refresh token/session rotation | Stock-exchange-BE | Done |
| user/account/refresh session DB schema와 migration | Stock-exchange-BE | Done |
| 회원가입 시 mock USD 계좌 자동 생성 | Stock-exchange-BE | Done |
| 실제 결제 없는 mock 달러 충전 | Stock-exchange-BE | Done |
| Hana-OmniLens-API 단건 시세 조회 | Stock-exchange-BE | Done |
| Hana-OmniLens-API 종목/orderability 조회 | Stock-exchange-BE | Done |
| 단건 실시간 시세 REST API 제공 | Stock-exchange-BE | Done |
| 전체/시장별/다건/watchlist/보유종목 실시간 시세 REST API 제공 | Stock-exchange-BE | Done |
| 전체/시장별/watchlist/보유종목 실시간 시세 WebSocket 제공 | Stock-exchange-BE | Done |
| KRW 가격과 USD 환산 가격을 FE에 함께 전달 | Stock-exchange-BE | Done |
| 환율 기준시각/출처/stale flag 전달 | Stock-exchange-BE | Done |
| Hana-OmniLens-API의 KRX 기반 과거 시세 API client/proxy | Stock-exchange-BE | Done |
| FE용 과거 시세 차트 REST API 제공 | Stock-exchange-BE | Done |
| 모든 국내주식 KRX 과거 시세 수집·정규화·DB 저장·history API 완성 | Hana-OmniLens-API | Planned |
| Hana-OmniLens-API 실시간 quote snapshot short-cache | Stock-exchange-BE | Done |
| Hana-OmniLens-API market quote stream 구독과 FE 재배포 | Stock-exchange-BE | Done |
| Hana-OmniLens-API 단일가 매매 상태 수신과 주문 경고 반영 | Stock-exchange-BE | Done |
| 사용자 보유종목과 자체 mock 주문 상태 DB 영속화 | Stock-exchange-BE | Done |
| watchlist 저장소 영속화 | Stock-exchange-BE | Done |
| watchlist 관심종목 API와 알림 대상 입력 데이터 | Stock-exchange-BE | Done |
| KIS 모의투자 API가 아닌 BE 내부 원장 기반 가짜 매수·매도 | Stock-exchange-BE | Done |
| 평균단가, 평가손익, 매도 실현손익 계산 | Stock-exchange-BE | Done |
| 외국인 한도, VI, 상·하한가 기반 주문 가능 여부 계산과 주문 실행 차단 | Stock-exchange-BE | Done |
| 실시간 평가금액과 주문 경고 API 제공 | Stock-exchange-BE | Done |
| 실제 주문 실행, 체결, 정산, 환전 | Out of scope | Out of scope |

## 2. 뉴스·공시 인텔리전스

| 기능 | 책임 | 상태 |
| --- | --- | --- |
| Hana-OmniLens-API WebSocket 구독 | Stock-exchange-BE | Done |
| 이벤트 저장과 중복 제거 | Stock-exchange-BE | Done |
| AI 번역 금융용어 glossary와 translation quality flag 수신·저장·응답 | Stock-exchange-BE | Done |
| `holderTarget`, `watchlistTarget` 기반 사용자 매칭 | Stock-exchange-BE | Done |
| 알림함 저장, local push delivery 상태, 실패/미발송 retry worker | Stock-exchange-BE | Done |
| FCM/APNS/web push provider routing과 미설정 provider SKIPPED 상태 기록 | Stock-exchange-BE | Done |
| iOS/Android/web notification device token 등록·조회·비활성화 | Stock-exchange-BE | Done |
| 실제 FCM/APNS/web push 자격증명과 외부 provider 실발송 연동 | Stock-exchange-BE | Planned |
| 종목별 인텔리전스 피드 API | Stock-exchange-BE | Done |

## 3. 세무 전산화 및 환급금 선지급

| 기능 | 책임 | 상태 |
| --- | --- | --- |
| 거주자증명서, 제한세율신청서 업로드 접수 | Stock-exchange-BE | Done |
| 파일 metadata와 거래원장/sub-ledger 매칭 | Stock-exchange-BE | Done |
| 매도 실현손익 기반 세무 환급/선지급 입력 데이터 연결 | Stock-exchange-BE | Done |
| Hana-OmniLens-API 세무 상태 조회/동기화 | Stock-exchange-BE | Done |
| 환급/선지급 상태와 Hana sync 기반 사후 환수 리스크 알림 | Stock-exchange-BE | Done |
| 국세청 경정청구 실제 제출 | Hana/업무 운영 시스템 | Out of scope until compliance approval |

## 4. 운영 감사와 보안

| 기능 | 책임 | 상태 |
| --- | --- | --- |
| 주문 체결 감사 이벤트 저장과 조회 | Stock-exchange-BE | Done |
| notification 읽음 감사 이벤트 저장과 조회 | Stock-exchange-BE | Done |
| tax refund case 생성/갱신 감사 이벤트 저장과 조회 | Stock-exchange-BE | Done |
| 감사 로그 개인정보 마스킹과 보존 정책 | Stock-exchange-BE | Done |
| API rate limit | Stock-exchange-BE | Done |
