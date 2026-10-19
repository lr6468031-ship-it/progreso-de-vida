package com.progresodevida

import android.app.Activity
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.roundToInt

class StatsActivity : Activity() {
    private lateinit var planner: PlannerStore
    private lateinit var progress: ProgressStore
    private lateinit var body: LinearLayout

    private var mode = Mode.DAY
    private var displayMonth: Calendar = Calendar.getInstance().apply {
        set(Calendar.DAY_OF_MONTH, 1)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        planner = PlannerStore(this)
        progress = ProgressStore(this)
        window.statusBarColor = Color.WHITE
        window.navigationBarColor = Color.WHITE
        @Suppress("DEPRECATION")
        run { window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR }
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

        val scroll = ScrollView(this).apply {
            isFillViewport = true
            clipToPadding = false
        }
        body = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(14), dp(16), dp(28))
        }
        scroll.addView(
            body,
            ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        )
        root.addView(scroll, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
        root.addView(bottomNav())
        root.setOnApplyWindowInsetsListener { view, insets ->
            view.setPadding(
                insets.systemWindowInsetLeft,
                insets.systemWindowInsetTop,
                insets.systemWindowInsetRight,
                insets.systemWindowInsetBottom
            )
            insets.consumeSystemWindowInsets()
        }
        return root
    }

    private fun render() {
        body.removeAllViews()
        val tasks = planner.tasks()

        body.addView(header())
        body.addView(periodTabs(), top(16))
        body.addView(statisticsCard(tasks), top(18))
        body.addView(calendarCard(tasks), top(16))
        body.addView(weekSummaryCard(tasks), top(16))
    }

    private fun header(): View = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL

        addView(TextView(this@StatsActivity).apply {
            text = "▥"
            textSize = 30f
            gravity = Gravity.CENTER
            setTextColor(UiKit.blue)
            background = UiKit.rounded(this@StatsActivity, UiKit.blueSoft, 18)
        }, LinearLayout.LayoutParams(dp(58), dp(58)).apply { marginEnd = dp(12) })

        addView(LinearLayout(this@StatsActivity).apply {
            orientation = LinearLayout.VERTICAL
            addView(UiKit.title(this@StatsActivity, "Mi progreso", size = 31f))
            addView(UiKit.label(this@StatsActivity, "Tu avance y estadísticas", 14f, UiKit.muted))
        }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))

        addView(UiKit.label(this@StatsActivity, "⚙", 29f, UiKit.ink, false).apply {
            gravity = Gravity.CENTER
            contentDescription = "Configuración"
        }, LinearLayout.LayoutParams(dp(48), dp(48)))
    }

    private fun periodTabs(): View = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        addView(periodTab("Día", Mode.DAY), weight(0, 5))
        addView(periodTab("Semana", Mode.WEEK), weight(5, 5))
        addView(periodTab("Mes", Mode.MONTH), weight(5, 5))
        addView(periodTab("Año", Mode.YEAR), weight(5, 0))
    }

    private fun periodTab(label: String, target: Mode): View =
        UiKit.label(this, label, 14f, if (mode == target) Color.WHITE else UiKit.muted, true).apply {
            gravity = Gravity.CENTER
            minimumHeight = dp(54)
            background = UiKit.rounded(
                this@StatsActivity,
                if (mode == target) Color.parseColor("#3C8DFF") else Color.WHITE,
                18,
                if (mode == target) Color.parseColor("#3C8DFF") else UiKit.line
            )
            setOnClickListener {
                if (mode != target) {
                    mode = target
                    render()
                }
            }
        }

    private fun statisticsCard(tasks: List<RecurringTask>): View = UiKit.card(this).apply {
        background = UiKit.rounded(this@StatsActivity, Color.WHITE, 24, UiKit.line)

        val heading = LinearLayout(this@StatsActivity).apply {
            gravity = Gravity.CENTER_VERTICAL
            addView(LinearLayout(this@StatsActivity).apply {
                orientation = LinearLayout.VERTICAL
                addView(UiKit.label(this@StatsActivity, "Estadísticas del ${mode.title.lowercase()}", 21f, UiKit.ink, true))
                addView(UiKit.label(this@StatsActivity, "Rendimiento según tareas completadas", 12f, UiKit.muted))
            }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
            addView(UiKit.label(this@StatsActivity, periodCaption(), 12f, UiKit.muted, true))
        }
        addView(heading)

        val bars = barPoints(tasks)
        addView(
            ProductivityBarChartView(this@StatsActivity, bars),
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(255)).apply { topMargin = dp(12) }
        )

        addView(UiKit.label(
            this@StatsActivity,
            "Rojo: bajo  ·  Amarillo: medio  ·  Verde: alto",
            11f,
            UiKit.muted
        ), top(7))
    }

    private fun calendarCard(tasks: List<RecurringTask>): View = UiKit.card(this).apply {
        background = UiKit.rounded(this@StatsActivity, Color.WHITE, 24, UiKit.line)

        val heading = LinearLayout(this@StatsActivity).apply {
            gravity = Gravity.CENTER_VERTICAL
            addView(LinearLayout(this@StatsActivity).apply {
                orientation = LinearLayout.VERTICAL
                addView(UiKit.label(this@StatsActivity, "Calendario de productividad", 21f, UiKit.ink, true))
                addView(UiKit.label(this@StatsActivity, monthCaption(displayMonth), 12f, UiKit.muted))
            }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))

            addView(monthArrow("‹", -1))
            addView(monthArrow("›", 1), start(4))
        }
        addView(heading)

        addView(
            CalendarHeatmapView(this@StatsActivity, displayMonth, tasks, planner),
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(320)).apply { topMargin = dp(10) }
        )

        val legendTop = LinearLayout(this@StatsActivity).apply {
            orientation = LinearLayout.HORIZONTAL
            addView(legend("Excelente", Color.parseColor("#28D85F")), LinearLayout.LayoutParams(0, -2, 1f))
            addView(legend("Bueno", Color.parseColor("#8BE84A")), LinearLayout.LayoutParams(0, -2, 1f))
            addView(legend("Regular", Color.parseColor("#FFD43B")), LinearLayout.LayoutParams(0, -2, 1f))
        }
        addView(legendTop, top(8))

        val legendBottom = LinearLayout(this@StatsActivity).apply {
            orientation = LinearLayout.HORIZONTAL
            addView(legend("Bajo", Color.parseColor("#FF8A3D")), LinearLayout.LayoutParams(0, -2, 1f))
            addView(legend("Muy bajo", Color.parseColor("#FF4D57")), LinearLayout.LayoutParams(0, -2, 1f))
            addView(legend("Sin registro", Color.parseColor("#DCE1E8")), LinearLayout.LayoutParams(0, -2, 1f))
        }
        addView(legendBottom, top(6))
    }

    private fun monthArrow(symbol: String, delta: Int): View =
        UiKit.label(this, symbol, 28f, UiKit.muted, true).apply {
            gravity = Gravity.CENTER
            background = UiKit.rounded(this@StatsActivity, Color.WHITE, 14, UiKit.line)
            setOnClickListener {
                displayMonth = (displayMonth.clone() as Calendar).apply { add(Calendar.MONTH, delta) }
                render()
            }
        }.also {
            it.layoutParams = LinearLayout.LayoutParams(dp(42), dp(42))
        }

    private fun legend(label: String, color: Int): View =
        LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL
            addView(TextView(this@StatsActivity).apply {
                background = UiKit.rounded(this@StatsActivity, color, 6)
            }, LinearLayout.LayoutParams(dp(15), dp(15)).apply { marginEnd = dp(5) })
            addView(UiKit.label(this@StatsActivity, label, 10f, UiKit.muted, true))
        }

    private fun weekSummaryCard(tasks: List<RecurringTask>): View = UiKit.card(this).apply {
        background = UiKit.rounded(this@StatsActivity, Color.WHITE, 24, UiKit.line)

        val start = progress.startOfWeek(Calendar.getInstance())
        val end = (start.clone() as Calendar).apply { add(Calendar.DAY_OF_MONTH, 7) }
        val stats = periodTaskStats(start, end, tasks)

        addView(UiKit.label(this@StatsActivity, "Resumen de la semana", 21f, UiKit.ink, true))
        addView(
            UiKit.label(
                this@StatsActivity,
                "${weekLabel(start, end)} · basado en tus tareas",
                12f,
                UiKit.muted
            )
        )

        val row1 = LinearLayout(this@StatsActivity).apply { orientation = LinearLayout.HORIZONTAL }
        row1.addView(summaryMetric("✓", stats.completed.toString(), "Tareas completadas", UiKit.green, UiKit.greenSoft), weight(0, 6))
        row1.addView(summaryMetric("▥", "${stats.percent}%", "Productividad", UiKit.blue, UiKit.blueSoft), weight(6, 0))
        addView(row1, top(14))

        val row2 = LinearLayout(this@StatsActivity).apply { orientation = LinearLayout.HORIZONTAL }
        row2.addView(
            summaryMetric("★", String.format(Locale.US, "%.1f/10", stats.averageDailyScore), "Promedio diario", UiKit.yellow, Color.parseColor("#FFF9E6")),
            weight(0, 6)
        )
        row2.addView(
            summaryMetric("◷", formatMinutes(stats.completedMinutes), "Tiempo total", UiKit.purple, Color.parseColor("#F6EEFF")),
            weight(6, 0)
        )
        addView(row2, top(8))
    }

    private fun summaryMetric(icon: String, value: String, label: String, color: Int, soft: Int): View =
        LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(10), dp(13), dp(10), dp(13))
            background = UiKit.rounded(this@StatsActivity, soft, 20)
            addView(UiKit.label(this@StatsActivity, icon, 25f, color, true).apply { gravity = Gravity.CENTER })
            addView(UiKit.label(this@StatsActivity, value, 24f, UiKit.ink, true).apply { gravity = Gravity.CENTER })
            addView(UiKit.label(this@StatsActivity, label, 11f, UiKit.muted, false).apply { gravity = Gravity.CENTER })
        }

    private fun bottomNav(): View = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER
        setPadding(dp(12), dp(5), dp(12), dp(5))
        setBackgroundColor(Color.WHITE)

        addView(navItem("⌂", "Hoy", false) {
            startActivity(Intent(this@StatsActivity, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP))
            finish()
        }, LinearLayout.LayoutParams(0, dp(60), 1f))

        addView(navItem("▥", "Progreso", true) {}, LinearLayout.LayoutParams(0, dp(60), 1f))

        addView(navItem("★", "Metas", false) {
            startActivity(Intent(this@StatsActivity, GoalsActivity::class.java))
        }, LinearLayout.LayoutParams(0, dp(60), 1f))
    }

    private fun navItem(icon: String, label: String, selected: Boolean, action: () -> Unit): View =
        LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            addView(UiKit.label(
                this@StatsActivity,
                icon,
                20f,
                if (selected) Color.parseColor("#267BFF") else Color.parseColor("#76829B"),
                true
            ).apply { gravity = Gravity.CENTER })
            addView(UiKit.label(
                this@StatsActivity,
                label,
                11f,
                if (selected) Color.parseColor("#267BFF") else Color.parseColor("#76829B"),
                selected
            ).apply { gravity = Gravity.CENTER })
            setOnClickListener { action() }
        }

    private fun barPoints(tasks: List<RecurringTask>): List<BarPoint> {
        val now = Calendar.getInstance()
        return when (mode) {
            Mode.DAY -> {
                val start = progress.startOfWeek(now)
                (0 until 7).map { offset ->
                    val day = (start.clone() as Calendar).apply { add(Calendar.DAY_OF_MONTH, offset) }
                    val ratio = dayRatio(day, tasks)
                    BarPoint(
                        label = SimpleDateFormat("EEE", Locale("es", "PE")).format(day.time).take(3).replaceFirstChar { it.uppercase() },
                        ratio = ratio
                    )
                }
            }
            Mode.WEEK -> {
                val monthStart = (now.clone() as Calendar).apply {
                    set(Calendar.DAY_OF_MONTH, 1)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                val monthEnd = (monthStart.clone() as Calendar).apply { add(Calendar.MONTH, 1) }
                val cursor = progress.startOfWeek(monthStart)
                val points = mutableListOf<BarPoint>()
                var index = 1
                while (cursor.before(monthEnd) && index <= 6) {
                    val weekEnd = (cursor.clone() as Calendar).apply { add(Calendar.DAY_OF_MONTH, 7) }
                    val clippedStart = if (cursor.before(monthStart)) monthStart else cursor
                    val clippedEnd = if (weekEnd.after(monthEnd)) monthEnd else weekEnd
                    points += BarPoint("S$index", periodRatio(clippedStart, clippedEnd, tasks))
                    cursor.add(Calendar.DAY_OF_MONTH, 7)
                    index++
                }
                points
            }
            Mode.MONTH -> {
                val yearStart = (now.clone() as Calendar).apply {
                    set(Calendar.MONTH, Calendar.JANUARY)
                    set(Calendar.DAY_OF_MONTH, 1)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                (0 until 12).map { offset ->
                    val start = (yearStart.clone() as Calendar).apply { add(Calendar.MONTH, offset) }
                    val end = (start.clone() as Calendar).apply { add(Calendar.MONTH, 1) }
                    BarPoint(
                        SimpleDateFormat("MMM", Locale("es", "PE")).format(start.time).take(3).replaceFirstChar { it.uppercase() },
                        periodRatio(start, end, tasks)
                    )
                }
            }
            Mode.YEAR -> {
                val thisYear = now.get(Calendar.YEAR)
                ((thisYear - 4)..thisYear).map { year ->
                    val start = Calendar.getInstance().apply {
                        set(Calendar.YEAR, year)
                        set(Calendar.MONTH, Calendar.JANUARY)
                        set(Calendar.DAY_OF_MONTH, 1)
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }
                    val end = (start.clone() as Calendar).apply { add(Calendar.YEAR, 1) }
                    BarPoint(year.toString(), periodRatio(start, end, tasks))
                }
            }
        }
    }

    private fun dayRatio(day: Calendar, tasks: List<RecurringTask>): Float? {
        val key = dateKey(day)
        if (key > planner.todayKey()) return null
        val scheduled = tasks.filter { planner.scheduledFor(it, day) }
        if (scheduled.isEmpty()) return null
        val done = scheduled.count { key in it.completedDates }
        return done.toFloat() / scheduled.size.toFloat()
    }

    private fun periodRatio(start: Calendar, end: Calendar, tasks: List<RecurringTask>): Float? {
        val cursor = (start.clone() as Calendar)
        val today = planner.todayKey()
        var scheduledCount = 0
        var completedCount = 0
        while (cursor.before(end)) {
            val key = dateKey(cursor)
            if (key > today) break
            val scheduled = tasks.filter { planner.scheduledFor(it, cursor) }
            scheduledCount += scheduled.size
            completedCount += scheduled.count { key in it.completedDates }
            cursor.add(Calendar.DAY_OF_MONTH, 1)
        }
        return if (scheduledCount == 0) null else completedCount.toFloat() / scheduledCount.toFloat()
    }

    private fun periodTaskStats(start: Calendar, end: Calendar, tasks: List<RecurringTask>): TaskStats {
        val cursor = (start.clone() as Calendar)
        val today = planner.todayKey()
        var total = 0
        var completed = 0
        var minutes = 0
        val dailyRatios = mutableListOf<Float>()

        while (cursor.before(end)) {
            val key = dateKey(cursor)
            if (key > today) break
            val scheduled = tasks.filter { planner.scheduledFor(it, cursor) }
            if (scheduled.isNotEmpty()) {
                val done = scheduled.filter { key in it.completedDates }
                total += scheduled.size
                completed += done.size
                minutes += done.sumOf { it.durationMinutes.coerceAtLeast(0) }
                dailyRatios += done.size.toFloat() / scheduled.size.toFloat()
            }
            cursor.add(Calendar.DAY_OF_MONTH, 1)
        }

        val ratio = if (total == 0) 0f else completed.toFloat() / total.toFloat()
        val average = if (dailyRatios.isEmpty()) 0f else dailyRatios.average().toFloat() * 10f
        return TaskStats(completed, total, (ratio * 100f).roundToInt(), average, minutes)
    }

    private fun periodCaption(): String {
        val now = Calendar.getInstance()
        return when (mode) {
            Mode.DAY -> {
                val start = progress.startOfWeek(now)
                val end = (start.clone() as Calendar).apply { add(Calendar.DAY_OF_MONTH, 6) }
                "${start.get(Calendar.DAY_OF_MONTH)}–${end.get(Calendar.DAY_OF_MONTH)} ${SimpleDateFormat("MMM", Locale("es", "PE")).format(end.time)}"
            }
            Mode.WEEK -> monthCaption(now)
            Mode.MONTH -> now.get(Calendar.YEAR).toString()
            Mode.YEAR -> "Últimos 5 años"
        }
    }

    private fun monthCaption(cal: Calendar): String =
        SimpleDateFormat("MMMM yyyy", Locale("es", "PE")).format(cal.time)
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale("es", "PE")) else it.toString() }

    private fun weekLabel(start: Calendar, endExclusive: Calendar): String {
        val end = (endExclusive.clone() as Calendar).apply { add(Calendar.DAY_OF_MONTH, -1) }
        val month = SimpleDateFormat("MMMM", Locale("es", "PE")).format(end.time)
        return "${start.get(Calendar.DAY_OF_MONTH)}–${end.get(Calendar.DAY_OF_MONTH)} de $month"
    }

    private fun formatMinutes(minutes: Int): String {
        val h = minutes / 60
        val m = minutes % 60
        return when {
            h > 0 && m > 0 -> "${h}h ${m}m"
            h > 0 -> "${h}h"
            else -> "${m}m"
        }
    }

    private fun dateKey(cal: Calendar): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(cal.time)

    private fun top(value: Int): LinearLayout.LayoutParams =
        LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            topMargin = dp(value)
        }

    private fun start(value: Int): LinearLayout.LayoutParams =
        LinearLayout.LayoutParams(dp(42), dp(42)).apply { marginStart = dp(value) }

    private fun weight(start: Int, end: Int): LinearLayout.LayoutParams =
        LinearLayout.LayoutParams(0, dp(54), 1f).apply {
            marginStart = dp(start)
            marginEnd = dp(end)
        }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private enum class Mode(val title: String) {
        DAY("Día"),
        WEEK("Semana"),
        MONTH("Mes"),
        YEAR("Año")
    }

    private data class TaskStats(
        val completed: Int,
        val total: Int,
        val percent: Int,
        val averageDailyScore: Float,
        val completedMinutes: Int
    )
}

