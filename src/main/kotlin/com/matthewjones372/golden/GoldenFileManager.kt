package com.matthewjones372.golden

import com.fasterxml.jackson.core.type.TypeReference
import java.io.File
import kotlin.reflect.KClass

/**
 * Manages golden file storage and retrieval.
 * Golden files are stored in test resources directory with consistent naming.
 */
class GoldenFileManager<T>(
    val typeReference: TypeReference<T>,
    private val typeName: String,
    private val resourcePath: String = "golden"
) {
    companion object {
        /**
         * Creates a GoldenFileManager for a specific type.
         */
        inline fun <reified T> create(
            resourcePath: String = "golden"
        ): GoldenFileManager<T> {
            val typeName = T::class.simpleName ?: "Unknown"
            return GoldenFileManager(
                typeReference = object : TypeReference<T>() {},
                typeName = typeName,
                resourcePath = resourcePath
            )
        }

        /**
         * Creates a GoldenFileManager from a KClass.
         */
        fun <T : Any> fromKClass(
            kClass: KClass<T>,
            resourcePath: String = "golden"
        ): GoldenFileManager<T> {
            val typeName = kClass.simpleName ?: "Unknown"
            return GoldenFileManager(
                typeReference = object : TypeReference<T>() {},
                typeName = typeName,
                resourcePath = resourcePath
            )
        }
    }

    /**
     * Gets the golden file for a specific sample index.
     * Format: src/test/resources/{resourcePath}/{TypeName}_{index}.json
     */
    fun getGoldenFile(index: Int): File {
        val testResourcesDir = File("src/test/resources/$resourcePath")
        return File(testResourcesDir, "${typeName}_${index.toString().padStart(3, '0')}.json")
    }

    /**
     * Gets the "_new" golden file for a specific sample index.
     * This file is created when no golden file exists yet (first run).
     * Format: src/test/resources/{resourcePath}/{TypeName}_{index}_new.json
     */
    fun getNewGoldenFile(index: Int): File {
        val testResourcesDir = File("src/test/resources/$resourcePath")
        return File(testResourcesDir, "${typeName}_${index.toString().padStart(3, '0')}_new.json")
    }

    /**
     * Gets the "_changed" golden file for a specific sample index.
     * This file is created when the current encoding doesn't match the existing golden file.
     * Format: src/test/resources/{resourcePath}/{TypeName}_{index}_changed.json
     */
    fun getChangedGoldenFile(index: Int): File {
        val testResourcesDir = File("src/test/resources/$resourcePath")
        return File(testResourcesDir, "${typeName}_${index.toString().padStart(3, '0')}_changed.json")
    }

    /**
     * Gets all existing golden files for this type.
     */
    fun getAllGoldenFiles(): List<File> {
        val testResourcesDir = File("src/test/resources/$resourcePath")
        if (!testResourcesDir.exists()) {
            return emptyList()
        }

        return testResourcesDir.listFiles { file ->
            file.name.startsWith(typeName) && file.name.endsWith(".json")
        }?.toList() ?: emptyList()
    }

    /**
     * Cleans up all golden files for this type.
     * Useful for regenerating golden files from scratch.
     */
    fun cleanGoldenFiles() {
        getAllGoldenFiles().forEach { it.delete() }
    }
}
