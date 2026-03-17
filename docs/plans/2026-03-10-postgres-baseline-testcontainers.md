# PostgreSQL Baseline Testcontainers Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Verify that a fresh real PostgreSQL database can boot the backend by applying `B2__full_schema_baseline.sql` through Flyway and then passing Hibernate `validate`.

**Architecture:** Add one dedicated Spring Boot test that uses `PostgreSQLContainer` and `@DynamicPropertySource` to inject a real container-backed datasource. Keep the test narrowly scoped to startup verification only: Flyway migrations must run, Hibernate must validate, and the Spring context must load. Reuse the existing password encryption stub pattern so external secrets do not contaminate the result. Do not convert the resource integration tests to Testcontainers in this round.

**Tech Stack:** Spring Boot 3.2, Flyway, Testcontainers PostgreSQL, JUnit 5, Spring Test.

---

### Task 1: Add a real-PostgreSQL baseline test profile

**Files:**
- Create: `backend/src/test/resources/application-flyway-postgres.yml`
- Read: `backend/src/test/resources/application-flyway-baseline.yml`

**Step 1: Create a Postgres-specific baseline profile**
- Enable Flyway.
- Set Hibernate to `validate`.
- Disable H2-specific datasource settings.
- Keep Redis/JWT test defaults compatible with startup.

**Step 2: Leave datasource URL empty in the profile**
- The container-backed test should provide datasource properties through `@DynamicPropertySource`.
- Do not hardcode localhost ports or assume a running external Postgres.

### Task 2: Add the Testcontainers startup test

**Files:**
- Create: `backend/src/test/java/com/xiyu/bid/support/FlywayPostgresContainerTest.java`
- Read: `backend/src/test/java/com/xiyu/bid/support/FlywayBaselineContextTest.java`

**Step 1: Define the PostgreSQL container**
- Use `@Testcontainers` and a static `@Container PostgreSQLContainer<?>`.
- Pin an explicit Postgres image tag.

**Step 2: Wire Spring to the container**
- Use `@DynamicPropertySource` to inject:
  - `spring.datasource.url`
  - `spring.datasource.username`
  - `spring.datasource.password`
  - `spring.datasource.driver-class-name`
- Activate the `flyway-postgres` profile.

**Step 3: Reuse the password encryption stub**
- Add the same `@TestConfiguration` override used by the H2 baseline test.
- Keep the test isolated from `PLATFORM_ACCOUNT_ENCRYPTION_KEY`.

**Step 4: Keep assertions minimal**
- One `contextLoadsWithFlywayBaselineOnRealPostgres()` test is enough.
- Success means the container starts, Flyway applies the baseline, and Hibernate validate passes.

### Task 3: Verify the new startup test and existing baseline tests

**Files:**
- No new code files beyond the test/profile.

**Step 1: Run the real-Postgres baseline test**
- Run: `cd /Users/user/xiyu/xiyu-bid-poc/backend && mvn -Dtest=FlywayPostgresContainerTest test`
- Expected: PASS, assuming Docker is available.

**Step 2: Re-run the H2 baseline and resource regressions**
- Run: `cd /Users/user/xiyu/xiyu-bid-poc/backend && mvn -Dtest=FlywayBaselineContextTest,ExpenseControllerIntegrationTest,BarCertificateControllerIntegrationTest test`
- Expected: PASS.

**Step 3: Re-run backend compile**
- Run: `cd /Users/user/xiyu/xiyu-bid-poc/backend && mvn -DskipTests compile`
- Expected: PASS.

### Task 4: Record operational boundary

**Files:**
- Update if needed: `docs/plans/2026-03-10-postgres-baseline-testcontainers.md`

**Step 1: Document Docker dependency**
- If Docker/Testcontainers is unavailable in the environment, record that the test is implemented but not runnable here.
- Do not misreport this as a schema failure.

**Step 2: Keep scope tight**
- This test proves baseline startup on real Postgres only.
- It does not replace API integration tests or seed-data verification.