private data class BarPoint(
    val label: String,
    val ratio: Float?
)

private class ProductivityBarChartView(
    context: android.content.Context,
    private val points: List<BarPoint>
) : View(context) {
    private val density = resources.displayMetrics.density
    private val scaled = resources.displayMetrics.scaledDensity

    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#E4E9F1")
        strokeWidth = density
    }
    private val axisPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#7A859D")
        strokeWidth = density
    }
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#4E5D7D")
        textSize = 11f * scaled
        textAlign = Paint.Align.CENTER
    }
    private val valuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#111827")
        textSize = 11f * scaled
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
    }
    private val sidePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#7A859D")
        textSize = 9f * scaled
        textAlign = Paint.Align.RIGHT
    }
    private val barPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (points.isEmpty()) return

        val left = 34f * density
        val right = width - 8f * density
        val top = 22f * density
        val bottom = height - 34f * density
        val chartHeight = bottom - top

        for (step in 0..4) {
            val ratio = step / 4f
            val y = bottom - chartHeight * ratio
            canvas.drawLine(left, y, right, y, gridPaint)
            canvas.drawText("${(ratio * 100).toInt()}", left - 6f * density, y + 3f * density, sidePaint)
        }
        canvas.drawLine(left, top, left, bottom, axisPaint)
        canvas.drawLine(left, bottom, right, bottom, axisPaint)

        val slot = (right - left) / points.size
        val maxBarWidth = 38f * density
        val barWidth = minOf(maxBarWidth, slot * 0.55f)
        val radius = 7f * density

        points.forEachIndexed { index, point ->
            val centerX = left + slot * index + slot / 2f
            val ratio = point.ratio?.coerceIn(0f, 1f)
            if (ratio != null) {
                val barTop = bottom - chartHeight * ratio
                val strong = performanceColor(ratio)
                val soft = blendWithWhite(strong, 0.62f)
                barPaint.shader = LinearGradient(
                    centerX,
                    barTop,
                    centerX,
                    bottom,
                    strong,
                    soft,
                    Shader.TileMode.CLAMP
                )
                canvas.drawRoundRect(
                    RectF(centerX - barWidth / 2f, barTop, centerX + barWidth / 2f, bottom),
                    radius,
                    radius,
                    barPaint
                )
                barPaint.shader = null
                canvas.drawText("${(ratio * 100f).roundToInt()}%", centerX, maxOf(top + 10f * density, barTop - 6f * density), valuePaint)
            } else {
                canvas.drawText("—", centerX, bottom - 8f * density, valuePaint)
            }
            canvas.drawText(point.label, centerX, height - 10f * density, labelPaint)
        }
    }
}

