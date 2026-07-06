package com.mosedotten.json.migrator.engine.dsl.clause

import com.mosedotten.json.migrator.engine.dsl.MigrationBuilder
import com.mosedotten.json.migrator.engine.operation.RemoveIfEmpty

fun MigrationBuilder.removeIfEmpty(path: String, cascade: Boolean = false) = record(RemoveIfEmpty(path, cascade))
