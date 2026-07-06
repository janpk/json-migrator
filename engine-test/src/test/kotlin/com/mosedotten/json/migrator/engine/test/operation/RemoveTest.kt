package com.mosedotten.json.migrator.engine.test.operation

import com.mosedotten.json.migrator.engine.exception.MissingFieldException
import com.mosedotten.json.migrator.engine.operation.Remove
import com.mosedotten.json.migrator.engine.test.util.TestFixtures
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("When removing a field")
internal class RemoveTest : TestFixtures() {

    @Test
    fun `at the top level removes the field`() {
        assertMigrates(
            """{"name":"John Doe","deprecated":true}""",
            """{"name":"John Doe"}""",
        ) {
            Remove("/deprecated").apply(this)
        }
    }

    @Test
    fun `sibling fields are left unchanged`() {
        assertMigrates(
            """{"name":"John Doe","age":30,"deprecated":true}""",
            """{"name":"John Doe","age":30}""",
        ) {
            Remove("/deprecated").apply(this)
        }
    }

    @Test
    fun `at a nested path removes only that field`() {
        assertMigrates(
            """{"name":"John Doe","address":{"city":"Oslo","zip":"0001"}}""",
            """{"name":"John Doe","address":{"city":"Oslo"}}""",
        ) {
            Remove("/address/zip").apply(this)
        }
    }

    @Test
    fun `an entire object is removed with its children`() {
        assertMigrates(
            """{"name":"John Doe","address":{"city":"Oslo","zip":"0001"}}""",
            """{"name":"John Doe"}""",
        ) {
            Remove("/address").apply(this)
        }
    }

    @Test
    fun `emptying a nested object leaves the parent in place`() {
        assertMigrates(
            """{"name":"John Doe","address":{"city":"Oslo"}}""",
            """{"name":"John Doe","address":{}}""",
        ) {
            Remove("/address/city").apply(this)
        }
    }

    @Test
    fun `a field holding a null value is removed`() {
        assertMigrates(
            """{"name":"John Doe","middleName":null}""",
            """{"name":"John Doe"}""",
        ) {
            Remove("/middleName").apply(this)
        }
    }

    @Test
    fun `a missing top-level field fails the migration`() {
        assertMigratesThrows<MissingFieldException>("""{"name":"John Doe"}""") {
            Remove("/deprecated").apply(this)
        }
    }

    @Test
    fun `a missing nested field fails the migration`() {
        assertMigratesThrows<MissingFieldException>("""{"name":"John Doe","address":{"city":"Oslo"}}""") {
            Remove("/address/zip").apply(this)
        }
    }

    @Test
    fun `a missing parent fails the migration`() {
        assertMigratesThrows<MissingFieldException>("""{"name":"John Doe"}""") {
            Remove("/address/city").apply(this)
        }
    }
}
