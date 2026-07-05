package com.mosedotten.json.migrator.engine.operation

import com.mosedotten.json.migrator.engine.exception.InvalidJsonPathException

internal class JsonPath private constructor(val πraw: String, internal val segments: List<String>) {
    val leaf = segments.last()

    companion object {
        fun parse(raw: String): JsonPath {
            validatePath(raw)
            return JsonPath(raw, raw.drop(1).split("/").map(::unescape))
        }
    }
}

private fun validatePath(raw: String) {
    if (raw.isEmpty()) throw InvalidJsonPathException(raw, "path must point to a field, not the document root")
    if (!raw.startsWith("/")) throw InvalidJsonPathException(raw, "path must start with '/'")
    validateEscapes(raw)
}

private fun validateEscapes(raw: String) {
    raw.withIndex()
        .firstOrNull { (index, character) -> character == '~' && raw.getOrNull(index + 1) !in listOf('0', '1') }
        ?.let { throw InvalidJsonPathException(raw, "escape sequences must be '~0' for '~' or '~1' for '/'") }
}

private fun unescape(segment: String) = segment.replace("~1", "/").replace("~0", "~")
