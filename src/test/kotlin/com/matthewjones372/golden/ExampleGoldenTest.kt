package com.matthewjones372.golden

import io.kotest.core.spec.style.FunSpec
import io.kotest.property.Arb
import io.kotest.property.arbitrary.bind
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.list
import io.kotest.property.arbitrary.string

/**
 * Example data class for demonstrating golden testing.
 */
data class Person(
    val name: String,
    val age: Int,
    val email: String,
)

/**
 * Example data class with nested structure.
 */
data class Company(
    val name: String,
    val employees: List<Person>,
    val founded: Int
)

/**
 * Example tests demonstrating the golden testing library.
 */
class ExampleGoldenTest : FunSpec({
    val mapper = createGoldenTestObjectMapper()

    test("test person golden codec") {
        goldenCodecTest(
            mapper = mapper,
            arb = Arb.bind(
                Arb.string(1..50),
                Arb.int(0..120),
                Arb.string(5..100),
                ::Person
            ),
            config = GoldenCodecTestConfig(sampleCount = 5)
        )
    }

    test("test company golden codec with nested structure") {
        val personArb = Arb.bind(
            Arb.string(1..50),
            Arb.int(0..120),
            Arb.string(5..100),
            ::Person
        )

        val companyArb = Arb.bind(
            Arb.string(1..100),
            Arb.list(personArb, 0..10),
            Arb.int(1800..2024),
            ::Company
        )

        goldenCodecTest(
            mapper = mapper,
            arb = companyArb,
            config = GoldenCodecTestConfig(sampleCount = 3)
        )
    }

    test("test person with custom config - encoding only") {
        goldenCodecTest(
            mapper = mapper,
            arb = Arb.bind(
                Arb.string(1..50),
                Arb.int(0..120),
                Arb.string(5..100),
                ::Person
            ),
            config = GoldenCodecTestConfig(
                sampleCount = 2,
                resourcePath = "golden/custom",
                testEncoding = true,
                testDecoding = true,
                testRoundTrip = false
            )
        )
    }

    test("test person codec properties - full property testing") {
        codecPropertyTest(
            mapper = mapper,
            arb = Arb.bind(
                Arb.string(1..50),
                Arb.int(0..120),
                Arb.string(5..100),
                ::Person
            ),
            config = CodecPropertyTestConfig(iterations = 1000)
        )
    }
})
