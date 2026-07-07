package com.mosedotten.json.migrator.engine.dsl.clause

import com.mosedotten.json.migrator.engine.dsl.MigrationBuilder
import com.mosedotten.json.migrator.engine.operation.Transform
import tools.jackson.databind.JsonNode

fun MigrationBuilder.transform(path: String, lenient: Boolean = false, transformation: JsonNode.() -> JsonNode) =
    record(Transform(path, lenient, transformation))
