package com.progresodevida

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class MainActivity : Activity() {
    private lateinit var body: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.WHITE
        window.navigationBarColor = Color.WHITE
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR

        try {
            StorageGuard.repair(this)
            setContentView(buildScreen())
        } catch (error: Exception) {
            setContentView(buildRecoveryScreen(error))
        }
    }

    override fun onResume() {
        super.onResume()
        if (!::body.isInitialized) return
        try {
            StorageGuard.repair(this)
            render()
        } catch (error: Exception) {
            setContentView(buildRecoveryScreen(error))
        }
    }

    private fun buildScreen(): View {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(UiKit.bg)
        }
        val scroll = ScrollView(this).apply { isFillViewport = true }
        body = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(12), dp(16), dp(24))
        }
        scroll.addView(body, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        root.addView(scroll, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
        val nav = LinearLayout(this).apply {
            setPadding(dp(12), dp(4), dp(12), dp(4))
            setBackgroundColor(Color.WHITE)
        }
        listOf("⌂  Hoy", "▥  Progreso", "★  Metas").forEachIndexed { index, title ->
            nav.addView(UiKit.label(this, title, 14f, if (index == 0) UiKit.blue else UiKit.muted, true).apply {
                gravity = Gravity.CENTER
                setOnClickListener {
                    when (index) {
                        0 -> scroll.smoothScrollTo(0, 0)
                        1 -> startActivity(Intent(this@MainActivity, StatsActivity::class.java))
                        2 -> startActivity(Intent(this@MainActivity, GoalsActivity::class.java))
                    }
                }
            }, LinearLayout.LayoutParams(0, dp(52), 1f))
        }
        root.addView(nav)
        root.setOnApplyWindowInsetsListener { view, insets ->
            view.setPadding(insets.systemWindowInsetLeft, insets.systemWindowInsetTop, insets.systemWindowInsetRight, insets.systemWindowInsetBottom)
            insets.consumeSystemWindowInsets()
        }
        render()
        return root
    }

    private fun buildRecoveryScreen(error: Exception): View {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(48, 48, 48, 48)
            setBackgroundColor(Color.WHITE)

            addView(TextView(this@MainActivity).apply {
                text = "Progreso de Vida"
                textSize = 28f
                setTextColor(Color.parseColor("#111827"))
                setTypeface(typeface, Typeface.BOLD)
            })

            addView(TextView(this@MainActivity).apply {
                text = "Encontré datos locales incompatibles. La app ya no se cerrará sola. Puedes limpiar esos datos y volver a entrar."
                textSize = 15f
                setTextColor(Color.parseColor("#6B7280"))
                setPadding(0, 24, 0, 24)
            })

            addView(Button(this@MainActivity).apply {
                text = "Reparar datos y reintentar"
                isAllCaps = false
                setOnClickListener {
                    StorageGuard.prefs(this@MainActivity).edit().clear().commit()
                    recreate()
                }
            }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))

            addView(TextView(this@MainActivity).apply {
                text = "Diagnóstico: ${error.javaClass.simpleName}"
                textSize = 11f
                setTextColor(Color.parseColor("#9CA3AF"))
                setPadding(0, 16, 0, 0)
            })
        }
    }

    private fun render() {
        body.removeAllViews()
        val planner = PlannerStore(this)
        val tasks = planner.tasks()
        val key = planner.todayKey()
        val scheduled = tasks.filter { planner.scheduledFor(it, Calendar.getInstance()) }
        val done = scheduled.count { key in it.completedDates }
        val ratio = if (scheduled.isEmpty()) 0f else done.toFloat() / scheduled.size
        body.addView(UiKit.title(this, "Proyecto de vida", size = 28f))
        body.addView(UiKit.label(this, "Tu avance de hoy · ${prettyToday()}", 12f, UiKit.muted), marginTop(3))
        body.addView(FlameView(this, ratio).apply {
            contentDescription = "Llama de progreso: $done de ${scheduled.size} tareas completas"
            background = UiKit.rounded(this@MainActivity, Color.BLACK, 20)
            clipToOutline = true
        }, LinearLayout.LayoutParams(-1, dp(158)).apply { topMargin = dp(14) })
        val accent = when { scheduled.isEmpty() -> UiKit.muted; ratio >= 1f -> UiKit.green; ratio >= .5f -> UiKit.yellow; else -> UiKit.red }
        val soft = when { scheduled.isEmpty() -> UiKit.line; ratio >= 1f -> UiKit.greenSoft; ratio >= .5f -> UiKit.orangeSoft; else -> UiKit.redSoft }
        body.addView(UiKit.card(this).apply {
            background = UiKit.rounded(this@MainActivity, soft, 20)
            addView(UiKit.label(this@MainActivity, "Tareas completas", 15f, UiKit.ink, true))
            val row = LinearLayout(this@MainActivity).apply { gravity = Gravity.CENTER_VERTICAL }
            row.addView(UiKit.label(this@MainActivity, "$done/${scheduled.size}", 30f, UiKit.ink, true), LinearLayout.LayoutParams(0, -2, 1f))
            row.addView(UiKit.label(this@MainActivity, "${(ratio * 100).toInt()}%", 17f, UiKit.muted, true))
            addView(row)
            addView(android.widget.ProgressBar(this@MainActivity, null, android.R.attr.progressBarStyleHorizontal).apply {
                max = 100
                progress = (ratio * 100).toInt()
                progressTintList = android.content.res.ColorStateList.valueOf(accent)
                progressBackgroundTintList = android.content.res.ColorStateList.valueOf(UiKit.line)
            }, LinearLayout.LayoutParams(-1, dp(12)))
        }, marginTop(12))
        val heading = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        heading.addView(UiKit.title(this, "Hoy", size = 24f), LinearLayout.LayoutParams(0, -2, 1f))
        heading.addView(UiKit.pill(this, "+ Tarea").apply {
            minimumHeight = dp(48)
            setOnClickListener { startActivity(Intent(this@MainActivity, PlannerActivity::class.java)) }
        })
        body.addView(heading, marginTop(20))
        body.addView(UiKit.label(this, "Tus tareas y metas del día", 13f, UiKit.muted), marginTop(2))
        if (scheduled.isEmpty()) body.addView(UiKit.card(this).apply {
            addView(UiKit.label(this@MainActivity, "Programa tu primera tarea con + Tarea para encender tu llama.", 14f, UiKit.muted))
        }, marginTop(10))
        scheduled.forEach { task -> body.addView(taskRow(task, key), marginTop(9)) }
        body.addView(UiKit.section(this, "Esta semana"), marginTop(22))
        body.addView(weekCard(tasks, planner), marginTop(10))
        body.addView(featureCard("★", "Metas", "Estudios, habilidades y proyectos", UiKit.orange, UiKit.orangeSoft) {
            startActivity(Intent(this, GoalsActivity::class.java))
        }, marginTop(16))
        body.addView(featureCard("＋", "Registrar mi día", "Horas, experiencias y estado de ánimo", UiKit.blue, UiKit.blueSoft) {
            startActivity(Intent(this, ProgressActivity::class.java).putExtra(ProgressActivity.EXTRA_OPEN_TODAY, true))
        }, marginTop(10))
    }

    private fun taskRow(task: RecurringTask, key: String): View = UiKit.card(this, 12).apply {
        val done = key in task.completedDates
        val missed = key in task.missedDates
        background = UiKit.rounded(this@MainActivity, Color.WHITE, 18, if (done) UiKit.green else if (missed) UiKit.red else UiKit.line)
        val row = LinearLayout(this@MainActivity).apply { gravity = Gravity.CENTER_VERTICAL }
        row.addView(LinearLayout(this@MainActivity).apply {
            orientation = LinearLayout.VERTICAL
            addView(UiKit.label(this@MainActivity, task.category, 14f, UiKit.ink, true))
            addView(UiKit.label(this@MainActivity, task.title, 13f, UiKit.muted))
            val duration = if (task.durationMinutes > 0) " · ${task.durationMinutes} min" else ""
            addView(UiKit.label(this@MainActivity, task.time + duration, 12f, UiKit.muted), marginTop(4))
        }, LinearLayout.LayoutParams(0, -2, 1f))
        listOf(false, true).forEach { complete ->
            val selected = if (complete) done else missed
            val color = if (complete) UiKit.green else UiKit.red
            val fill = if (complete) UiKit.greenSoft else UiKit.redSoft
            row.addView(UiKit.label(this@MainActivity, if (complete) "✓" else "×", 26f, if (selected) Color.WHITE else color, true).apply {
                gravity = Gravity.CENTER
                background = UiKit.rounded(this@MainActivity, if (selected) color else fill, 12)
                contentDescription = "${task.title}: " + if (complete) "cumplida" else "no cumplida"
                isSelected = selected
                setOnClickListener {
                    PlannerStore(this@MainActivity).mark(task.id, key, complete)
                    render()
                }
            }, LinearLayout.LayoutParams(dp(48), dp(48)).apply { marginStart = dp(7) })
        }
        addView(row)
    }

    private fun weekCard(tasks: List<RecurringTask>, planner: PlannerStore): View = UiKit.card(this, 10).apply {
        val progress = ProgressStore(this@MainActivity)
        val start = progress.startOfWeek()
        val row = LinearLayout(this@MainActivity)
        val labels = listOf("Lun", "Mar", "Mié", "Jue", "Vie", "Sáb", "Dom")
        repeat(7) { offset ->
            val day = (start.clone() as Calendar).apply { add(Calendar.DAY_OF_MONTH, offset) }
            val date = progress.dateKey(day)
            val scheduled = tasks.filter { planner.scheduledFor(it, day) }
            val done = scheduled.count { date in it.completedDates }
            val allDone = scheduled.isNotEmpty() && done == scheduled.size
            val futureOrEmpty = date > planner.todayKey() || scheduled.isEmpty()
            val color = when { futureOrEmpty -> UiKit.muted; allDone -> UiKit.green; date < planner.todayKey() -> UiKit.red; else -> UiKit.yellow }
            val symbol = when { futureOrEmpty -> "—"; allDone -> "✓"; date < planner.todayKey() -> "×"; else -> "◔" }
            row.addView(LinearLayout(this@MainActivity).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                addView(UiKit.label(this@MainActivity, labels[offset], 11f, UiKit.muted))
                addView(UiKit.label(this@MainActivity, day.get(Calendar.DAY_OF_MONTH).toString(), 16f, UiKit.ink, true))
                addView(UiKit.label(this@MainActivity, symbol, 24f, color, true))
                contentDescription = "$date: $done de ${scheduled.size} tareas completas"
            }, LinearLayout.LayoutParams(0, -2, 1f))
        }
        addView(row)
    }

    private fun featureCard(icon: String, title: String, subtitle: String, accent: Int, soft: Int, action: () -> Unit): View = UiKit.card(this).apply {
        val row = LinearLayout(this@MainActivity).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        row.addView(TextView(this@MainActivity).apply {
            text = icon
            textSize = 25f
            gravity = Gravity.CENTER
            setTextColor(accent)
            background = UiKit.rounded(this@MainActivity, soft, 18)
        }, squareEnd(56, 56, 13))
        row.addView(LinearLayout(this@MainActivity).apply {
            orientation = LinearLayout.VERTICAL
            addView(UiKit.label(this@MainActivity, title, 17f, UiKit.ink, true))
            addView(UiKit.label(this@MainActivity, subtitle, 12f, UiKit.muted), marginTop(2))
        }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        row.addView(UiKit.label(this@MainActivity, "›", 28f, UiKit.muted, true))
        addView(row)
        isClickable = true
        setOnClickListener { action() }
    }

    private fun prettyToday(): String = SimpleDateFormat("EEEE d 'de' MMMM", Locale("es", "PE")).format(Calendar.getInstance().time)
        .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale("es", "PE")) else it.toString() }

    private fun number(value: Double): String = if (value % 1.0 == 0.0) value.toLong().toString() else String.format(Locale.US, "%.1f", value)
    private fun hours(value: Double): String = "${number(value)} h"

    private fun marginTop(value: Int): LinearLayout.LayoutParams {
        val params = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        params.topMargin = dp(value)
        return params
    }

    private fun wrapTop(value: Int): LinearLayout.LayoutParams {
        val params = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        params.topMargin = dp(value)
        return params
    }

    private fun squareEnd(width: Int, height: Int, end: Int): LinearLayout.LayoutParams {
        val params = LinearLayout.LayoutParams(dp(width), dp(height))
        params.marginEnd = dp(end)
        return params
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
