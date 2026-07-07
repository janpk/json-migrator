package com.mosedotten.json.migrator.demo.java;

import java.util.List;

/**
 * The application's single DTO — always the latest schema version.
 *
 * <p>A real application only ever holds the current shape. Documents read from the database or a
 * Kafka topic are migrated up to this version before being deserialized into it for processing, and
 * it is serialized straight back out when the document is stored again. It models the whole document
 * so a read/migrate/write round-trip loses nothing.
 */
public record CreditApplication(
    int schemaVersion,
    String applicationId,
    String status,
    String submittedAt,
    Applicant applicant,
    List<Product> requestedProducts,
    List<SupportingDocument> documents,
    Preferences preferences,
    Legal legal,
    Metadata metadata) {

    public record Applicant(String customerId, String firstName, String middleName, String lastName,
                            String birthDate, Contact contact, List<Address> addresses,
                            Employment employment, Financials financials) {
    }

    public record Contact(String email, String mobilePhone) {
    }

    public record Address(String type, String street, String city, String zipCode, Country country) {
    }

    public record Country(String iso2) {
    }

    public record Employment(String status, String employer) {
    }

    public record Financials(Income income) {
    }

    public record Income(MonthlyIncome monthly) {
    }

    public record MonthlyIncome(int amount, String currency) {
    }

    public record Product(String type, CreditLimit creditLimit) {
    }

    public record CreditLimit(int amount, String currency) {
    }

    public record SupportingDocument(String documentId, String fileName, String mimeType, String uploadedAt) {
    }

    public record Preferences(Marketing marketing) {
    }

    public record Marketing(boolean email, boolean sms) {
    }

    public record Legal(String termsAcceptedAt) {
    }

    public record Metadata(String sourceSystem, String correlationId) {
    }
}
