#!/usr/bin/env bash
set -euo pipefail

# Запуск всех микросервисов в tmux.

if ! command -v mvn >/dev/null 2>&1; then
  echo "❌ Maven not found. Install Maven 3.9+ and try again." >&2
  exit 1
fi
if ! command -v java >/dev/null 2>&1; then
  echo "❌ Java not found. Install JDK 17 and try again." >&2
  exit 1
fi

echo "➡️  Building project quickly (skip tests)..."
mvn -U -DskipTests clean package

if command -v tmux >/dev/null 2>&1; then
  SESSION="springfinal"
  echo "➡️  Starting tmux session: $SESSION"
  tmux has-session -t "$SESSION" 2>/dev/null && tmux kill-session -t "$SESSION"

  # first window index may be 0 or 1 depending on base-index, so don't hardcode indices
  tmux new-session -d -s "$SESSION" -n eureka "mvn -f discovery-server/pom.xml spring-boot:run"
  tmux new-window  -t "$SESSION" -n hotel   "mvn -f hotel-service/pom.xml spring-boot:run"
  tmux new-window  -t "$SESSION" -n booking "mvn -f booking-service/pom.xml spring-boot:run"
  tmux new-window  -t "$SESSION" -n gateway "mvn -f api-gateway/pom.xml spring-boot:run"
  tmux new-window  -t "$SESSION" -n test    "bash --noprofile --norc"

  # Determine first window index dynamically and select it
  FIRST_IDX="$(tmux show -gv base-index 2>/dev/null || echo 0)"
  # sanity: make sure window exists; if not, pick the lowest index actually present
  if ! tmux list-windows -t "$SESSION" -F "#{window_index}" | grep -qx "$FIRST_IDX"; then
    FIRST_IDX="$(tmux list-windows -t "$SESSION" -F "#{window_index}" | sort -n | head -n1)"
  fi
  tmux select-window -t "$SESSION:$FIRST_IDX"
  tmux attach -t "$SESSION"
  exit 0
fi
echo "Установите tmux для работы."
