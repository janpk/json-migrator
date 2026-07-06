package com.mosedotten.json.migrator.engine.test.operation

import com.mosedotten.json.migrator.engine.exception.InvalidFieldTypeException
import com.mosedotten.json.migrator.engine.exception.MissingFieldException
import com.mosedotten.json.migrator.engine.operation.JsonType
import com.mosedotten.json.migrator.engine.operation.JsonType.ARRAY
import com.mosedotten.json.migrator.engine.operation.JsonType.BOOLEAN
import com.mosedotten.json.migrator.engine.operation.JsonType.NULL
import com.mosedotten.json.migrator.engine.operation.JsonType.NUMBER
import com.mosedotten.json.migrator.engine.operation.JsonType.OBJECT
import com.mosedotten.json.migrator.engine.operation.JsonType.STRING
import com.mosedotten.json.migrator.engine.operation.RequireType
import com.mosedotten.json.migrator.engine.test.util.JsonFixtures
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource

@DisplayName("When requiring a field type")
@TestInstance(Lifecycle.PER_CLASS)
internal class RequireTypeTest : JsonFixtures() {

    @ParameterizedTest(name = "{2} matches value at {1}")
    @MethodSource("matchingTypes")
    fun `passes when the value matches the required type`(json: String, path: String, type: JsonType) =
        assertUnchanged(json) { RequireType(path, type).apply(this) }

    @ParameterizedTest(name = "{2} required but wrong type in {0}")
    @MethodSource("typeMismatches")
    fun `fails when the value has the wrong type`(json: String, path: String, type: JsonType) =
        assertMigratesThrows<InvalidFieldTypeException>(json) { RequireType(path, type).apply(this) }

    @ParameterizedTest(name = "fails: {1} not resolvable in {0}")
    @MethodSource("unresolvablePaths")
    fun `fails when the field cannot be resolved`(json: String, path: String) =
        assertMigratesThrows<MissingFieldException>(json) { RequireType(path, STRING).apply(this) }

    @Suppress("LongMethod") // MethodSource providers are data tables, not logic
    fun matchingTypes() = listOf(
        Arguments.of("""{"name":"John"}""", "/name", STRING),
        Arguments.of("""{"age":30}""", "/age", NUMBER),
        Arguments.of("""{"score":3.5}""", "/score", NUMBER),
        Arguments.of("""{"enabled":true}""", "/enabled", BOOLEAN),
        Arguments.of("""{"enabled":false}""", "/enabled", BOOLEAN),
        Arguments.of("""{"address":{"city":"Oslo"}}""", "/address", OBJECT),
        Arguments.of("""{"address":{}}""", "/address", OBJECT),
        Arguments.of("""{"items":[1,2,3]}""", "/items", ARRAY),
        Arguments.of("""{"items":[]}""", "/items", ARRAY),
        Arguments.of("""{"middleName":null}""", "/middleName", NULL),
        Arguments.of("""{"address":{"zip":"0001"}}""", "/address/zip", STRING),
        Arguments.of("""{"id":"123","age":30,"address":{"city":"Oslo"}}""", "/age", NUMBER),
    )

    fun typeMismatches() = listOf(
        Arguments.of("""{"age":"30"}""", "/age", NUMBER),
        Arguments.of("""{"name":42}""", "/name", STRING),
        Arguments.of("""{"age":true}""", "/age", NUMBER),
        Arguments.of("""{"address":[1,2,3]}""", "/address", OBJECT),
        Arguments.of("""{"items":{"a":1}}""", "/items", ARRAY),
        Arguments.of("""{"middleName":"John"}""", "/middleName", NULL),
        Arguments.of("""{"name":null}""", "/name", STRING),
    )

    fun unresolvablePaths() = listOf(
        Arguments.of("""{"name":"John"}""", "/age"),
        Arguments.of("""{"address":{"city":"Oslo"}}""", "/address/zip"),
        Arguments.of("""{"name":"John"}""", "/address/city"),
        Arguments.of("""{"address":"Oslo"}""", "/address/city"),
    )
}
