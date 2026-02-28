package com.matthewjones372.golden.avro

import com.github.avrokotlin.avro4k.Avro
import kotlinx.serialization.Serializable
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

@Serializable
data class SimplePerson(val name: String, val age: Int)

class SimpleAvroTest {
    @Test
    fun `test avro encode decode`() {
        val person = SimplePerson("Alice", 30)
        val bytes = Avro.encodeToByteArray(person)
        val decoded = Avro.decodeFromByteArray<SimplePerson>(bytes)
        assertEquals(person, decoded)
    }
}
