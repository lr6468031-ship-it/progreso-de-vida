package com.progresodevida

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class ProgressWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        appWidgetIds.forEach { id -> updateWidget(context, appWidgetManager, id) }
    }

    companion object {
        fun updateWidget(context: Context, manager: AppWidgetManager, id: Int) {
            val store = PlannerStore(context)
            val today = Calendar.getInstance()
            val tasks = store.tasks().filter { store.scheduledFor(it, today) }
            val done = tasks.count { it.completedDates.contains(store.todayKey()) }
            val total = tasks.size
            val pct = if (total == 0) 0 else done * 100 / total
            val next = tasks.firstOrNull { !it.completedDates.contains(store.todayKey()) }
            val date = SimpleDateFormat("EEE, d MMM", Locale("es", "PE")).format(today.time)

            val views = RemoteViews(context.packageName, R.layout.widget_progress)
            views.setTextViewText(R.id.widget_date, "Hoy, $date")
            views.setTextViewText(R.id.widget_progress_text, "$done/$total ($pct%)")
            views.setProgressBar(R.id.widget_progress_bar, 100, pct, false)
            views.setTextViewText(R.id.widget_next, next?.let { "○ ${it.title}   ${it.time}" } ?: if (total == 0) "+ Crea tu primera tarea" else "✓ Todo completado")

            val intent = Intent(context, PlannerActivity::class.java)
            val pending = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            views.setOnClickPendingIntent(R.id.widget_root, pending)
            manager.updateAppWidget(id, views)
        }
    }
}
