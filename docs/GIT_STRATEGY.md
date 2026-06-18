# Git Strategy

## 브랜치 전략
- 기본: `feature` 통합 브랜치 기반 개발 흐름
- 운영 브랜치: `main`
- 개발 통합 브랜치: `feature`
- 작업 브랜치 형식: `<유형>/<기능명>`
- 유형은 아래 값만 사용한다.
  - `Feat`: 기능 추가
  - `Fix`: 버그 수정
  - `Hotfix`: 긴급 장애 수정
  - `Refactor`: 리팩터링
  - `Chore`: 빌드/설정/의존성 작업
  - `Docs`: 문서 작업
  - `Test`: 테스트 작업
  - `Security`: 보안 작업
  - `Release`: 배포 작업
- 브랜치명은 영문 소문자 케밥 케이스를 사용한다.
- 예시:
  - `Feat/exchange-backend-harness`
  - `Fix/quote-stream-reconnect`
  - `Security/api-key-hardening`

## 커밋 메시지 규칙 (한글 필수)
Conventional Commits 구조를 따르되 제목과 본문은 모두 한글로 작성한다.

형식:
`type(scope): 제목`

규칙:
- `type`, `scope`는 영문 키워드를 사용한다.
- `제목`은 한글로 작성하고 50자 내외로 유지한다.
- 본문은 왜 변경했는지와 무엇을 어떻게 바꿨는지 작성한다.
- 본문은 항목형으로 작성한다.
- 제목과 본문은 존댓말을 쓰지 않고 `~함`, `~다`, 명사형으로 간결하게 끝낸다.
- 이슈 번호가 있으면 푸터에 `관련 이슈: #번호`로 작성한다.

타입 목록:
- `feat`, `fix`, `refactor`, `test`, `docs`, `chore`, `security`, `perf`, `build`, `ci`, `revert`

커밋 본문 템플릿:
- 배경: 변경이 필요한 이유
- 변경: 핵심 변경 사항
- 검증: 수행한 테스트와 결과
- 관련 이슈: 이슈 번호(있으면 작성)

## PR 메시지 규칙 (한글 필수)
- PR 제목과 본문은 모두 한글로 작성한다.
- PR 본문은 배경, 변경 사항, 검증 결과, 영향 범위, 롤백 방법, 체크리스트를 포함한다.
- 직접 `main`과 `feature` 푸시를 금지하고 PR 병합만 허용한다.
- PR 본문에는 문자 그대로의 `\n`을 넣지 않는다. Markdown 실제 줄바꿈을 사용한다.
- 브라우저에서 PR을 만들 때는 `.github/pull_request_template.md`를 기준으로 작성한다.
- GitHub API로 PR을 만들 때는 `scripts/render-pr-body.sh`로 본문을 생성하고 `scripts/validate-pr-body.sh`로 검증한 결과만 전송한다.
- GitHub Actions `pr-body` 체크가 같은 검증을 수행하므로, 본문이 깨진 PR은 merge하지 않는다.

API PR 생성 예:
```bash
BODY="$(
  BACKGROUND="변경 배경" \
  CHANGES="핵심 변경 사항" \
  VERIFICATION="./gradlew test --no-daemon 통과" \
  IMPACT="영향 범위" \
  ROLLBACK="merge commit revert" \
  scripts/render-pr-body.sh
)"
printf '%s' "$BODY" | scripts/validate-pr-body.sh
jq -n --arg body "$BODY" '{body:$body}'
```
