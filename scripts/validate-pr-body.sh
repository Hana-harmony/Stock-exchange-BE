#!/usr/bin/env bash
set -euo pipefail

body="$(cat)"

if [[ "$body" == *'\\n'* ]]; then
  echo "PR body contains literal \\\\n. Use real Markdown newlines instead." >&2
  exit 1
fi

for section in "배경" "변경 사항" "검증 결과" "영향 범위" "롤백 방법" "체크리스트"; do
  if [[ "$body" != *"- ${section}:"* ]]; then
    echo "PR body is missing section: ${section}" >&2
    exit 1
  fi
done
