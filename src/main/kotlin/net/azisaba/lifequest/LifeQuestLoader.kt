package net.azisaba.lifequest

import com.charleskorn.kaml.Yaml
import jakarta.inject.Inject
import jakarta.inject.Singleton
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.serializer
import net.azisaba.lifequest.data.yaml.QuestConverter
import net.azisaba.lifequest.data.yaml.QuestDef
import net.azisaba.lifequest.registry.DomainQuestTypes
import java.io.File
import java.util.logging.Logger

/**
 * Loads quest definitions from YAML files and registers them as domain [net.azisaba.lifequest.domain.quest.model.QuestType] objects.
 */
@Singleton
class LifeQuestLoader
    @Inject
    constructor(
        private val logger: Logger,
    ) {
        private val namespaceRegex = Regex("^@([a-zA-Z0-9_-]+)$")
        private val fileNameRegex = Regex("^[a-zA-Z0-9_-]+$")
        private val yaml = Yaml.default
        private val mapSerializer: KSerializer<Map<String, QuestDef>> =
            MapSerializer(serializer<String>(), QuestDef.serializer())

        fun loadAll(dataFolder: File) {
            val namespaces = scanNamespaces(dataFolder)
            namespaces.forEach { (namespace, dir) ->
                loadNamespace(namespace, dir)
            }
            logger.info("Loaded ${DomainQuestTypes.size} quest types from ${namespaces.size} namespace(s)")
        }

        private fun scanNamespaces(dataFolder: File): List<Pair<String, File>> {
            if (!dataFolder.exists()) return emptyList()
            return dataFolder
                .listFiles()
                ?.filter { it.isDirectory }
                ?.mapNotNull { dir ->
                    namespaceRegex
                        .find(dir.name)
                        ?.groupValues
                        ?.get(1)
                        ?.let { it to dir }
                } ?: emptyList()
        }

        private fun loadNamespace(
            namespace: String,
            dir: File,
        ) {
            val typesDir = File(dir, "types")
            if (!typesDir.exists()) return
            scanYamlFiles(typesDir).forEach { file ->
                loadQuestFile(namespace, typesDir, file)
            }
        }

        private fun scanYamlFiles(dir: File): List<File> =
            dir
                .walkTopDown()
                .filter { it.isFile }
                .filter { it.extension == "yml" || it.extension == "yaml" }
                .filter { fileNameRegex.matches(it.nameWithoutExtension) }
                .toList()

        private fun loadQuestFile(
            namespace: String,
            baseDir: File,
            file: File,
        ) {
            try {
                val text = file.readText()
                val raw = yaml.decodeFromString(mapSerializer, text)

                raw.forEach { (_, questDef) ->
                    val relativePath = baseDir.toURI().relativize(file.toURI()).path
                    val keyStr = relativePath.substringBeforeLast(".")
                    val fullKey = "$namespace/$keyStr"
                    val questType = QuestConverter.toQuestType(fullKey, questDef)
                    DomainQuestTypes.unregister(fullKey)
                    DomainQuestTypes.register(questType)
                }
            } catch (e: Exception) {
                logger.warning("Failed to load quest file: ${file.name}")
            }
        }
    }
