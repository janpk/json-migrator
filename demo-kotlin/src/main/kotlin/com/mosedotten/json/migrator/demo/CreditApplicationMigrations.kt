package com.mosedotten.json.migrator.demo

import com.mosedotten.json.migrator.engine.dsl.clause.add
import com.mosedotten.json.migrator.engine.dsl.clause.custom
import com.mosedotten.json.migrator.engine.dsl.clause.forEach
import com.mosedotten.json.migrator.engine.dsl.clause.move
import com.mosedotten.json.migrator.engine.dsl.clause.removeIfEmpty
import com.mosedotten.json.migrator.engine.dsl.clause.split
import com.mosedotten.json.migrator.engine.dsl.schema
import tools.jackson.databind.JsonNode
import tools.jackson.databind.node.ObjectNode
import tools.jackson.databind.node.StringNode

/**
 * Worked example: migrating a credit-application document through every schema version with the Kotlin DSL.
 *
 * The migrations are declared once as a single `schema { }` pipeline. The engine reads the document's
 * current version, skips the migrations it has already applied, and runs the rest through to the
 * latest — so the same definition upgrades a brand-new v1 document or resumes a partially-migrated
 * one. The version table lives in the module README.
 */
object CreditApplicationMigrations {

    private const val CURRENCY = "NOK"

    @Suppress("LongMethod", "MagicNumber") // A migration pipeline is a linear changelog of version steps.
    fun migrate(root: ObjectNode): ObjectNode = schema(root) {
        migration(1, 2) {
            split("/applicant/fullName").into(
                "/applicant/firstName",
                "/applicant/middleName",
                "/applicant/lastName",
            )
            move("/applicant/email") to "/applicant/contact/email"
            move("/applicant/mobilePhone") to "/applicant/contact/mobilePhone"
        }
        migration(2, 3) {
            forEach("/applicant/addresses") {
                move("/postalCode") to "/zipCode"
                move("/countryCode") to "/country/iso2"
            }
        }
        migration(3, 4) {
            move("/applicant/employment/monthlyIncome") to "/applicant/financials/income/monthly/amount"
            add("/applicant/financials/income/monthly/currency") with StringNode.valueOf(CURRENCY)
            move("/applicant/employment/employerName") to "/applicant/employment/employer"
        }
        migration(4, 5) {
            move("/consents/emailMarketing") to "/preferences/marketing/email"
            move("/consents/smsMarketing") to "/preferences/marketing/sms"
            move("/consents/termsAcceptedAt") to "/legal/termsAcceptedAt"
            removeIfEmpty("/consents")
        }
        migration(5, 6) {
            forEach("/requestedProducts") {
                move("/productCode") to "/type"
                custom { it.normalizeLimit() }
            }
        }
    }

    private fun ObjectNode.normalizeLimit() {
        val limit: JsonNode = remove("limit") ?: return
        putObject("creditLimit").also {
            it.set("amount", limit)
            it.put("currency", CURRENCY)
        }
    }
}