private class CalendarHeatmapView(
    context: android.content.Context,
    month: Calendar,
    private val tasks: List<RecurringTask>,
    private val planner: PlannerStore
) : View(context) {
    private val density = resources.displayMetrics.density
    private val scaled = resources.displayMetrics.scaledDensity
    private val monthStart = (month.clone() as Calendar).apply {
        set(Calendar.DAY_OF_MONTH, 1)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#25324B")
        textSize = 11f * scaled
        textAlign = Paint.Align.CENTER
    }
    private val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#67748F")
        textSize = 10f * scaled
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
    }
    private val cellPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val todayStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#20B15A")
        style = Paint.Style.STROKE
        strokeWidth = 2f * density
    }
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val labels = listOf("Lun", "Mar", "Mié", "Jue", "Vie", "Sáb", "Dom")
        val gap = 4f * density
        val cellW = (width - gap * 6f) / 7f
        val headerH = 28f * density
        val cellH = (height - headerH - gap * 5f) / 6f

        labels.forEachIndexed { index, label ->
            val x = index * (cellW + gap) + cellW / 2f
            canvas.drawText(label, x, 15f * density, headerPaint)
        }

        val firstDay = monthStart.get(Calendar.DAY_OF_WEEK)
        val mondayIndex = (firstDay + 5) % 7
        val daysInMonth = monthStart.getActualMaximum(Calendar.DAY_OF_MONTH)
        val today = planner.todayKey()

        for (day in 1..daysInMonth) {
            val cellIndex = mondayIndex + day - 1
            val row = cellIndex / 7
            val col = cellIndex % 7
            val left = col * (cellW + gap)
            val top = headerH + row * (cellH + gap)
            val rect = RectF(left, top, left + cellW, top + cellH)

            val cal = (monthStart.clone() as Calendar).apply { set(Calendar.DAY_OF_MONTH, day) }
            val key = dateFormat.format(cal.time)
            val ratio = if (key > today) null else ratioFor(cal, key)
            val fill = heatColor(ratio)

            cellPaint.color = fill
            cellPaint.style = Paint.Style.FILL
            canvas.drawRoundRect(rect, 8f * density, 8f * density, cellPaint)

            if (key == today) {
                val inset = 1.5f * density
                canvas.drawRoundRect(
                    RectF(rect.left + inset, rect.top + inset, rect.right - inset, rect.bottom - inset),
                    8f * density,
                    8f * density,
                    todayStroke
                )
            }

            textPaint.color = if (ratio != null && ratio >= 0.8f) Color.WHITE else Color.parseColor("#25324B")
            canvas.drawText(day.toString(), rect.centerX(), rect.centerY() + 4f * density, textPaint)
        }
    }

    private fun ratioFor(cal: Calendar, key: String): Float? {
        val scheduled = tasks.filter { planner.scheduledFor(it, cal) }
        if (scheduled.isEmpty()) return null
        val done = scheduled.count { key in it.completedDates }
        return done.toFloat() / scheduled.size.toFloat()
    }
}

private fun performanceColor(ratio: Float): Int = when {
    ratio >= 0.67f -> Color.parseColor("#34D665")
    ratio >= 0.34f -> Color.parseColor("#FFD43B")
    else -> Color.parseColor("#FF4D57")
}

private fun heatColor(ratio: Float?): Int = when {
    ratio == null -> Color.parseColor("#E4E8EE")
    ratio >= 0.80f -> Color.parseColor("#28D85F")
    ratio >= 0.60f -> Color.parseColor("#8BE84A")
    ratio >= 0.40f -> Color.parseColor("#FFD43B")
    ratio >= 0.20f -> Color.parseColor("#FF8A3D")
    else -> Color.parseColor("#FF4D57")
}

private fun blendWithWhite(color: Int, amount: Float): Int {
    val a = amount.coerceIn(0f, 1f)
    val r = (Color.red(color) + (255 - Color.red(color)) * a).roundToInt()
    val g = (Color.green(color) + (255 - Color.green(color)) * a).roundToInt()
    val b = (Color.blue(color) + (255 - Color.blue(color)) * a).roundToInt()
    return Color.rgb(r, g, b)
}
