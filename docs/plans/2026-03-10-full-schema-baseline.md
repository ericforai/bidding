# Full Schema Baseline Migration Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Make the backend boot on a fresh empty PostgreSQL database by adding a complete Flyway baseline migration for the existing schema while preserving the current incremental upgrade path for environments that already have live tables.

**Architecture:** Do not rewrite the current `V1__resources_contracts.sql` incremental migration. Instead, add a Flyway baseline migration using the `B` prefix that represents the full current schema state, including the new resource-contract tables. Empty databases will apply the baseline directly; existing databases with no Flyway history will still rely on `baseline-on-migrate`, and databases that have already recorded `V1` will remain compatible. Generate the baseline from the current JPA schema shape, then hand-normalize it for PostgreSQL and verify it against Hibernate `validate`.

**Tech Stack:** Spring Boot 3.2, Flyway 9, PostgreSQL dialect, Hibernate JPA, H2 test profile, MockMvc.

---

### Task 1: Lock the Flyway baseline strategy

**Files:**
- Modify: `backend/src/main/resources/application.yml`
- Read: `backend/src/main/resources/db/migration/V1__resources_contracts.sql`
- Document in: `docs/plans/2026-03-10-full-schema-baseline.md`

**Step 1: Keep the existing versioned migration intact**
- Do not renumber or delete `V1__resources_contracts.sql`.
- Treat it as the live incremental upgrade path for older deployed databases.

**Step 2: Add a baseline migration strategy for fresh databases**
- Use Flyway baseline migration naming, e.g. `B2__full_schema_baseline.sql`.
- Baseline version must be greater than `1` so Flyway does not try to apply `V1` after the baseline on empty databases.

**Step 3: Preserve old-environment compatibility**
- Keep `baseline-on-migrate: true` in runtime config.
- Do not force old non-empty databases to replay the full baseline script.

### Task 2: Build the full baseline schema migration

**Files:**
- Create: `backend/src/main/resources/db/migration/B2__full_schema_baseline.sql`
- Read for coverage: all entity files under `backend/src/main/java/**/entity/*.java`
- Read for implicit tables: `backend/src/main/java/com/xiyu/bid/entity/Project.java`, `backend/src/main/java/com/xiyu/bid/entity/Template.java`

**Step 1: Enumerate the complete schema surface**
- Cover every current Hibernate-managed table, including:
  - domain tables (`users`, `projects`, `tenders`, `tasks`, `qualifications`, `cases`, `templates`, `fees`, `platform_accounts`, etc.)
  - analytics/AI tables
  - resources tables
  - collaboration/documents/versioning tables
  - implicit collection tables (`project_team_members`, `template_tags`)
- Include indexes and unique constraints that are required by entity annotations or existing repository access paths.

**Step 2: Author PostgreSQL-safe DDL**
- Use PostgreSQL column types and constraints, not H2-only syntax.
- Prefer explicit `BIGSERIAL`, `TEXT`, `NUMERIC`, `TIMESTAMP`, `DATE`, `BOOLEAN` and `CHECK` constraints for enums where Hibernate currently generates them.
- Include foreign keys only where the current model truly depends on them; do not invent new relations not present in the entities.

**Step 3: Include the resources contract in the baseline**
- The baseline must already contain:
  - `expenses` with the new approval/return columns
  - `expense_approval_records`
  - `bar_certificates`
  - `bar_certificate_borrow_records`
- This ensures empty databases do not need `V1` after baseline application.

**Step 4: Keep additive migration semantics intact**
- Leave `V1__resources_contracts.sql` in place for already-deployed schemas.
- Accept that `B2` is for fresh schemas only.

### Task 3: Add a fresh-database verification profile

**Files:**
- Create or modify: `backend/src/test/resources/application-flyway-baseline.yml`
- Optionally create: `backend/src/test/java/com/xiyu/bid/support/FlywayBaselineContextTest.java`

**Step 1: Create a dedicated profile for baseline verification**
- Configure H2 with PostgreSQL compatibility mode if needed.
- Enable Flyway for this profile.
- Set Hibernate to `validate` so the test checks schema-vs-entity alignment instead of auto-creating tables.

**Step 2: Add a minimal context-load verification test**
- Use `@SpringBootTest` with the baseline profile.
- Stub or replace any unrelated environment-bound beans required for app startup (for example the platform password encryption bean), so the test measures schema bootstrap rather than external secrets.
- The single success condition is: Flyway applies the baseline and Spring context starts with Hibernate validation passing.

### Task 4: Verify the existing resource integration path still works

**Files:**
- Reuse: `backend/src/test/java/com/xiyu/bid/resources/integration/ExpenseControllerIntegrationTest.java`
- Reuse: `backend/src/test/java/com/xiyu/bid/resources/integration/BarCertificateControllerIntegrationTest.java`

**Step 1: Re-run focused resource tests**
- Confirm the new baseline work does not break the current H2 `create-drop` integration tests.

**Step 2: Re-run backend compile**
- Ensure the new migration file is packaged and application config still compiles.

### Task 5: Final verification and boundary recording

**Files:**
- Modify if needed: `docs/plans/2026-03-10-full-schema-baseline.md`

**Step 1: Verification commands**
- Run: `cd /Users/user/xiyu/xiyu-bid-poc/backend && mvn -DskipTests compile`
- Run: `cd /Users/user/xiyu/xiyu-bid-poc/backend && mvn -Dtest=FlywayBaselineContextTest,ExpenseControllerIntegrationTest,BarCertificateControllerIntegrationTest test`
- Run: `npm run build`
- Run: `VITE_API_MODE=api npm run build`

**Step 2: Record the remaining boundary**
- If the baseline is verified only on H2 PostgreSQL mode rather than a real Postgres container, document that the next hardening step is a PostgreSQL Testcontainers bootstrap test.
- Do not claim production-proof schema bootstrap unless a real PostgreSQL verification has actually been run.
