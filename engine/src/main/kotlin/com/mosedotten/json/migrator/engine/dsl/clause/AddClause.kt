package com.mosedotten.json.migrator.engine.dsl.clause

import com.mosedotten.json.migrator.engine.dsl.MigrationBuilder
import com.mosedotten.json.migrator.engine.operation.Add
import tools.jackson.databind.JsonNode

infix fun MigrationBuilder.add(path: String) = AddClause(this, path)

class AddClause internal constructor(builder: MigrationBuilder, path: String) :
    WithValueClause(builder, "add", path) {

    override fun operation(value: JsonNode) = Add(path, value)
}
