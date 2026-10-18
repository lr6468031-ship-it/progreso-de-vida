package com.progresodevida

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class ProgressStore(context: Context) {
    private val prefs = StorageGuard.prefs(context)
    private val dayFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { isLenient = false }

    fun todayKey(): String = dayFormat.format(Date())

    private fun loadDays(): JSONArray {
        val raw = StorageGuard.readArrayString(prefs, "days")
        return try { JSONArray(raw) } catch (_: Exception) { JSONArray() }
    }

    private fun saveDays(array: JSONArray) {
        prefs.edit().putString("days", array.toString()).apply()
    }

    fun day(date: String): ProgressDay? = allDays().firstOrNull { it.date == date }

    fun allDays(): List<ProgressDay> {
        val source = loadDays()
        val result = mutableListOf<ProgressDay>()
        for (i in 0 until source.length()) {
            val item = source.optJSONObject(i) ?: continue
            result += ProgressDay(
                date = item.optString("date", ""),
                exerciseHours = item.optDouble("exerciseHours", 0.0).coerceAtLeast(0.0),
                musicHours = item.optDouble("musicHours", 0.0).coerceAtLeast(0.0),
                socialHours = item.optDouble("socialHours", 0.0).coerceAtLeast(0.0),
                workHours = item.optDouble("workHours", 0.0).coerceAtLeast(0.0),
                did = item.optString("did", ""),
                feeling = item.optString("feeling", ""),
                mood = item.optInt("mood", 0).coerceIn(0, 5),
                photoUri = if (item.isNull("photoUri")) null else item.optString("photoUri", "").ifBlank { null },
                updatedAt = item.optLong("updatedAt", 0L)
            )
        }
        return result.filter { parseDate(it.date) != null }.sortedByDescending { it.date }
    }

    fun saveDay(day: ProgressDay) {
        val source = loadDays()
        val json = JSONObject()
            .put("date", day.date)
            .put("exerciseHours", day.exerciseHours.coerceAtLeast(0.0))
            .put("musicHours", day.musicHours.coerceAtLeast(0.0))
            .put("socialHours", day.socialHours.coerceAtLeast(0.0))
            .put("workHours", day.workHours.coerceAtLeast(0.0))
            .put("did", day.did.trim())
            .put("feeling", day.feeling.trim())
            .put("mood", day.mood.coerceIn(0, 5))
            .put("photoUri", day.photoUri ?: JSONObject.NULL)
            .put("updatedAt", System.currentTimeMillis())

        var found = false
        for (i in 0 until source.length()) {
            val current = source.optJSONObject(i) ?: continue
            if (current.optString("date") == day.date) {
                source.put(i, json)
                found = true
                break
            }
        }
        if (!found) source.put(json)
        saveDays(source)
    }

    fun addHours(date: String, area: String, hours: Double, activityNote: String = "") {
        if (hours <= 0.0) return
        val current = day(date) ?: ProgressDay(date = date)
        val updated = when (area.trim().lowercase(Locale.ROOT)) {
            "ejercicio" -> current.copy(exerciseHours = current.exerciseHours + hours)
            "musica", "música" -> current.copy(musicHours = current.musicHours + hours)
            "vida social", "social" -> current.copy(socialHours = current.socialHours + hours)
            "trabajo" -> current.copy(workHours = current.workHours + hours)
            else -> current
        }
        val withNote = if (activityNote.isBlank()) updated else updated.copy(
            did = listOf(updated.did, activityNote.trim()).filter { it.isNotBlank() }.joinToString(" · ")
        )
        saveDay(withNote)
    }

    fun deleteDay(date: String): Boolean {
        val source = loadDays()
        for (i in 0 until source.length()) {
            if (source.optJSONObject(i)?.optString("date") == date) {
                source.remove(i)
                saveDays(source)
                return true
            }
        }
        return false
    }

    fun range(startInclusive: Calendar, endExclusive: Calendar): List<ProgressDay> {
        val start = startOfDay(startInclusive).timeInMillis
        val end = startOfDay(endExclusive).timeInMillis
        return allDays().filter {
            val value = parseDate(it.date)?.time ?: return@filter false
            value >= start && value < end
        }
    }

    fun summary(startInclusive: Calendar, endExclusive: Calendar): ProgressSummary {
        val start = startOfDay(startInclusive)
        val requestedEnd = startOfDay(endExclusive)
        val tomorrow = startOfDay(Calendar.getInstance()).apply { add(Calendar.DAY_OF_MONTH, 1) }
        val end = if (requestedEnd.after(tomorrow)) tomorrow else requestedEnd
        if (!end.after(start)) return ProgressSummary.empty()

        val entries = range(start, end)
        val totalDays = daysBetween(start, end).coerceAtLeast(1)
        val activeDays = entries.count { it.totalHours > 0.0 || it.did.isNotBlank() || it.feeling.isNotBlank() || it.mood > 0 }
        val moodEntries = entries.filter { it.mood > 0 }
        val totals = linkedMapOf(
            "Ejercicio" to entries.sumOf { it.exerciseHours },
            "Música" to entries.sumOf { it.musicHours },
            "Vida social" to entries.sumOf { it.socialHours },
            "Trabajo" to entries.sumOf { it.workHours }
        )
        val best = totals.maxByOrNull { it.value }?.takeIf { it.value > 0.0 }?.key
        return ProgressSummary(
            totalHours = totals.values.sum(),
            exerciseHours = totals.getValue("Ejercicio"),
            musicHours = totals.getValue("Música"),
            socialHours = totals.getValue("Vida social"),
            workHours = totals.getValue("Trabajo"),
            activeDays = activeDays,
            inactiveDays = (totalDays - activeDays).coerceAtLeast(0),
            totalDays = totalDays,
            averageMood = if (moodEntries.isEmpty()) 0.0 else moodEntries.map { it.mood }.average(),
            bestCategory = best,
            currentStreak = currentStreak(end),
            entries = entries
        )
    }

    private fun currentStreak(endExclusive: Calendar): Int {
        val active = allDays().filter { it.totalHours > 0.0 || it.did.isNotBlank() || it.feeling.isNotBlank() || it.mood > 0 }
            .map { it.date }.toSet()
        var cursor = startOfDay(endExclusive).apply { add(Calendar.DAY_OF_MONTH, -1) }
        var streak = 0
        while (active.contains(dayFormat.format(cursor.time))) {
            streak++
            cursor.add(Calendar.DAY_OF_MONTH, -1)
        }
        return streak
    }

    fun startOfWeek(reference: Calendar = Calendar.getInstance()): Calendar = startOfDay(reference).apply {
        firstDayOfWeek = Calendar.MONDAY
        while (get(Calendar.DAY_OF_WEEK) != Calendar.MONDAY) add(Calendar.DAY_OF_MONTH, -1)
    }

    fun startOfMonth(reference: Calendar = Calendar.getInstance()): Calendar = startOfDay(reference).apply {
        set(Calendar.DAY_OF_MONTH, 1)
    }

    fun startOfYear(reference: Calendar = Calendar.getInstance()): Calendar = startOfDay(reference).apply {
        set(Calendar.DAY_OF_YEAR, 1)
    }

    fun dateKey(calendar: Calendar): String = dayFormat.format(calendar.time)

    fun parseDate(value: String): Date? = try { dayFormat.parse(value) } catch (_: Exception) { null }

    fun calendarFor(value: String): Calendar? = parseDate(value)?.let { date ->
        Calendar.getInstance().apply { time = date }
    }

    fun startOfDay(base: Calendar): Calendar = (base.clone() as Calendar).apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }

    private fun daysBetween(start: Calendar, end: Calendar): Int {
        var count = 0
        val cursor = startOfDay(start)
        val limit = startOfDay(end)
        while (cursor.before(limit)) {
            count++
            cursor.add(Calendar.DAY_OF_MONTH, 1)
        }
        return count
    }
}

data class ProgressDay(
    val date: String,
    val exerciseHours: Double = 0.0,
    val musicHours: Double = 0.0,
    val socialHours: Double = 0.0,
    val workHours: Double = 0.0,
    val did: String = "",
    val feeling: String = "",
    val mood: Int = 0,
    val photoUri: String? = null,
    val updatedAt: Long = 0L
) {
    val totalHours: Double get() = exerciseHours + musicHours + socialHours + workHours
}

data class ProgressSummary(
    val totalHours: Double,
    val exerciseHours: Double,
    val musicHours: Double,
    val socialHours: Double,
    val workHours: Double,
    val activeDays: Int,
    val inactiveDays: Int,
    val totalDays: Int,
    val averageMood: Double,
    val bestCategory: String?,
    val currentStreak: Int,
    val entries: List<ProgressDay>
) {
    companion object {
        fun empty() = ProgressSummary(0.0, 0.0, 0.0, 0.0, 0.0, 0, 0, 0, 0.0, null, 0, emptyList())
    }
}
