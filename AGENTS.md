# AGENTS.md — familie-ks-sak

Backend for case management of cash-for-care benefit (kontantstøtte). Spring Boot 4 + Kotlin 2, Maven, PostgreSQL, Flyway, Kafka.
Runs on Nais (GCP). Owned by team-baks (teamfamilie namespace).

## Build & Test Commands

Always include `-Dkotlin.compiler.daemon=false` in every `mvn` command.
The Kotlin compiler daemon fails to start in this environment due to RMI registry permission restrictions (`Operation not permitted`), so the compiler must run in-process instead.

```bash
# Build (skip tests)
mvn package -DskipTests -Dkotlin.compiler.daemon=false

# Run all tests (needs Docker for Testcontainers PostgreSQL)
mvn test -Dkotlin.compiler.daemon=false

# Unit tests only (no DB, no Spring context)
mvn test -Penhetstest -Dkotlin.compiler.daemon=false

# Integration tests only (starts Testcontainers PostgreSQL + Spring context)
mvn test -Pintegrasjonstest -Dkotlin.compiler.daemon=false

# Format check / fix (ktlint via Maven Antrun)
mvn antrun:run@ktlint-format -Dkotlin.compiler.daemon=false

# Run single test class
mvn test -Dtest=MyTestClass -Dkotlin.compiler.daemon=false

# Run single test method
mvn test -Dtest=MyTestClass#myMethod -Dkotlin.compiler.daemon=false
```

Note: `mvn verify` does **not** run ktlint automatically — the Antrun plugin is only in `<pluginManagement>`, not bound into the actual build lifecycle. ktlint is enforced by a dedicated CI job on pull requests instead; run it locally with the command above before pushing.

There is no failsafe plugin — integration tests run via surefire using the JUnit tag `integration`, not the `*IT` naming convention.

CI (pull requests) runs ktlint, unit tests, and integration tests in parallel.
Sonar runs after unit and integration tests complete. All jobs must pass.
Push to main builds and deploys dev-gcp → prod-gcp; tests run as part of the reused build workflow (not skipped, unlike some sibling repos).

## Project Structure

```text
src/
  main/kotlin/no/nav/familie/ks/sak/
    api/                       # REST controllers, dto, mapper — separate layer from kjerne/
    kjerne/                    # Domain logic, each area with its own domene/ subpackage
                               #   (behandling, beregning, brev, eøs, fagsak, tilbakekreving, vilkårsvurdering, ...)
    config/                    # Spring configuration
    integrasjon/               # Clients for other services (PDL, oppdrag, oppgave, journalføring, økonomi, ...)
    sikkerhet/                 # Auth/security
    statistikk/                # Statistics/reporting
    task/                      # Async task definitions (prosessering framework)
    internal/                  # Internal endpoints
    common/                    # Shared utilities
  main/resources/
    db/migration/              # Flyway migrations (sequential V<N>__description.sql)
    avro/                      # Avro IDL for Kafka schemas
    application.yaml           # Main config (port 8083)
  test/
    enhetstester/kotlin/       # Unit tests (no Spring context, no DB)
    integrasjonstester/kotlin/ # Integration tests (@Tag("integration"), Testcontainers)
    common/                    # Shared test code (compiled into all test source roots)
    testdata/kotlin/           # Shared data generators (datagenerator/*.kt)
    resources/cucumber/        # Cucumber .feature files (BDD scenarios)
```

## Architecture

- Separate `api/` layer: REST controllers, `dto`, and `mapper` live outside `kjerne/`, unlike some sibling repos where controllers sit alongside domain services.
- Domain entities/repositories consistently use a `domene/` subpackage across `kjerne/` (e.g. `kjerne/fagsak/domene/FagsakRepository.kt`) — more consistent here than in some sibling repos.
- Behandlingssteg (case processing step) pattern lives in `kjerne/behandling/steg/`: `IBehandlingSteg` interface, orchestrated by `StegService`.
- External contracts (`no.nav.familie.kontrakter:*`, `no.nav.familie.eksterne.kontrakter:*`) are consumed as published artifacts, not as a local API layer.
- Spring profiles are enum-driven in `config/SpringProfile.kt`.

## Code Style

### Minimal Editing

When fixing a bug or implementing a feature, change only what is necessary.
Do not rename variables, restructure working code, or refactor beyond the task at hand.
Keep diffs small and focused so they are easy to review.

### Logging & PII

Use `secureLogger` when logging national identity numbers (fødselsnummer) or other PII — never the standard logger. This codebase instantiates it ad hoc per file (`LoggerFactory.getLogger("secureLogger")`, as a top-level `val`, private field, or companion member) rather than importing a single shared value — follow whichever pattern the file you're editing already uses.

### Testing Conventions

