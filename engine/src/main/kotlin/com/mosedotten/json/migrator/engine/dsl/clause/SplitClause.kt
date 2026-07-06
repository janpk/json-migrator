package com.mosedotten.json.migrator.engine.dsl.clause

import com.mosedotten.json.migrator.engine.dsl.JsonMigratorDsl
import com.mosedotten.json.migrator.engine.dsl.MigrationBuilder
import com.mosedotten.json.migrator.engine.operation.Split
import com.mosedotten.json.migrator.engine.operation.ValueSplitterStrategy

fun MigrationBuilder.split(source: String, splitter: ValueSplitterStrategy = ValueSplitterStrategy.SpaceSeparated) =
    SplitClause(this, source, splitter)

@JsonMigratorDsl
class SplitClause internal constructor(
    builder: MigrationBuilder,
    private val source: String,
    private val splitter: ValueSplitterStrategy,
) : CompleteOnceClause(builder) {

    fun into(vararg targets: String) = complete(Split(source, targets.toList(), splitter)) {
        "split(\"$source\") was already completed"
    }

    override fun describe() = "split(\"$source\") is missing `into <targets>`"
}
