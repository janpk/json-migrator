package com.mosedotten.json.migrator.engine.dsl.clause

import com.mosedotten.json.migrator.engine.dsl.JsonMigratorDsl
import com.mosedotten.json.migrator.engine.dsl.MigrationBuilder
import com.mosedotten.json.migrator.engine.operation.Merge
import com.mosedotten.json.migrator.engine.operation.ValueJoinerStrategy

fun MigrationBuilder.merge(vararg sources: String, joiner: ValueJoinerStrategy = ValueJoinerStrategy.SpaceSeparated) =
    MergeClause(this, sources.toList(), joiner)

@JsonMigratorDsl
class MergeClause internal constructor(
    builder: MigrationBuilder,
    private val sources: List<String>,
    private val joiner: ValueJoinerStrategy,
) : CompleteOnceClause(builder) {

    infix fun into(target: String) = complete(Merge(sources, target, joiner)) {
        "merge(${describeSources()}) was already completed"
    }

    override fun describe() = "merge(${describeSources()}) is missing `into <target>`"

    private fun describeSources() = sources.joinToString { "\"$it\"" }
}
