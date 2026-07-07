package com.mosedotten.json.migrator.engine.operation

import com.mosedotten.json.migrator.engine.exception.InvalidOperationException

internal fun requireAtLeast(count: Int, min: Int, operationName: String, items: String) {
    if (count < min) {
        throw InvalidOperationException("$operationName requires at least $min $items, but got $count")
    }
}
