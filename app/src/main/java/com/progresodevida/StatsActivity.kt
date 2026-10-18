package com.progresodevida

import android.app.Activity
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.max

class StatsActivity : Activity() {
    private lateinit var progress: ProgressStore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        progress = ProgressStore(this)
        window.statusBarColor = UiKit.blue
        window.navigationBarColor = Color.WHITE
        setContentView(buildScreen())
    }

    private fun buildScreen(): View {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(UiKit.blue)
        }
        val scroll = ScrollView(this).apply { isFillViewport = true }
        val body = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(18), dp(18), dp(36))
        }
        scroll.addView(body, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        root.addView(scroll, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))

        val header = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        header.addView(Button(this).apply {
            text = "‹"
            textSize = 28f
            setTextColor(Color.WHITE)
            background = UiKit.rounded(this@StatsActivity, Color.TRANSPARENT, 14)
            setOnClickListener { finish() }
        }, LinearLayout.LayoutParams(dp(52), dp(52)))
        header.addView(UiKit.label(this, "Estadísticas", 32f, Color.WHITE, true), LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        body.addView(header)

        val now = Calendar.getInstance()
        val start = progress.startOfWeek(now)
        val end = (start.clone() as Calendar).apply { add(Calendar.DAY_OF_MONTH, 7) }
        val summary = progress.summary(start, end)
        val days = mutableListOf<ProgressDay?>()
        val labels = mutableListOf<String>()
        repeat(7) { offset ->
            val cal = (start.clone() as Calendar).apply { add(Calendar.DAY_OF_MONTH, offset) }
            val key = progress.dateKey(cal)
            days += progress.day(key)
            labels += SimpleDateFormat("EEE", Locale("es", "PE")).format(cal.time).take(3)
        }

        body.addView(summaryCard(summary), marginTop(18))
        body.addView(chartCard(days, labels), marginTop(12))
        body.addView(areaCard(summary), marginTop(12))
        return root
    }

    private fun summaryCard(summary: ProgressSummary): View = UiKit.card(this).apply {
        addView(UiKit.label(this@StatsActivity, "Resumen semanal", 13f, UiKit.muted, true))
        addView(UiKit.label(this@StatsActivity, hours(summary.totalHours), 34f, UiKit.ink, true), marginTop(4))
        addView(UiKit.label(this@StatsActivity, "Tiempo total registrado", 12f, UiKit.muted))
        addView(metricRow("●  Días activos", summary.activeDays.toString(), UiKit.green), marginTop(14))
        addView(metricRow("●  Días sin actividad", summary.inactiveDays.toString(), UiKit.orange), marginTop(7))
        addView(metricRow("●  Racha actual", "${summary.currentStreak} días", UiKit.blue), marginTop(7))
        val mood = if (summary.averageMood > 0) String.format(Locale.US, "%.1f/5", summary.averageMood) else "Sin datos"
        addView(metricRow("●  Ánimo promedio", mood, UiKit.purple), marginTop(7))
    }

    private fun metricRow(label: String, value: String, color: Int): View = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        addView(UiKit.label(this@StatsActivity, label, 14f, color, false), LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        addView(UiKit.label(this@StatsActivity, value, 14f, color, true))
    }

    private fun chartCard(days: List<ProgressDay?>, labels: List<String>): View = UiKit.card(this).apply {
        addView(UiKit.label(this@StatsActivity, "Progreso diario", 20f, UiKit.ink, true))
        addView(UiKit.label(this@StatsActivity, "Últimos 7 días", 12f, UiKit.muted), marginTop(2))
        val chartParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(240))
        chartParams.topMargin = dp(14)
        addView(ProgressChartView(this@StatsActivity, days.map { it?.totalHours ?: 0.0 }, labels), chartParams)
        val legend = LinearLayout(this@StatsActivity).apply { orientation = LinearLayout.HORIZONTAL }
        legend.addView(UiKit.pill(this@StatsActivity, "● Horas", UiKit.blueSoft, UiKit.blue))
        legend.addView(UiKit.pill(this@StatsActivity, "● Activo", UiKit.greenSoft, UiKit.green), wrapStart(8))
        addView(legend, marginTop(10))
    }

    private fun areaCard(summary: ProgressSummary): View = UiKit.card(this).apply {
        addView(UiKit.label(this@StatsActivity, "Tiempo por área", 20f, UiKit.ink, true))
        addView(areaRow("🏋 Ejercicio", summary.exerciseHours, UiKit.green), marginTop(13))
        addView(areaRow("🎵 Música", summary.musicHours, UiKit.blue), marginTop(9))
        addView(areaRow("👥 Vida social", summary.socialHours, UiKit.purple), marginTop(9))
        addView(areaRow("💼 Trabajo", summary.workHours, UiKit.orange), marginTop(9))
    }

    private fun areaRow(label: String, value: Double, color: Int): View = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        addView(UiKit.label(this@StatsActivity, label, 14f, UiKit.ink, true), LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        addView(UiKit.pill(this@StatsActivity, hours(value), Color.argb(24, Color.red(color), Color.green(color), Color.blue(color)), color))
    }

    private fun hours(value: Double): String {
        val n = if (value % 1.0 == 0.0) value.toLong().toString() else String.format(Locale.US, "%.1f", value)
        return "$n h"
    }

    private fun marginTop(value: Int): LinearLayout.LayoutParams {
        val params = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        params.topMargin = dp(value)
        return params
    }

    private fun wrapStart(value: Int): LinearLayout.LayoutParams {
        val params = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        params.marginStart = dp(value)
        return params
    }

    private fun dp(value: Int): Int = UiKit.run { this@StatsActivity.dp(value) }
}

