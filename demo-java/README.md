# json-migrator-demo-java

A worked example of the migration library's core use case, built with the Java `JsonMigrator` facade:
credit-application documents live in a database (or arrive on a Kafka topic) written by many past
releases, and the application migrates each one **just-in-time**, on access, up to the single schema
version it understands. It mirrors [json-migrator-demo-kotlin](../demo-kotlin) so you can compare the
two surfaces side by side.

## The scenario

- The application holds **one DTO** — [`CreditApplication`](src/main/java/com/mosedotten/json/migrator/demo/java/CreditApplication.java),
  always the latest shape (currently v6). There are no per-version DTOs; a real application only ever
  models the current version.
- The database contains documents at **every historical version** (v1..v6), because they were written
  by the releases that were current at the time.
- On read, a document is fed through the migration script, which upgrades it from whatever version it
  declares to the latest, and is then deserialized into the record for processing. For the database
  case it is serialized straight back out — so accessing an old row rewrites it at the current version.
- Migration is just-in-time per document; there is no bulk upgrade of the whole database.

## The migration script

[`CreditApplicationMigrations`](src/main/java/com/mosedotten/json/migrator/demo/java/CreditApplicationMigrations.java)
is a single `JsonMigrator.migrate(...).migration(...)….run()` chain. Over the application's lifetime it
grew one step per release (the first release shipped only `1 → 2`, the next added `2 → 3`, and so on);
what you see is the script as it stands now that the application is at v6. The engine reads a
document's current version, skips the steps it has already applied, and runs the rest to the latest.

| Version | Change |
| ------- | ------ |
| 1 → 2 | Split `applicant.fullName` into `firstName`/`middleName`/`lastName`; move `email` and `mobilePhone` under `applicant.contact`. |
| 2 → 3 | For each address, rename `postalCode` → `zipCode` and nest `countryCode` under `country.iso2`. |
| 3 → 4 | Move `employment.monthlyIncome` → `financials.income.monthly.amount`, add a `currency`, and rename `employerName` → `employer`. |
| 4 → 5 | Move `consents` into `preferences.marketing` and `legal.termsAcceptedAt`, then drop the now-empty `consents` object. |
| 5 → 6 | For each requested product, rename `productCode` → `type` and normalize the optional `limit` into `creditLimit.amount`/`currency`. |

The 5 → 6 step also shows the `custom(...)` escape hatch: only products that actually carry a `limit`
(a credit card, but not a savings account) get a `creditLimit`.

## Driver programs

The `src/test` classes are **showcase drivers**, not exhaustive tests — they run the scenario end to
end and assert just enough to prove the flow worked. The `credit-application-v{n}.json` resources
stand in for documents as stored in the database at each version.

- [`JustInTimeMigrationDemo`](src/test/java/com/mosedotten/json/migrator/demo/java/JustInTimeMigrationDemo.java) —
  reads a document stored at each version (v1..v6), migrates it to the latest, and deserializes it into
  the one application record.
- [`DatabaseRoundTripDemo`](src/test/java/com/mosedotten/json/migrator/demo/java/DatabaseRoundTripDemo.java) —
  reads an old (v1) row, migrates, processes, and serializes it back, showing it is stored again at the
  latest version with nothing lost.

## Run it

```bash
./mvnw -pl demo-java -am test
```

For details on the facade itself, see [Using json-migrator from Java](../docs/using-java.md).
