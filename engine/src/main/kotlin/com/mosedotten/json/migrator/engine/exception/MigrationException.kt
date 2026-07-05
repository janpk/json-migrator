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
