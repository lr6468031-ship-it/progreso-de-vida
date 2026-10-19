package com.progresodevida

import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.GridLayout
import android.widget.LinearLayout
import android.widget.NumberPicker
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
                addView(UiKit.label(this@PlannerActivity, "${formatTime12(task.time)}  ·  ${task.recurrence}", 12f, UiKit.muted))
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
            chips.addView(UiKit.pill(this@PlannerActivity, "⏰ ${formatTime12(task.time)}"), wrapStart(8))
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
                addView(UiKit.label(this@PlannerActivity, if (next == null) "No tienes pendientes ahora" else "Próximo recordatorio · ${formatTime12(next.time)}", 12f, UiKit.muted))
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
        val initialTime = parseTime24(existing?.time ?: "19:00")
        val initialHour24 = initialTime.first
        val initialMinute = initialTime.second
        val initialHour12 = when (val hour = initialHour24 % 12) {
            0 -> 12
            else -> hour
        }
        var isPm = initialHour24 >= 12

        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)

        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(20), dp(20), dp(18))
            background = UiKit.rounded(this@PlannerActivity, UiKit.surface, 24, UiKit.line)
        }

        card.addView(UiKit.label(this, if (existing == null) "Nueva tarea recurrente" else "Editar tarea", 24f, UiKit.ink, true))
        card.addView(UiKit.label(this, "Configura el hábito con el mismo estilo de Proyecto de vida.", 12f, UiKit.muted), marginTop(4))

        val title = EditText(this).apply {
            hint = "Nombre de la tarea"
            setText(existing?.title.orEmpty())
            inputType = InputType.TYPE_CLASS_TEXT
            setTextColor(UiKit.ink)
            setHintTextColor(UiKit.muted)
            textSize = 16f
            setPadding(dp(14), 0, dp(14), 0)
            background = UiKit.rounded(this@PlannerActivity, Color.WHITE, 14, UiKit.line)
        }
        card.addView(UiKit.label(this, "Nombre de la tarea", 12f, UiKit.muted, true), marginTop(18))
        card.addView(title, fixedTop(56, 7))

        card.addView(UiKit.label(this, "Hora de inicio", 12f, UiKit.muted, true), marginTop(18))
        card.addView(UiKit.label(this, "Desliza hora y minutos; no necesitas escribir.", 11f, UiKit.muted), marginTop(3))

        val startHour = wheelPicker(1, 12, initialHour12)
        val startMinute = wheelPicker(0, 59, initialMinute)
        val startRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(dp(8), dp(4), dp(8), dp(4))
            background = UiKit.rounded(this@PlannerActivity, UiKit.bg, 18, UiKit.line)
            addView(startHour, LinearLayout.LayoutParams(0, dp(112), 1f))
            addView(UiKit.label(this@PlannerActivity, ":", 28f, UiKit.ink, true).apply { gravity = Gravity.CENTER }, LinearLayout.LayoutParams(dp(28), dp(112)))
            addView(startMinute, LinearLayout.LayoutParams(0, dp(112), 1f))
        }
        card.addView(startRow, marginTop(8))

        val amButton = periodButton("a. m.")
        val pmButton = periodButton("p. m.")
        fun refreshPeriodButtons() {
            stylePeriodButton(amButton, !isPm)
            stylePeriodButton(pmButton, isPm)
        }
        amButton.setOnClickListener { isPm = false; refreshPeriodButtons() }
        pmButton.setOnClickListener { isPm = true; refreshPeriodButtons() }
        refreshPeriodButtons()

        val periodRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            addView(amButton, LinearLayout.LayoutParams(0, dp(48), 1f).apply { marginEnd = dp(6) })
            addView(pmButton, LinearLayout.LayoutParams(0, dp(48), 1f).apply { marginStart = dp(6) })
        }
        card.addView(periodRow, marginTop(8))

        val categories = listOf("Deporte", "Música", "Hábitos", "Trabajo", "Metas", "Vida social")
        val category = Spinner(this).apply {
            adapter = ArrayAdapter(this@PlannerActivity, android.R.layout.simple_spinner_dropdown_item, categories)
            setSelection(categories.indexOf(existing?.category ?: "Hábitos").coerceAtLeast(0))
        }
        card.addView(UiKit.label(this, "Categoría", 12f, UiKit.muted, true), marginTop(18))
        card.addView(category, fixedTop(52, 5))

        val durationTotal = (existing?.durationMinutes ?: 0).coerceIn(0, 1440)
        val durationHour = wheelPicker(0, 24, durationTotal / 60)
        val durationMinute = wheelPicker(0, 59, durationTotal % 60)
        durationHour.setOnValueChangedListener { _, _, value ->
            if (value == 24) durationMinute.value = 0
        }
        durationMinute.setOnValueChangedListener { _, _, value ->
            if (durationHour.value == 24 && value != 0) durationMinute.value = 0
        }

        card.addView(UiKit.label(this, "Duración (HH : MM)", 12f, UiKit.muted, true), marginTop(18))
        card.addView(UiKit.label(this, "Izquierda: horas. Derecha: minutos.", 11f, UiKit.muted), marginTop(3))
        val durationRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(dp(8), dp(4), dp(8), dp(4))
            background = UiKit.rounded(this@PlannerActivity, UiKit.bg, 18, UiKit.line)
            addView(durationHour, LinearLayout.LayoutParams(0, dp(112), 1f))
            addView(UiKit.label(this@PlannerActivity, ":", 28f, UiKit.ink, true).apply { gravity = Gravity.CENTER }, LinearLayout.LayoutParams(dp(28), dp(112)))
            addView(durationMinute, LinearLayout.LayoutParams(0, dp(112), 1f))
        }
        card.addView(durationRow, marginTop(8))

        val recurrenceOptions = listOf("Todos los días", "Días laborables", "Fines de semana", "Lun, mié, vie", "Mar, jue, sáb")
        val recurrence = Spinner(this).apply {
            adapter = ArrayAdapter(this@PlannerActivity, android.R.layout.simple_spinner_dropdown_item, recurrenceOptions)
            val idx = recurrenceOptions.indexOf(existing?.recurrence)
            if (idx >= 0) setSelection(idx)
        }
        card.addView(UiKit.label(this, "Repetir", 12f, UiKit.muted, true), marginTop(18))
        card.addView(recurrence, fixedTop(52, 5))

        val reminder = CheckBox(this).apply {
            text = "Activar recordatorio"
            setTextColor(UiKit.ink)
            isChecked = existing?.reminder ?: true
        }
        card.addView(reminder, marginTop(12))

        val cancel = Button(this).apply {
            text = "Cancelar"
            isAllCaps = false
            textSize = 14f
            setTextColor(UiKit.ink)
            background = UiKit.rounded(this@PlannerActivity, UiKit.bg, 14, UiKit.line)
            stateListAnimator = null
            setOnClickListener { dialog.dismiss() }
        }
        val save = Button(this).apply {
            text = "Guardar"
            isAllCaps = false
            textSize = 14f
            setTextColor(Color.WHITE)
            setTypeface(typeface, Typeface.BOLD)
            background = UiKit.rounded(this@PlannerActivity, UiKit.green, 14)
            stateListAnimator = null
        }
        val actions = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            addView(cancel, LinearLayout.LayoutParams(0, dp(50), 1f).apply { marginEnd = dp(6) })
            addView(save, LinearLayout.LayoutParams(0, dp(50), 1f).apply { marginStart = dp(6) })
        }
        card.addView(actions, marginTop(20))

        val scroll = ScrollView(this).apply {
            isFillViewport = false
            setBackgroundColor(Color.TRANSPARENT)
            addView(card, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        }
        val shell = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(20), dp(14), dp(20))
            setBackgroundColor(Color.TRANSPARENT)
            addView(scroll, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        }
        dialog.setContentView(shell)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        save.setOnClickListener {
            if (title.text.toString().trim().isBlank()) {
                title.error = "Escribe una tarea"
                return@setOnClickListener
            }

            val selectedHour12 = startHour.value
            val selectedHour24 = when {
                isPm && selectedHour12 == 12 -> 12
                isPm -> selectedHour12 + 12
                selectedHour12 == 12 -> 0
                else -> selectedHour12
            }
            val storedTime = String.format(Locale.US, "%02d:%02d", selectedHour24, startMinute.value)
            val storedDuration = if (durationHour.value == 24) 1440 else durationHour.value * 60 + durationMinute.value

            store.save(
                RecurringTask(
                    id = existing?.id.orEmpty(),
                    title = title.text.toString().trim(),
                    time = storedTime,
                    recurrence = recurrence.selectedItem.toString(),
                    startDate = existing?.startDate ?: store.todayKey(),
                    reminder = reminder.isChecked,
                    completedDates = existing?.completedDates ?: emptySet(),
                    missedDates = existing?.missedDates ?: emptySet(),
                    category = category.selectedItem.toString(),
                    durationMinutes = storedDuration.coerceIn(0, 1440)
                )
            )
            dialog.dismiss()
            render()
        }

        dialog.setOnShowListener {
            dialog.window?.setLayout((resources.displayMetrics.widthPixels * 0.94f).toInt(), ViewGroup.LayoutParams.WRAP_CONTENT)
        }
        dialog.show()
    }

    private fun wheelPicker(min: Int, max: Int, selected: Int): NumberPicker = NumberPicker(this).apply {
        minValue = min
        maxValue = max
        value = selected.coerceIn(min, max)
        wrapSelectorWheel = true
        descendantFocusability = NumberPicker.FOCUS_BLOCK_DESCENDANTS
        setFormatter { value -> String.format(Locale.US, "%02d", value) }
    }

    private fun periodButton(label: String): Button = Button(this).apply {
        text = label
        isAllCaps = false
        textSize = 14f
        setTypeface(typeface, Typeface.BOLD)
        stateListAnimator = null
    }

    private fun stylePeriodButton(button: Button, selected: Boolean) {
        button.setTextColor(if (selected) Color.WHITE else UiKit.green)
        button.background = UiKit.rounded(this, if (selected) UiKit.green else UiKit.greenSoft, 14, UiKit.green)
    }

    private fun parseTime24(value: String): Pair<Int, Int> = try {
        val parts = value.trim().split(":")
        val hour = parts.getOrNull(0)?.toIntOrNull()?.coerceIn(0, 23) ?: 19
        val minute = parts.getOrNull(1)?.take(2)?.toIntOrNull()?.coerceIn(0, 59) ?: 0
        hour to minute
    } catch (_: Exception) {
        19 to 0
    }

    private fun formatTime12(value: String): String {
        val (hour24, minute) = parseTime24(value)
        val hour12 = when (val hour = hour24 % 12) {
            0 -> 12
            else -> hour
        }
        val period = if (hour24 >= 12) "p. m." else "a. m."
        return String.format(Locale.US, "%d:%02d %s", hour12, minute, period)
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

    private fun fixedTop(height: Int, top: Int): LinearLayout.LayoutParams {
        val params = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(height))
        params.topMargin = dp(top)
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