private class ProgressChartView(
    context: android.content.Context,
    private val values: List<Double>,
    private val labels: List<String>
) : View(context) {
    private val bluePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = UiKit.blue
        strokeWidth = 4f * resources.displayMetrics.density
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#DCE9FF")
        style = Paint.Style.FILL
    }
    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = UiKit.line
        strokeWidth = resources.displayMetrics.density
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = UiKit.muted
        textSize = 11f * resources.displayMetrics.scaledDensity
        textAlign = Paint.Align.CENTER
    }
    private val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = UiKit.blue
        style = Paint.Style.FILL
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (values.isEmpty()) return
        val left = 20f * resources.displayMetrics.density
        val right = width - 20f * resources.displayMetrics.density
        val top = 18f * resources.displayMetrics.density
        val bottom = height - 34f * resources.displayMetrics.density
        repeat(4) { i ->
            val y = top + (bottom - top) * i / 3f
            canvas.drawLine(left, y, right, y, gridPaint)
        }
        val maxValue = max(1.0, values.maxOrNull() ?: 1.0)
        val points = values.mapIndexed { index, value ->
            val x = if (values.size <= 1) left else left + (right - left) * index / (values.size - 1f)
            val y = bottom - ((value / maxValue).toFloat() * (bottom - top))
            x to y
        }
        val path = Path()
        points.forEachIndexed { i, p ->
            if (i == 0) path.moveTo(p.first, p.second) else path.lineTo(p.first, p.second)
        }
        val fill = Path(path).apply {
            lineTo(points.last().first, bottom)
            lineTo(points.first().first, bottom)
            close()
        }
        canvas.drawPath(fill, fillPaint)
        canvas.drawPath(path, bluePaint)
        points.forEach { (x, y) -> canvas.drawCircle(x, y, 5f * resources.displayMetrics.density, dotPaint) }
        labels.forEachIndexed { index, label ->
            val x = if (labels.size <= 1) left else left + (right - left) * index / (labels.size - 1f)
            canvas.drawText(label, x, height - 9f * resources.displayMetrics.density, textPaint)
        }
    }
}
