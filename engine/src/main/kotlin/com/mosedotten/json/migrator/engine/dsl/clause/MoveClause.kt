package com.mosedotten.json.migrator.engine.dsl.clause

import com.mosedotten.json.migrator.engine.dsl.MigrationBuilder
import com.mosedotten.json.migrator.engine.operation.Move

infix fun MigrationBuilder.move(from: String) = MoveClause(this, from)

class MoveClause internal constructor(builder: MigrationBuilder, from: String) :
    ToTargetClause(builder, "move", from) {

    override fun operation(target: String) = Move(from, target)
}
