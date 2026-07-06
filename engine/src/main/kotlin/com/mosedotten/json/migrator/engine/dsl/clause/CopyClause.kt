package com.mosedotten.json.migrator.engine.dsl.clause

import com.mosedotten.json.migrator.engine.dsl.MigrationBuilder
import com.mosedotten.json.migrator.engine.operation.Copy

infix fun MigrationBuilder.copy(from: String) = CopyClause(this, from)

class CopyClause internal constructor(builder: MigrationBuilder, from: String) :
    ToTargetClause(builder, "copy", from) {

    override fun operation(target: String) = Copy(from, target)
}
