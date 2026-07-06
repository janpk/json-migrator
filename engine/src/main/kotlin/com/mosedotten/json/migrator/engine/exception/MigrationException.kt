package com.mosedotten.json.migrator.engine.exception

open class MigrationException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)

open class FieldMigrationException(val path: String, message: String, cause: Throwable? = null) :
    MigrationException(message, cause)

class ExistingFieldException(path: String, label: String = "Field") :
    FieldMigrationException(path, "$label '$path' already exists")

class InvalidJsonPathException(path: String, reason: String) :
    MigrationException("Invalid JSON path '$path': $reason")

class MissingFieldException(path: String, label: String = "Field") :
    FieldMigrationException(path, "$label '$path' does not exist")

class MigrationExecutionException(
    val fromVersion: Int,
    val toVersion: Int,
    val operationIndex: Int,
    val operationDescription: String,
    val failure: MigrationException,
) : MigrationException(
    "Migration $fromVersion -> $toVersion failed at operation #$operationIndex " +
        "($operationDescription): ${failure.message}",
    failure,
)

class MigrationVersionException(fromVersion: Int, toVersion: Int, message: String) :
    MigrationException("Migration $fromVersion -> $toVersion failed version validation: $message")

class IncompleteDslClauseException(message: String) : MigrationException(message)

class DslClauseAlreadyCompletedException(message: String) : MigrationException(message)

class InvalidOperationException(message: String) : MigrationException(message)
