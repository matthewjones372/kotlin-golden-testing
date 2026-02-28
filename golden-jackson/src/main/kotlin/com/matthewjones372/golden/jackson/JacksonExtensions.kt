package com.matthewjones372.golden.jackson

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.module.kotlin.registerKotlinModule

/**
 * Creates a Jackson ObjectMapper configured for Kotlin with sensible defaults for golden testing.
 */
fun createGoldenTestObjectMapper(): ObjectMapper {
    return ObjectMapper().apply {
        registerKotlinModule()
        enable(SerializationFeature.INDENT_OUTPUT)
        disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS)
    }
}
