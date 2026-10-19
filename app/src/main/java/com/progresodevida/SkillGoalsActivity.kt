package com.progresodevida

import android.app.Activity
import android.app.AlertDialog
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.InputType
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.TextView
import java.util.Locale

class SkillGoalsActivity : Activity() {
    private val fondo = Color.parseColor("#F6F7FB")
    private val superficie = Color.parseColor("#FFFFFF")
    private val verde = Color.parseColor("#2F6FED")
    private val verdeSuave = Color.parseColor("#1E5ED8")
    private val texto = Color.parseColor("#101828")
    private val texto2 = Color.parseColor("#667085")
    private val amarillo = Color.parseColor("#B26A00")
    private val borde = Color.parseColor("#D8DEE9")

    private lateinit var body: LinearLayout
    private lateinit var store: SkillGoalStore
    private lateinit var plannerStore: PlannerStore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = fondo
        window.navigationBarColor = fondo
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
        store = SkillGoalStore(this)
        plannerStore = PlannerStore(this)
        setContentView(buildScreen())
        render()
    }

    override fun onResume() {
        super.onResume()
        if (::body.isInitialized) render()
    }

    private fun buildScreen(): ViewGroup {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(fondo)
        }
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(14), dp(16), dp(10))
        }
        header.addView(TextView(this).apply {
            text = "🎯 Metas, estudios y habilidades"
            textSize = 24f
            setTextColor(texto)
            setTypeface(typeface, Typeface.BOLD)
        })
        header.addView(TextView(this).apply {
            text = "Crea metas con duración, inicio y fin. Las tareas vinculadas actualizan el avance automáticamente."
            textSize = 12f
            setTextColor(texto2)
        })
        root.addView(header)
        root.addView(primaryButton("＋ Nueva meta / estudio / habilidad") { showEditor(null) }, horizontalMargin())

        val scroll = ScrollView(this).apply { isFillViewport = true }
        body = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(10), dp(14), dp(30))
        }
        scroll.addView(body, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        root.addView(scroll, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
        return root
    }

    private fun render() {
        body.removeAllViews()
        val items = store.items()
        val active = items.filter { it.status != "completado" }
        val completed = items.filter { it.status == "completado" }
        val totalHours = items.sumOf { it.accumulatedHours }
        body.addView(summary("${active.size} activos", "${completed.size} completados · ${hours(totalHours)} acumuladas"))

        if (items.isEmpty()) {
            body.addView(infoCard("Ejemplos: “Aprender Serum”, “Terminar curso de mezcla”, “Entrenar 3 meses”, “Mejorar ventas”, “Aprender inglés”. Tú eliges duración, inicio y fin."), top(10))
            return
        }

        if (active.isNotEmpty()) {
            body.addView(sectionTitle("En progreso"), top(14))
            active.forEach { body.addView(goalCard(it), top(8)) }
        }
        if (completed.isNotEmpty()) {
            body.addView(sectionTitle("Completados"), top(16))
            completed.forEach { body.addView(goalCard(it), top(8)) }
        }
    }

    private fun goalCard(goal: SkillGoal): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(13), dp(12), dp(13), dp(12))
        background = cardBg()
        isClickable = true
        setOnClickListener { showActions(goal) }

        val linkedProgress = plannerStore.goalProgress(goal.id)

        addView(TextView(this@SkillGoalsActivity).apply {
            text = "${iconFor(goal.type)} ${goal.name}"
            textSize = 15f
            setTextColor(if (goal.status == "completado") verde else texto)
            setTypeface(typeface, Typeface.BOLD)
        })
        addView(TextView(this@SkillGoalsActivity).apply {
            val progress = if (goal.targetHours > 0) {
                "${goal.progressPercent}% · ${hours(goal.accumulatedHours)} / ${hours(goal.targetHours)}"
            } else {
                "${hours(goal.accumulatedHours)} registradas"
            }
            text = "${goal.type} · ${goal.area}\n$progress · ${goal.sessionCount} sesiones"
            textSize = 11f
            setTextColor(texto2)
            setPadding(0, dp(5), 0, 0)
        })
        addView(TextView(this@SkillGoalsActivity).apply {
            val unit = when (goal.period) {
                "Semana" -> if (goal.periodCount == 1) "semana" else "semanas"
                "Mes" -> if (goal.periodCount == 1) "mes" else "meses"
                else -> if (goal.periodCount == 1) "año" else "años"
            }
            val dates = buildString {
                if (goal.startDate.isNotBlank()) append("Inicio ${goal.startDate}")
                if (goal.endDate.isNotBlank()) {
                    if (isNotEmpty()) append(" · ")
                    append("Fin ${goal.endDate}")
                }
            }
            text = "Duración: ${goal.periodCount} $unit${if (dates.isBlank()) "" else "\n$dates"}"
            textSize = 11f
            setTextColor(texto2)
            setPadding(0, dp(5), 0, 0)
        })
        if (linkedProgress.total > 0) addView(TextView(this@SkillGoalsActivity).apply {
            text = "Tareas de la meta: ${linkedProgress.completed}/${linkedProgress.total} completadas · ${linkedProgress.remaining} faltan · ${linkedProgress.percent}%"
            textSize = 11f
            setTextColor(verdeSuave)
            setPadding(0, dp(5), 0, 0)
        })
        if (goal.target.isNotBlank()) addView(TextView(this@SkillGoalsActivity).apply {
            text = "Objetivo: ${goal.target}"
            textSize = 11f
            setTextColor(verdeSuave)
            setPadding(0, dp(4), 0, 0)
        })
        if (goal.deadline.isNotBlank()) addView(TextView(this@SkillGoalsActivity).apply {
            text = "Fecha objetivo: ${goal.deadline}"
            textSize = 10f
            setTextColor(amarillo)
            setPadding(0, dp(3), 0, 0)
        })
    }

    private fun showActions(goal: SkillGoal) {
        val options = mutableListOf("Registrar sesión", "Ver sesiones", "Editar")
        options += if (goal.status == "completado") "Reabrir" else "Marcar completado"
        options += "Eliminar"
        AlertDialog.Builder(this)
            .setTitle(goal.name)
            .setItems(options.toTypedArray()) { _, which ->
                when (which) {
                    0 -> showSession(goal)
                    1 -> showSessions(goal)
                    2 -> showEditor(goal)
                    3 -> {
                        store.setStatus(goal.id, if (goal.status == "completado") "activo" else "completado")
                        render()
                    }
                    4 -> confirmDelete(goal)
                }
            }
            .setNegativeButton("Cerrar", null)
            .show()
    }

    private fun showEditor(existing: SkillGoal?) {
        val box = dialogBox()
        val types = listOf("Habilidad", "Estudio", "Meta", "Proyecto", "Hábito")
        val areas = listOf("Música", "Ejercicio", "Vida social", "Trabajo", "Otro")
        val periods = listOf("Semana", "Mes", "Año")
        val type = spinner(types, existing?.type)
        val name = field("Nombre · ej. Aprender Serum", existing?.name)
        val area = spinner(areas, existing?.area)
        val period = spinner(periods, existing?.period ?: "Mes")
        val periodCount = integerField("Cantidad · ej. 3", existing?.periodCount ?: 1)
        val startDate = field("Inicio · AAAA-MM-DD", existing?.startDate)
        val endDate = field("Fin · AAAA-MM-DD", existing?.endDate)
        val target = field("Objetivo concreto · ej. terminar 20 clases", existing?.target)
        val targetHours = numberField("Meta de horas · opcional", existing?.targetHours)
        val deadline = field("Fecha límite adicional · opcional", existing?.deadline)
        val notes = field("Notas / por qué quiero aprenderlo", existing?.notes, true)

        box.addView(label("Tipo")); box.addView(type)
        box.addView(name)
        box.addView(label("Área principal (las sesiones se suman aquí)")); box.addView(area)
        box.addView(label("Duración")); box.addView(period)
        box.addView(periodCount)
        box.addView(label("Cuándo empieza y cuándo termina")); box.addView(startDate); box.addView(endDate)
        box.addView(target); box.addView(targetHours); box.addView(deadline); box.addView(notes)

        val dialog = AlertDialog.Builder(this)
            .setTitle(if (existing == null) "Nueva meta / estudio / habilidad" else "Editar ${existing.name}")
            .setView(ScrollView(this).apply { addView(box) })
            .setPositiveButton("Guardar", null)
            .setNegativeButton("Cancelar", null)
            .create()
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val n = name.text.toString().trim()
                if (n.isBlank()) {
                    name.error = "Ponle un nombre"
                    return@setOnClickListener
                }
                val count = periodCount.text.toString().toIntOrNull()?.coerceAtLeast(1) ?: 1
                store.saveItem(
                    SkillGoal(
                        id = existing?.id.orEmpty(),
                        type = type.selectedItem.toString(),
                        name = n,
                        area = area.selectedItem.toString(),
                        period = period.selectedItem.toString(),
                        periodCount = count,
                        startDate = startDate.text.toString().trim(),
                        endDate = endDate.text.toString().trim(),
                        target = target.text.toString().trim(),
                        targetHours = parseNumber(targetHours) ?: 0.0,
                        deadline = deadline.text.toString().trim(),
                        notes = notes.text.toString().trim(),
                        status = existing?.status ?: "activo",
                        createdAt = existing?.createdAt.orEmpty()
                    )
                )
                dialog.dismiss()
                render()
            }
        }
        dialog.show()
    }

    private fun showSession(goal: SkillGoal) {
        val box = dialogBox()
        val hours = numberField("Horas de esta sesión", null)
        val note = field("¿Qué estudiaste o practicaste?", null, true)
        box.addView(TextView(this).apply {
            text = "Se sumará a ${goal.name}${if (goal.area == "Otro") "" else " y también al área ${goal.area} de Mi progreso"}."
            textSize = 12f
            setTextColor(Color.DKGRAY)
            setPadding(0, 0, 0, dp(6))
        })
        box.addView(hours)
        box.addView(note)

        val dialog = AlertDialog.Builder(this)
            .setTitle("Registrar sesión")
            .setView(box)
            .setPositiveButton("Guardar", null)
            .setNegativeButton("Cancelar", null)
            .create()
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val value = parseNumber(hours)
                if (value == null || value <= 0.0) {
                    hours.error = "Ingresa las horas"
                    return@setOnClickListener
                }
                store.addSession(goal.id, value, note.text.toString().trim())
                dialog.dismiss()
                render()
            }
        }
        dialog.show()
    }

    private fun showSessions(goal: SkillGoal) {
        val sessions = store.sessionsFor(goal.id)
        val message = if (sessions.isEmpty()) {
            "Todavía no registraste sesiones."
        } else {
            sessions.take(40).joinToString("\n\n") {
                "${it.date} · ${hours(it.hours)}${if (it.note.isBlank()) "" else "\n${it.note}"}"
            }
        }
        AlertDialog.Builder(this)
            .setTitle("Sesiones · ${goal.name}")
            .setMessage(message)
            .setPositiveButton("Cerrar", null)
            .show()
    }

    private fun confirmDelete(goal: SkillGoal) {
        AlertDialog.Builder(this)
            .setTitle("Eliminar")
            .setMessage("¿Eliminar ${goal.name} y su historial de sesiones?")
            .setNegativeButton("No", null)
            .setPositiveButton("Sí, eliminar") { _, _ ->
                store.deleteItem(goal.id)
                render()
            }
            .show()
    }

    private fun iconFor(type: String): String = when (type) {
        "Estudio" -> "📚"
        "Meta" -> "🎯"
        "Proyecto" -> "🛠️"
        "Hábito" -> "🔁"
        else -> "🧠"
    }

    private fun summary(main: String, sub: String) = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(14), dp(13), dp(14), dp(13))
        background = cardBg()
        addView(TextView(this@SkillGoalsActivity).apply {
            text = main; textSize = 18f; setTextColor(verde); setTypeface(typeface, Typeface.BOLD)
        })
        addView(TextView(this@SkillGoalsActivity).apply {
            text = sub; textSize = 11f; setTextColor(texto2); setPadding(0, dp(4), 0, 0)
        })
    }

    private fun infoCard(value: String) = TextView(this).apply {
        text = value; textSize = 13f; setTextColor(texto2); setPadding(dp(14), dp(16), dp(14), dp(16)); background = cardBg()
    }

    private fun sectionTitle(value: String) = TextView(this).apply {
        text = value; textSize = 17f; setTextColor(texto); setTypeface(typeface, Typeface.BOLD)
    }

    private fun primaryButton(label: String, action: () -> Unit) = Button(this).apply {
        text = label; isAllCaps = false; textSize = 14f; setTextColor(Color.WHITE); setTypeface(typeface, Typeface.BOLD)
        background = GradientDrawable().apply { setColor(verde); cornerRadius = dp(14).toFloat() }
        stateListAnimator = null
        setOnClickListener { action() }
    }

    private fun dialogBox() = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(20), dp(4), dp(20), dp(4))
    }

    private fun label(value: String) = TextView(this).apply {
        text = value; textSize = 11f; setTextColor(Color.DKGRAY); setPadding(0, dp(7), 0, dp(2))
    }

    private fun spinner(options: List<String>, selected: String?): Spinner = Spinner(this).apply {
        adapter = ArrayAdapter(this@SkillGoalsActivity, android.R.layout.simple_spinner_dropdown_item, options)
        val index = options.indexOfFirst { it.equals(selected, ignoreCase = true) }
        if (index >= 0) setSelection(index)
    }

    private fun field(hint: String, value: String?, multiline: Boolean = false) = EditText(this).apply {
        this.hint = hint
        setText(value.orEmpty())
        setTextColor(Color.BLACK)
        setHintTextColor(Color.DKGRAY)
        inputType = InputType.TYPE_CLASS_TEXT or if (multiline) InputType.TYPE_TEXT_FLAG_MULTI_LINE else 0
        isSingleLine = !multiline
        if (multiline) minLines = 2
    }

    private fun numberField(hint: String, value: Double?) = EditText(this).apply {
        this.hint = hint
        inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
        if (value != null && value > 0.0) setText(number(value))
        setTextColor(Color.BLACK)
        setHintTextColor(Color.DKGRAY)
        isSingleLine = true
    }

    private fun integerField(hint: String, value: Int) = EditText(this).apply {
        this.hint = hint
        inputType = InputType.TYPE_CLASS_NUMBER
        if (value > 0) setText(value.toString())
        setTextColor(Color.BLACK)
        setHintTextColor(Color.DKGRAY)
        isSingleLine = true
    }

    private fun parseNumber(field: EditText): Double? = field.text.toString().trim().replace(',', '.').toDoubleOrNull()
    private fun number(value: Double): String = if (value % 1.0 == 0.0) value.toLong().toString() else String.format(Locale.US, "%.2f", value)
    private fun hours(value: Double): String = "${number(value)} h"

    private fun cardBg() = GradientDrawable().apply {
        setColor(superficie); cornerRadius = dp(15).toFloat(); setStroke(dp(1), borde)
    }

    private fun top(value: Int) = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(value) }
    private fun horizontalMargin() = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { marginStart = dp(14); marginEnd = dp(14) }
    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
