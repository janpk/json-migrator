package com.mosedotten.json.migrator.engine.dsl

import tools.jackson.databind.node.ObjectNode

fun schema(
    rootNode: ObjectNode,
    versionField: String = "schemaVersion",
    allowNoVersionField: Boolean = false,
    execution: ExecutionStrategy = ExecutionStrategy.Atomic,
    block: SchemaBuilder.() -> Unit,
) = SchemaBuilder(rootNode, versionField, allowNoVersionField, execution).apply(block).execute()

@JsonMigratorDsl
class SchemaBuilder internal constructor(
    private val rootNode: ObjectNode,
    private val versionField: String,
    private val allowNoVersionField: Boolean,
    private val execution: ExecutionStrategy,
) {
    private val migrations = mutableListOf<Migration>()

    fun migration(from: Int, to: Int, block: MigrationBuilder.() -> Unit) {
        migrations += MigrationBuilder(from, to).apply(block).build(versionField, allowNoVersionField, execution)
    }

    internal fun execute(): ObjectNode = execution.execute(rootNode) { runMigrations() }

    private fun runMigrations(): ObjectNode {
        migrations.forEach { it.execute(rootNode) }
        return rootNode
    }
}
