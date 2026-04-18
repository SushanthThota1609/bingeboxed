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

# Map service name to test class names
case $SERVICE in
  auth)
    TEST_CLASSES="AuthServiceTest,AuthControllerTest"
    ;;
  profiles)
    TEST_CLASSES="ProfileServiceTest"
    ;;
  catalog)
    TEST_CLASSES="CatalogServiceTest"
    ;;
  watchlist)
    TEST_CLASSES="WatchlistServiceTest"
    ;;
  reviews)
    TEST_CLASSES="ReviewServiceTest"
    ;;
  social)
    TEST_CLASSES="SocialGraphServiceTest"
    ;;
  recommendations)
    TEST_CLASSES="RecommendationServiceTest"
    ;;
esac

# Run tests and show output in real time AND capture it
TEST_OUTPUT=$(./mvnw test -Dtest="$TEST_CLASSES" 2>&1 | tee /dev/tty)

echo ""
echo "======================================"
echo "Parsing results..."
echo ""

# Extract results - Windows Git Bash compatible
TESTS_RUN=$(echo "$TEST_OUTPUT" | grep "Tests run:" | tail -1 | sed 's/.*Tests run: \([0-9]*\).*/\1/')
FAILURES=$(echo "$TEST_OUTPUT" | grep "Tests run:" | tail -1 | sed 's/.*Failures: \([0-9]*\).*/\1/')
ERRORS=$(echo "$TEST_OUTPUT" | grep "Tests run:" | tail -1 | sed 's/.*Errors: \([0-9]*\).*/\1/')

echo "Tests run:  $TESTS_RUN"
echo "Failures:   $FAILURES"
echo "Errors:     $ERRORS"

PASSED=$((TESTS_RUN - FAILURES - ERRORS))
RATE=$((PASSED * 100 / TESTS_RUN))

echo "Passed:     $PASSED"
echo "Pass rate:  $RATE%"
echo ""

# Save to CSV
CSV_FILE="evaluation/logs/$MODEL/${SERVICE}.csv"

# Write header if file is empty or doesn't exist
if [ ! -s "$CSV_FILE" ]; then
    echo "timestamp,model,service,prompt,passed,total,rate" > $CSV_FILE
    echo "Created log file with headers: $CSV_FILE"
fi

echo "$TIMESTAMP,$MODEL,$SERVICE,$PROMPT,$PASSED,$TESTS_RUN,$RATE%" >> $CSV_FILE

echo "Logged to $CSV_FILE"
echo "======================================"