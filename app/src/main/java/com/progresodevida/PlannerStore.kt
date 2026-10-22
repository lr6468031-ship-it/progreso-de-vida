package com.progresodevida

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class PlannerStore(context: Context) {
    private val prefs = StorageGuard.prefs(context)
    private val dayFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    fun todayKey(): String = dayFormat.format(Date())

    fun tasks(): List<RecurringTask> {
        val raw = StorageGuard.readArrayString(prefs, "recurring_tasks")
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
                startDate = item.optString("startDate", ""),
                endDate = item.optString("endDate", ""),
                goalId = item.optString("goalId", ""),
                reminder = item.optBoolean("reminder", true),
                completedDates = done,
                category = item.optString("category", "Hábitos"),
                durationMinutes = item.optInt("durationMinutes", 0).coerceAtLeast(0),
                timesPerDay = item.optInt("timesPerDay", 1).coerceIn(1, 20),
                missedDates = item.optJSONArray("missedDates").let { a ->
                    (0 until (a?.length() ?: 0)).map { a!!.optString(it) }.filter { it.isNotBlank() }.toSet()
                }
            )
        }
        return result.sortedBy { it.time }
    }

    fun save(task: RecurringTask): RecurringTask {
        val all = tasks().toMutableList()
        val id = task.id.ifBlank { "TASK-${System.currentTimeMillis()}" }
        val clean = task.copy(
            id = id,
            title = task.title.trim().ifBlank { "Nueva tarea" },
            durationMinutes = task.durationMinutes.coerceAtLeast(0),
            timesPerDay = task.timesPerDay.coerceIn(1, 20)
        )
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
        all[idx] = task.copy(completedDates = dates, missedDates = task.missedDates - date)
        persist(all)
    }

    fun mark(id: String, date: String, completed: Boolean) {
        val task = tasks().firstOrNull { it.id == id } ?: return
        save(task.copy(
            completedDates = if (completed) task.completedDates + date else task.completedDates - date,
            missedDates = if (completed) task.missedDates - date else task.missedDates + date
        ))
    }

    fun tasksForGoal(goalId: String): List<RecurringTask> = tasks().filter { it.goalId == goalId }

    fun goalProgress(goalId: String): GoalTaskProgress {
        val linked = tasksForGoal(goalId)
        var total = 0
        var completed = 0
        linked.forEach { task ->
            val start = parseDate(task.startDate)
            val end = parseDate(task.endDate)
            if (start == null || end == null || end.before(start)) {
                completed += task.completedDates.size
                total += task.completedDates.size
                return@forEach
            }
            val cursor = start.clone() as Calendar
            var guard = 0
            while (!cursor.after(end) && guard < 4000) {
                if (scheduledFor(task, cursor)) {
                    total += 1
                    val key = dayFormat.format(cursor.time)
                    if (task.completedDates.contains(key)) completed += 1
                }
                cursor.add(Calendar.DAY_OF_MONTH, 1)
                guard += 1
            }
        }
        return GoalTaskProgress(completed = completed, total = total)
    }

    fun scheduledFor(task: RecurringTask, cal: Calendar): Boolean {
        val weekdays = TaskWeekdays.parse(task.recurrence)
        if (cal.get(Calendar.DAY_OF_WEEK) !in weekdays) return false

        // Hábitos no caducan: viven únicamente en los días de la semana elegidos.
        // Esto hace que la pantalla Hoy y la pantalla Hábitos usen exactamente la misma regla.
        if (task.category.equals("Hábitos", ignoreCase = true)) return true

        val key = dayFormat.format(cal.time)
        if (task.startDate.isNotBlank() && key < task.startDate) return false
        if (task.endDate.isNotBlank() && key > task.endDate) return false
        return true
    }

    private fun parseDate(value: String): Calendar? = try {
        val parsed = dayFormat.parse(value) ?: return null
        Calendar.getInstance().apply { time = parsed }
    } catch (_: Exception) {
        null
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
                    .put("endDate", task.endDate)
                    .put("goalId", task.goalId)
                    .put("reminder", task.reminder)
                    .put("completedDates", done)
                    .put("category", task.category)
                    .put("durationMinutes", task.durationMinutes)
                    .put("timesPerDay", task.timesPerDay)
                    .put("missedDates", JSONArray(task.missedDates.sorted()))
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
    val endDate: String = "",
    val goalId: String = "",
    val reminder: Boolean = true,
    val completedDates: Set<String> = emptySet(),
    val category: String = "Hábitos",
    val durationMinutes: Int = 0,
    val timesPerDay: Int = 1,
    val missedDates: Set<String> = emptySet()
)

data class GoalTaskProgress(
    val completed: Int,
    val total: Int
) {
    val remaining: Int get() = (total - completed).coerceAtLeast(0)
    val percent: Int get() = if (total <= 0) 0 else (completed * 100 / total).coerceIn(0, 100)
}

/** Keeps existing stored presets compatible with arbitrary selected weekdays. */
object TaskWeekdays {
    val days = listOf(
        Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY,
        Calendar.THURSDAY, Calendar.FRIDAY, Calendar.SATURDAY, Calendar.SUNDAY
    )
    val names = listOf("Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado", "Domingo")
    private val labels = listOf("Lun", "mar", "mié", "jue", "vie", "sáb", "dom")

    fun parse(value: String): Set<Int> = when (value) {
        "Todos los días" -> days.toSet()
        "Días laborables" -> days.take(5).toSet()
        "Fines de semana" -> days.takeLast(2).toSet()
        else -> {
            val tokens = value.lowercase(Locale.ROOT).split(",").map { it.trim() }.toSet()
            days.filterIndexed { index, _ -> labels[index].lowercase(Locale.ROOT) in tokens }.toSet()
        }
    }

    fun format(selected: Set<Int>): String = if (days.all { it in selected }) {
        "Todos los días"
    } else {
        days.indices.filter { days[it] in selected }.joinToString(", ") { labels[it] }
    }
}
