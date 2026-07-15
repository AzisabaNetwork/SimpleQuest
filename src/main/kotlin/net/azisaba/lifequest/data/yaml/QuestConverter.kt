package net.azisaba.lifequest.data.yaml

import net.azisaba.lifequest.domain.action.Action
import net.azisaba.lifequest.domain.action.ActionSet
import net.azisaba.lifequest.domain.action.ActionType
import net.azisaba.lifequest.domain.data.Icon
import net.azisaba.lifequest.domain.data.Location
import net.azisaba.lifequest.domain.quest.model.AcceptConditions
import net.azisaba.lifequest.domain.quest.model.GameGuide
import net.azisaba.lifequest.domain.quest.model.PlayLimits
import net.azisaba.lifequest.domain.quest.model.QuestRequirement
import net.azisaba.lifequest.domain.quest.model.QuestType
import net.azisaba.lifequest.domain.script.Script

/**
 * Converts YAML-deserialized [QuestDef] objects into domain [QuestType] objects.
 */
object QuestConverter {
    fun toQuestType(
        key: String,
        def: QuestDef,
    ): QuestType {
        val icon = parseIcon(def.icon, def.aura)
        val location = def.location?.let { parseLocation(it) }
        val (minPlayers, maxPlayers) = parseMaxParty(def.options?.maxParty)
        val playLimits = parsePlayLimits(def.options?.limits)
        val acceptConditions = parseAcceptConditions(def.requirements)
        val requirements = parseObjectives(def.objectives)
        val actions = def.actions?.let { parseActions(it) }
        val guides = def.guides?.map { parseGuide(it, location) } ?: emptyList()
        val scripts = def.scripts?.flatMap { parseScripts(it.key, it.value) } ?: emptyList()

        return QuestType(
            key = key,
            title = def.title,
            icon = icon,
            description = def.description,
            category = def.category,
            location = location,
            giver = def.giver,
            playLimits = playLimits,
            acceptConditions = acceptConditions,
            maxPlayers = maxPlayers,
            minPlayers = minPlayers,
            deathLimit = def.options?.deathLimit,
            guides = guides,
            requirements = requirements,
            actions = actions,
            scripts = scripts,
        )
    }

    // -- Icon: "MATERIAL:CMD" --

    private fun parseIcon(
        raw: String,
        aura: Boolean,
    ): Icon {
        val parts = raw.split(":", limit = 2)
        return Icon(
            type = parts[0].trim(),
            customModelData = parts.getOrNull(1)?.trim()?.toIntOrNull(),
            aura = aura,
        )
    }

    // -- Location: "world,x,y,z[,yaw,pitch]" --

    private fun parseLocation(raw: String): Location {
        val parts = raw.split(",", limit = 6)
        return Location(
            world = parts.getOrElse(0) { "world" },
            x = parts.getOrElse(1) { "0" }.toDoubleOrNull() ?: 0.0,
            y = parts.getOrElse(2) { "0" }.toDoubleOrNull() ?: 0.0,
            z = parts.getOrElse(3) { "0" }.toDoubleOrNull() ?: 0.0,
            yaw = parts.getOrElse(4) { "0" }.toFloatOrNull() ?: 0.0f,
            pitch = parts.getOrElse(5) { "0" }.toFloatOrNull() ?: 0.0f,
        )
    }

    // -- MaxParty: "1-4" or "4" --

    private fun parseMaxParty(raw: String?): Pair<Int?, Int?> {
        if (raw == null) return null to null
        val parts = raw.split("-", limit = 2)
        return if (parts.size == 2) {
            parts[0].trim().toIntOrNull() to parts[1].trim().toIntOrNull()
        } else {
            null to raw.trim().toIntOrNull()
        }
    }

    // -- PlayLimits --

    private fun parsePlayLimits(def: PlayLimitDef?): PlayLimits {
        if (def == null) return PlayLimits()
        return PlayLimits(
            daily = def.daily,
            weekly = def.weekly,
            monthly = def.monthly,
            yearly = def.yearly,
            lifetime = def.lifetime,
        )
    }

