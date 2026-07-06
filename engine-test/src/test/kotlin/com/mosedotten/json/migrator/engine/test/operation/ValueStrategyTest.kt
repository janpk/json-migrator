package com.mosedotten.json.migrator.engine.test.operation

import com.mosedotten.json.migrator.engine.dsl.clause.merge
import com.mosedotten.json.migrator.engine.dsl.clause.split
import com.mosedotten.json.migrator.engine.operation.Merge
import com.mosedotten.json.migrator.engine.operation.Split
import com.mosedotten.json.migrator.engine.operation.ValueJoinerStrategy
import com.mosedotten.json.migrator.engine.operation.ValueSplitterStrategy
import com.mosedotten.json.migrator.engine.test.util.TestFixtures
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("When composing strings with custom strategies")
internal class ValueStrategyTest : TestFixtures() {

    private val commaJoiner = ValueJoinerStrategy { values -> values.joinToString(",") { it.asString() } }
    private val commaSplitter = ValueSplitterStrategy { value -> value.asString().split(",") }

    @Test
    fun `merge uses a custom joiner`() {
        assertMigrates(
            """{"firstName":"John","lastName":"Doe"}""",
            """{"fullName":"John,Doe"}""",
        ) {
            Merge(listOf("/firstName", "/lastName"), "/fullName", commaJoiner).apply(this)
        }
    }

    @Test
    fun `split uses a custom splitter`() {
        assertMigrates(
            """{"fullName":"John,Doe"}""",
            """{"firstName":"John","lastName":"Doe"}""",
        ) {
            Split("/fullName", listOf("/firstName", "/lastName"), commaSplitter).apply(this)
        }
    }

    @Test
    fun `merge defaults to space-separated joining`() {
        assertMigrates(
            """{"firstName":"John","lastName":"Doe"}""",
            """{"fullName":"John Doe"}""",
        ) {
            Merge(listOf("/firstName", "/lastName"), "/fullName").apply(this)
        }
    }

    @Test
    fun `split defaults to space-separated splitting`() {
        assertMigrates(
            """{"fullName":"John Doe"}""",
            """{"firstName":"John","lastName":"Doe"}""",
        ) {
            Split("/fullName", listOf("/firstName", "/lastName")).apply(this)
        }
    }

    @Test
    fun `merge DSL accepts a custom joiner`() {
        assertSchemaMigrates(
            """{"schemaVersion":1,"firstName":"John","lastName":"Doe"}""",
            """{"schemaVersion":2,"fullName":"John,Doe"}""",
        ) {
            migration(1, 2) {
                merge("/firstName", "/lastName", joiner = commaJoiner) into "/fullName"
            }
        }
    }

    @Test
    fun `split DSL accepts a custom splitter`() {
        assertSchemaMigrates(
            """{"schemaVersion":1,"fullName":"John,Doe"}""",
            """{"schemaVersion":2,"firstName":"John","lastName":"Doe"}""",
        ) {
            migration(1, 2) {
                split("/fullName", splitter = commaSplitter).into("/firstName", "/lastName")
            }
        }
    }

    @Test
    fun `split then merge round-trips with the same custom strategy`() {
        assertMigrates(
            """{"full":"a,b"}""",
            """{"full":"a,b"}""",
        ) {
            Split("/full", listOf("/x", "/y"), commaSplitter).apply(this)
            Merge(listOf("/x", "/y"), "/full", commaJoiner).apply(this)
        }
    }
}
