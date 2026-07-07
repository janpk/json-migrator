package com.mosedotten.json.migrator.engine.java

import com.mosedotten.json.migrator.engine.dsl.ExecutionStrategy
import com.mosedotten.json.migrator.engine.dsl.Migration
import com.mosedotten.json.migrator.engine.operation.Add
import com.mosedotten.json.migrator.engine.operation.Copy
import com.mosedotten.json.migrator.engine.operation.CreateObject
import com.mosedotten.json.migrator.engine.operation.Custom
import com.mosedotten.json.migrator.engine.operation.ForEach
import com.mosedotten.json.migrator.engine.operation.JsonType
import com.mosedotten.json.migrator.engine.operation.Merge
import com.mosedotten.json.migrator.engine.operation.Move
import com.mosedotten.json.migrator.engine.operation.Operation
import com.mosedotten.json.migrator.engine.operation.Remove
import com.mosedotten.json.migrator.engine.operation.RemoveIfEmpty
import com.mosedotten.json.migrator.engine.operation.RequireExists
import com.mosedotten.json.migrator.engine.operation.RequireType
import com.mosedotten.json.migrator.engine.operation.Set
import com.mosedotten.json.migrator.engine.operation.Split
import com.mosedotten.json.migrator.engine.operation.Transform
import com.mosedotten.json.migrator.engine.operation.ValueJoinerStrategy
import com.mosedotten.json.migrator.engine.operation.ValueSplitterStrategy
import tools.jackson.databind.JsonNode
import tools.jackson.databind.node.ObjectNode
import java.util.function.Consumer
import java.util.function.Function

/**
 * Java-facing facade over the migration engine.
 */
class JsonMigrator private constructor(private val root: ObjectNode) {

    private var versionField = "schemaVersion"
    private var missingVersionAllowed = false
    private var execution: ExecutionStrategy = ExecutionStrategy.Atomic
    private val specs = mutableListOf<Spec>()

    private class Spec(val from: Int, val to: Int, val operations: List<Operation>)

    companion object {
        @JvmStatic
        fun migrate(root: ObjectNode) = JsonMigrator(root)
    }

    fun versionField(name: String) = apply { versionField = name }

    fun allowMissingVersionField() = apply { missingVersionAllowed = true }

    fun atomic() = apply { execution = ExecutionStrategy.Atomic }

    fun nonAtomic() = apply { execution = ExecutionStrategy.NonAtomic }

    fun execution(strategy: ExecutionStrategy) = apply { execution = strategy }

    fun migration(from: Int, to: Int, steps: Consumer<MigrationSteps>) = apply {
        val collected = MigrationSteps()
        steps.accept(collected)
        specs += Spec(from, to, collected.build())
    }

    fun run(): ObjectNode {
        val migrations = specs.map {
            Migration(it.from, it.to, it.operations, versionField, missingVersionAllowed, execution)
        }
        return execution.execute(root) {
            migrations.forEach { it.execute(root) }
            root
        }
    }
}

@Suppress("TooManyFunctions") // One builder method per operation; a wide surface is the point of a facade.
class MigrationSteps internal constructor() {
    private val operations = mutableListOf<Operation>()

    internal fun build(): List<Operation> = operations.toList()

    fun add(path: String, value: JsonNode) = apply { operations += Add(path, value) }

    fun set(path: String, value: JsonNode) = apply { operations += Set(path, value) }

    fun copy(from: String, to: String) = apply { operations += Copy(from, to) }

    fun move(from: String, to: String) = apply { operations += Move(from, to) }

    fun remove(path: String) = apply { operations += Remove(path) }

    fun merge(target: String, vararg sources: String) = apply { operations += Merge(sources.toList(), target) }

    fun merge(target: String, joiner: ValueJoinerStrategy, vararg sources: String) =
        apply { operations += Merge(sources.toList(), target, joiner) }

    fun split(source: String, vararg targets: String) = apply { operations += Split(source, targets.toList()) }

    fun split(source: String, splitter: ValueSplitterStrategy, vararg targets: String) =
        apply { operations += Split(source, targets.toList(), splitter) }

    fun createObject(path: String) = apply { operations += CreateObject(path) }

    fun removeIfEmpty(path: String) = apply { operations += RemoveIfEmpty(path) }

    fun removeIfEmpty(path: String, cascade: Boolean) = apply { operations += RemoveIfEmpty(path, cascade) }

    fun requireExists(path: String) = apply { operations += RequireExists(path) }

    fun requireType(path: String, type: JsonType) = apply { operations += RequireType(path, type) }

    fun transform(path: String, fn: Function<JsonNode, JsonNode>) =
        apply { operations += Transform(path, lenient = false) { fn.apply(this) } }

    fun transformLenient(path: String, fn: Function<JsonNode, JsonNode>) =
        apply { operations += Transform(path, lenient = true) { fn.apply(this) } }

    fun custom(block: Consumer<ObjectNode>) = apply { operations += Custom { node -> block.accept(node) } }

    fun forEach(path: String, steps: Consumer<MigrationSteps>) = apply {
        val nested = MigrationSteps()
        steps.accept(nested)
        operations += ForEach(path, nested.build())
    }
}
