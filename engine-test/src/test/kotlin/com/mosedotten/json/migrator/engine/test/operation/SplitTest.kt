package com.mosedotten.json.migrator.engine.test.operation

import com.mosedotten.json.migrator.engine.exception.ExistingFieldException
import com.mosedotten.json.migrator.engine.exception.InvalidFieldValueException
import com.mosedotten.json.migrator.engine.exception.InvalidOperationException
import com.mosedotten.json.migrator.engine.exception.MissingFieldException
import com.mosedotten.json.migrator.engine.operation.Split
import com.mosedotten.json.migrator.engine.test.util.JsonFixtures
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("When splitting a field")
internal class SplitTest : JsonFixtures() {

    @Test
    fun `a field is split on spaces into the targets`() {
        assertMigrates(
            """{"fullName":"John Doe"}""",
            """{"firstName":"John","lastName":"Doe"}""",
        ) {
            Split("/fullName", listOf("/firstName", "/lastName")).apply(this)
        }
    }

    @Test
    fun `unrelated fields are left untouched`() {
        assertMigrates(
            """{"fullName":"John Doe","age":30}""",
            """{"firstName":"John","lastName":"Doe","age":30}""",
        ) {
            Split("/fullName", listOf("/firstName", "/lastName")).apply(this)
        }
    }

    @Test
    fun `into more than two targets`() {
        assertMigrates(
            """{"full":"one two three"}""",
            """{"a":"one","b":"two","c":"three"}""",
        ) {
            Split("/full", listOf("/a", "/b", "/c")).apply(this)
        }
    }

    @Test
    fun `pieces are assigned in target-list order`() {
        assertMigrates(
            """{"fullName":"John Doe"}""",
            """{"lastName":"John","firstName":"Doe"}""",
        ) {
            Split("/fullName", listOf("/lastName", "/firstName")).apply(this)
        }
    }

    @Test
    fun `a nested source is removed leaving its parent object in place`() {
        assertMigrates(
            """{"name":{"full":"John Doe"}}""",
            """{"name":{},"firstName":"John","lastName":"Doe"}""",
        ) {
            Split("/name/full", listOf("/firstName", "/lastName")).apply(this)
        }
    }

    @Test
    fun `nested target paths create missing parent objects`() {
        assertMigrates(
            """{"fullName":"John Doe"}""",
            """{"a":{"first":"John"},"b":{"last":"Doe"}}""",
        ) {
            Split("/fullName", listOf("/a/first", "/b/last")).apply(this)
        }
    }

    @Test
    fun `a missing source fails the operation`() {
        assertMigratesThrows<MissingFieldException>("""{"age":30}""") {
            Split("/fullName", listOf("/firstName", "/lastName")).apply(this)
        }
    }

    @Test
    fun `an existing target fails the operation`() {
        assertMigratesThrows<ExistingFieldException>("""{"fullName":"John Doe","firstName":"Johnny"}""") {
            Split("/fullName", listOf("/firstName", "/lastName")).apply(this)
        }
    }

    @Test
    fun `splitting into the source path fails the operation`() {
        assertMigratesThrows<ExistingFieldException>("""{"fullName":"John Doe"}""") {
            Split("/fullName", listOf("/fullName", "/lastName")).apply(this)
        }
    }

    @Test
    fun `more pieces than targets fails the operation`() {
        assertMigratesThrows<InvalidFieldValueException>("""{"fullName":"John van Doe"}""") {
            Split("/fullName", listOf("/firstName", "/lastName")).apply(this)
        }
    }

    @Test
    fun `fewer pieces than targets fails the operation`() {
        assertMigratesThrows<InvalidFieldValueException>("""{"fullName":"John"}""") {
            Split("/fullName", listOf("/firstName", "/lastName")).apply(this)
        }
    }

    @Test
    fun `a single target fails the operation`() {
        assertMigratesThrows<InvalidOperationException>("""{"fullName":"John"}""") {
            Split("/fullName", listOf("/firstName")).apply(this)
        }
    }

    @Test
    fun `no targets fails the operation`() {
        assertMigratesThrows<InvalidOperationException>("""{"fullName":"John"}""") {
            Split("/fullName", emptyList()).apply(this)
        }
    }
}
