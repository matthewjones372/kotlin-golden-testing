package com.matthewjones372.golden.avro

import com.github.avrokotlin.avro4k.Avro
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.checkAll
import kotlinx.serialization.KSerializer

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
 * - Round-trip law: encode -> decode -> encode produces the same Avro binary
 * - Decode stability: decoded value can be re-encoded
 *
 * Use this for comprehensive property testing. Use `goldenCodecTest` for regression testing.
 *
 * @param avro The Avro instance to use for serialization/deserialization
 * @param serializer The KSerializer for type T
 * @param arb The Kotest Arb generator for producing test values
 * @param config Configuration for the property test
 *
 * Usage:
 * ```
 * test("test person codec properties") {
 *     codecPropertyTest(
 *         avro = Avro,
 *         serializer = Person.serializer(),
 *         arb = Arb.bind(Arb.string(), Arb.int(), ::Person)
 *     )
 * }
 * ```
 */
suspend fun <T> codecPropertyTest(
    avro: Avro,
    serializer: KSerializer<T>,
    arb: Arb<T>,
    config: CodecPropertyTestConfig = CodecPropertyTestConfig()
) {
    checkAll(config.iterations, arb) { value ->
        // Test round-trip: encode -> decode -> encode should produce same binary
        val bytes1 = avro.encodeToByteArray(serializer, value)
        val decoded: T = avro.decodeFromByteArray(serializer, bytes1)
        val bytes2 = avro.encodeToByteArray(serializer, decoded)

        bytes1.contentEquals(bytes2) shouldBe true
    }
}
