package com.progresodevida

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import java.util.Calendar

class GoalsActivity : Activity() {
    private lateinit var body: LinearLayout
    private lateinit var store: SkillGoalStore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        store = SkillGoalStore(this)
        window.statusBarColor = Color.WHITE
        window.navigationBarColor = Color.WHITE
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        setContentView(buildScreen())
    }

    override fun onResume() {
        super.onResume()
        if (::body.isInitialized) render()
    }

    private fun buildScreen(): View {
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(UiKit.bg) }
        val scroll = ScrollView(this).apply { isFillViewport = true }
        body = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(18), dp(18), dp(18), dp(40)) }
        scroll.addView(body, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        root.addView(scroll, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
        return root
    }

    private fun render() {
        body.removeAllViews()
        val items = store.items()
        val active = items.filter { it.status != "completado" }
        val completed = items.filter { it.status == "completado" }

        val header = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        header.addView(Button(this).apply {
            text = "‹"; textSize = 28f; setTextColor(UiKit.ink); background = UiKit.rounded(this@GoalsActivity, Color.TRANSPARENT, 14); setOnClickListener { finish() }
        }, LinearLayout.LayoutParams(dp(52), dp(52)))
        header.addView(UiKit.title(this, "Metas", UiKit.ink, 34f), LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        body.addView(header)

        val year = Calendar.getInstance().get(Calendar.YEAR)
        body.addView(UiKit.card(this).apply {
            val top = LinearLayout(this@GoalsActivity).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
            top.addView(UiKit.label(this@GoalsActivity, "▱", 28f, UiKit.orange, true))
            top.addView(UiKit.label(this@GoalsActivity, year.toString(), 26f, UiKit.ink, true), LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply { marginStart = dp(10) })
            top.addView(Button(this@GoalsActivity).apply {
                text = "+"; textSize = 22f; setTextColor(Color.WHITE); background = UiKit.rounded(this@GoalsActivity, UiKit.orange, 24); stateListAnimator = null; setOnClickListener { openManager() }
            }, LinearLayout.LayoutParams(dp(46), dp(46)))
            addView(top)
            val total = items.size.coerceAtLeast(1)
            val pct = completed.size * 100 / total
            addView(progressLine(pct, UiKit.orange), marginTop(15))
            addView(UiKit.label(this@GoalsActivity, "${completed.size}/${items.size} completadas ($pct%)", 12f, UiKit.muted), marginTop(5))
        }, marginTop(16))

        val tabs = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        tabs.addView(UiKit.pill(this, "Activas (${active.size})", UiKit.orangeSoft, UiKit.orange), LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        tabs.addView(UiKit.pill(this, "Completadas (${completed.size})", Color.WHITE, UiKit.muted), LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply { marginStart = dp(8) })
        body.addView(tabs, marginTop(14))

        if (active.isEmpty()) {
            body.addView(UiKit.card(this).apply { addView(UiKit.label(this@GoalsActivity, "Aún no tienes metas activas. Crea una para empezar.", 13f, UiKit.muted)) }, marginTop(12))
        } else {
            active.forEach { body.addView(goalCard(it), marginTop(10)) }
        }

        if (completed.isNotEmpty()) {
            body.addView(UiKit.section(this, "Completadas"), marginTop(24))
            completed.take(5).forEach { body.addView(goalCard(it), marginTop(10)) }
        }

        body.addView(Button(this).apply {
            text = "Administrar metas, sesiones y estudios"
            isAllCaps = false
            textSize = 14f
            setTextColor(Color.WHITE)
            background = UiKit.rounded(this@GoalsActivity, UiKit.orange, 17)
            stateListAnimator = null
            setOnClickListener { openManager() }
        }, marginTop(22))
    }

    private fun goalCard(goal: SkillGoal): View = UiKit.card(this).apply {
        val row = LinearLayout(this@GoalsActivity).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        row.addView(TextView(this@GoalsActivity).apply {
            text = if (goal.status == "completado") "✓" else "○"
            textSize = 21f; gravity = Gravity.CENTER
            setTextColor(if (goal.status == "completado") Color.WHITE else UiKit.orange)
            background = UiKit.rounded(this@GoalsActivity, if (goal.status == "completado") UiKit.orange else Color.WHITE, 22, UiKit.orange, 2)
        }, LinearLayout.LayoutParams(dp(42), dp(42)).apply { marginEnd = dp(11) })
        row.addView(LinearLayout(this@GoalsActivity).apply {
            orientation = LinearLayout.VERTICAL
            addView(UiKit.label(this@GoalsActivity, "${icon(goal.type)} ${goal.name}", 16f, UiKit.ink, true))
            addView(UiKit.label(this@GoalsActivity, "${goal.type} · ${goal.area}", 11f, UiKit.muted), marginTop(2))
        }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        row.addView(UiKit.label(this@GoalsActivity, "⋮", 26f, UiKit.muted))
        addView(row)
        val pct = goal.progressPercent
        if (goal.targetHours > 0) {
            addView(progressLine(pct, UiKit.orange), marginTop(12))
            addView(UiKit.label(this@GoalsActivity, "$pct% · ${hours(goal.accumulatedHours)} / ${hours(goal.targetHours)}", 11f, UiKit.muted), marginTop(4))
        } else if (goal.accumulatedHours > 0) {
            addView(UiKit.label(this@GoalsActivity, "${hours(goal.accumulatedHours)} · ${goal.sessionCount} sesiones", 11f, UiKit.muted), marginTop(9))
        }
        if (goal.target.isNotBlank()) addView(UiKit.label(this@GoalsActivity, goal.target, 12f, UiKit.muted), marginTop(7))
        isClickable = true
        setOnClickListener { openManager() }
    }

    private fun progressLine(percent: Int, accent: Int): View = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
        max = 100
        progress = percent.coerceIn(0, 100)
        progressTintList = android.content.res.ColorStateList.valueOf(accent)
        progressBackgroundTintList = android.content.res.ColorStateList.valueOf(UiKit.line)
    }

    private fun openManager() = startActivity(Intent(this, SkillGoalsActivity::class.java))
    private fun icon(type: String) = when (type) { "Estudio" -> "📚"; "Meta" -> "🎯"; "Proyecto" -> "🛠"; "Hábito" -> "↻"; else -> "🧠" }
    private fun hours(value: Double): String = if (value % 1.0 == 0.0) "${value.toLong()} h" else String.format(java.util.Locale.US, "%.1f h", value)
    private fun marginTop(value: Int) = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(value) }
    private fun dp(value: Int) = UiKit.run { this@GoalsActivity.dp(value) }
}
