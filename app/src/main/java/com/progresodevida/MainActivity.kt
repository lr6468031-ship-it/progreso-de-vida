package com.progresodevida

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
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
    private val fondo = Color.parseColor("#0E130B")
    private val superficie = Color.parseColor("#1B2516")
    private val verde = Color.parseColor("#4CD964")
    private val verdeSuave = Color.parseColor("#CFF5D5")
    private val texto = Color.parseColor("#F5F7EA")
    private val texto2 = Color.parseColor("#B9C6AD")
    private val amarillo = Color.parseColor("#E1C75B")

    private lateinit var body: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.BLACK
        window.navigationBarColor = Color.BLACK
        setContentView(buildScreen())
    }

    override fun onResume() {
        super.onResume()
        if (::body.isInitialized) render()
    }

    private fun buildScreen(): View {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(fondo)
        }
        val scroll = ScrollView(this).apply { isFillViewport = true }
        body = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(16), dp(16), dp(32))
        }
        scroll.addView(body, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        root.addView(scroll, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
        render()
        return root
    }

    private fun render() {
        body.removeAllViews()
        val progress = ProgressStore(this)
        val goals = SkillGoalStore(this)
        val today = progress.day(progress.todayKey())
        val weekStart = progress.startOfWeek()
        val weekEnd = (weekStart.clone() as Calendar).apply { add(Calendar.DAY_OF_MONTH, 7) }
        val week = progress.summary(weekStart, weekEnd)
        val activeGoals = goals.items().count { it.status != "completado" }

        body.addView(TextView(this).apply {
            text = "🌱 Progreso de Vida"
            textSize = 29f
            setTextColor(texto)
            setTypeface(typeface, Typeface.BOLD)
        })
        body.addView(TextView(this).apply {
            text = SimpleDateFormat("EEEE d 'de' MMMM", Locale("es", "PE")).format(Calendar.getInstance().time)
                .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale("es", "PE")) else it.toString() }
            textSize = 12f
            setTextColor(texto2)
            setPadding(0, dp(2), 0, dp(14))
        })

        body.addView(todayCard(today))

        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dp(10), 0, 0)
        }
        row.addView(metric("Semana", hours(week.totalHours), verde), weight(0, 4))
        row.addView(metric("Racha", "${week.currentStreak} días", verdeSuave), weight(4, 0))
        body.addView(row)

        val row2 = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dp(8), 0, 0)
        }
        row2.addView(metric("Días activos", week.activeDays.toString(), verde), weight(0, 4))
        row2.addView(metric("Metas activas", activeGoals.toString(), amarillo), weight(4, 0))
        body.addView(row2)

        body.addView(section("Registrar"), top(18))
        body.addView(primaryButton("✍️ Registrar / editar hoy") {
            startActivity(Intent(this, ProgressActivity::class.java).putExtra(ProgressActivity.EXTRA_OPEN_TODAY, true))
        })

        body.addView(section("Ver y analizar"), top(18))
        body.addView(actionButton("📈 Mi progreso", "Día · semana · mes · año · horas · rachas · ánimo") {
            startActivity(Intent(this, ProgressActivity::class.java))
        })
        body.addView(actionButton("🎯 Metas, estudios y habilidades", "Habilidad · estudio · meta · proyecto · hábito · sesiones") {
            startActivity(Intent(this, SkillGoalsActivity::class.java))
        }, top(8))

        body.addView(infoCard(
            "Cuando registres una sesión de una habilidad y la vincules a Música, Ejercicio, Vida social o Trabajo, esas horas también se sumarán automáticamente a tu progreso del día."
        ), top(16))
    }

    private fun todayCard(day: ProgressDay?): View {
        val box = card()
        box.addView(TextView(this).apply {
            text = "HOY"
            textSize = 10f
            setTextColor(texto2)
            setTypeface(typeface, Typeface.BOLD)
        })
        if (day == null) {
            box.addView(TextView(this).apply {
                text = "Aún no registraste tu día"
                textSize = 19f
                setTextColor(amarillo)
                setTypeface(typeface, Typeface.BOLD)
                setPadding(0, dp(5), 0, dp(4))
            })
            box.addView(TextView(this).apply {
                text = "Puedes registrar horas, qué hiciste, cómo te fue, cómo te sentiste y una foto."
                textSize = 12f
                setTextColor(texto2)
            })
        } else {
            box.addView(TextView(this).apply {
                text = hours(day.totalHours)
                textSize = 24f
                setTextColor(verde)
                setTypeface(typeface, Typeface.BOLD)
                setPadding(0, dp(5), 0, dp(4))
            })
            box.addView(TextView(this).apply {
                text = "🏋️ ${hours(day.exerciseHours)}   🎵 ${hours(day.musicHours)}\n🧑‍🤝‍🧑 ${hours(day.socialHours)}   💼 ${hours(day.workHours)}"
                textSize = 12f
                setTextColor(texto2)
            })
            if (day.did.isNotBlank()) box.addView(TextView(this).apply {
                text = day.did
                textSize = 12f
                setTextColor(verdeSuave)
                setPadding(0, dp(7), 0, 0)
            })
        }
        return box
    }

    private fun metric(label: String, value: String, accent: Int): View = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(12), dp(11), dp(12), dp(11))
        background = cardBg()
        addView(TextView(this@MainActivity).apply { text = label; textSize = 10f; setTextColor(texto2) })
        addView(TextView(this@MainActivity).apply {
            text = value; textSize = 17f; setTextColor(accent); setTypeface(typeface, Typeface.BOLD)
        })
    }

    private fun actionButton(title: String, subtitle: String, action: () -> Unit): View = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(14), dp(13), dp(14), dp(13))
        background = cardBg()
        isClickable = true
        setOnClickListener { action() }
        addView(TextView(this@MainActivity).apply {
            text = title; textSize = 16f; setTextColor(texto); setTypeface(typeface, Typeface.BOLD)
        })
        addView(TextView(this@MainActivity).apply {
            text = subtitle; textSize = 11f; setTextColor(texto2); setPadding(0, dp(4), 0, 0)
        })
    }

    private fun primaryButton(label: String, action: () -> Unit) = Button(this).apply {
        text = label
        isAllCaps = false
        textSize = 14f
        setTextColor(Color.BLACK)
        setTypeface(typeface, Typeface.BOLD)
        background = GradientDrawable().apply { setColor(verde); cornerRadius = dp(14).toFloat() }
        stateListAnimator = null
        setOnClickListener { action() }
    }

    private fun section(value: String) = TextView(this).apply {
        text = value; textSize = 16f; setTextColor(texto); setTypeface(typeface, Typeface.BOLD)
    }

    private fun infoCard(value: String) = TextView(this).apply {
        text = value; textSize = 12f; setTextColor(texto2); setPadding(dp(14), dp(14), dp(14), dp(14)); background = cardBg()
    }

    private fun card() = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(14), dp(14), dp(14), dp(14))
        background = cardBg()
    }

    private fun cardBg() = GradientDrawable().apply {
        setColor(superficie); cornerRadius = dp(16).toFloat(); setStroke(dp(2), verde)
    }

    private fun number(value: Double): String = if (value % 1.0 == 0.0) value.toLong().toString() else String.format(Locale.US, "%.2f", value)
    private fun hours(value: Double): String = "${number(value)} h"
    private fun weight(start: Int = 3, end: Int = 3) = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply { marginStart = dp(start); marginEnd = dp(end) }
    private fun top(value: Int) = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(value) }
    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
