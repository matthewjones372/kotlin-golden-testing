package com.matthewjones372.golden.kotlinx

import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.checkAll
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json

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
 * @param json The Json instance to use for serialization/deserialization
 * @param serializer The KSerializer for type T
 * @param arb The Kotest Arb generator for producing test values
 * @param config Configuration for the property test
 *
 * Usage:
 * ```
 * test("test person codec properties") {
 *     codecPropertyTest(
 *         json = Json { prettyPrint = true },
 *         serializer = Person.serializer(),
 *         arb = Arb.bind(Arb.string(), Arb.int(), ::Person)
 *     )
 * }
 * ```
 */
suspend fun <T> codecPropertyTest(
    json: Json,
    serializer: KSerializer<T>,
    arb: Arb<T>,
    config: CodecPropertyTestConfig = CodecPropertyTestConfig()
) {
    checkAll(config.iterations, arb) { value ->
        // Test round-trip: encode -> decode -> encode should produce same JSON
        val json1 = json.encodeToString(serializer, value)
        val decoded: T = json.decodeFromString(serializer, json1)
        val json2 = json.encodeToString(serializer, decoded)

        // Normalize JSON to ignore insignificant differences
        val element1 = json.parseToJsonElement(json1)
        val element2 = json.parseToJsonElement(json2)
        val normalized1 = json.encodeToString(kotlinx.serialization.json.JsonElement.serializer(), element1)
        val normalized2 = json.encodeToString(kotlinx.serialization.json.JsonElement.serializer(), element2)

        normalized1 shouldBe normalized2
    }
}
