package com.mosedotten.json.migrator.engine.dsl

import com.mosedotten.json.migrator.engine.exception.MigrationException
import com.mosedotten.json.migrator.engine.exception.MigrationExecutionException
import com.mosedotten.json.migrator.engine.exception.MigrationVersionException
import com.mosedotten.json.migrator.engine.operation.Document
import com.mosedotten.json.migrator.engine.operation.Operation
import tools.jackson.databind.node.ObjectNode
import kotlin.math.abs

class Migration(
    private val from: Int,
    private val to: Int,
    private val operations: List<Operation>,
    private val versionField: String = "schemaVersion",
    private val allowNoVersionField: Boolean = false,
    private val execution: ExecutionStrategy = ExecutionStrategy.Atomic,
) {
    init {
        if (from == 0) throw MigrationVersionException(from, to, "from must not be 0")
        if (to == 0) throw MigrationVersionException(from, to, "to must not be 0")
        if (abs(from - to) != 1) {
            throw MigrationVersionException(from, to, "from and to must be adjacent versions")
        }
    }

    fun execute(rootNode: ObjectNode): ObjectNode {
        val version = resolveVersion(rootNode) ?: return runMigration(rootNode)
        return applyOrSkip(rootNode, version)
    }

    private fun applyOrSkip(rootNode: ObjectNode, version: Int): ObjectNode {
        if (version == from) return runMigration(rootNode)
        if (isAlreadyApplied(version)) return rootNode
        throw gapException(version)
    }

    private fun runMigration(rootNode: ObjectNode): ObjectNode =
        execution.execute(rootNode) { applyOperations(rootNode) }

    private fun applyOperations(rootNode: ObjectNode): ObjectNode {
        val document = Document(rootNode)
        operations.forEachIndexed { index, operation -> executeOperation(document, index, operation) }
        updateVersionField(rootNode)
        return rootNode
    }

    private fun executeOperation(document: Document, index: Int, operation: Operation) {
        try {
            operation.apply(document)
        } catch (exception: MigrationException) {
            throw MigrationExecutionException(from, to, index + 1, operation.describe(), exception)
        }
    }

    private fun resolveVersion(rootNode: ObjectNode): Int? {
        val version = rootNode.get(versionField) ?: return resolveMissingVersion()
        if (!version.isInt) {
            throw MigrationVersionException(
                from,
                to,
                "version field '$versionField' must be an integer",
            )
        }
        return version.intValue()
    }

    private fun resolveMissingVersion(): Int? {
        if (allowNoVersionField) return null
        throw MigrationVersionException(from, to, "root node must contain version field '$versionField'")
    }

    private fun isAlreadyApplied(version: Int) = if (to > from) version > from else version < from

    private fun gapException(version: Int) = MigrationVersionException(
        from,
        to,
        "root node version field '$versionField' is $version; no migration advances it to from version $from",
    )

    private fun updateVersionField(rootNode: ObjectNode) {
        rootNode.put(versionField, to)
    }
}
