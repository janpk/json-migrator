package com.mosedotten.json.migrator.engine.test.operation

import com.mosedotten.json.migrator.engine.exception.ExistingFieldException
import com.mosedotten.json.migrator.engine.exception.MissingFieldException
import com.mosedotten.json.migrator.engine.operation.Copy
import com.mosedotten.json.migrator.engine.test.util.TestFixtures
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("When copying a field")
internal class CopyTest : TestFixtures() {

    @Test
    fun `within the same object duplicates the field`() {
        assertMigrates(
            """{"name":"John Doe","age":30}""",
            """{"name":"John Doe","displayName":"John Doe","age":30}""",
        ) {
            Copy("/name", "/displayName").apply(this)
        }
    }

    @Test
    fun `to a nested path creates missing parent objects`() {
        assertMigrates(
            """{"id":"123","name":"John Doe"}""",
            """{"id":"123","name":"John Doe","metadata":{"legacyId":"123"}}""",
        ) {
            Copy("/id", "/metadata/legacyId").apply(this)
        }
    }

    @Test
    fun `from a nested path copies the value to the top level`() {
        assertMigrates(
            """{"name":"John Doe","address":{"city":"Oslo"}}""",
            """{"name":"John Doe","address":{"city":"Oslo"},"city":"Oslo"}""",
        ) {
            Copy("/address/city", "/city").apply(this)
        }
    }

    @Test
    fun `between objects copies the value across`() {
        assertMigrates(
            """{"address":{"city":"Oslo"},"contact":{"email":"john@doe.com"}}""",
            """{"address":{"city":"Oslo"},"contact":{"email":"john@doe.com","city":"Oslo"}}""",
        ) {
            Copy("/address/city", "/contact/city").apply(this)
        }
    }

    @Test
    fun `an entire object copies with its children`() {
        assertMigrates(
            """{"name":"John Doe","address":{"city":"Oslo","zip":"0001"}}""",
            """{"name":"John Doe","address":{"city":"Oslo","zip":"0001"},""" +
                """"backup":{"address":{"city":"Oslo","zip":"0001"}}}""",
        ) {
            Copy("/address", "/backup/address").apply(this)
        }
    }

    @Test
    fun `the original remains unchanged`() {
        assertMigrates(
            """{"id":"123","name":"John Doe"}""",
            """{"id":"123","name":"John Doe","legacyId":"123"}""",
        ) {
            Copy("/id", "/legacyId").apply(this)
        }
    }

    @Test
    fun `a missing source fails the migration`() {
        assertMigratesThrows<MissingFieldException>("""{"name":"John Doe"}""") {
            Copy("/city", "/location").apply(this)
        }
    }

    @Test
    fun `an existing target fails the migration`() {
        assertMigratesThrows<ExistingFieldException>("""{"id":"123","legacyId":"999"}""") {
            Copy("/id", "/legacyId").apply(this)
        }
    }
}
