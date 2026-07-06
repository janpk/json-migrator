package com.mosedotten.json.migrator.engine.dsl.clause

import com.mosedotten.json.migrator.engine.dsl.MigrationBuilder
import com.mosedotten.json.migrator.engine.operation.JsonType
import com.mosedotten.json.migrator.engine.operation.RequireType

fun MigrationBuilder.requireType(path: String, type: JsonType) = record(RequireType(path, type))
