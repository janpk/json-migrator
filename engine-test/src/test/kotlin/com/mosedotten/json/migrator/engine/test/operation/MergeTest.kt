package com.mosedotten.json.migrator.engine.test.operation

import com.mosedotten.json.migrator.engine.exception.ExistingFieldException
import com.mosedotten.json.migrator.engine.exception.InvalidOperationException
import com.mosedotten.json.migrator.engine.exception.MissingFieldException
import com.mosedotten.json.migrator.engine.operation.Merge
import com.mosedotten.json.migrator.engine.test.util.JsonFixtures
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("When merging fields")
internal class MergeTest : JsonFixtures() {

    @Test
    fun `two fields are concatenated with a space into the target`() {
        assertMigrates(
            """{"firstName":"John","lastName":"Doe"}""",
            """{"fullName":"John Doe"}""",
        ) {
            Merge(listOf("/firstName", "/lastName"), "/fullName").apply(this)
        }
    }

    @Test
    fun `unrelated fields are left untouched`() {
        assertMigrates(
            """{"firstName":"John","lastName":"Doe","age":30}""",
            """{"fullName":"John Doe","age":30}""",
        ) {
            Merge(listOf("/firstName", "/lastName"), "/fullName").apply(this)
        }
    }

    @Test
    fun `more than two sources are all concatenated in order`() {
        assertMigrates(
            """{"a":"one","b":"two","c":"three"}""",
            """{"joined":"one two three"}""",
        ) {
            Merge(listOf("/a", "/b", "/c"), "/joined").apply(this)
        }
    }

    @Test
    fun `values are joined in source-list order, not document order`() {
        assertMigrates(
            """{"firstName":"John","lastName":"Doe"}""",
            """{"fullName":"Doe John"}""",
        ) {
            Merge(listOf("/lastName", "/firstName"), "/fullName").apply(this)
        }
    }

    @Test
    fun `non-string scalar values are concatenated using their text form`() {
        assertMigrates(
            """{"name":"John","age":30}""",
            """{"summary":"John 30"}""",
        ) {
            Merge(listOf("/name", "/age"), "/summary").apply(this)
        }
    }

    @Test
    fun `nested sources are removed leaving their parent object in place`() {
        assertMigrates(
            """{"name":{"first":"John","last":"Doe"}}""",
            """{"name":{},"fullName":"John Doe"}""",
        ) {
            Merge(listOf("/name/first", "/name/last"), "/fullName").apply(this)
        }
    }

    @Test
    fun `sources from different objects are merged`() {
        assertMigrates(
            """{"a":{"x":"John"},"b":{"y":"Doe"}}""",
            """{"a":{},"b":{},"fullName":"John Doe"}""",
        ) {
            Merge(listOf("/a/x", "/b/y"), "/fullName").apply(this)
        }
    }

    @Test
    fun `a nested target path creates missing parent objects`() {
        assertMigrates(
            """{"firstName":"John","lastName":"Doe"}""",
            """{"profile":{"fullName":"John Doe"}}""",
        ) {
            Merge(listOf("/firstName", "/lastName"), "/profile/fullName").apply(this)
        }
    }

    @Test
    fun `a missing source fails the operation`() {
        assertMigratesThrows<MissingFieldException>("""{"firstName":"John"}""") {
            Merge(listOf("/firstName", "/lastName"), "/fullName").apply(this)
        }
    }

    @Test
    fun `an existing target fails the operation`() {
        assertMigratesThrows<ExistingFieldException>("""{"firstName":"John","lastName":"Doe","fullName":"Johnny"}""") {
            Merge(listOf("/firstName", "/lastName"), "/fullName").apply(this)
        }
    }

    @Test
    fun `merging into an existing source fails the operation`() {
        assertMigratesThrows<ExistingFieldException>("""{"firstName":"John","lastName":"Doe"}""") {
            Merge(listOf("/firstName", "/lastName"), "/firstName").apply(this)
        }
    }

    @Test
    fun `a single source fails the operation`() {
        assertMigratesThrows<InvalidOperationException>("""{"firstName":"John"}""") {
            Merge(listOf("/firstName"), "/fullName").apply(this)
        }
    }

    @Test
    fun `no sources fails the operation`() {
        assertMigratesThrows<InvalidOperationException>("""{"firstName":"John"}""") {
            Merge(emptyList(), "/fullName").apply(this)
        }
    }
}
