package com.mosedotten.json.migrator.engine.test.dsl

import com.mosedotten.json.migrator.engine.dsl.clause.add
import com.mosedotten.json.migrator.engine.dsl.clause.copy
import com.mosedotten.json.migrator.engine.dsl.clause.createObject
import com.mosedotten.json.migrator.engine.dsl.clause.forEach
import com.mosedotten.json.migrator.engine.dsl.clause.merge
import com.mosedotten.json.migrator.engine.dsl.clause.move
import com.mosedotten.json.migrator.engine.dsl.clause.requireExists
import com.mosedotten.json.migrator.engine.dsl.clause.requireType
import com.mosedotten.json.migrator.engine.dsl.clause.set
import com.mosedotten.json.migrator.engine.dsl.clause.split
import com.mosedotten.json.migrator.engine.dsl.clause.transform
import com.mosedotten.json.migrator.engine.dsl.schema
import com.mosedotten.json.migrator.engine.exception.DslClauseAlreadyCompletedException
import com.mosedotten.json.migrator.engine.exception.IncompleteDslClauseException
import com.mosedotten.json.migrator.engine.exception.InvalidFieldTypeException
import com.mosedotten.json.migrator.engine.exception.MigrationExecutionException
import com.mosedotten.json.migrator.engine.exception.MissingFieldException
import com.mosedotten.json.migrator.engine.operation.JsonType.NUMBER
import com.mosedotten.json.migrator.engine.test.util.JsonFixtures
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import tools.jackson.databind.node.BooleanNode
import tools.jackson.databind.node.IntNode

@DisplayName("When a DSL clause is incomplete, then")
@Suppress("LargeClass") // An incomplete + double-completion pair per clause accumulates; acceptable for test classes
internal class DslClauseValidationTest : JsonFixtures() {

    @Test
    fun `an add without a value fails the migration`() {
        assertThrows<IncompleteDslClauseException> {
            schema(obj("""{"schemaVersion":1,"name":"John Doe"}""")) {
                migration(1, 2) {
                    add("/enabled")
                }
            }
        }
    }

    @Test
    fun `completing an add clause twice fails the migration`() {
        assertThrows<DslClauseAlreadyCompletedException> {
            schema(obj("""{"schemaVersion":1,"name":"John Doe"}""")) {
                migration(1, 2) {
                    val clause = add("/enabled")
                    clause with BooleanNode.TRUE
                    clause with BooleanNode.FALSE
                }
            }
        }
    }

    @Test
    fun `a set without a value fails the migration`() {
        assertThrows<IncompleteDslClauseException> {
            schema(obj("""{"schemaVersion":1,"name":"John Doe"}""")) {
                migration(1, 2) {
                    set("/enabled")
                }
            }
        }
    }

    @Test
    fun `completing a set clause twice fails the migration`() {
        assertThrows<DslClauseAlreadyCompletedException> {
            schema(obj("""{"schemaVersion":1,"name":"John Doe"}""")) {
                migration(1, 2) {
                    val clause = set("/enabled")
                    clause with BooleanNode.TRUE
                    clause with BooleanNode.FALSE
                }
            }
        }
    }

    @Test
    fun `a copy without a target fails the migration`() {
        assertThrows<IncompleteDslClauseException> {
            schema(obj("""{"schemaVersion":1,"id":"123"}""")) {
                migration(1, 2) {
                    copy("/id")
                }
            }
        }
    }

    @Test
    fun `completing a copy clause twice fails the migration`() {
        assertThrows<DslClauseAlreadyCompletedException> {
            schema(obj("""{"schemaVersion":1,"id":"123"}""")) {
                migration(1, 2) {
                    val clause = copy("/id")
                    clause to "/legacyId"
                    clause to "/backupId"
                }
            }
        }
    }

    @Test
    fun `a move without a target fails the migration`() {
        assertThrows<IncompleteDslClauseException> {
            schema(obj("""{"schemaVersion":1,"name":"John Doe"}""")) {
                migration(1, 2) {
                    move("/name")
                }
            }
        }
    }

    @Test
    fun `completing a move clause twice fails the migration`() {
        assertThrows<DslClauseAlreadyCompletedException> {
            schema(obj("""{"schemaVersion":1,"name":"John Doe"}""")) {
                migration(1, 2) {
                    val clause = move("/name")
                    clause to "/fullName"
                    clause to "/displayName"
                }
            }
        }
    }

    @Test
    fun `a merge without a target fails the migration`() {
        assertThrows<IncompleteDslClauseException> {
            schema(obj("""{"schemaVersion":1,"firstName":"John","lastName":"Doe"}""")) {
                migration(1, 2) {
                    merge("/firstName", "/lastName")
                }
            }
        }
    }

    @Test
    fun `completing a merge clause twice fails the migration`() {
        assertThrows<DslClauseAlreadyCompletedException> {
            schema(obj("""{"schemaVersion":1,"firstName":"John","lastName":"Doe"}""")) {
                migration(1, 2) {
                    val clause = merge("/firstName", "/lastName")
                    clause into "/fullName"
                    clause into "/displayName"
                }
            }
        }
    }

    @Test
    fun `a split without targets fails the migration`() {
        assertThrows<IncompleteDslClauseException> {
            schema(obj("""{"schemaVersion":1,"fullName":"John Doe"}""")) {
                migration(1, 2) {
                    split("/fullName")
                }
            }
        }
    }

    @Test
    fun `completing a split clause twice fails the migration`() {
        assertThrows<DslClauseAlreadyCompletedException> {
            schema(obj("""{"schemaVersion":1,"fullName":"John Doe"}""")) {
                migration(1, 2) {
                    val clause = split("/fullName")
                    clause.into("/firstName", "/lastName")
                    clause.into("/a", "/b")
                }
            }
        }
    }

    @Test
    fun `a forEach with an incomplete nested clause fails the migration`() {
        assertThrows<IncompleteDslClauseException> {
            schema(obj("""{"schemaVersion":1,"users":[{"name":"John"}]}""")) {
                migration(1, 2) {
                    forEach("/users") {
                        move("/name")
                    }
                }
            }
        }
    }

    @Test
    fun `createObject fails when the path exists as a non-object`() {
        assertThrows<MigrationExecutionException> {
            schema(obj("""{"schemaVersion":1,"address":"123"}""")) {
                migration(1, 2) {
                    createObject("/address")
                }
            }
        }.also {
            assertEquals(InvalidFieldTypeException::class, it.failure::class)
        }
    }

    @Test
    fun `requireExists fails when the required field is missing`() {
        assertThrows<MigrationExecutionException> {
            schema(obj("""{"schemaVersion":1,"name":"John"}""")) {
                migration(1, 2) {
                    requireExists("/id")
                }
            }
        }.also {
            assertEquals(MissingFieldException::class, it.failure::class)
        }
    }

    @Test
    fun `requireType fails when the value has the wrong type`() {
        assertThrows<MigrationExecutionException> {
            schema(obj("""{"schemaVersion":1,"age":"30"}""")) {
                migration(1, 2) {
                    requireType("/age", NUMBER)
                }
            }
        }.also {
            assertEquals(InvalidFieldTypeException::class, it.failure::class)
        }
    }

    @Test
    fun `transform fails when the field is missing`() {
        assertThrows<MigrationExecutionException> {
            schema(obj("""{"schemaVersion":1,"name":"John"}""")) {
                migration(1, 2) {
                    transform("/age") { IntNode.valueOf(asInt() + 1) }
                }
            }
        }.also {
            assertEquals(MissingFieldException::class, it.failure::class)
        }
    }
}
