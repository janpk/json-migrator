package com.mosedotten.json.migrator.engine.test.operation

import com.mosedotten.json.migrator.engine.exception.ExistingFieldException
import com.mosedotten.json.migrator.engine.exception.MissingFieldException
import com.mosedotten.json.migrator.engine.operation.Move
import com.mosedotten.json.migrator.engine.test.util.TestFixtures
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("When moving a field")
internal class MoveTest : TestFixtures() {

    @Test
    fun `within the same object renames the field`() {
        assertMigrates(
            """{"name":"John Doe","age":30}""",
            """{"fullName":"John Doe","age":30}""",
        ) {
            Move("/name", "/fullName").apply(this)
        }
    }

    @Test
    fun `to a nested path creates missing parent objects`() {
        assertMigrates(
            """{"name":"John Doe","city":"Oslo"}""",
            """{"name":"John Doe","address":{"city":"Oslo"}}""",
        ) {
            Move("/city", "/address/city").apply(this)
        }
    }

    @Test
    fun `from a nested path flattens the value to the top level`() {
        assertMigrates(
            """{"name":"John Doe","address":{"city":"Oslo"}}""",
            """{"name":"John Doe","address":{},"city":"Oslo"}""",
        ) {
            Move("/address/city", "/city").apply(this)
        }
    }

    @Test
    fun `between objects moves the value across`() {
        assertMigrates(
            """{"address":{"city":"Oslo"},"contact":{"email":"john@doe.com"}}""",
            """{"address":{},"contact":{"email":"john@doe.com","city":"Oslo"}}""",
        ) {
            Move("/address/city", "/contact/city").apply(this)
        }
    }

    @Test
    fun `an entire object moves with its children`() {
        assertMigrates(
            """{"name":"John Doe","address":{"city":"Oslo","zip":"0001"}}""",
            """{"name":"John Doe","location":{"address":{"city":"Oslo","zip":"0001"}}}""",
        ) {
            Move("/address", "/location/address").apply(this)
        }
    }

    @Test
    fun `a missing source fails the migration`() {
        assertMigratesThrows<MissingFieldException>("""{"name":"John Doe"}""") {
            Move("/city", "/address/city").apply(this)
        }
    }

    @Test
    fun `an existing target fails the migration`() {
        assertMigratesThrows<ExistingFieldException>("""{"name":"John Doe","fullName":"Johnny"}""") {
            Move("/name", "/fullName").apply(this)
        }
    }
}
