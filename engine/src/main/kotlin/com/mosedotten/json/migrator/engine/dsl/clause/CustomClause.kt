package com.mosedotten.json.migrator.engine.dsl.clause

import com.mosedotten.json.migrator.engine.dsl.MigrationBuilder
import com.mosedotten.json.migrator.engine.operation.Custom
import tools.jackson.databind.node.ObjectNode

fun MigrationBuilder.custom(block: (ObjectNode) -> Unit) = record(Custom(block))
