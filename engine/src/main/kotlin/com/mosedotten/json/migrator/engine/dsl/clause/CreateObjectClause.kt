package com.mosedotten.json.migrator.engine.dsl.clause

import com.mosedotten.json.migrator.engine.dsl.MigrationBuilder
import com.mosedotten.json.migrator.engine.operation.CreateObject

fun MigrationBuilder.createObject(path: String) = record(CreateObject(path))
