package com.progresodevida

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray

object StorageGuard {
    private const val PREFS = "progreso_vida_data"
    private val jsonArrayKeys = setOf("days", "skill_goals", "skill_sessions", "recurring_tasks")

    fun prefs(context: Context): SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun repair(context: Context) {
        val prefs = prefs(context)
        val all = try { prefs.all } catch (_: Exception) { emptyMap<String, Any?>() }
        val editor = prefs.edit()
        var changed = false

        jsonArrayKeys.forEach { key ->
            val value = all[key] ?: return@forEach
            val valid = value is String && try {
                JSONArray(value)
                true
            } catch (_: Exception) {
                false
            }
            if (!valid) {
                editor.remove(key)
                changed = true
            }
        }

        if (changed) editor.commit()
    }

    fun readArrayString(prefs: SharedPreferences, key: String): String {
        val value = try { prefs.all[key] } catch (_: Exception) { null }
        if (value !is String) return "[]"
        return try {
            JSONArray(value)
            value
        } catch (_: Exception) {
            "[]"
        }
    }
}
