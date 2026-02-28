package com.matthewjones372.golden.kotlinx

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json

/**
 * Creates a Json instance configured with sensible defaults for golden testing.
 */
@OptIn(ExperimentalSerializationApi::class)
fun createGoldenTestJson(): Json {
    return Json {
        prettyPrint = true
        prettyPrintIndent = "  "
        encodeDefaults = true
    }
}
