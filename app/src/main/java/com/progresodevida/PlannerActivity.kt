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
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.NumberPicker
import android.widget.ScrollView
import android.widget.TextView
import android.widget.ToggleButton
import java.util.Calendar
import java.util.Locale

class PlannerActivity : Activity() {
    private lateinit var store: PlannerStore
    private lateinit var body: LinearLayout

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
            setPadding(dp(18), dp(18), dp(18), dp(44))
        }
        scroll.addView(
            body,
            ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        )
        root.addView(scroll, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
        return root
    }

    private fun render() {
        body.removeAllViews()
        val all = store.tasks()
        val habits = all.filter { it.category.equals("Hábitos", ignoreCase = true) }
        val today = store.todayKey()
        val todayHabits = habits.filter { store.scheduledFor(it, Calendar.getInstance()) }

        val top = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        top.addView(Button(this).apply {
            text = "‹"
            textSize = 28f
            setTextColor(UiKit.ink)
            background = UiKit.rounded(this@PlannerActivity, Color.TRANSPARENT, 16)
            setOnClickListener { finish() }
        }, LinearLayout.LayoutParams(dp(52), dp(52)))
        top.addView(LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addView(UiKit.label(this@PlannerActivity, "Tus hábitos", 30f, UiKit.ink, true))
            addView(UiKit.label(this@PlannerActivity, "Días, hora y duración", 13f, UiKit.blue, true))
        }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        body.addView(top)

        body.addView(
            UiKit.label(
                this,
                "Cada hábito aparece en Hoy únicamente los días que selecciones. No necesita fecha de finalización.",
                13f,
                UiKit.muted
            ),
            marginTop(8)
        )

        body.addView(Button(this).apply {
            text = "+ Nuevo hábito"
            isAllCaps = false
            textSize = 15f
            setTextColor(Color.WHITE)
            setTypeface(typeface, Typeface.BOLD)
            stateListAnimator = null
            background = UiKit.rounded(this@PlannerActivity, UiKit.blue, 16)
            setOnClickListener { editHabit(null) }
        }, fixedTop(54, 18))

        body.addView(UiKit.section(this, "Hoy"), marginTop(26))
        if (todayHabits.isEmpty()) {
            body.addView(UiKit.card(this).apply {
                addView(UiKit.label(this@PlannerActivity, "Hoy no tienes hábitos programados.", 14f, UiKit.muted))
            }, marginTop(10))
        } else {
            todayHabits.forEach { body.addView(habitCard(it, today, showCheck = true), marginTop(10)) }
        }

        body.addView(UiKit.section(this, "Todos tus hábitos"), marginTop(26))
        if (habits.isEmpty()) {
            body.addView(UiKit.card(this).apply {
                addView(UiKit.label(this@PlannerActivity, "Crea tu primer hábito y elige los días en que quieres hacerlo.", 14f, UiKit.muted))
            }, marginTop(10))
        } else {
            habits.forEach { body.addView(habitCard(it, today, showCheck = false), marginTop(10)) }
        }
    }

    private fun habitCard(task: RecurringTask, date: String, showCheck: Boolean): View = UiKit.card(this).apply {
        val done = task.completedDates.contains(date)
        val row = LinearLayout(this@PlannerActivity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        if (showCheck) {
            row.addView(TextView(this@PlannerActivity).apply {
                text = if (done) "✓" else ""
                gravity = Gravity.CENTER
                textSize = 20f
                setTextColor(Color.WHITE)
                background = UiKit.rounded(
                    this@PlannerActivity,
                    if (done) UiKit.green else Color.WHITE,
                    24,
                    if (done) UiKit.green else UiKit.line,
                    2
                )
                setOnClickListener {
                    store.toggle(task.id, date)
                    render()
                }
            }, squareEnd(46, 46, 12))
        }

        row.addView(LinearLayout(this@PlannerActivity).apply {
            orientation = LinearLayout.VERTICAL
            addView(UiKit.label(this@PlannerActivity, task.title, 17f, UiKit.ink, true))
            addView(
                UiKit.label(
                    this@PlannerActivity,
                    "${formatTime(task.time)} · ${task.durationMinutes} min · ${task.timesPerDay} ${if (task.timesPerDay == 1) "vez" else "veces"}",
                    12f,
                    UiKit.muted
                ),
                marginTop(3)
            )
            addView(UiKit.label(this@PlannerActivity, task.recurrence, 12f, UiKit.blue, true), marginTop(4))
        }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))

        row.addView(TextView(this@PlannerActivity).apply {
            text = "⋮"
            textSize = 28f
            gravity = Gravity.CENTER
            setTextColor(UiKit.muted)
            setOnClickListener { habitMenu(task) }
        }, LinearLayout.LayoutParams(dp(44), dp(48)))

        addView(row)
    }

    private fun habitMenu(task: RecurringTask) {
        AlertDialog.Builder(this)
            .setTitle(task.title)
            .setItems(arrayOf("Editar", "Eliminar")) { _, which ->
                if (which == 0) {
                    editHabit(task)
                } else {
                    AlertDialog.Builder(this)
                        .setTitle("Eliminar hábito")
                        .setMessage("¿Quieres eliminar ${task.title}?")
                        .setPositiveButton("Eliminar") { _, _ ->
                            store.delete(task.id)
                            render()
                        }
                        .setNegativeButton("Cancelar", null)
                        .show()
                }
            }
            .setNegativeButton("Cerrar", null)
            .show()
    }

    private fun editHabit(existing: RecurringTask?) {
        val initialTime = parseTime(existing?.time ?: "19:00")
        val initialDuration = (existing?.durationMinutes ?: 30).coerceIn(1, 600)
        val initialTimes = (existing?.timesPerDay ?: 1).coerceIn(1, 10)
        val selectedDays = TaskWeekdays.parse(existing?.recurrence ?: "Todos los días").toMutableSet()

        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)

        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(20), dp(20), dp(20))
            background = UiKit.rounded(this@PlannerActivity, UiKit.surface, 24, UiKit.line)
        }

        card.addView(UiKit.label(this, if (existing == null) "Nuevo hábito" else "Editar hábito", 25f, UiKit.ink, true))
        card.addView(
            UiKit.label(
                this,
                "El hábito se mantendrá activo en los días que marques hasta que tú lo edites o elimines.",
                12f,
                UiKit.muted
            ),
            marginTop(5)
        )

        val title = EditText(this).apply {
            hint = "Ej. Bass house"
            setText(existing?.title.orEmpty())
            inputType = InputType.TYPE_CLASS_TEXT
            setTextColor(UiKit.ink)
            setHintTextColor(UiKit.muted)
            textSize = 16f
            setPadding(dp(14), 0, dp(14), 0)
            background = UiKit.rounded(this@PlannerActivity, Color.WHITE, 14, UiKit.line)
        }
        card.addView(UiKit.label(this, "Nombre", 12f, UiKit.muted, true), marginTop(20))
        card.addView(title, fixedTop(56, 7))

        card.addView(UiKit.label(this, "Días", 12f, UiKit.muted, true), marginTop(18))
        card.addView(UiKit.label(this, "Selecciona únicamente los días en que debe aparecer.", 11f, UiKit.muted), marginTop(3))

        val dayError = UiKit.label(this, "Selecciona al menos un día.", 12f, UiKit.red).apply {
            visibility = View.GONE
        }

        TaskWeekdays.names.indices.chunked(2).forEach { indices ->
            val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            indices.forEach { index ->
                val day = TaskWeekdays.days[index]
                val button = ToggleButton(this).apply {
                    textOn = "✓ ${TaskWeekdays.names[index]}"
                    textOff = TaskWeekdays.names[index]
                    isAllCaps = false
                    textSize = 14f
                    stateListAnimator = null
                    isChecked = day in selectedDays
                    setPadding(dp(4), 0, dp(4), 0)
                }
                fun refresh() {
                    button.setTextColor(if (button.isChecked) Color.WHITE else UiKit.green)
                    button.background = UiKit.rounded(
                        this,
                        if (button.isChecked) UiKit.green else UiKit.greenSoft,
                        14,
                        UiKit.green
                    )
                }
                refresh()
                button.setOnCheckedChangeListener { _, checked ->
                    if (checked) selectedDays.add(day) else selectedDays.remove(day)
                    dayError.visibility = if (selectedDays.isEmpty()) View.VISIBLE else View.GONE
                    refresh()
                }
                row.addView(button, LinearLayout.LayoutParams(0, dp(52), 1f).apply {
                    if (index % 2 == 0) marginEnd = dp(4) else marginStart = dp(4)
                })
            }
            if (indices.size == 1) row.addView(View(this), LinearLayout.LayoutParams(0, dp(52), 1f))
            card.addView(row, marginTop(8))
        }
        card.addView(dayError, marginTop(6))

        val timesPicker = wheelPicker(1, 10, initialTimes)
        card.addView(UiKit.label(this, "Veces al día", 12f, UiKit.muted, true), marginTop(18))
        card.addView(timesPicker, fixedTop(96, 6))

        val hourPicker = wheelPicker(0, 23, initialTime.first)
        val minutePicker = wheelPicker(0, 59, initialTime.second)
        card.addView(UiKit.label(this, "Hora de inicio", 12f, UiKit.muted, true), marginTop(18))
        card.addView(LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            background = UiKit.rounded(this@PlannerActivity, UiKit.bg, 18, UiKit.line)
            addView(hourPicker, LinearLayout.LayoutParams(0, dp(108), 1f))
            addView(UiKit.label(this@PlannerActivity, ":", 28f, UiKit.ink, true).apply {
                gravity = Gravity.CENTER
            }, LinearLayout.LayoutParams(dp(30), dp(108)))
            addView(minutePicker, LinearLayout.LayoutParams(0, dp(108), 1f))
        }, marginTop(7))

        val durationPicker = wheelPicker(1, 600, initialDuration)
        card.addView(UiKit.label(this, "Duración en minutos", 12f, UiKit.muted, true), marginTop(18))
        card.addView(UiKit.label(this, "Ejemplo: 45 significa que el hábito debe tomar 45 minutos.", 11f, UiKit.muted), marginTop(3))
        card.addView(durationPicker, fixedTop(96, 6))

        val reminder = CheckBox(this).apply {
            text = "Activar recordatorio"
            setTextColor(UiKit.ink)
            isChecked = existing?.reminder ?: true
        }
        card.addView(reminder, marginTop(14))

        val cancel = Button(this).apply {
            text = "Cancelar"
            isAllCaps = false
            setTextColor(UiKit.ink)
            stateListAnimator = null
            background = UiKit.rounded(this@PlannerActivity, UiKit.bg, 14, UiKit.line)
            setOnClickListener { dialog.dismiss() }
        }
        val save = Button(this).apply {
            text = "Guardar"
            isAllCaps = false
            setTextColor(Color.WHITE)
            setTypeface(typeface, Typeface.BOLD)
            stateListAnimator = null
            background = UiKit.rounded(this@PlannerActivity, UiKit.green, 14)
        }
        card.addView(LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            addView(cancel, LinearLayout.LayoutParams(0, dp(50), 1f).apply { marginEnd = dp(6) })
            addView(save, LinearLayout.LayoutParams(0, dp(50), 1f).apply { marginStart = dp(6) })
        }, marginTop(20))

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
            val cleanTitle = title.text.toString().trim()
            if (cleanTitle.isBlank()) {
                title.error = "Escribe el nombre del hábito"
                return@setOnClickListener
            }
            if (selectedDays.isEmpty()) {
                dayError.visibility = View.VISIBLE
                return@setOnClickListener
            }

            val storedTime = String.format(Locale.US, "%02d:%02d", hourPicker.value, minutePicker.value)
            store.save(
                RecurringTask(
                    id = existing?.id.orEmpty(),
                    title = cleanTitle,
                    time = storedTime,
                    recurrence = TaskWeekdays.format(selectedDays),
                    startDate = "",
                    endDate = "",
                    goalId = existing?.goalId.orEmpty(),
                    reminder = reminder.isChecked,
                    completedDates = existing?.completedDates ?: emptySet(),
                    category = "Hábitos",
                    durationMinutes = durationPicker.value,
                    timesPerDay = timesPicker.value,
                    missedDates = existing?.missedDates ?: emptySet()
                )
            )
            dialog.dismiss()
            render()
        }

        dialog.show()
        dialog.window?.setLayout((resources.displayMetrics.widthPixels * 0.94f).toInt(), ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    private fun wheelPicker(min: Int, max: Int, selected: Int): NumberPicker = NumberPicker(this).apply {
        minValue = min
        maxValue = max
        value = selected.coerceIn(min, max)
        wrapSelectorWheel = true
        descendantFocusability = NumberPicker.FOCUS_BLOCK_DESCENDANTS
        setFormatter { value -> String.format(Locale.US, "%02d", value) }
    }

    private fun parseTime(value: String): Pair<Int, Int> = try {
        val parts = value.split(":")
        (parts.getOrNull(0)?.toIntOrNull() ?: 19).coerceIn(0, 23) to
            (parts.getOrNull(1)?.take(2)?.toIntOrNull() ?: 0).coerceIn(0, 59)
    } catch (_: Exception) {
        19 to 0
    }

    private fun formatTime(value: String): String {
        val (hour24, minute) = parseTime(value)
        val hour12 = when (val h = hour24 % 12) { 0 -> 12; else -> h }
        val period = if (hour24 >= 12) "p. m." else "a. m."
        return String.format(Locale.US, "%d:%02d %s", hour12, minute, period)
    }

    private fun marginTop(value: Int): LinearLayout.LayoutParams =
        LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            topMargin = dp(value)
        }

    private fun fixedTop(height: Int, top: Int): LinearLayout.LayoutParams =
        LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(height)).apply {
            topMargin = dp(top)
        }

    private fun squareEnd(width: Int, height: Int, end: Int): LinearLayout.LayoutParams =
        LinearLayout.LayoutParams(dp(width), dp(height)).apply {
            marginEnd = dp(end)
        }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