- **Unit tests** go in `src/test/enhetstester/`. No `@Tag` needed (selected by excluding the `integration` tag).
- **Integration tests** go in `src/test/integrasjonstester/`. Must extend `OppslagSpringRunnerTest` (which adds `@Tag("integration")` and activates mock/fake profiles). Fakes live in `integrasjonstester/kotlin/.../fake/`.
- **Test data generators** live in `src/test/testdata/kotlin/.../datagenerator/`. Use these instead of creating ad-hoc test objects.
- **Cucumber tests** are in `src/test/resources/cucumber/` with step defs in enhetstester. Run as unit tests (no DB needed). Controlled by `RunCucumberTest.kt`.
- All four test source roots (`enhetstester`, `integrasjonstester`, `common`, `testdata`) are registered via `build-helper-maven-plugin`.
- Test execution order is randomized (`runOrder=random`).
- Integration tests require Docker (Testcontainers PostgreSQL via `DbContainerInitializer`). They will fail without a running Docker daemon.
- Structure test bodies with `// Arrange`, `// Act`, and `// Assert` comments. Use `// Act & Assert` when combined, e.g. with `assertThrows { ... }`.
- Prefer AssertJ (`assertThat`) for new assertions. The codebase historically also uses JUnit Jupiter assertions and, more than usual, Hamcrest — don't copy that pattern in new code.
- Testnamn: backticks with a `skal` prefix, e.g. `` fun `skal beregne riktig beløp`() ``. Use `@Nested` for grouping.

### Key Dependencies & Frameworks

- **JDK 25** (required; set in `pom.xml` and CI workflows)
- **Spring Boot 4** with Jetty (Tomcat excluded), Spring Data JPA, Spring Kafka
- **Kotlin 2** with `spring` and `jpa` compiler plugins (allopen/noarg), language/API version 2.3
- **Auth**: Spring Security for Azure AD token validation (no TokenX in this repo)
- **Async tasks**: Nav's `prosessering` framework for background jobs
- **Feature toggles**: Unleash. Mock via profile `mock-unleash` (`FakeFeatureToggleService`) in tests.
- **Coverage**: JaCoCo (not Kover) — reports go to `target/coverage/{enhetstest,integrasjonstest}/jacoco.xml`
- **Mocking**: MockK (not Mockito)
- **Cucumber** for BDD tests

## Database

- Flyway migrations in `src/main/resources/db/migration/`. Note: `application.yaml` also lists `classpath:db/init` in `spring.flyway.locations`, but that directory does not exist in this repo — harmless, but don't assume it does.
- Naming convention: `V<n>__snake_case_description.sql` with a sequential integer (not a timestamp). Check the highest existing `V<n>` before creating a new one.
- Migrations that have been merged to main are immutable — write a new migration instead of editing an existing one.

## Auth Model

- **Inbound**: Azure AD only (from frontend `familie-ks-sak-frontend`, prosessering, klage, bisys/bidrag-grunnlag, ef-sak, mottak). No TokenX — this service has no citizen-facing self-service flow.
- **Outbound**: Azure AD on-behalf-of / client_credentials (via Texas / `token-klient`) to integrasjoner, klage, tilbakekreving, oppdrag, PDL, and the Tilgangsmaskin (OBO only).
- Role groups configured in Nais manifest (veileder, saksbehandler, beslutter, forvaltning, strengt fortrolig, fortrolig).
- Namespace: `teamfamilie`. Kafka pool: `nav-dev`/`nav-prod`.

## Git Workflow

- Merge to `main` → auto-deploy: build (tests run as part of the reused workflow) → dev-gcp → prod-gcp.
- Emergency deploy: manual workflow `manual-deploy-prod` (runs full test suite by default, then build and deploy) or `manual-deploy-with-image` (deploy existing image).
- Nais manifests: `.nais/app-dev.yaml`, `.nais/app-prod.yaml`.

## Gotchas

- Build requires GitHub Packages authentication (`GITHUB_USERNAME`/`GITHUB_TOKEN`), configured against `maven.pkg.github.com/navikt/familie-felles`, otherwise dependency resolution fails.
- `maven-enforcer-plugin` bans JUnit in `compile` scope — watch for transitive dependencies.

## Boundaries

### ✅ Always

- Run tests and ktlint after changes
- Follow existing code patterns in the project
- Preserve existing code structure — do not reorganize or refactor beyond the task
- Validate all external input
- Use `secureLogger` when logging national identity numbers (fødselsnummer) or other PII — never the standard logger
- Keep this file in sync with the codebase when making changes that affect the information described here

### ⚠️ Ask First

- Changing authentication mechanisms
- Adding new dependencies
- Modifying database schema
- Creating new Flyway migrations (check `src/main/resources/db/migration/` for the next available version number)

### 🚫 Never

- Commit secrets or credentials
- Skip input validation on external boundaries
- Edit generated Avro files in `target/generated-sources/`
- Commit or push changes unless the user explicitly asks you to

## Keeping This File Current

Update this file in the same commit whenever a change affects the information described here:

- **New top-level packages** under `src/main/kotlin/.../sak/` or new test source roots: update "Project Structure".
- **CI workflow changes**: update "Build & Test Commands" if profiles, job order, or JDK version change.
- **Auth/access policy changes**: update "Auth Model" if inbound/outbound rules or auth mechanisms change.
- **New test conventions**: update "Testing Conventions" if new base classes, tags, or test source roots are introduced.
- **New code generation**: update "Key Dependencies & Frameworks" if new generators or plugin phases are added.

Note: Exact dependency versions and Flyway migration numbers are intentionally omitted — they change frequently (e.g. via Dependabot) and are easy for an agent to look up directly from `pom.xml` or the migration directory.

If unsure whether a change warrants an update, ask: "Would a future agent get this wrong without the update?" If yes, update the file.
