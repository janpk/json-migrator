package com.mosedotten.json.migrator.engine.operation

class Move(private val from: String, private val to: String) : CompositeOperation() {
    private val copy = Copy(from, to)
    private val remove = Remove(from)

    override fun steps(document: Document) = listOf(copy, remove)

    override fun describe() = "move(\"$from\") to \"$to\""
}
