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
- `GET /api/v1/market/quotes`
- GitHub Actions CI: `./gradlew test`, `./gradlew bootJar`

## Hana-OmniLens-API 연동
- REST: 종목 검색, 단건/다건/전체 실시간 시세 snapshot, KRX 기반 과거 시세, 호가, orderability, tax refund status 조회
- WebSocket: 뉴스·공시 알림과 market quote stream 구독
- 구독 topic:
  - `/topic/partners/{partnerId}/alerts`
  - `/topic/stocks/{stockCode}/alerts`
- 인증: 서버 간 요청에서만 `X-HANA-OMNILENS-API-KEY`를 사용하고 프론트엔드에는 노출하지 않는다.

## 주요 흐름
1. Hana-OmniLens-API의 KIS 기반 실시간 시세 snapshot을 초기 로딩/복구용으로 조회한다.
2. Hana-OmniLens-API의 market quote WebSocket stream을 구독해 현지 거래소 cache에 반영한다.
3. Stock-exchange-BE는 Hana가 내려준 `currentPriceKrw`, `localCurrencyPrice`, `localCurrency`, `fxRate`, `fxRateTime`, `fxRateSource`를 보존해 FE에 전달한다.
4. FE가 전체 종목, 시장별 종목, watchlist, 보유종목, 단건 상세 시세를 REST로 요청하면 Stock-exchange-BE가 초기 snapshot을 응답한다.
5. FE가 quote WebSocket을 구독하면 Stock-exchange-BE가 사용자 권한과 watchlist/portfolio 컨텍스트에 맞는 KRW/USD 실시간 tick을 송신한다.
6. FE가 과거 차트를 요청하면 Stock-exchange-BE는 Hana-OmniLens-API의 KRX 기반 과거 시세 DB 조회 API를 호출해 차트 응답으로 재가공한다.
7. 사용자가 종목을 검색하거나 상세 화면에 진입하면 Hana-OmniLens-API에서 시세, 외국인 보유율, VI, 상·하한가 상태를 조회한다.
8. 사용자가 가입하면 아이디/비밀번호 계정과 mock USD cash account를 생성한다.
9. 사용자가 달러 충전 금액을 입력하면 실제 결제 없이 mock USD 잔고를 증가시킨다.
10. 사용자가 모의 주문을 입력하면 현재 가격, 외국인 한도, VI/제한가격 상태를 기준으로 주문 가능 여부와 경고를 계산하고 BE 내부 원장에 가짜 매수·매도를 기록한다.
11. 매도 체결로 계산된 실현손익과 거래원장 항목은 세무 환급/선지급 기능의 입력 데이터로 연결한다.
12. Hana-OmniLens-API의 뉴스·공시 WebSocket 이벤트를 수신해 이벤트 저장소에 적재한다.
13. 이벤트의 `holderTarget`, `watchlistTarget`, `stockCode`, `relatedStocks`를 사용자 보유종목/watchlist와 매칭한다.
14. 매칭된 사용자에게 푸시 발송, 알림함 저장, 종목별 인텔리전스 피드 갱신을 수행한다.
15. 세무 서류 업로드와 거래원장 데이터를 Hana-OmniLens-API의 세무 상태 계약과 동기화하고 환급/선지급 상태를 사용자에게 제공한다.

## 문서
- [아키텍처](docs/ARCHITECTURE.md)
- [기능 분류와 레포 책임](docs/FEATURE_CLASSIFICATION.md)
- [API 표준](docs/API_STANDARD.md)
- [로드맵](docs/ROADMAP.md)
