# 1. Runtime baselines: Java, Kotlin, and Jackson versions

- Status: Accepted
- Date: 2026-07-07

## Context

`json-migrator` is intended to be published to Maven Central and consumed as a dependency by other
applications and libraries. Unlike an application, a library **imposes its baselines on every
consumer**: the Java bytecode level dictates the minimum JVM a consumer must run, the Jackson major
version must match the consumer's (Jackson types appear in our public API), and the Kotlin toolchain
influences which Kotlin compilers can read the artifact. These choices are effectively part of the
library's compatibility contract, so they should favour reach over novelty.

Current state:

- **Java bytecode: 25** — `maven.compiler.source`/`target` and Kotlin `jvmTarget` are all `25`, so
  consumers must run **JDK 25+**.
- **Kotlin: 2.3.21** — no `apiVersion`/`languageVersion` pin, so the artifact tracks the compiler.
- **Jackson: 3.1.4** — the `tools.jackson` (Jackson 3) namespace; `ObjectNode`/`JsonNode` appear in
  the public API, so the Jackson **major** version is part of that API.

Ecosystem norms for libraries (targets, not build JDKs):

- Jackson 2 → Java 8; **Jackson 3 → Java 17**.
- Spring Framework 6 / Boot 3 → Java 17; kotlin-stdlib and kotlinx.coroutines → Java 8.
- **Quarkus 3.x → Java 17 baseline** (runs on 17/21) — a stated target audience for this library.
  Its platform BOM currently manages **Jackson 2.x**, but **Quarkus 4 (scheduled Oct/Nov 2026)
  targets Jackson 3**, so this audience is moving to Jackson 3 within months.
- Widely-used libraries target the lowest reasonable LTS (8/11/17/21); essentially none require the
  newest JDK. Java 25 is an LTS, but the production installed base is dominated by 17 and 21.

## Decision

1. **Target Java 17 bytecode.** Build on a current JDK (21 or 25) but emit Java 17 bytecode via
   `maven.compiler.release=17` and Kotlin `jvmTarget=17`. 17 is chosen because it is Jackson 3's own
   floor (so nothing is lost by matching it) and it is the widest reach available while on Jackson 3.
   Using `release` (rather than `source`/`target`) also prevents accidental use of post-17 JDK APIs.

2. **Standardize on Jackson 3 (`tools.jackson`).** Depend on a 3.1.x baseline via the `jackson-bom`,
   and treat the Jackson **major version as part of the public API** — documented as such, and only
   changed with a major version bump of this library. Consumers must be on Jackson 3.

   **Dependency scope.** Jackson is a normal `compile`-scope dependency — not `provided` or
   `optional`. It is mandatory (the library cannot function without it) and appears in the public API,
   so a container-style `provided` scope would only give consumers a runtime `NoClassDefFoundError`
   if they forgot to add it. The `3.1.x` version is a **floor**: a consumer who manages Jackson
   themselves (e.g. by importing `jackson-bom` in their own `dependencyManagement`) overrides it via
   Maven's nearest-wins resolution, while a consumer who declares nothing still gets a working
   artifact transitively. `provided` is therefore unnecessary to let consumers pick their own
   Jackson 3 version, and harmful to out-of-the-box usage.

3. **Build with current Kotlin, but cap the consumer baseline.** Compile with Kotlin 2.3.x, and set
   `languageVersion`/`apiVersion` to **2.0** with kotlin-stdlib aligned to that baseline, so the
   artifact stays consumable by a broad range of Kotlin toolchains rather than forcing the newest
   compiler on consumers.

## Consequences

Positive:

- Runs on Java 17, 21, and 25 — the large majority of the market — instead of 25 only.
- The Java floor matches Jackson 3's, so the two are consistent and nothing extra is sacrificed.
- Broad Kotlin consumer range; consumers are not forced to upgrade their Kotlin compiler.
- Forward-looking on Jackson: the library is aligned with the ecosystem's direction, including where
  the primary target audience is heading — **Quarkus 4 (Oct/Nov 2026) targets Jackson 3**.

Negative / trade-offs:

- **Jackson 3 adoption is still early.** The main cost of this decision: consumers who are on
  Jackson 2 cannot use the library until they move to Jackson 3. This is the decision most worth
  revisiting; see Alternatives.
- **Quarkus users (a target audience) — transitional only.** Quarkus 3.x manages Jackson 2, so on
  that line a Quarkus app carries both majors on the classpath (they coexist under distinct
  namespaces). In practice this is fine when the migration is encapsulated in a persistence/repository
  module that reads bytes, migrates, and returns DTOs with its own Jackson 3 mapper: the Jackson 3
  tree never reaches Quarkus's Jackson 2 layer, and only plain DTOs cross module boundaries (and
  `jackson-annotations` is shared across majors, so the DTOs bind under either mapper). Residual costs
  are the extra classpath footprint and, for native image, registering any repository-only DTOs for
  reflection. Friction only arises if a Jackson 3 tree is handed directly to the app's Jackson 2
  mapper. And it is short-lived: **Quarkus 4 (scheduled Oct/Nov 2026) targets Jackson 3**, aligning
  this audience with our choice.
- The library cannot use Java 18–25 language or bytecode features; CI must build/test on 17 (or use
  `release`) to guarantee no newer API leaks in.
- Capping Kotlin `apiVersion` forgoes the newest Kotlin language features inside the library.

Follow-ups (out of scope for this ADR, tracked separately):

- Replace `source`/`target` with `maven.compiler.release`; decouple build JDK from target bytecode.
- Maven Central readiness: sources + Javadoc/Dokka jars, GPG signing, `Automatic-Module-Name`,
  minimal transitive dependencies.
- A CI matrix that runs the tests on Java 17, 21, and 25.

## Alternatives considered

- **Java 21 baseline** instead of 17: a newer LTS with more language features, but it sheds Java 17
  users for no benefit while on Jackson 3 (whose floor is 17). Reconsider only if a 21+ feature is
  genuinely needed in the engine.
- **Keep Java 25**: rejected — excludes the bulk of consumers for a library that uses no 25-specific
  features.
- **Jackson 2 instead of 3**: maximizes adoption today at the cost of building on the outgoing major.
  A reasonable choice if near-term uptake is the priority.
- **Support Jackson 2 and 3 together** (a Jackson-agnostic core with `-jackson2`/`-jackson3` adapter
  modules): maximum reach, but a significant architectural cost that is not justified at this stage.
  Jackson types (`ObjectNode`/`JsonNode`) are woven through the public API (`schema`, the `with`
  clause, `custom`, the value strategies) and the internals (`Document`, the operations), so the core
  would have to re-declare its own node abstraction and each adapter re-implement it — doubling the
  test surface and eroding the DSL's ergonomics (consumers write real `BooleanNode.TRUE` today; an
  abstract core would force the library's own node type or a per-adapter DSL). The abstraction can be
  extracted later if Jackson-2 demand actually appears, so this is not a one-way door.
- **Make Jackson an internal implementation detail** by moving the API boundary to serialized JSON
  (`migrate(json: String): String` / `ByteArray`): the library parses with whatever Jackson it
  bundles and returns text, so Jackson never appears in the public API and the consumer's Jackson
  version becomes irrelevant. This fits the primary use case (read raw JSON from a store/topic →
  migrate → deserialize into the DTO). The trade-off is losing the current "migrate the consumer's
  live `ObjectNode` in place" capability — it forces a parse/serialize cycle, and the `custom` escape
  hatch no longer hands back the consumer's own node type. A larger design shift than the current
  stage warrants, but the cleanest way to decouple from Jackson entirely if that becomes the goal.
