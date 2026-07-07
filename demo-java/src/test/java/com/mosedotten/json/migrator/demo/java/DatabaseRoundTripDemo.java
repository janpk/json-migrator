package com.mosedotten.json.migrator.demo.java;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Driver 2 — the database round-trip.
 *
 * <p>A document written long ago (here, a v1 row) is read, migrated to the latest, deserialized into
 * the application DTO for processing, and then serialized straight back to be stored again. The
 * effect is that simply accessing an old row rewrites it at the current schema version, with no data
 * lost along the way.
 */
@DisplayName("Reading, migrating, processing and storing a document back at the latest version (Java)")
class DatabaseRoundTripDemo extends DemoFixtures {

    @Test
    void accessingAnOldRowStoresItBackAtTheLatestVersionWithoutLosingData() {
        var stored = storedDocument(1); // a v1 row that has sat in the database since the first release

        CreditApplication application =
            mapper.treeToValue(CreditApplicationMigrations.migrate(stored), CreditApplication.class);

        // ... the application processes `application` here (validation, business logic, ...) ...

        JsonNode writtenBack = mapper.readTree(mapper.writeValueAsString(application));

        // It goes back to the database at the current schema version, not the version it came in at.
        assertEquals(6, writtenBack.get("schemaVersion").intValue());
        // And nothing was dropped: the DTO read from the stored form matches what we wrote.
        assertEquals(application, mapper.treeToValue(writtenBack, CreditApplication.class));
    }
}
