package com.mosedotten.json.migrator.engine.dsl

import com.mosedotten.json.migrator.engine.dsl.clause.PendingClause
import com.mosedotten.json.migrator.engine.exception.DslClauseAlreadyCompletedException
import com.mosedotten.json.migrator.engine.exception.IncompleteDslClauseException
import com.mosedotten.json.migrator.engine.operation.Operation

@JsonMigratorDsl
class MigrationBuilder(private val from: Int, private val to: Int) {
    private val operations = mutableListOf<Operation>()
    private val pendingClauses = mutableSetOf<PendingClause>()

    internal fun register(clause: PendingClause) {
        pendingClauses += clause
    }

    internal fun record(operation: Operation) {
        operations += operation
    }

    internal fun complete(clause: PendingClause, operation: Operation, lazyMessage: () -> String) {
        if (clause !in pendingClauses) {
            throw DslClauseAlreadyCompletedException(lazyMessage())
        }
        pendingClauses -= clause
        operations += operation
    }

    internal fun build(versionField: String, allowNoVersionField: Boolean, execution: ExecutionStrategy): Migration {
        if (pendingClauses.isNotEmpty()) {
            throw IncompleteDslClauseException(
                "Migration $from -> $to has incomplete operations: " +
                    pendingClauses.joinToString { it.describe() },
            )
        }
        return Migration(from, to, operations.toList(), versionField, allowNoVersionField, execution)
    }
}
