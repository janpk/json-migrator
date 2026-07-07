package com.mosedotten.json.migrator.engine.dsl

import com.mosedotten.json.migrator.engine.dsl.clause.PendingClause
import com.mosedotten.json.migrator.engine.exception.DslClauseAlreadyCompletedException
import com.mosedotten.json.migrator.engine.exception.IncompleteDslClauseException
import com.mosedotten.json.migrator.engine.operation.Operation

@JsonMigratorDsl
class MigrationBuilder internal constructor(private val from: Int, private val to: Int) {
    private val operations = mutableListOf<Operation>()
    private val pendingClauses = mutableSetOf<PendingClause>()

    internal fun register(clause: PendingClause) {
        pendingClauses += clause
    }

    internal fun record(operation: Operation) {
        operations += operation
    }

    internal fun complete(clause: PendingClause, operation: Operation, alreadyCompletedMessage: () -> String) {
        if (clause !in pendingClauses) {
            throw DslClauseAlreadyCompletedException(alreadyCompletedMessage())
        }
        pendingClauses -= clause
        operations += operation
    }

    internal fun build(
        versionField: String,
        allowMissingVersionField: Boolean,
        execution: ExecutionStrategy,
    ): Migration {
        if (pendingClauses.isNotEmpty()) {
            throw IncompleteDslClauseException(
                "Migration $from -> $to has incomplete operations: " +
                    pendingClauses.joinToString { it.describe() },
            )
        }
        return Migration(from, to, operations.toList(), versionField, allowMissingVersionField, execution)
    }

    internal fun nestedOperations(block: MigrationBuilder.() -> Unit) = MigrationBuilder(from, to)
        .apply(block)
        .also { it.validateNestedClauses() }
        .operations
        .toList()

    private fun validateNestedClauses() {
        if (pendingClauses.isEmpty()) return
        throw IncompleteDslClauseException(
            "nested block has incomplete operations: " +
                pendingClauses.joinToString { it.describe() },
        )
    }
}
