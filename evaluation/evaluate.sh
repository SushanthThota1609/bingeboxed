#!/bin/bash

MODEL=$1
SERVICE=$2
PROMPT=$3
TIMESTAMP=$(date +"%Y-%m-%d %H:%M")

echo "======================================"
echo "Model: $MODEL | Service: $SERVICE | Prompt: $PROMPT"
echo "======================================"
echo "Running tests..."
echo ""

case $SERVICE in
  auth)           TEST_CLASSES="AuthServiceTest,AuthControllerTest" ;;
  profiles)       TEST_CLASSES="ProfileServiceTest" ;;
  catalog)        TEST_CLASSES="CatalogServiceTest" ;;
  watchlist)      TEST_CLASSES="WatchlistServiceTest" ;;
  reviews)        TEST_CLASSES="ReviewServiceTest" ;;
  social)         TEST_CLASSES="SocialGraphServiceTest" ;;
  recommendations) TEST_CLASSES="RecommendationServiceTest" ;;
esac

# Fix: use temp file instead of /dev/tty
TMPFILE=$(mktemp)
./mvnw test -Dtest="$TEST_CLASSES" 2>&1 | tee "$TMPFILE"
TEST_OUTPUT=$(cat "$TMPFILE")
rm "$TMPFILE"

echo ""
echo "======================================"
echo "Parsing results..."
echo ""

TESTS_RUN=$(echo "$TEST_OUTPUT" | grep "Tests run:" | tail -1 | sed 's/.*Tests run: \([0-9]*\).*/\1/')
FAILURES=$(echo "$TEST_OUTPUT" | grep "Tests run:" | tail -1 | sed 's/.*Failures: \([0-9]*\).*/\1/')
ERRORS=$(echo "$TEST_OUTPUT" | grep "Tests run:" | tail -1 | sed 's/.*Errors: \([0-9]*\).*/\1/')

# Guard against empty/zero
if [ -z "$TESTS_RUN" ] || [ "$TESTS_RUN" -eq 0 ]; then
  echo "ERROR: Could not parse test results."
  exit 1
fi

PASSED=$((TESTS_RUN - FAILURES - ERRORS))
RATE=$((PASSED * 100 / TESTS_RUN))

echo "Tests run:  $TESTS_RUN"
echo "Failures:   $FAILURES"
echo "Errors:     $ERRORS"
echo "Passed:     $PASSED"
echo "Pass rate:  $RATE%"
echo ""

# Write to CSV
CSV_FILE="evaluation/logs/$MODEL/${SERVICE}.csv"
mkdir -p "$(dirname "$CSV_FILE")"

# Write header if file doesn't exist
if [ ! -f "$CSV_FILE" ]; then
  echo "timestamp,model,service,prompt,tests_run,failures,errors,passed,pass_rate" > "$CSV_FILE"
fi

echo "$TIMESTAMP,$MODEL,$SERVICE,$PROMPT,$TESTS_RUN,$FAILURES,$ERRORS,$PASSED,$RATE%" >> "$CSV_FILE"

echo "Results saved to $CSV_FILE"