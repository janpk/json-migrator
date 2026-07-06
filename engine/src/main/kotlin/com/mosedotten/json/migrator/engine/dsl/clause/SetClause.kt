package com.mosedotten.json.migrator.engine.dsl.clause

import com.mosedotten.json.migrator.engine.dsl.MigrationBuilder
import com.mosedotten.json.migrator.engine.operation.Set
import tools.jackson.databind.JsonNode

infix fun MigrationBuilder.set(path: String) = SetClause(this, path)

class SetClause internal constructor(builder: MigrationBuilder, path: String) :
    WithValueClause(builder, "set", path) {

    override fun operation(value: JsonNode) = Set(path, value)
}
