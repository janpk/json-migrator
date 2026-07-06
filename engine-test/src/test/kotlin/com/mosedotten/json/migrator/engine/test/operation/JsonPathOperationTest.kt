package com.mosedotten.json.migrator.engine.test.operation

import com.mosedotten.json.migrator.engine.exception.InvalidJsonPathException
import com.mosedotten.json.migrator.engine.exception.MissingFieldException
import com.mosedotten.json.migrator.engine.operation.Move
import com.mosedotten.json.migrator.engine.operation.RemoveIfEmpty
import com.mosedotten.json.migrator.engine.operation.RequireExists
import com.mosedotten.json.migrator.engine.operation.Set
import com.mosedotten.json.migrator.engine.test.util.JsonFixtures
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import tools.jackson.databind.node.BooleanNode

@DisplayName("When using JSON paths in operations")
internal class JsonPathOperationTest : JsonFixtures() {

    @Test
    fun `escaped slash and tilde segments target literal field names`() {
        assertMigrates(
            """{"a/b":{"c~d":false}}""",
            """{"a/b":{"c~d":true}}""",
        ) {
            Set("/a~1b/c~0d", BooleanNode.TRUE).apply(this)
        }
    }

    @Test
    fun `escaped parent paths are preserved when cascading empty removals`() {
        assertMigrates(
            """{"a/b":{"c~d":{}}}""",
            """{}""",
        ) {
            RemoveIfEmpty("/a~1b/c~0d", cascade = true).apply(this)
        }
    }

    @Test
    fun `path must not target document root`() {
        val failure = assertThrows<InvalidJsonPathException> {
            Set("", BooleanNode.TRUE)
        }

        assertEquals("Invalid JSON path '': path must point to a field, not the document root", failure.message)
    }

    @Test
    fun `path must start with slash`() {
        val failure = assertThrows<InvalidJsonPathException> {
            Set("enabled", BooleanNode.TRUE)
        }

        assertEquals("Invalid JSON path 'enabled': path must start with '/'", failure.message)
    }

    @Test
    fun `path rejects unknown escape sequences`() {
        val failure = assertThrows<InvalidJsonPathException> {
            Set("/bad~2path", BooleanNode.TRUE)
        }

        assertEquals(
            "Invalid JSON path '/bad~2path': escape sequences must be '~0' for '~' or '~1' for '/'",
            failure.message,
        )
    }

    @Test
    fun `delegating operations validate their paths at construction`() {
        val failure = assertThrows<InvalidJsonPathException> {
            Move("name", "/fullName")
        }

        assertEquals("Invalid JSON path 'name': path must start with '/'", failure.message)
    }

    @Test
    fun `navigating a deep path stops when an ancestor above the parent is missing`() {
        assertMigratesThrows<MissingFieldException>("""{"name":"John"}""") {
            RequireExists("/a/b/c").apply(this)
        }
    }
}
