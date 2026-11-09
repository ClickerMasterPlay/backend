#!/bin/bash
set -e

BASE="http://localhost:8080"
PASS=0
FAIL=0

check() {
  if [ "$1" = "$2" ]; then
    echo "  PASS: $3"
    PASS=$((PASS+1))
  else
    echo "  FAIL: $3 (expected '$2', got '$1')"
    FAIL=$((FAIL+1))
  fi
}

echo "=== 1. Create guest ==="
GUEST=$(curl -s -X POST "$BASE/api/v1/auth/guest")
USER_ID=$(echo "$GUEST" | grep -o '"userId":"[^"]*"' | cut -d'"' -f4)
echo "userId: $USER_ID"
[ -n "$USER_ID" ] && PASS=$((PASS+1)) || { echo "FAIL: no userId"; FAIL=$((FAIL+1)); }

echo "=== 2. Click batch (damage block, not destroyed) ==="
BATCH1=$(uuidgen)
R1=$(curl -s -X POST "$BASE/api/v1/game/click-batch" \
  -H "X-User-Id: $USER_ID" \
  -H "Content-Type: application/json" \
  -d "{\"clientBatchId\": \"$BATCH1\", \"clicks\": 1}")
echo "$R1"
DESTROYED=$(echo "$R1" | grep -o '"destroyed":[^,]*' | cut -d':' -f2)
HP=$(echo "$R1" | grep -o '"currentHp":[^,]*' | cut -d':' -f2)
check "$DESTROYED" "false" "block not destroyed on first click"
check "$HP" "9" "HP reduced to 9 after 1 click"

echo "=== 3. Rate limit (immediate repeat) ==="
BATCH2=$(uuidgen)
R2=$(curl -s -X POST "$BASE/api/v1/game/click-batch" \
  -H "X-User-Id: $USER_ID" \
  -H "Content-Type: application/json" \
  -d "{\"clientBatchId\": \"$BATCH2\", \"clicks\": 1}")
echo "$R2"
ERR_CODE=$(echo "$R2" | grep -o '"error_code":"[^"]*"' | cut -d'"' -f4)
check "$ERR_CODE" "RATE_LIMIT_EXCEEDED" "rate limit triggers on rapid request"

echo "=== 4. Invalid batch size (too many clicks) ==="
BATCH3=$(uuidgen)
R3=$(curl -s -X POST "$BASE/api/v1/game/click-batch" \
  -H "X-User-Id: $USER_ID" \
  -H "Content-Type: application/json" \
  -d "{\"clientBatchId\": \"$BATCH3\", \"clicks\": 101}")
echo "$R3"
ERR_CODE3=$(echo "$R3" | grep -o '"error_code":"[^"]*"' | cut -d'"' -f4)
check "$ERR_CODE3" "INVALID_BATCH_SIZE" "invalid batch size rejected"

echo "=== 5. Invalid user ==="
BATCH4=$(uuidgen)
R4=$(curl -s -X POST "$BASE/api/v1/game/click-batch" \
  -H "X-User-Id: 00000000-0000-0000-0000-000000000000" \
  -H "Content-Type: application/json" \
  -d "{\"clientBatchId\": \"$BATCH4\", \"clicks\": 1}")
echo "$R4"
ERR_CODE4=$(echo "$R4" | grep -o '"error_code":"[^"]*"' | cut -d'"' -f4)
check "$ERR_CODE4" "USER_NOT_FOUND" "unknown user rejected"

echo "=== 6. Idempotency (same clientBatchId twice) ==="
BATCH5=$(uuidgen)
R5A=$(curl -s -X POST "$BASE/api/v1/game/click-batch" \
  -H "X-User-Id: $USER_ID" \
  -H "Content-Type: application/json" \
  -d "{\"clientBatchId\": \"$BATCH5\", \"clicks\": 3}")
R5B=$(curl -s -X POST "$BASE/api/v1/game/click-batch" \
  -H "X-User-Id: $USER_ID" \
  -H "Content-Type: application/json" \
  -d "{\"clientBatchId\": \"$BATCH5\", \"clicks\": 3}")
HP_A=$(echo "$R5A" | grep -o '"currentHp":[^,]*' | cut -d':' -f2)
HP_B=$(echo "$R5B" | grep -o '"currentHp":[^,]*' | cut -d':' -f2)
check "$HP_A" "$HP_B" "idempotent: same response for repeated batchId"

echo "=== 7. Destroy block and get reward + new block ==="
# Damage remaining HP (currently 6 after idempotency test) with 10 clicks
BATCH6=$(uuidgen)
R6=$(curl -s -X POST "$BASE/api/v1/game/click-batch" \
  -H "X-User-Id: $USER_ID" \
  -H "Content-Type: application/json" \
  -d "{\"clientBatchId\": \"$BATCH6\", \"clicks\": 10}")
echo "$R6"
DESTROYED2=$(echo "$R6" | grep -o '"destroyed":[^,]*' | cut -d':' -f2)
REWARDS_CREDITS=$(echo "$R6" | grep -o '"credits":[0-9]*' | head -1 | grep -o '[0-9]*')
BALANCE_CREDITS=$(echo "$R6" | grep -o '"credits":[0-9]*' | tail -1 | grep -o '[0-9]*')
NEW_HP=$(echo "$R6" | grep -o '"currentHp":[^,]*' | cut -d':' -f2)
check "$DESTROYED2" "true" "block destroyed on fatal click"
[ -n "$REWARDS_CREDITS" ] && [ "$REWARDS_CREDITS" -gt 0 ] && PASS=$((PASS+1)) && echo "  PASS: credits reward > 0" || { echo "FAIL: no credits reward"; FAIL=$((FAIL+1)); }
[ -n "$NEW_HP" ] && [ "$NEW_HP" -gt 0 ] && PASS=$((PASS+1)) && echo "  PASS: new block issued (hp=$NEW_HP)" || { echo "FAIL: no new block"; FAIL=$((FAIL+1)); }

echo ""
echo "=============================="
echo "Results: $PASS passed, $FAIL failed"
echo "=============================="
[ "$FAIL" -eq 0 ] && exit 0 || exit 1