# AGENTS.md

## 공통 지침
- 시크릿, API key, 인증 토큰, 외부 API credential은 코드와 문서 예시에 원문으로 남기지 않는다.
- Hana-OmniLens-API key는 서버 간 통신에만 사용하고 프론트엔드로 전달하지 않는다.
- 사용자 식별자, 세무 서류, 거래원장 데이터는 개인정보와 민감 금융정보로 보고 최소 수집, 마스킹, 접근 로그를 기본으로 한다.
- 변경 후 가능한 범위에서 테스트와 정적 검증을 실행하고 결과를 기록한다.

## 서비스 경계
- 이 레포는 현지 거래소 백엔드다.
- Java 17, Spring Boot 3.5.x, Gradle Wrapper 기준을 유지한다.
- 사용자, 계좌, 보유종목, watchlist, 알림함, 푸시 대상자 매칭, 모의 주문, 세무 신청 상태를 관리한다.
- 한국 주식 원천 데이터 수집, 뉴스·공시 수집, 번역 공급자 연동, 협력사용 B2B API 송신은 Hana-OmniLens-API 책임이다.
- 뉴스·공시 NLP 분석은 Hannah-Montana-AI 책임이다.
- Flutter 기반 iOS/Android MTS 화면 렌더링은 Stock-exchange-FE 책임이다.

## 구현 원칙
- `Hana-OmniLens-API`와 같은 기능별 `api`, `application`, `domain` 패키지 구조를 따른다.
- controller는 routing과 validation만 담당하고, 조합 로직은 application service에 둔다.
- 외부 연동 client와 runtime 설정은 domain/controller와 섞지 않는다.
- Hana-OmniLens-API와의 REST/WebSocket 계약 변경 시 이 레포의 contract test와 문서를 함께 갱신한다.
- 푸시 발송은 idempotency key를 사용해 중복 발송을 방지한다.
- 세무/환급금 선지급 기능은 법무, 컴플라이언스, 보안 검토 전 운영 기능으로 확정하지 않는다.

## 필수 확인
- `./gradlew test --no-daemon`
- `./gradlew bootJar --no-daemon`
