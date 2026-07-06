package com.mosedotten.json.migrator.engine.test.operation

import com.mosedotten.json.migrator.engine.operation.Remove
import com.mosedotten.json.migrator.engine.operation.RemoveIfEmpty
import com.mosedotten.json.migrator.engine.test.util.JsonFixtures
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource

@DisplayName("When removing if empty")
@TestInstance(Lifecycle.PER_CLASS)
internal class RemoveIfEmptyTest : JsonFixtures() {

    @ParameterizedTest(name = "removes empty {1} from {0}")
    @MethodSource("emptyContainers")
    fun `removes an empty container`(json: String, path: String, expected: String) =
        assertMigrates(json, expected) { RemoveIfEmpty(path).apply(this) }

    @ParameterizedTest(name = "leaves {0} unchanged for {1}")
    @MethodSource("preservedFields")
    fun `is a no-op when the field is absent, unresolvable, or not an empty container`(json: String, path: String) =
        assertUnchanged(json) { RemoveIfEmpty(path).apply(this) }

    @ParameterizedTest(name = "cascade is ignored for {1} in {0}")
    @MethodSource("cascadeIgnored")
    fun `cascade is ignored for scalars and missing fields`(json: String, path: String) =
        assertUnchanged(json) { RemoveIfEmpty(path, cascade = true).apply(this) }

    @Test
    fun `removes multiple empty fields independently`() {
        assertMigrates(
            """{"a":{},"b":{"x":1},"c":[]}""",
            """{"b":{"x":1}}""",
        ) {
            RemoveIfEmpty("/a").apply(this)
            RemoveIfEmpty("/c").apply(this)
        }
    }

    @Test
    fun `removes an object when a field is deleted making it empty`() {
        assertMigrates(
            """{"address":{"city":"Oslo"}}""",
            """{}""",
        ) {
            Remove("/address/city").apply(this)
            RemoveIfEmpty("/address").apply(this)
        }
    }

    @Test
    fun `with cascade false, leaves empty parents in place`() {
        assertMigrates(
            """{"profile":{"contact":{"email":"john@example.com"}}}""",
            """{"profile":{}}""",
        ) {
            Remove("/profile/contact/email").apply(this)
            RemoveIfEmpty("/profile/contact", cascade = false).apply(this)
        }
    }

    @Test
    fun `with cascade true, removes empty parents recursively`() {
        assertMigrates(
            """{"profile":{"contact":{"email":"john@example.com"}}}""",
            """{}""",
        ) {
            Remove("/profile/contact/email").apply(this)
            RemoveIfEmpty("/profile/contact", cascade = true).apply(this)
        }
    }

    @Test
    fun `with cascade true, stops when encountering non-empty parent`() {
        assertMigrates(
            """{"profile":{"contact":{"email":"john@example.com"},"name":"John"}}""",
            """{"profile":{"name":"John"}}""",
        ) {
            Remove("/profile/contact/email").apply(this)
            RemoveIfEmpty("/profile/contact", cascade = true).apply(this)
        }
    }

    @Test
    fun `with cascade true, cascades multiple levels`() {
        assertMigrates(
            """{"a":{"b":{"c":{"d":"value"}}}}""",
            """{}""",
        ) {
            Remove("/a/b/c/d").apply(this)
            RemoveIfEmpty("/a/b/c", cascade = true).apply(this)
        }
    }

    @Suppress("LongMethod") // MethodSource providers are data tables, not logic
    fun emptyContainers() = listOf(
        Arguments.of("""{"address":{}}""", "/address", """{}"""),
        Arguments.of("""{"items":[]}""", "/items", """{}"""),
        Arguments.of("""{"name":"John","address":{},"age":30}""", "/address", """{"name":"John","age":30}"""),
        Arguments.of("""{"profile":{"contact":{}}}""", "/profile/contact", """{"profile":{}}"""),
        Arguments.of(
            """{"profile":{"contact":{},"name":"John"}}""",
            "/profile/contact",
            """{"profile":{"name":"John"}}""",
        ),
        Arguments.of("""{"data":{"nested":{}}}""", "/data/nested", """{"data":{}}"""),
    )

    @Suppress("LongMethod") // MethodSource providers are data tables, not logic
    fun preservedFields() = listOf(
        Arguments.of("""{"address":{"city":"Oslo"}}""", "/address"),
        Arguments.of("""{"items":[1,2,3]}""", "/items"),
        Arguments.of("""{"name":"John"}""", "/address"),
        Arguments.of("""{"address":"123"}""", "/address"),
        Arguments.of("""{"address":42}""", "/address"),
        Arguments.of("""{"address":true}""", "/address"),
        Arguments.of("""{"address":null}""", "/address"),
        Arguments.of("""{"profile":{"name":"John"}}""", "/profile/contact"),
        Arguments.of("""{"name":"John"}""", "/profile/contact"),
        Arguments.of("""{"profile":"John"}""", "/profile/contact"),
        Arguments.of("""{"profile":[1,2,3]}""", "/profile/contact"),
        Arguments.of("""{"address":{"city":null}}""", "/address"),
        Arguments.of("""{"items":[null,null]}""", "/items"),
    )

    fun cascadeIgnored() = listOf(
        Arguments.of("""{"field":"value"}""", "/field"),
        Arguments.of("""{"name":"John"}""", "/missing"),
    )
}
