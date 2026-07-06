package com.mosedotten.json.migrator.engine.dsl.clause

import com.mosedotten.json.migrator.engine.dsl.MigrationBuilder
import com.mosedotten.json.migrator.engine.operation.RequireExists

fun MigrationBuilder.requireExists(path: String) = record(RequireExists(path))
