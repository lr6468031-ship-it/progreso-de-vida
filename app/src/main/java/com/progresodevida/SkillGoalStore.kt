package com.progresodevida

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SkillGoalStore(context: Context) {
    private val appContext = context.applicationContext
    private val prefs = StorageGuard.prefs(appContext)
    private val dayFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    private fun load(key: String): JSONArray {
        val raw = StorageGuard.readArrayString(prefs, key)
        return try { JSONArray(raw) } catch (_: Exception) { JSONArray() }
    }

    private fun save(key: String, value: JSONArray) {
        prefs.edit().putString(key, value.toString()).apply()
    }

    fun items(): List<SkillGoal> {
        val source = load("skill_goals")
        val sessions = sessions()
        val result = mutableListOf<SkillGoal>()
        for (i in 0 until source.length()) {
            val item = source.optJSONObject(i) ?: continue
            val id = item.optString("id", "GOAL-$i")
            result += SkillGoal(
                id = id,
                type = item.optString("type", "Habilidad"),
                name = item.optString("name", "Nueva meta"),
                area = item.optString("area", "Otro"),
                target = item.optString("target", ""),
                targetHours = item.optDouble("targetHours", 0.0).coerceAtLeast(0.0),
                deadline = item.optString("deadline", ""),
                notes = item.optString("notes", ""),
                status = item.optString("status", "activo"),
                createdAt = item.optString("createdAt", ""),
                accumulatedHours = sessions.filter { it.goalId == id }.sumOf { it.hours },
                sessionCount = sessions.count { it.goalId == id }
            )
        }
        return result.sortedWith(compareBy<SkillGoal> { it.status == "completado" }.thenBy { it.name.lowercase(Locale.ROOT) })
    }

    fun saveItem(value: SkillGoal): SkillGoal {
        val source = load("skill_goals")
        val id = value.id.ifBlank { "GOAL-${System.currentTimeMillis()}" }
        val created = value.createdAt.ifBlank { dayFormat.format(Date()) }
        val json = JSONObject()
            .put("id", id)
            .put("type", value.type.ifBlank { "Habilidad" })
            .put("name", value.name.trim())
            .put("area", value.area.ifBlank { "Otro" })
            .put("target", value.target.trim())
            .put("targetHours", value.targetHours.coerceAtLeast(0.0))
            .put("deadline", value.deadline.trim())
            .put("notes", value.notes.trim())
            .put("status", value.status.ifBlank { "activo" })
            .put("createdAt", created)

        var replaced = false
        for (i in 0 until source.length()) {
            val current = source.optJSONObject(i) ?: continue
            if (current.optString("id") == id) {
                source.put(i, json)
                replaced = true
                break
            }
        }
        if (!replaced) source.put(json)
        save("skill_goals", source)
        return value.copy(id = id, createdAt = created)
    }

    fun deleteItem(id: String): Boolean {
        val source = load("skill_goals")
        var removed = false
        for (i in source.length() - 1 downTo 0) {
            if (source.optJSONObject(i)?.optString("id") == id) {
                source.remove(i)
                removed = true
            }
        }
        if (removed) save("skill_goals", source)

        val sessionArray = load("skill_sessions")
        for (i in sessionArray.length() - 1 downTo 0) {
            if (sessionArray.optJSONObject(i)?.optString("goalId") == id) sessionArray.remove(i)
        }
        save("skill_sessions", sessionArray)
        return removed
    }

    fun setStatus(id: String, status: String): Boolean {
        val source = load("skill_goals")
        for (i in 0 until source.length()) {
            val item = source.optJSONObject(i) ?: continue
            if (item.optString("id") == id) {
                item.put("status", status)
                source.put(i, item)
                save("skill_goals", source)
                return true
            }
        }
        return false
    }

    fun addSession(goalId: String, hours: Double, note: String, date: String = dayFormat.format(Date())) {
        val cleanHours = hours.coerceAtLeast(0.0)
        if (cleanHours <= 0.0) return
        val goal = items().firstOrNull { it.id == goalId }
        val source = load("skill_sessions")
        source.put(
            JSONObject()
                .put("id", "SESSION-${System.currentTimeMillis()}")
                .put("goalId", goalId)
                .put("date", date)
                .put("hours", cleanHours)
                .put("note", note.trim())
        )
        save("skill_sessions", source)

        if (goal != null && !goal.area.equals("Otro", ignoreCase = true)) {
            ProgressStore(appContext).addHours(
                date = date,
                area = goal.area,
                hours = cleanHours,
                activityNote = "${goal.type}: ${goal.name}${if (note.isBlank()) "" else " · ${note.trim()}"}"
            )
        }
    }

    fun sessions(): List<SkillSession> {
        val source = load("skill_sessions")
        val result = mutableListOf<SkillSession>()
        for (i in 0 until source.length()) {
            val item = source.optJSONObject(i) ?: continue
            result += SkillSession(
                id = item.optString("id", "SESSION-$i"),
                goalId = item.optString("goalId", ""),
                date = item.optString("date", ""),
                hours = item.optDouble("hours", 0.0).coerceAtLeast(0.0),
                note = item.optString("note", "")
            )
        }
        return result.sortedWith(compareByDescending<SkillSession> { it.date }.thenByDescending { it.id })
    }

    fun sessionsFor(goalId: String): List<SkillSession> = sessions().filter { it.goalId == goalId }
}

data class SkillGoal(
    val id: String = "",
    val type: String = "Habilidad",
    val name: String,
    val area: String = "Otro",
    val target: String = "",
    val targetHours: Double = 0.0,
    val deadline: String = "",
    val notes: String = "",
    val status: String = "activo",
    val createdAt: String = "",
    val accumulatedHours: Double = 0.0,
    val sessionCount: Int = 0
) {
    val progressPercent: Int
        get() = if (targetHours <= 0.0) 0 else ((accumulatedHours / targetHours) * 100.0).toInt().coerceIn(0, 100)
}

data class SkillSession(
    val id: String,
    val goalId: String,
    val date: String,
    val hours: Double,
    val note: String = ""
)
