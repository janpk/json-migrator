package com.mosedotten.json.migrator.engine.dsl.clause

import com.mosedotten.json.migrator.engine.dsl.JsonMigratorDsl
import com.mosedotten.json.migrator.engine.dsl.MigrationBuilder
import com.mosedotten.json.migrator.engine.operation.Operation
import tools.jackson.databind.JsonNode

interface PendingClause {
    fun describe(): String
}

@JsonMigratorDsl
abstract class CompleteOnceClause(private val builder: MigrationBuilder) : PendingClause {

    init {
        builder.register(this)
    }

    protected fun complete(operation: Operation, alreadyCompleted: () -> String) =
        builder.complete(this, operation, alreadyCompleted)
}

@JsonMigratorDsl
abstract class WithValueClause(builder: MigrationBuilder, private val verb: String, protected val path: String) :
    CompleteOnceClause(builder) {

    protected abstract fun operation(value: JsonNode): Operation

    infix fun with(value: JsonNode) = complete(operation(value)) {
        "$verb(\"$path\") was already completed with a value"
    }

    override fun describe() = "$verb(\"$path\") is missing `with <value>`"
}

@JsonMigratorDsl
abstract class ToTargetClause(builder: MigrationBuilder, private val verb: String, protected val from: String) :
    CompleteOnceClause(builder) {

    protected abstract fun operation(target: String): Operation

    infix fun to(target: String) = complete(operation(target)) {
        "$verb(\"$from\") was already completed"
    }

    override fun describe() = "$verb(\"$from\") is missing `to <target>`"
}
