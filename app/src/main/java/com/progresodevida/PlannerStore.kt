package com.progresodevida

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class PlannerStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("progreso_vida_data", Context.MODE_PRIVATE)
    private val dayFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    fun todayKey(): String = dayFormat.format(Date())

    fun tasks(): List<RecurringTask> {
        val raw = prefs.getString("recurring_tasks", "[]") ?: "[]"
        val source = try { JSONArray(raw) } catch (_: Exception) { JSONArray() }
        val result = mutableListOf<RecurringTask>()
        for (i in 0 until source.length()) {
            val item = source.optJSONObject(i) ?: continue
            val done = mutableSetOf<String>()
            val arr = item.optJSONArray("completedDates") ?: JSONArray()
            for (j in 0 until arr.length()) arr.optString(j).takeIf { it.isNotBlank() }?.let(done::add)
            result += RecurringTask(
                id = item.optString("id", "TASK-$i"),
                title = item.optString("title", "Nueva tarea"),
                time = item.optString("time", "19:00"),
                recurrence = item.optString("recurrence", "Todos los días"),
                startDate = item.optString("startDate", todayKey()),
                reminder = item.optBoolean("reminder", true),
                completedDates = done
            )
        }
        return result.sortedBy { it.time }
    }

    fun save(task: RecurringTask): RecurringTask {
        val all = tasks().toMutableList()
        val id = task.id.ifBlank { "TASK-${System.currentTimeMillis()}" }
        val clean = task.copy(id = id, title = task.title.trim().ifBlank { "Nueva tarea" })
        val idx = all.indexOfFirst { it.id == id }
        if (idx >= 0) all[idx] = clean else all += clean
        persist(all)
        return clean
    }

    fun delete(id: String) {
        persist(tasks().filterNot { it.id == id })
    }

    fun toggle(id: String, date: String = todayKey()) {
        val all = tasks().toMutableList()
        val idx = all.indexOfFirst { it.id == id }
        if (idx < 0) return
        val task = all[idx]
        val dates = task.completedDates.toMutableSet()
        if (!dates.add(date)) dates.remove(date)
        all[idx] = task.copy(completedDates = dates)
        persist(all)
    }

    fun scheduledFor(task: RecurringTask, cal: Calendar): Boolean {
        val key = dayFormat.format(cal.time)
        if (key < task.startDate) return false
        return when (task.recurrence) {
            "Lun, mié, vie" -> cal.get(Calendar.DAY_OF_WEEK) in setOf(Calendar.MONDAY, Calendar.WEDNESDAY, Calendar.FRIDAY)
            "Mar, jue, sáb" -> cal.get(Calendar.DAY_OF_WEEK) in setOf(Calendar.TUESDAY, Calendar.THURSDAY, Calendar.SATURDAY)
            "Días laborables" -> cal.get(Calendar.DAY_OF_WEEK) !in setOf(Calendar.SATURDAY, Calendar.SUNDAY)
            "Fines de semana" -> cal.get(Calendar.DAY_OF_WEEK) in setOf(Calendar.SATURDAY, Calendar.SUNDAY)
            else -> true
        }
    }

    private fun persist(tasks: List<RecurringTask>) {
        val source = JSONArray()
        tasks.forEach { task ->
            val done = JSONArray()
            task.completedDates.sorted().forEach(done::put)
            source.put(
                JSONObject()
                    .put("id", task.id)
                    .put("title", task.title)
                    .put("time", task.time)
                    .put("recurrence", task.recurrence)
                    .put("startDate", task.startDate)
                    .put("reminder", task.reminder)
                    .put("completedDates", done)
            )
        }
        prefs.edit().putString("recurring_tasks", source.toString()).apply()
    }
}

data class RecurringTask(
    val id: String = "",
    val title: String,
    val time: String = "19:00",
    val recurrence: String = "Todos los días",
    val startDate: String = "",
    val reminder: Boolean = true,
    val completedDates: Set<String> = emptySet()
)
