# Contributing to json-migrator

Thanks for your interest in contributing! `json-migrator` is a Kotlin DSL for migrating JSON
documents between schema versions on top of Jackson 3. This guide covers how to build the project,
the quality gates your change must pass, and the conventions that keep the codebase readable.

By contributing you agree that your contributions are licensed under the project's [MIT License](LICENSE).

## Prerequisites

- **JDK 25** (the build targets Java 25 and Kotlin 2.3.21).
- **No local Maven install needed** — use the bundled wrapper (`./mvnw`, or `mvnw.cmd` on Windows).
- **Optional:** [`mvnd`](https://github.com/apache/maven-mvnd) (Maven Daemon) for a much faster warm
  build. The `Makefile` uses it automatically when you set `MVN_DAEMON=true`.

## Project layout

| Module | What it is |
| --- | --- |
| `engine` | The core engine and the Kotlin `schema { }` DSL. This is the library. |
| `engine-java` | A thin, Java-friendly `JsonMigrator` facade over the engine. |
| `demo-kotlin` | A worked example of the core use case, using the Kotlin DSL. |
| `demo-java` | The same example, using the Java facade. |
| `engine-test` | The test suite for the engine (kept in its own module). |
| `report` | Aggregation-only module: produces the single Kover coverage and surefire reports. |

Deep design notes for the engine live in [`docs/internals.md`](docs/internals.md); user-facing docs
live in [`docs/`](docs).

## Building and running the checks

The full quality gate is `make build` — a clean build that runs the formatting check, tests, detekt,
and Kover coverage. Run it before pushing; there is currently no PR CI check, so green locally is the
contract:

```bash
make build         # or: ./mvnw clean verify
```

For faster feedback, `make verify` runs the same lifecycle but skips coverage:

```bash
make verify        # or: ./mvnw verify -Dkover.skip=true
```

Run `make help` to list every target. The most useful:

| Command | Does |
| --- | --- |
| `make build` | Clean build with the full gate **and** coverage — what your change must pass. |
| `make verify` | Full lifecycle (formatting, tests, detekt) **without** coverage — faster feedback. |
| `make test` | Compile and run all tests. |
| `make format` | Apply Spotless/ktlint formatting to all Kotlin sources. |
| `make detekt` | Run detekt static analysis. |
| `make demo-kotlin` / `make demo-java` | Run a demo module's driver tests. |

## Quality gates

Every change must keep `make build` green. The gates are:

- **Formatting — Spotless + ktlint.** Enforced at the `validate` phase, so an unformatted file fails
  the build. Run `make format` before committing. Style is pinned in [`.editorconfig`](.editorconfig):
  4-space indent, 120-column lines, `intellij_idea` ktlint style.
- **Static analysis — detekt.** Configured in [`config/detekt.yml`](config/detekt.yml) and
  deliberately strict — notably `LongMethod` (max 12 lines), `CyclomaticComplexMethod` (max 4), and
  `LargeClass` (max 120 lines). detekt only analyses production Kotlin (`src/main/kotlin`). Prefer
  refactoring to satisfy a rule; use `@Suppress` only with a short comment explaining why (see the
  existing suppressions for the expected tone).
- **Tests — JUnit 5.** Tests run in parallel. Add tests with your change; see below.
- **Coverage — Kover.** An aggregate report is written to `target/site/kover/` by the `report`
  module. Keep coverage high — new branches should be exercised by tests.

## Code style and philosophy

Readability is a first-class goal: most time on a codebase is spent reading it, so **code should read
like a novel — a clear story with no ambiguity.** Concretely:

- Name for intent. Functions are verbs, predicates start with `is`/`has`, and a helper that creates
  something says so (`objectOrCreateIn`, not `objectIn`). One concept gets one name across the whole
  call chain.
- Keep functions small and single-purpose (detekt enforces the ceiling; aim well under it).
- The DSL should read as prose (`move("/name") to "/fullName"`, `merge(...) into "/full"`). Preserve
  that when extending it.

## Adding a new operation

Operations follow a consistent contract so they behave predictably. The authoritative checklist is in
[`docs/internals.md`](docs/internals.md); in short:

1. Implement `Operation` (or extend `CompositeOperation` for multi-step operations); parse each path
   in the constructor via `JsonPath.parse`, read/write only through the `Document` facade, throw a
   typed `MigrationException` subtype attributed to the offending path, and override `describe()` with
   the user-facing DSL syntax.
2. Add the DSL entry point in `engine/.../dsl/clause/` (a `MigrationBuilder` extension, plus a
   `PendingClause` subtype if it needs a second part like `with`/`to`/`into`).
3. Add tests in `engine-test`: an operation test (happy path + failure modes), a DSL happy-path test
   in `DslOperationTest`, and — if it introduces a pending clause — a clause-validation test in
   `DslClauseValidationTest`.
4. Document it: add a section (with an `<a id="...">` anchor) to [`docs/operations.md`](docs/operations.md)
   and a row to the README operation table. If it affects the Java surface, update
   [`docs/using-java.md`](docs/using-java.md).

## Tests

- Engine tests live in the `engine-test` module; shared helpers are in `TestFixtures`.
- Use descriptive `` `backtick test names` `` that read as sentences, and prefer
  `@ParameterizedTest` where it removes duplication.
- Tests run concurrently (JUnit 5 parallel execution), so keep them independent and side-effect free.
- The demo modules' driver classes are named `*Demo` (surefire is configured to run them); they are
  illustrative walk-throughs, not exhaustive tests.

## Documentation

Docs and code are reviewed together. When behavior changes, update the relevant file under
[`docs/`](docs) and, for operations, the README table. Keeping the docs honest is part of "done".

## Commits and pull requests

- Branch off `main`.
- Keep each PR focused on one logical change, with code, tests, and docs together.
- Write commit messages in the imperative mood, lowercase, and concise — matching the existing
  history (e.g. `add split operation`, `move coverage aggregation into report module`).
- Make sure `make build` is green before opening the PR.

Questions or proposals that are larger than a small fix are welcome as an issue first, so the approach
can be discussed before you invest in the implementation.