    // -- AcceptConditions --

    private fun parseAcceptConditions(def: RequirementDef?): AcceptConditions {
        if (def == null) return AcceptConditions()
        return AcceptConditions(
            pveLevel = def.pveLevel,
            money = def.money,
            partyMode = def.partyMode,
        )
    }

    // -- Objectives --

    private fun parseObjectives(raw: Map<String, String>): Map<String, QuestRequirement> =
        raw.mapValues { (key, value) ->
            val amount =
                if (value.contains("*")) {
                    value.substringAfter("*").toIntOrNull() ?: 1
                } else {
                    value.toIntOrNull() ?: 1
                }
            QuestRequirement(key = key, amount = amount)
        }

    // -- Actions --

    private fun parseActions(def: ActionsDef): ActionSet =
        ActionSet(
            onFirstComplete = def.onFirstComplete.map { parseAction(it) },
            onComplete = def.onComplete.map { parseAction(it) },
        )

    private fun parseAction(entry: ActionEntry): Action =
        when (entry.type) {
            "Command" -> {
                Action(type = ActionType.COMMAND, command = entry.params)
            }

            "Item" -> {
                val parts = entry.params.split(",", limit = 2)
                Action(
                    type = ActionType.ITEM_GIVE,
                    material = parts[0].trim(),
                    amount = parts.getOrNull(1)?.trim()?.toIntOrNull() ?: 1,
                )
            }

            "MythicItem" -> {
                val parts = entry.params.split(",", limit = 2)
                Action(
                    type = ActionType.MYTHIC_ITEM_GIVE,
                    item = parts[0].trim(),
                    amount = parts.getOrNull(1)?.trim()?.toIntOrNull() ?: 1,
                )
            }

            "PvELevel" -> {
                Action(type = ActionType.PVELEVEL_EXP, amount = entry.params.toIntOrNull() ?: 0)
            }

            else -> {
                Action(type = ActionType.COMMAND, command = entry.params)
            }
        }

    // -- Guides --

    private fun parseGuide(
        def: GuideDef,
        questLocation: Location?,
    ): GameGuide {
        val location = parseGuideLocation(def.location, questLocation)
        val condition = def.condition?.let { parseCondition(it) } ?: emptyMap()
        return GameGuide(title = def.title, location = location, requirements = condition)
    }

    private fun parseGuideLocation(
        raw: String,
        questLocation: Location?,
    ): Location {
        val parts = raw.split(",", limit = 6)
        val firstIsNumeric = parts.firstOrNull()?.toDoubleOrNull() != null
        return if (parts.size < 4 || firstIsNumeric) {
            Location(
                world = questLocation?.world ?: "world",
                x = parts.getOrElse(0) { "0" }.toDoubleOrNull() ?: 0.0,
                y = parts.getOrElse(1) { "0" }.toDoubleOrNull() ?: 0.0,
                z = parts.getOrElse(2) { "0" }.toDoubleOrNull() ?: 0.0,
            )
        } else {
            parseLocation(raw)
        }
    }

    private fun parseCondition(raw: String): Map<String, Int> {
        val parts = raw.split("=", limit = 2)
        return if (parts.size == 2) {
            mapOf(parts[0].trim() to (parts[1].trim().toIntOrNull() ?: 1))
        } else {
            emptyMap()
        }
    }

    // -- Scripts: "OnStart", "OnStart+20", "OnComplete" --

    private fun parseScripts(
        triggerRaw: String,
        commands: List<String>,
    ): List<Script> {
        val triggerName = triggerRaw.substringBefore("+")
        val delayStr = triggerRaw.substringAfter("+", "")
        val delay = delayStr.toLongOrNull() ?: 0L

        val trigger =
            when (triggerName.lowercase()) {
                "onstart" -> Script.Trigger.START
                "onend" -> Script.Trigger.END
                "oncomplete" -> Script.Trigger.COMPLETE
                "oncancel" -> Script.Trigger.CANCEL
                else -> return emptyList()
            }
        return listOf(Script(trigger = trigger, delay = delay, commands = commands))
    }
}
