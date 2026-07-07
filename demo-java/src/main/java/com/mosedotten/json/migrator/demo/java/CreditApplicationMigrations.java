package com.mosedotten.json.migrator.demo.java;

import com.mosedotten.json.migrator.engine.java.JsonMigrator;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;
import tools.jackson.databind.node.StringNode;

/**
 * Worked example: migrating a credit-application document through every schema version with the
 * Java-facing {@link JsonMigrator} facade.
 *
 * <p>The migrations are declared once as a single chain. The engine reads the document's current
 * version, skips the migrations it has already applied, and runs the rest through to the latest — so
 * the same definition upgrades a brand-new v1 document or resumes a partially-migrated one. The
 * version table lives in the module README.
 */
public final class CreditApplicationMigrations {

    private static final String CURRENCY = "NOK";

    private CreditApplicationMigrations() {
    }

    public static ObjectNode migrate(ObjectNode root) {
        return JsonMigrator.migrate(root)
            .migration(1, 2, m -> m
                .split("/applicant/fullName",
                    "/applicant/firstName", "/applicant/middleName", "/applicant/lastName")
                .move("/applicant/email", "/applicant/contact/email")
                .move("/applicant/mobilePhone", "/applicant/contact/mobilePhone"))
            .migration(2, 3, m -> m
                .forEach("/applicant/addresses", address -> address
                    .move("/postalCode", "/zipCode")
                    .move("/countryCode", "/country/iso2")))
            .migration(3, 4, m -> m
                .move("/applicant/employment/monthlyIncome", "/applicant/financials/income/monthly/amount")
                .add("/applicant/financials/income/monthly/currency", StringNode.valueOf(CURRENCY))
                .move("/applicant/employment/employerName", "/applicant/employment/employer"))
            .migration(4, 5, m -> m
                .move("/consents/emailMarketing", "/preferences/marketing/email")
                .move("/consents/smsMarketing", "/preferences/marketing/sms")
                .move("/consents/termsAcceptedAt", "/legal/termsAcceptedAt")
                .removeIfEmpty("/consents"))
            .migration(5, 6, m -> m
                .forEach("/requestedProducts", product -> product
                    .move("/productCode", "/type")
                    .custom(CreditApplicationMigrations::normalizeLimit)))
            .run();
    }

    private static void normalizeLimit(ObjectNode product) {
        JsonNode limit = product.remove("limit");
        if (limit != null) {
            ObjectNode creditLimit = product.putObject("creditLimit");
            creditLimit.set("amount", limit);
            creditLimit.put("currency", CURRENCY);
        }
    }
}
