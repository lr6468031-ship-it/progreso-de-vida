package com.progresodevida

import android.app.Activity
import android.app.AlertDialog
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.GridLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.TextView
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class PlannerActivity : Activity() {
    private lateinit var store: PlannerStore
    private lateinit var body: LinearLayout
    private var month: Calendar = Calendar.getInstance().apply { set(Calendar.DAY_OF_MONTH, 1) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        store = PlannerStore(this)
        window.statusBarColor = Color.WHITE
        window.navigationBarColor = Color.WHITE
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        setContentView(buildScreen())
        render()
    }

    override fun onResume() {
        super.onResume()
        if (::body.isInitialized) render()
    }

    private fun buildScreen(): View {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(UiKit.bg)
        }
        val scroll = ScrollView(this).apply { isFillViewport = true }
        body = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(18), dp(18), dp(40))
        }
        scroll.addView(body, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        root.addView(scroll, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
        return root
    }

    private fun render() {
        body.removeAllViews()
        val tasks = store.tasks()
        val today = store.todayKey()

        val top = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        top.addView(Button(this).apply {
            text = "‹"
            textSize = 28f
            setTextColor(UiKit.ink)
            background = UiKit.rounded(this@PlannerActivity, Color.TRANSPARENT, 16)
            setOnClickListener { finish() }
        }, LinearLayout.LayoutParams(dp(52), dp(52)))
        top.addView(LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addView(UiKit.label(this@PlannerActivity, "Tareas", 31f, UiKit.ink, true))
            addView(UiKit.label(this@PlannerActivity, "recurrentes", 31f, UiKit.blue, true))
        }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        body.addView(top)

        body.addView(UiKit.label(this, "Organiza hábitos y tareas que se repiten. Toca el círculo para marcar el día como completado.", 13f, UiKit.muted), marginTop(6))
        body.addView(Button(this).apply {
            text = "+ Nueva tarea recurrente"
            isAllCaps = false
            textSize = 14f
            setTextColor(Color.WHITE)
            setTypeface(typeface, Typeface.BOLD)
            background = UiKit.rounded(this@PlannerActivity, UiKit.blue, 16)
            stateListAnimator = null
            setOnClickListener { editTask(null) }
        }, marginTop(16))

        body.addView(UiKit.section(this, "Hoy"), marginTop(24))
        val scheduled = tasks.filter { store.scheduledFor(it, Calendar.getInstance()) }
        if (scheduled.isEmpty()) {
            body.addView(emptyCard("No tienes tareas recurrentes para hoy."), marginTop(10))
        } else {
            scheduled.forEach { body.addView(taskCard(it, today), marginTop(10)) }
        }

        body.addView(UiKit.section(this, "Esta semana"), marginTop(24))
        body.addView(weekCard(tasks), marginTop(10))

        body.addView(UiKit.section(this, "Recordatorios inteligentes"), marginTop(24))
        body.addView(reminderCard(scheduled), marginTop(10))

        body.addView(UiKit.section(this, "Calendario"), marginTop(24))
        body.addView(calendarCard(tasks), marginTop(10))
    }

    private fun taskCard(task: RecurringTask, date: String): View {
        val done = task.completedDates.contains(date)
        return UiKit.card(this).apply {
            val row = LinearLayout(this@PlannerActivity).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
            val check = TextView(this@PlannerActivity).apply {
                text = if (done) "✓" else ""
                gravity = Gravity.CENTER
                textSize = 20f
                setTextColor(Color.WHITE)
                background = UiKit.rounded(this@PlannerActivity, if (done) UiKit.green else Color.WHITE, 24, if (done) UiKit.green else UiKit.line, 2)
                setOnClickListener { store.toggle(task.id, date); render() }
            }
            row.addView(check, squareEnd(46, 46, 12))
            row.addView(LinearLayout(this@PlannerActivity).apply {
                orientation = LinearLayout.VERTICAL
                addView(UiKit.label(this@PlannerActivity, task.title, 17f, UiKit.ink, true))
                addView(UiKit.label(this@PlannerActivity, "${task.time}  ·  ${task.recurrence}", 12f, UiKit.muted))
            }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
            row.addView(TextView(this@PlannerActivity).apply {
                text = "⋮"
                textSize = 28f
                gravity = Gravity.CENTER
                setTextColor(UiKit.muted)
                setOnClickListener { taskMenu(task) }
            }, LinearLayout.LayoutParams(dp(40), dp(46)))
            addView(row)

            val chips = LinearLayout(this@PlannerActivity).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(0, dp(12), 0, 0)
            }
            chips.addView(UiKit.pill(this@PlannerActivity, "📅 ${prettyDate(task.startDate)}"))
            chips.addView(UiKit.pill(this@PlannerActivity, "⏰ ${task.time}"), wrapStart(8))
            if (task.reminder) chips.addView(UiKit.pill(this@PlannerActivity, "🔔", UiKit.blueSoft, UiKit.blue), wrapStart(8))
            addView(chips)
        }
    }

    private fun weekCard(tasks: List<RecurringTask>): View = UiKit.card(this).apply {
        val start = Calendar.getInstance().apply {
            firstDayOfWeek = Calendar.MONDAY
            while (get(Calendar.DAY_OF_WEEK) != Calendar.MONDAY) add(Calendar.DAY_OF_MONTH, -1)
        }
        val row = LinearLayout(this@PlannerActivity).apply { orientation = LinearLayout.HORIZONTAL }
        repeat(7) { offset ->
            val day = (start.clone() as Calendar).apply { add(Calendar.DAY_OF_MONTH, offset) }
            val key = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(day.time)
            val scheduled = tasks.filter { store.scheduledFor(it, day) }
            val allDone = scheduled.isNotEmpty() && scheduled.all { it.completedDates.contains(key) }
            val missed = key < store.todayKey() && scheduled.isNotEmpty() && !allDone
            val color = when {
                allDone -> UiKit.green
                missed -> UiKit.red
                else -> UiKit.line
            }
            val cell = LinearLayout(this@PlannerActivity).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                addView(UiKit.label(this@PlannerActivity, SimpleDateFormat("EEE", Locale("es", "PE")).format(day.time).take(3), 10f, UiKit.muted, true))
                addView(UiKit.label(this@PlannerActivity, day.get(Calendar.DAY_OF_MONTH).toString(), 12f, UiKit.ink, true))
                addView(TextView(this@PlannerActivity).apply {
                    text = if (allDone) "✓" else ""
                    gravity = Gravity.CENTER
                    setTextColor(Color.WHITE)
                    background = UiKit.rounded(this@PlannerActivity, color, 22, color)
                }, squareTop(36, 36, 7))
            }
            row.addView(cell, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        }
        addView(row)
    }

    private fun reminderCard(todayTasks: List<RecurringTask>): View {
        val next = todayTasks.filterNot { it.completedDates.contains(store.todayKey()) }.minByOrNull { it.time }
        return UiKit.card(this).apply {
            setPadding(dp(18), dp(18), dp(18), dp(18))
            val row = LinearLayout(this@PlannerActivity).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
            row.addView(TextView(this@PlannerActivity).apply {
                text = "🔔"
                textSize = 28f
                gravity = Gravity.CENTER
                background = UiKit.rounded(this@PlannerActivity, UiKit.orangeSoft, 16)
            }, squareEnd(54, 54, 12))
            row.addView(LinearLayout(this@PlannerActivity).apply {
                orientation = LinearLayout.VERTICAL
                addView(UiKit.label(this@PlannerActivity, next?.title ?: "Todo al día", 16f, UiKit.ink, true))
                addView(UiKit.label(this@PlannerActivity, if (next == null) "No tienes pendientes ahora" else "Próximo recordatorio · ${next.time}", 12f, UiKit.muted))
            }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
            addView(row)
        }
    }

    private fun calendarCard(tasks: List<RecurringTask>): View = UiKit.card(this).apply {
        val header = LinearLayout(this@PlannerActivity).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        header.addView(Button(this@PlannerActivity).apply {
            text = "‹"
            textSize = 24f
            background = UiKit.rounded(this@PlannerActivity, Color.TRANSPARENT, 12)
            setOnClickListener { month.add(Calendar.MONTH, -1); render() }
        }, LinearLayout.LayoutParams(dp(44), dp(44)))
        header.addView(UiKit.label(this@PlannerActivity, monthName(month), 18f, UiKit.ink, true).apply { gravity = Gravity.CENTER }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        header.addView(Button(this@PlannerActivity).apply {
            text = "›"
            textSize = 24f
            background = UiKit.rounded(this@PlannerActivity, Color.TRANSPARENT, 12)
            setOnClickListener { month.add(Calendar.MONTH, 1); render() }
        }, LinearLayout.LayoutParams(dp(44), dp(44)))
        addView(header)

        val grid = GridLayout(this@PlannerActivity).apply {
            columnCount = 7
            rowCount = 7
            setPadding(0, dp(10), 0, 0)
        }
        listOf("L", "M", "X", "J", "V", "S", "D").forEach { d ->
            val params = GridLayout.LayoutParams().apply {
                width = 0
                height = dp(34)
                columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
            }
            grid.addView(UiKit.label(this@PlannerActivity, d, 11f, UiKit.muted, true).apply { gravity = Gravity.CENTER }, params)
        }
        val first = month.clone() as Calendar
        val mondayIndex = (first.get(Calendar.DAY_OF_WEEK) + 5) % 7
        repeat(mondayIndex) {
            val params = GridLayout.LayoutParams().apply {
                width = 0
                height = dp(46)
                columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
            }
            grid.addView(TextView(this@PlannerActivity), params)
        }
        val max = month.getActualMaximum(Calendar.DAY_OF_MONTH)
        for (dayNum in 1..max) {
            val cal = (month.clone() as Calendar).apply { set(Calendar.DAY_OF_MONTH, dayNum) }
            val key = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(cal.time)
            val scheduled = tasks.filter { store.scheduledFor(it, cal) }
            val allDone = scheduled.isNotEmpty() && scheduled.all { it.completedDates.contains(key) }
            val anyMissed = scheduled.isNotEmpty() && key < store.todayKey() && !allDone
            val isToday = key == store.todayKey()
            val fill = when {
                allDone -> UiKit.green
                anyMissed -> UiKit.red
                else -> Color.TRANSPARENT
            }
            val textColor = if (allDone || anyMissed) Color.WHITE else UiKit.ink
            val cell = TextView(this@PlannerActivity).apply {
                text = dayNum.toString()
                textSize = 12f
                gravity = Gravity.CENTER
                setTextColor(textColor)
                val stroke = if (isToday && fill == Color.TRANSPARENT) UiKit.blue else if (fill == Color.TRANSPARENT) Color.TRANSPARENT else fill
                background = UiKit.rounded(this@PlannerActivity, fill, 22, stroke, if (isToday) 2 else 1)
            }
            val params = GridLayout.LayoutParams().apply {
                width = 0
                height = dp(46)
                columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                setMargins(dp(2), dp(2), dp(2), dp(2))
            }
            grid.addView(cell, params)
        }
        addView(grid)
    }

    private fun taskMenu(task: RecurringTask) {
        AlertDialog.Builder(this)
            .setTitle(task.title)
            .setItems(arrayOf("Editar", "Eliminar")) { _, which ->
                if (which == 0) editTask(task) else {
                    store.delete(task.id)
                    render()
                }
            }
            .setNegativeButton("Cerrar", null)
            .show()
    }

    private fun editTask(existing: RecurringTask?) {
        val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20), dp(4), dp(20), dp(4)) }
        val title = EditText(this).apply { hint = "Nombre de la tarea"; setText(existing?.title.orEmpty()); inputType = InputType.TYPE_CLASS_TEXT }
        val time = EditText(this).apply { hint = "Hora · 19:00"; setText(existing?.time ?: "19:00"); inputType = InputType.TYPE_CLASS_DATETIME }
        val recurrenceOptions = listOf("Todos los días", "Días laborables", "Fines de semana", "Lun, mié, vie", "Mar, jue, sáb")
        val recurrence = Spinner(this).apply {
            adapter = ArrayAdapter(this@PlannerActivity, android.R.layout.simple_spinner_dropdown_item, recurrenceOptions)
            val idx = recurrenceOptions.indexOf(existing?.recurrence)
            if (idx >= 0) setSelection(idx)
        }
        val categories = listOf("Deporte", "Música", "Hábitos", "Trabajo", "Metas", "Vida social")
        val category = Spinner(this).apply {
            adapter = ArrayAdapter(this@PlannerActivity, android.R.layout.simple_spinner_dropdown_item, categories)
            setSelection(categories.indexOf(existing?.category ?: "Hábitos").coerceAtLeast(0))
        }
        val duration = EditText(this).apply {
            hint = "Duración en minutos (opcional)"
            inputType = InputType.TYPE_CLASS_NUMBER
            if ((existing?.durationMinutes ?: 0) > 0) setText(existing!!.durationMinutes.toString())
        }
        val reminder = CheckBox(this).apply { text = "Activar recordatorio"; isChecked = existing?.reminder ?: true }
        box.addView(title)
        box.addView(time)
        box.addView(category)
        box.addView(duration)
        box.addView(UiKit.label(this, "Repetir", 12f, UiKit.muted, true))
        box.addView(recurrence)
        box.addView(reminder)

        val dialog = AlertDialog.Builder(this)
            .setTitle(if (existing == null) "Nueva tarea recurrente" else "Editar tarea")
            .setView(box)
            .setPositiveButton("Guardar", null)
            .setNegativeButton("Cancelar", null)
            .create()
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                if (title.text.toString().trim().isBlank()) {
                    title.error = "Escribe una tarea"
                    return@setOnClickListener
                }
                store.save(
                    RecurringTask(
                        id = existing?.id.orEmpty(),
                        title = title.text.toString(),
                        time = time.text.toString().trim().ifBlank { "19:00" },
                        recurrence = recurrence.selectedItem.toString(),
                        startDate = existing?.startDate ?: store.todayKey(),
                        reminder = reminder.isChecked,
                        completedDates = existing?.completedDates ?: emptySet(),
                        missedDates = existing?.missedDates ?: emptySet(),
                        category = category.selectedItem.toString(),
                        durationMinutes = duration.text.toString().toIntOrNull()?.coerceIn(0, 1440) ?: 0
                    )
                )
                dialog.dismiss()
                render()
            }
        }
        dialog.show()
    }

    private fun emptyCard(text: String): View = UiKit.card(this).apply {
        addView(UiKit.label(this@PlannerActivity, text, 13f, UiKit.muted))
    }

    private fun monthName(cal: Calendar): String = SimpleDateFormat("MMMM yyyy", Locale("es", "PE"))
        .format(cal.time)
        .replaceFirstChar { it.titlecase(Locale("es", "PE")) }

    private fun prettyDate(value: String): String = try {
        val src = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val out = SimpleDateFormat("d MMM yyyy", Locale("es", "PE"))
        out.format(src.parse(value)!!)
    } catch (_: Exception) {
        value
    }

    private fun marginTop(value: Int): LinearLayout.LayoutParams {
        val params = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        params.topMargin = dp(value)
        return params
    }

    private fun squareEnd(width: Int, height: Int, end: Int): LinearLayout.LayoutParams {
        val params = LinearLayout.LayoutParams(dp(width), dp(height))
        params.marginEnd = dp(end)
        return params
    }

    private fun squareTop(width: Int, height: Int, top: Int): LinearLayout.LayoutParams {
        val params = LinearLayout.LayoutParams(dp(width), dp(height))
        params.topMargin = dp(top)
        return params
    }

    private fun wrapStart(start: Int): LinearLayout.LayoutParams {
        val params = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        params.marginStart = dp(start)
        return params
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
