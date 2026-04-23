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
  auth)            TEST_CLASSES="AuthServiceTest,AuthControllerTest" ;;
  profiles)        TEST_CLASSES="ProfileServiceTest" ;;
  catalog)         TEST_CLASSES="CatalogServiceTest,CatalogFrontendTest,SecurityConfigTest" ;;
  watchlist)       TEST_CLASSES="WatchlistServiceTest" ;;
  reviews)         TEST_CLASSES="ReviewServiceTest" ;;
  social)          TEST_CLASSES="SocialGraphServiceTest" ;;
  recommendations) TEST_CLASSES="RecommendationServiceTest" ;;
  security)        TEST_CLASSES="SecurityConfigTest" ;;
esac

# Run tests and capture output
TMPFILE=$(mktemp)
./mvnw test -Dtest="$TEST_CLASSES" 2>&1 | tee "$TMPFILE"
EXIT_CODE=${PIPESTATUS[0]}
TEST_OUTPUT=$(cat "$TMPFILE")
rm "$TMPFILE"

echo ""
echo "======================================"
echo "Parsing results..."
echo ""

# Write to CSV
CSV_FILE="evaluation/logs/$MODEL/${SERVICE}.csv"
mkdir -p "$(dirname "$CSV_FILE")"

if [ ! -f "$CSV_FILE" ]; then
  echo "timestamp,model,service,prompt,status,tests_run,failures,errors,passed,pass_rate,notes" > "$CSV_FILE"
fi

# Check for compilation error
if echo "$TEST_OUTPUT" | grep -q "COMPILATION ERROR"; then
  echo "Status: COMPILATION FAILURE"
  echo "$TIMESTAMP,$MODEL,$SERVICE,$PROMPT,COMPILATION_FAILURE,0,0,0,0,0%,compilation error" >> "$CSV_FILE"
  echo "Results saved to $CSV_FILE"
  echo "======================================"
  exit 1
fi

# Check for build failure before tests ran
if echo "$TEST_OUTPUT" | grep -q "BUILD FAILURE" && ! echo "$TEST_OUTPUT" | grep -q "Tests run:"; then
  echo "Status: BUILD FAILURE (no tests ran)"
  NOTES=$(echo "$TEST_OUTPUT" | grep "ERROR\]" | tail -3 | tr '\n' ' ' | sed 's/,/;/g')
  echo "$TIMESTAMP,$MODEL,$SERVICE,$PROMPT,BUILD_FAILURE,0,0,0,0,0%,$NOTES" >> "$CSV_FILE"
  echo "Results saved to $CSV_FILE"
  echo "======================================"
  exit 1
fi

# Parse test results
TESTS_RUN=$(echo "$TEST_OUTPUT" | grep "Tests run:" | tail -1 | sed 's/.*Tests run: \([0-9]*\).*/\1/')
FAILURES=$(echo "$TEST_OUTPUT" | grep "Tests run:" | tail -1 | sed 's/.*Failures: \([0-9]*\).*/\1/')
ERRORS=$(echo "$TEST_OUTPUT" | grep "Tests run:" | tail -1 | sed 's/.*Errors: \([0-9]*\).*/\1/')

if [ -z "$TESTS_RUN" ] || [ "$TESTS_RUN" -eq 0 ]; then
  echo "ERROR: Could not parse test results."
  echo "$TIMESTAMP,$MODEL,$SERVICE,$PROMPT,PARSE_ERROR,0,0,0,0,0%,could not parse test output" >> "$CSV_FILE"
  exit 1
fi

PASSED=$((TESTS_RUN - FAILURES - ERRORS))
RATE=$((PASSED * 100 / TESTS_RUN))

# Determine status
if [ "$PASSED" -eq "$TESTS_RUN" ]; then
  STATUS="PASS"
else
  STATUS="PARTIAL"
fi

echo "Status:     $STATUS"
echo "Tests run:  $TESTS_RUN"
echo "Failures:   $FAILURES"
echo "Errors:     $ERRORS"
echo "Passed:     $PASSED"
echo "Pass rate:  $RATE%"
echo ""

echo "$TIMESTAMP,$MODEL,$SERVICE,$PROMPT,$STATUS,$TESTS_RUN,$FAILURES,$ERRORS,$PASSED,$RATE%," >> "$CSV_FILE"

echo "Results saved to $CSV_FILE"
echo "======================================"