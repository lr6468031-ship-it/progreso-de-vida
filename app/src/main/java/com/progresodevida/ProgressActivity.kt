package com.progresodevida

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.text.InputType
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.abs

class ProgressActivity : Activity() {
    private val fondo = Color.parseColor("#0E130B")
    private val superficie = Color.parseColor("#1B2516")
    private val superficie2 = Color.parseColor("#26341F")
    private val verde = Color.parseColor("#4CD964")
    private val verdeSuave = Color.parseColor("#CFF5D5")
    private val texto = Color.parseColor("#F5F7EA")
    private val texto2 = Color.parseColor("#B9C6AD")
    private val amarillo = Color.parseColor("#E1C75B")

    private lateinit var body: LinearLayout
    private lateinit var store: ProgressStore
    private var mode = Mode.WEEK
    private var draftPhotoUri: String? = null
    private var draftPhotoView: ImageView? = null
    private var openTodayOnce = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.BLACK
        window.navigationBarColor = Color.BLACK
        store = ProgressStore(this)
        if (intent.getBooleanExtra(EXTRA_OPEN_TODAY, false)) {
            mode = Mode.DAY
            openTodayOnce = true
        }
        setContentView(buildScreen())
        render()
    }

    override fun onResume() {
        super.onResume()
        if (::body.isInitialized) render()
        if (openTodayOnce) {
            openTodayOnce = false
            body.post { showDayEditor(store.todayKey()) }
        }
    }

    private fun buildScreen(): View {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(fondo)
        }
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(14), dp(16), dp(8))
        }
        header.addView(TextView(this).apply {
            text = "📈 Mi progreso"
            textSize = 27f
            setTextColor(texto)
            setTypeface(typeface, Typeface.BOLD)
        })
        header.addView(TextView(this).apply {
            text = "Ejercicio · Música · Vida social · Trabajo"
            textSize = 12f
            setTextColor(texto2)
        })
        root.addView(header)

        val tabs = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(dp(12), dp(2), dp(12), dp(10))
        }
        tabs.addView(tab("Día", Mode.DAY), weight())
        tabs.addView(tab("Semana", Mode.WEEK), weight())
        tabs.addView(tab("Mes", Mode.MONTH), weight())
        tabs.addView(tab("Año", Mode.YEAR), weight())
        root.addView(tabs)

        val scroll = ScrollView(this).apply { isFillViewport = true }
        body = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(4), dp(14), dp(30))
        }
        scroll.addView(body, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        root.addView(scroll, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
        return root
    }

    private fun tab(label: String, target: Mode) = Button(this).apply {
        text = label
        textSize = 11f
        isAllCaps = false
        setTextColor(if (mode == target) Color.BLACK else verdeSuave)
        background = pill(if (mode == target) verde else superficie2)
        stateListAnimator = null
        setOnClickListener {
            mode = target
            setContentView(buildScreen())
            render()
        }
    }

    private fun render() {
        body.removeAllViews()
        val now = Calendar.getInstance()
        val period = period(now)
        val summary = store.summary(period.start, period.end)
        val previous = store.summary(period.previousStart, period.previousEnd)

        body.addView(TextView(this).apply {
            text = period.label
            textSize = 19f
            setTextColor(texto)
            setTypeface(typeface, Typeface.BOLD)
            setPadding(dp(2), dp(4), dp(2), dp(8))
        })

        if (mode == Mode.DAY) {
            val today = store.day(store.todayKey())
            body.addView(primaryButton(if (today == null) "＋ Registrar mi día" else "✎ Editar mi día") {
                showDayEditor(store.todayKey())
            })
            body.addView(todaySnapshot(today), top(10))
        }

        body.addView(summaryGrid(summary), top(10))
        body.addView(analysisCard(summary, previous), top(10))
        body.addView(categoryCard(summary), top(10))

        if (mode == Mode.YEAR) renderYearMonths(now) else renderTimeline(period.start, period.end)
    }

    private fun summaryGrid(summary: ProgressSummary): View {
        val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val row1 = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        row1.addView(metric("Horas", hours(summary.totalHours), verde), weight(0, 4))
        row1.addView(metric("Días activos", summary.activeDays.toString(), verde), weight(4, 0))
        box.addView(row1)
        val row2 = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dp(8), 0, 0)
        }
        row2.addView(metric("Días sin hacer", summary.inactiveDays.toString(), amarillo), weight(0, 4))
        row2.addView(metric("Racha", "${summary.currentStreak} días", verdeSuave), weight(4, 0))
        box.addView(row2)
        return box
    }

    private fun analysisCard(current: ProgressSummary, previous: ProgressSummary): View {
        val difference = current.totalHours - previous.totalHours
        val pct = if (previous.totalHours <= 0.0) null else difference / previous.totalHours * 100.0
        val consistency = if (current.totalDays <= 0) 0 else current.activeDays * 100 / current.totalDays
        val mood = if (current.averageMood <= 0.0) "Sin datos" else String.format(Locale.US, "%.1f/5", current.averageMood)
        val compare = when {
            previous.totalHours <= 0.0 && current.totalHours > 0.0 -> "Empezaste a registrar progreso en este periodo."
            pct == null -> "Todavía no hay periodo anterior para comparar."
            difference > 0 -> "Subiste ${String.format(Locale.US, "%.0f", pct)}% frente al periodo anterior."
            difference < 0 -> "Bajaste ${String.format(Locale.US, "%.0f", abs(pct))}% frente al periodo anterior."
            else -> "Invertiste el mismo tiempo que en el periodo anterior."
        }
        return card().apply {
            addView(title("Análisis"))
            addView(bodyText(
                "Constancia: $consistency% · Ánimo promedio: $mood\n" +
                    "Más tiempo: ${current.bestCategory ?: "Sin actividad dominante"}\n$compare"
            ))
        }
    }

    private fun categoryCard(summary: ProgressSummary): View = card().apply {
        addView(title("Tiempo por área"))
        addView(bodyText(
            "🏋️ Ejercicio: ${hours(summary.exerciseHours)}\n" +
                "🎵 Música: ${hours(summary.musicHours)}\n" +
                "🧑‍🤝‍🧑 Vida social: ${hours(summary.socialHours)}\n" +
                "💼 Trabajo: ${hours(summary.workHours)}"
        ))
    }

    private fun todaySnapshot(day: ProgressDay?): View {
        if (day == null) return infoCard("Hoy todavía no registraste nada. También puedes registrar un día de descanso con 0 horas y escribir cómo te sentiste.")
        val box = card()
        if (!day.photoUri.isNullOrBlank()) {
            val image = ImageView(this).apply {
                scaleType = ImageView.ScaleType.CENTER_CROP
                try { setImageURI(Uri.parse(day.photoUri)) } catch (_: Exception) { }
            }
            box.addView(image, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(180)).apply { bottomMargin = dp(10) })
        }
        box.addView(title("Hoy · ${hours(day.totalHours)}"))
        box.addView(bodyText(
            "Hice: ${day.did.ifBlank { "Sin descripción" }}\n" +
                "Me sentí: ${day.feeling.ifBlank { "Sin registro" }}\n" +
                "Ánimo: ${moodText(day.mood)}"
        ))
        return box
    }

    private fun renderTimeline(start: Calendar, requestedEnd: Calendar) {
        body.addView(title("Día por día"), top(14))
        val entries = store.allDays().associateBy { it.date }
        val tomorrow = store.startOfDay(Calendar.getInstance()).apply { add(Calendar.DAY_OF_MONTH, 1) }
        val end = if (requestedEnd.after(tomorrow)) tomorrow else requestedEnd
        val cursor = store.startOfDay(start)
        val rows = mutableListOf<Pair<String, ProgressDay?>>()
        while (cursor.before(end)) {
            val key = store.dateKey(cursor)
            rows += key to entries[key]
            cursor.add(Calendar.DAY_OF_MONTH, 1)
        }
        rows.asReversed().forEach { (date, day) -> body.addView(dayCard(date, day), top(7)) }
    }

    private fun dayCard(date: String, day: ProgressDay?): View {
        val cal = store.calendarFor(date)
        val readable = if (cal == null) date else SimpleDateFormat("EEEE d MMM", Locale("es", "PE")).format(cal.time)
        val box = card().apply {
            isClickable = true
            setOnClickListener { showDayEditor(date) }
        }
        if (day == null) {
            box.addView(TextView(this).apply {
                text = "$readable · No se registró actividad"
                textSize = 13f
                setTextColor(texto2)
            })
            return box
        }
        box.addView(TextView(this).apply {
            text = "$readable · ${hours(day.totalHours)}"
            textSize = 14f
            setTextColor(if (day.totalHours > 0) verde else amarillo)
            setTypeface(typeface, Typeface.BOLD)
        })
        box.addView(bodyText(
            "🏋️ ${hours(day.exerciseHours)}  🎵 ${hours(day.musicHours)}  🧑‍🤝‍🧑 ${hours(day.socialHours)}  💼 ${hours(day.workHours)}\n" +
                "${day.did.ifBlank { "Sin descripción" }}${if (day.feeling.isNotBlank()) "\nMe sentí: ${day.feeling}" else ""}"
        ))
        return box
    }

    private fun renderYearMonths(now: Calendar) {
        body.addView(title("Mes por mes"), top(14))
        val yearStart = store.startOfYear(now)
        for (month in 11 downTo 0) {
            val start = (yearStart.clone() as Calendar).apply { add(Calendar.MONTH, month) }
            if (start.after(now)) continue
            val end = (start.clone() as Calendar).apply { add(Calendar.MONTH, 1) }
            val summary = store.summary(start, end)
            val name = SimpleDateFormat("MMMM", Locale("es", "PE")).format(start.time)
            body.addView(card().apply {
                addView(TextView(this@ProgressActivity).apply {
                    text = "${name.replaceFirstChar { it.titlecase(Locale("es", "PE")) }} · ${hours(summary.totalHours)}"
                    textSize = 14f
                    setTextColor(if (summary.totalHours > 0) verde else texto2)
                    setTypeface(typeface, Typeface.BOLD)
                })
                addView(bodyText("${summary.activeDays} días activos · ${summary.inactiveDays} sin actividad · ${summary.bestCategory ?: "sin categoría dominante"}"))
            }, top(7))
        }
    }

    private fun showDayEditor(date: String) {
        val existing = store.day(date)
        draftPhotoUri = existing?.photoUri
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(4), dp(20), dp(4))
        }
        draftPhotoView = ImageView(this).apply {
            scaleType = ImageView.ScaleType.CENTER_CROP
            background = GradientDrawable().apply { setColor(Color.LTGRAY); cornerRadius = dp(12).toFloat() }
            refreshDraftPhoto(this)
        }
        box.addView(draftPhotoView, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(150)).apply { bottomMargin = dp(6) })
        box.addView(Button(this).apply {
            text = if (draftPhotoUri.isNullOrBlank()) "📷 Elegir foto del día" else "📷 Cambiar foto"
            isAllCaps = false
            setOnClickListener { choosePhoto() }
        })

        val exercise = numberField("Horas de ejercicio", existing?.exerciseHours)
        val music = numberField("Horas de música", existing?.musicHours)
        val social = numberField("Horas de vida social", existing?.socialHours)
        val work = numberField("Horas de trabajo", existing?.workHours)
        val did = textField("¿Qué hiciste hoy?", existing?.did, true)
        val feeling = textField("¿Cómo te fue y cómo te sentiste?", existing?.feeling, true)
        val mood = numberField("Ánimo del 1 al 5", existing?.mood?.toDouble())
        listOf(exercise, music, social, work, did, feeling, mood).forEach { box.addView(it) }

        val dialog = AlertDialog.Builder(this)
            .setTitle("Registro · ${prettyDate(date)}")
            .setView(ScrollView(this).apply { addView(box) })
            .setPositiveButton("Guardar", null)
            .setNeutralButton(if (existing == null) "" else "Eliminar", null)
            .setNegativeButton("Cancelar", null)
            .create()
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                store.saveDay(
                    ProgressDay(
                        date = date,
                        exerciseHours = parseNumber(exercise) ?: 0.0,
                        musicHours = parseNumber(music) ?: 0.0,
                        socialHours = parseNumber(social) ?: 0.0,
                        workHours = parseNumber(work) ?: 0.0,
                        did = did.text.toString().trim(),
                        feeling = feeling.text.toString().trim(),
                        mood = (parseNumber(mood) ?: 0.0).toInt().coerceIn(0, 5),
                        photoUri = draftPhotoUri
                    )
                )
                draftPhotoView = null
                dialog.dismiss()
                render()
            }
            if (existing != null) {
                dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener {
                    AlertDialog.Builder(this)
                        .setTitle("Eliminar registro")
                        .setMessage("¿Eliminar el registro de ${prettyDate(date)}?")
                        .setNegativeButton("No", null)
                        .setPositiveButton("Sí") { _, _ -> store.deleteDay(date); dialog.dismiss(); render() }
                        .show()
                }
            } else {
                dialog.getButton(AlertDialog.BUTTON_NEUTRAL).visibility = View.GONE
            }
        }
        dialog.setOnDismissListener { draftPhotoView = null }
        dialog.show()
    }

    private fun choosePhoto() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "image/*"
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
        }
        @Suppress("DEPRECATION") startActivityForResult(intent, REQ_PHOTO)
    }

    @Deprecated("Deprecated in Android API, kept for device compatibility")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != REQ_PHOTO || resultCode != RESULT_OK) return
        val uri = data?.data ?: return
        try { contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) } catch (_: Exception) { }
        draftPhotoUri = uri.toString()
        draftPhotoView?.let { refreshDraftPhoto(it) }
    }

    private fun refreshDraftPhoto(view: ImageView) {
        view.setImageDrawable(null)
        val uri = draftPhotoUri ?: return
        try { view.setImageURI(Uri.parse(uri)) } catch (_: Exception) { }
    }

    private fun period(now: Calendar): Period {
        val today = store.startOfDay(now)
        return when (mode) {
            Mode.DAY -> {
                val end = (today.clone() as Calendar).apply { add(Calendar.DAY_OF_MONTH, 1) }
                val previous = (today.clone() as Calendar).apply { add(Calendar.DAY_OF_MONTH, -1) }
                Period(today, end, previous, today, "Hoy")
            }
            Mode.WEEK -> {
                val start = store.startOfWeek(now)
                val end = (start.clone() as Calendar).apply { add(Calendar.DAY_OF_MONTH, 7) }
                val previousStart = (start.clone() as Calendar).apply { add(Calendar.DAY_OF_MONTH, -7) }
                Period(start, end, previousStart, start, "Esta semana")
            }
            Mode.MONTH -> {
                val start = store.startOfMonth(now)
                val end = (start.clone() as Calendar).apply { add(Calendar.MONTH, 1) }
                val previousStart = (start.clone() as Calendar).apply { add(Calendar.MONTH, -1) }
                Period(start, end, previousStart, start, "Este mes")
            }
            Mode.YEAR -> {
                val start = store.startOfYear(now)
                val end = (start.clone() as Calendar).apply { add(Calendar.YEAR, 1) }
                val previousStart = (start.clone() as Calendar).apply { add(Calendar.YEAR, -1) }
                Period(start, end, previousStart, start, "Este año")
            }
        }
    }

    private fun prettyDate(date: String): String {
        val cal = store.calendarFor(date) ?: return date
        return SimpleDateFormat("EEEE d 'de' MMMM", Locale("es", "PE")).format(cal.time)
    }

    private fun moodText(value: Int): String = when (value) {
        1 -> "1/5 · Muy bajo"
        2 -> "2/5 · Bajo"
        3 -> "3/5 · Normal"
        4 -> "4/5 · Bien"
        5 -> "5/5 · Muy bien"
        else -> "Sin registro"
    }

    private fun metric(label: String, value: String, accent: Int): View = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(12), dp(11), dp(12), dp(11))
        background = cardBg()
        addView(TextView(this@ProgressActivity).apply { text = label; textSize = 10f; setTextColor(texto2) })
        addView(TextView(this@ProgressActivity).apply { text = value; textSize = 17f; setTextColor(accent); setTypeface(typeface, Typeface.BOLD) })
    }

    private fun card() = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(13), dp(12), dp(13), dp(12))
        background = cardBg()
    }

    private fun infoCard(value: String) = TextView(this).apply {
        text = value; textSize = 13f; setTextColor(texto2); setPadding(dp(14), dp(16), dp(14), dp(16)); background = cardBg()
    }

    private fun title(value: String) = TextView(this).apply {
        text = value; textSize = 15f; setTextColor(texto); setTypeface(typeface, Typeface.BOLD); setPadding(0, 0, 0, dp(5))
    }

    private fun bodyText(value: String) = TextView(this).apply {
        text = value; textSize = 12f; setTextColor(texto2); setLineSpacing(0f, 1.15f)
    }

    private fun primaryButton(label: String, action: () -> Unit) = Button(this).apply {
        text = label; isAllCaps = false; textSize = 14f; setTextColor(Color.BLACK); setTypeface(typeface, Typeface.BOLD)
        background = pill(verde); stateListAnimator = null; setOnClickListener { action() }
    }

    private fun textField(hint: String, value: String?, multiline: Boolean = false) = EditText(this).apply {
        this.hint = hint; setText(value.orEmpty()); setTextColor(Color.BLACK); setHintTextColor(Color.DKGRAY)
        inputType = InputType.TYPE_CLASS_TEXT or if (multiline) InputType.TYPE_TEXT_FLAG_MULTI_LINE else 0
        isSingleLine = !multiline
        if (multiline) minLines = 2
    }

    private fun numberField(hint: String, value: Double?) = EditText(this).apply {
        this.hint = hint; inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
        if (value != null && value > 0.0) setText(number(value))
        setTextColor(Color.BLACK); setHintTextColor(Color.DKGRAY); isSingleLine = true
    }

    private fun parseNumber(field: EditText): Double? = field.text.toString().trim().replace(',', '.').toDoubleOrNull()
    private fun number(value: Double): String = if (value % 1.0 == 0.0) value.toLong().toString() else String.format(Locale.US, "%.2f", value)
    private fun hours(value: Double): String = "${number(value)} h"

    private fun cardBg() = GradientDrawable().apply { setColor(superficie); cornerRadius = dp(15).toFloat(); setStroke(dp(2), verde) }
    private fun pill(color: Int) = GradientDrawable().apply { setColor(color); cornerRadius = dp(13).toFloat(); if (color != verde) setStroke(dp(1), verde) }
    private fun weight(start: Int = 3, end: Int = 3) = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply { marginStart = dp(start); marginEnd = dp(end) }
    private fun top(value: Int) = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(value) }
    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private enum class Mode { DAY, WEEK, MONTH, YEAR }
    private data class Period(val start: Calendar, val end: Calendar, val previousStart: Calendar, val previousEnd: Calendar, val label: String)

    companion object {
        const val EXTRA_OPEN_TODAY = "open_today"
        private const val REQ_PHOTO = 830
    }
}
