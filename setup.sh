#!/bin/bash
# ── GitHub ────────────────────────────────────────────────────────────
mkdir -p .github
touch .github/PULL_REQUEST_TEMPLATE.md

# ── Docs ──────────────────────────────────────────────────────────────
mkdir -p docs/requirements
touch docs/requirements/auth_profiles.txt
touch docs/requirements/catalog_watchlist.txt
touch docs/requirements/reviews.txt
touch docs/requirements/social.txt
touch docs/requirements/recommendations.txt
touch docs/schema.sql

# ── Evaluation ────────────────────────────────────────────────────────
mkdir -p evaluation/runners
mkdir -p evaluation/logs/claude/auth
mkdir -p evaluation/logs/claude/catalog_watchlist
mkdir -p evaluation/logs/claude/reviews
mkdir -p evaluation/logs/claude/social
mkdir -p evaluation/logs/claude/recommendations
mkdir -p evaluation/logs/deepseek/auth
mkdir -p evaluation/logs/deepseek/catalog_watchlist
mkdir -p evaluation/logs/deepseek/reviews
mkdir -p evaluation/logs/deepseek/social
mkdir -p evaluation/logs/deepseek/recommendations
mkdir -p evaluation/outputs/claude
mkdir -p evaluation/outputs/deepseek
mkdir -p evaluation/results
touch evaluation/runners/claude_runner.py
touch evaluation/runners/deepseek_runner.py
touch evaluation/results/stage1_correctness.csv
touch evaluation/results/stage2_sonarqube.csv
touch evaluation/results/stage3_debugging.csv
touch evaluation/results/stage4_efficiency.csv

# ── Main Java Source ───────────────────────────────────────────────────
BASE=src/main/java/com/bingeboxed

# Shared
mkdir -p $BASE/shared/security
mkdir -p $BASE/shared/exception
mkdir -p $BASE/shared/client
touch $BASE/BingeBoxedApplication.java
touch $BASE/shared/security/JwtService.java
touch $BASE/shared/security/JwtFilter.java
touch $BASE/shared/security/SecurityConfig.java
touch $BASE/shared/exception/GlobalExceptionHandler.java
touch $BASE/shared/exception/ResourceNotFoundException.java
touch $BASE/shared/client/SocialGraphClient.java

# Auth — Person 1
mkdir -p $BASE/auth/controller
mkdir -p $BASE/auth/service
mkdir -p $BASE/auth/repository
mkdir -p $BASE/auth/entity
mkdir -p $BASE/auth/dto
touch $BASE/auth/controller/AuthController.java
touch $BASE/auth/service/AuthService.java
touch $BASE/auth/service/AuthServiceImpl.java
touch $BASE/auth/service/JwtServiceImpl.java
touch $BASE/auth/repository/AuthUserRepository.java
touch $BASE/auth/entity/AuthUser.java
touch $BASE/auth/dto/RegisterRequest.java
touch $BASE/auth/dto/LoginRequest.java
touch $BASE/auth/dto/AuthResponse.java

# Profiles — Person 1
mkdir -p $BASE/profiles/controller
mkdir -p $BASE/profiles/service
mkdir -p $BASE/profiles/repository
mkdir -p $BASE/profiles/entity
mkdir -p $BASE/profiles/dto
touch $BASE/profiles/controller/ProfileController.java
touch $BASE/profiles/service/ProfileService.java
touch $BASE/profiles/service/ProfileServiceImpl.java
touch $BASE/profiles/repository/ProfileRepository.java
touch $BASE/profiles/entity/UserProfile.java
touch $BASE/profiles/dto/ProfileResponse.java
touch $BASE/profiles/dto/ProfileUpdateRequest.java

# Catalog — Person 2
mkdir -p $BASE/catalog/controller
mkdir -p $BASE/catalog/service
mkdir -p $BASE/catalog/repository
mkdir -p $BASE/catalog/entity
mkdir -p $BASE/catalog/dto

# Watchlist — Person 2
mkdir -p $BASE/watchlist/controller
mkdir -p $BASE/watchlist/service
mkdir -p $BASE/watchlist/repository
mkdir -p $BASE/watchlist/entity
mkdir -p $BASE/watchlist/dto

# Reviews — Person 3
mkdir -p $BASE/reviews/controller
mkdir -p $BASE/reviews/service
mkdir -p $BASE/reviews/repository
mkdir -p $BASE/reviews/entity
mkdir -p $BASE/reviews/dto

# Social — Person 4
mkdir -p $BASE/social/controller
mkdir -p $BASE/social/service
mkdir -p $BASE/social/repository
mkdir -p $BASE/social/entity
mkdir -p $BASE/social/dto

# Recommendations — Person 5
mkdir -p $BASE/recommendations/controller
mkdir -p $BASE/recommendations/service
mkdir -p $BASE/recommendations/repository
mkdir -p $BASE/recommendations/entity
mkdir -p $BASE/recommendations/dto

# ── Resources ─────────────────────────────────────────────────────────
mkdir -p src/main/resources/templates/layout
mkdir -p src/main/resources/templates/auth
mkdir -p src/main/resources/templates/profiles
mkdir -p src/main/resources/templates/catalog
mkdir -p src/main/resources/templates/watchlist
mkdir -p src/main/resources/templates/reviews
mkdir -p src/main/resources/templates/social
mkdir -p src/main/resources/templates/recommendations
touch src/main/resources/application.properties
touch src/main/resources/application-dev.properties
touch src/main/resources/application-test.properties
touch src/main/resources/data.sql
touch src/main/resources/templates/layout/base.html
touch src/main/resources/templates/auth/login.html
touch src/main/resources/templates/auth/register.html
touch src/main/resources/templates/profiles/view.html
touch src/main/resources/templates/profiles/edit.html
touch src/main/resources/templates/catalog/index.html
touch src/main/resources/templates/catalog/detail.html
touch src/main/resources/templates/watchlist/index.html
touch src/main/resources/templates/reviews/index.html
touch src/main/resources/templates/social/index.html
touch src/main/resources/templates/recommendations/index.html

# ── Test Source ───────────────────────────────────────────────────────
TEST=src/test/java/com/bingeboxed
mkdir -p $TEST/auth
mkdir -p $TEST/profiles
mkdir -p $TEST/catalog
mkdir -p $TEST/watchlist
mkdir -p $TEST/reviews
mkdir -p $TEST/social
mkdir -p $TEST/recommendations
touch $TEST/auth/AuthServiceTest.java
touch $TEST/auth/AuthControllerTest.java
touch $TEST/profiles/ProfileServiceTest.java
touch $TEST/catalog/CatalogServiceTest.java
touch $TEST/watchlist/WatchlistServiceTest.java
touch $TEST/reviews/ReviewServiceTest.java
touch $TEST/social/SocialGraphServiceTest.java
touch $TEST/recommendations/RecommendationServiceTest.java

# ── Test Resources ────────────────────────────────────────────────────
mkdir -p src/test/resources
touch src/test/resources/application-test.properties

# ── SonarQube ─────────────────────────────────────────────────────────
mkdir -p sonarqube
touch sonarqube/sonar-project.properties

# ── Root files ────────────────────────────────────────────────────────
touch pom.xml
touch .gitignore
touch README.md

echo "✅ BingeBoxed structure created successfully"