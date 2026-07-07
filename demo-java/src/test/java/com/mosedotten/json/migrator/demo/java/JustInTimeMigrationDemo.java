package com.mosedotten.json.migrator.demo.java;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import tools.jackson.databind.node.ObjectNode;

import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Driver 1 — just-in-time migration on read.
 *
 * <p>The database holds credit-application documents written by every past release, so their
 * {@code schemaVersion} ranges from 1 to the current 6. When the application reads one (or consumes
 * it from Kafka) it feeds it through the migration script, which upgrades it from whatever version
 * it is at to the latest, and deserializes the result into the one DTO the application knows.
 *
 * <p>These are showcase drivers, not exhaustive tests: they assert just enough to prove the flow ran.
 */
@DisplayName("Reading a stored document and migrating it just-in-time to the application DTO (Java)")
class JustInTimeMigrationDemo extends DemoFixtures {

    // The database contains documents written by every release so far (v1 through the latest, v6).
    static IntStream storedVersions() {
        return IntStream.rangeClosed(1, 6);
    }

    @ParameterizedTest(name = "a document stored at v{0} is read as the latest application DTO")
    @MethodSource("storedVersions")
    void aStoredDocumentOfAnyVersionIsMigratedToTheLatestDto(int storedVersion) {
        ObjectNode stored = storedDocument(storedVersion);

        ObjectNode migrated = CreditApplicationMigrations.migrate(stored);
        CreditApplication application = mapper.treeToValue(migrated, CreditApplication.class);

        // Whatever version we pulled from the database, we now hold a fully-populated latest DTO.
        assertEquals(6, application.schemaVersion());
        assertEquals("Jane", application.applicant().firstName());
        assertEquals("NO", application.applicant().addresses().get(0).country().iso2());
        assertEquals("NOK", application.applicant().financials().income().monthly().currency());
        assertEquals("CREDIT_CARD", application.requestedProducts().get(0).type());
    }
}
