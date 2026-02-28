package com.matthewjones372.golden.jackson

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.checkAll

/**
 * Configuration for property-based codec tests.
 */
data class CodecPropertyTestConfig(
    val iterations: Int = 1000
)

/**
 * Full property-based testing of codec laws WITHOUT generating golden files.
 *
 * This tests codec correctness with many iterations (default 1000), checking:
 * - Round-trip law: encode -> decode -> encode produces the same JSON
 * - Decode stability: decoded value can be re-encoded
 *
 * Use this for comprehensive property testing. Use `goldenCodecTest` for regression testing.
 *
 * @param mapper The Jackson ObjectMapper to use for serialization/deserialization
 * @param arb The Kotest Arb generator for producing test values
 * @param config Configuration for the property test
 *
 * Usage:
 * ```
 * test("test person codec properties") {
 *     codecPropertyTest(
 *         mapper = createGoldenTestObjectMapper(),
 *         arb = Arb.bind(Arb.string(), Arb.int(), ::Person)
 *     )
 * }
 * ```
 */
suspend inline fun <reified T> codecPropertyTest(
    mapper: ObjectMapper,
    arb: Arb<T>,
    config: CodecPropertyTestConfig = CodecPropertyTestConfig()
) {
    val typeReference = object : TypeReference<T>() {}

    checkAll(config.iterations, arb) { value ->
        // Test round-trip: encode -> decode -> encode should produce same JSON
        val json1 = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(value)
        val decoded: T = mapper.readValue(json1, typeReference)
        val json2 = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(decoded)

        // Normalize JSON to ignore insignificant whitespace differences
        val normalized1 = mapper.readTree(json1).toString()
        val normalized2 = mapper.readTree(json2).toString()

        normalized1 shouldBe normalized2
    }
}
