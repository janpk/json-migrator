package com.mosedotten.json.migrator.engine.dsl.clause

import com.mosedotten.json.migrator.engine.dsl.MigrationBuilder
import com.mosedotten.json.migrator.engine.operation.ForEach

fun MigrationBuilder.forEach(path: String, block: MigrationBuilder.() -> Unit) =
    record(ForEach(path, nestedOperations(block)))
