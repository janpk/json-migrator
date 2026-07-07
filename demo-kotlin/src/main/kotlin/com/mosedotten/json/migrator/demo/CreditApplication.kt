@file:Suppress("LongParameterList") // Wide constructors deliberately mirror the JSON document shape.

package com.mosedotten.json.migrator.demo

/**
 * The application's single DTO — always the latest schema version.
 *
 * A real application only ever holds the current shape. Documents read from the database or a Kafka
 * topic are migrated up to this version before being deserialized into it for processing, and it is
 * serialized straight back out when the document is stored again. It models the whole document so a
 * read/migrate/write round-trip loses nothing.
 */
data class CreditApplication(
    val schemaVersion: Int,
    val applicationId: String,
    val status: String,
    val submittedAt: String,
    val applicant: Applicant,
    val requestedProducts: List<Product>,
    val documents: List<SupportingDocument>,
    val preferences: Preferences,
    val legal: Legal,
    val metadata: Metadata,
)

data class Applicant(
    val customerId: String,
    val firstName: String,
    val middleName: String?,
    val lastName: String,
    val birthDate: String,
    val contact: Contact,
    val addresses: List<Address>,
    val employment: Employment,
    val financials: Financials,
)

data class Contact(val email: String, val mobilePhone: String)

data class Address(val type: String, val street: String, val city: String, val zipCode: String, val country: Country)

data class Country(val iso2: String)

data class Employment(val status: String, val employer: String)

data class Financials(val income: Income)

data class Income(val monthly: MonthlyIncome)

data class MonthlyIncome(val amount: Int, val currency: String)

data class Product(val type: String, val creditLimit: CreditLimit? = null)

data class CreditLimit(val amount: Int, val currency: String)

data class SupportingDocument(
    val documentId: String,
    val fileName: String,
    val mimeType: String,
    val uploadedAt: String,
)

data class Preferences(val marketing: Marketing)

data class Marketing(val email: Boolean, val sms: Boolean)

data class Legal(val termsAcceptedAt: String)

data class Metadata(val sourceSystem: String, val correlationId: String)
