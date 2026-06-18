#!/usr/bin/env bash
set -euo pipefail

required() {
  local name="$1"
  if [[ -z "${!name:-}" ]]; then
    echo "Missing required environment variable: ${name}" >&2
    exit 1
  fi
}

indent_block() {
  printf '%s\n' "$1" | sed 's/^/  /'
}

required BACKGROUND
required CHANGES
required VERIFICATION
required IMPACT
required ROLLBACK

{
  printf '%s\n' "- 배경:"
  indent_block "$BACKGROUND"
  printf '%s\n' "- 변경 사항:"
  indent_block "$CHANGES"
  printf '%s\n' "- 검증 결과:"
  indent_block "$VERIFICATION"
  printf '%s\n' "- 영향 범위:"
  indent_block "$IMPACT"
  printf '%s\n' "- 롤백 방법:"
  indent_block "$ROLLBACK"
  printf '%s\n' "- 체크리스트:"
  printf '%s\n' "  - [x] CI 통과"
  printf '%s\n' "  - [x] 보안/민감정보 점검"
  printf '%s\n' "  - [x] 문서 업데이트"
}
