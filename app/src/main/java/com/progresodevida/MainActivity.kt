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
            setPadding(dp(18), dp(20), dp(18), dp(40))
        }
        scroll.addView(body, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        root.addView(scroll, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
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
        val progress = ProgressStore(this)
        val goals = SkillGoalStore(this)
        val planner = PlannerStore(this)
        val today = progress.day(progress.todayKey())
        val weekStart = progress.startOfWeek()
        val weekEnd = (weekStart.clone() as Calendar).apply { add(Calendar.DAY_OF_MONTH, 7) }
        val week = progress.summary(weekStart, weekEnd)
        val activeGoals = goals.items().count { it.status != "completado" }
        val todayTasks = planner.tasks().filter { planner.scheduledFor(it, Calendar.getInstance()) }
        val completedToday = todayTasks.count { it.completedDates.contains(planner.todayKey()) }

        body.addView(UiKit.label(this, "PROGRESO DE VIDA", 12f, UiKit.blue, true))
        body.addView(UiKit.title(this, "Tu progreso,\nen un solo lugar", UiKit.ink, 34f), marginTop(3))
        body.addView(UiKit.label(this, prettyToday(), 13f, UiKit.muted), marginTop(6))

        body.addView(heroCard(today, week.currentStreak), marginTop(20))

        body.addView(UiKit.section(this, "Hoy"), marginTop(24))
        body.addView(todayTasksCard(completedToday, todayTasks.size), marginTop(10))

        body.addView(UiKit.section(this, "Organiza y avanza"), marginTop(24))
        body.addView(featureCard(
            icon = "↻",
            title = "Tareas recurrentes",
            subtitle = "Calendario, hábitos y recordatorios",
            accent = UiKit.blue,
            soft = UiKit.blueSoft
        ) { startActivity(Intent(this, PlannerActivity::class.java)) }, marginTop(10))

        body.addView(featureCard(
            icon = "▥",
            title = "Estadísticas",
            subtitle = "Semana, rachas, horas y evolución",
            accent = UiKit.green,
            soft = UiKit.greenSoft
        ) { startActivity(Intent(this, StatsActivity::class.java)) }, marginTop(10))

        body.addView(featureCard(
            icon = "★",
            title = "Metas",
            subtitle = "$activeGoals activas · estudios, habilidades y proyectos",
            accent = UiKit.orange,
            soft = UiKit.orangeSoft
        ) { startActivity(Intent(this, GoalsActivity::class.java)) }, marginTop(10))

        body.addView(UiKit.section(this, "Registrar"), marginTop(24))
        body.addView(Button(this).apply {
            text = "＋ Registrar mi día"
            isAllCaps = false
            textSize = 15f
            setTextColor(Color.WHITE)
            setTypeface(typeface, Typeface.BOLD)
            background = UiKit.rounded(this@MainActivity, UiKit.blue, 17)
            stateListAnimator = null
            setOnClickListener {
                startActivity(Intent(this@MainActivity, ProgressActivity::class.java).putExtra(ProgressActivity.EXTRA_OPEN_TODAY, true))
            }
        }, marginTop(10))
    }

    private fun heroCard(day: ProgressDay?, streak: Int): View = UiKit.card(this).apply {
        background = UiKit.rounded(this@MainActivity, UiKit.blue, 24)
        setPadding(dp(20), dp(20), dp(20), dp(20))
        addView(UiKit.label(this@MainActivity, "Resumen de hoy", 13f, Color.WHITE, true))
        addView(UiKit.label(this@MainActivity, if (day == null) "Aún sin registro" else hours(day.totalHours), 30f, Color.WHITE, true), marginTop(5))
        addView(UiKit.label(this@MainActivity, if (day == null) "Registra lo que hiciste para empezar a construir tus estadísticas." else "🏋 ${hours(day.exerciseHours)}    🎵 ${hours(day.musicHours)}    💼 ${hours(day.workHours)}", 12f, Color.WHITE), marginTop(4))
        val pill = UiKit.pill(this@MainActivity, "🔥 Racha: $streak días", Color.argb(45, 255, 255, 255), Color.WHITE)
        addView(pill, wrapTop(14))
    }

    private fun todayTasksCard(done: Int, total: Int): View = UiKit.card(this).apply {
        val row = LinearLayout(this@MainActivity).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        row.addView(TextView(this@MainActivity).apply {
            text = if (total > 0 && done == total) "✓" else "○"
            textSize = 24f
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            background = UiKit.rounded(this@MainActivity, if (total > 0 && done == total) UiKit.green else UiKit.blue, 24)
        }, squareEnd(48, 48, 12))
        row.addView(LinearLayout(this@MainActivity).apply {
            orientation = LinearLayout.VERTICAL
            addView(UiKit.label(this@MainActivity, if (total == 0) "Sin tareas programadas" else "$done/$total completadas", 17f, UiKit.ink, true))
            addView(UiKit.label(this@MainActivity, if (total == 0) "Crea tu primera tarea recurrente" else "Toca para ver el calendario de hoy", 12f, UiKit.muted))
        }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        addView(row)
        isClickable = true
        setOnClickListener { startActivity(Intent(this@MainActivity, PlannerActivity::class.java)) }
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

    private fun dp(value: Int): Int = UiKit.run { this@MainActivity.dp(value) }
}
