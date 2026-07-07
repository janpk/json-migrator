package com.mosedotten.json.migrator.demo

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

/**
 * Driver 1 — just-in-time migration on read.
 *
 * The database holds credit-application documents written by every past release, so their
 * `schemaVersion` ranges from 1 to the current 6. When the application reads one (or consumes it
 * from Kafka) it feeds it through the migration script, which upgrades it from whatever version it
 * is at to the latest, and deserializes the result into the one DTO the application knows.
 *
 * These are showcase drivers, not exhaustive tests: they assert just enough to prove the flow ran.
 */
@DisplayName("Reading a stored document and migrating it just-in-time to the application DTO")
internal class JustInTimeMigrationDemo : DemoFixtures() {

    @ParameterizedTest(name = "a document stored at v{0} is read as the latest application DTO")
    @MethodSource("storedVersions")
    fun `a stored document of any version is migrated to the latest DTO`(storedVersion: Int) {
        val stored = storedDocument(storedVersion)

        val migrated = CreditApplicationMigrations.migrate(stored)
        val application = mapper.treeToValue(migrated, CreditApplication::class.java)

        // Whatever version we pulled from the database, we now hold a fully-populated latest DTO.
        assertEquals(6, application.schemaVersion)
        assertEquals("Jane", application.applicant.firstName)
        assertEquals("NO", application.applicant.addresses.first().country.iso2)
        assertEquals("NOK", application.applicant.financials.income.monthly.currency)
        assertEquals("CREDIT_CARD", application.requestedProducts.first().type)
    }

    private companion object {
        // The database contains documents written by every release so far (v1 through the latest, v6).
        @JvmStatic
        fun storedVersions() = (1..6).toList()
    }
}
