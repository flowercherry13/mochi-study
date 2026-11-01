package com.mochistudy.app

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.app.PendingIntent
import android.widget.RemoteViews
import org.json.JSONArray
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ScheduleWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        ids.forEach { updateOne(context, manager, it) }
    }

    companion object {
        fun refreshAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, ScheduleWidgetProvider::class.java))
            ids.forEach { updateOne(context, manager, it) }
        }

        private fun updateOne(context: Context, manager: AppWidgetManager, id: Int) {
            val views = RemoteViews(context.packageName, R.layout.widget_schedule)
            val prefs = context.getSharedPreferences("mochi_widget", Context.MODE_PRIVATE)
            val raw = prefs.getString("schedule_json", null)
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
            val now = SimpleDateFormat("HH:mm", Locale.US).format(Date())
            var title = "Открой Mochi Study 🍡"
            var detail = "Синхронизируй расписание в приложении"
            try {
                val lessons = JSONArray(raw ?: "[]")
                var bestStart = "99:99"
                var foundToday = false
                for (i in 0 until lessons.length()) {
                    val item = lessons.getJSONObject(i)
                    if (item.optString("date") != today) continue
                    foundToday = true
                    val fullTime = item.optString("time").trim()
                    val start = fullTime.substringBefore("–").substringBefore("-").trim()
                    if (start.matches(Regex("\\d{2}:\\d{2}")) && start >= now && start < bestStart) {
                        bestStart = start
                        title = item.optString("title", "Занятие")
                        val room = item.optString("room").ifBlank { "аудитория не указана" }
                        detail = "$fullTime · $room"
                    }
                }
                if (bestStart == "99:99" && raw != null) {
                    title = if (foundToday) "На сегодня пар больше нет 🌸" else "Сегодня занятий нет 🌸"
                    detail = "Хорошего дня!"
                }
            } catch (_: Exception) {
                title = "Не удалось прочитать расписание"
                detail = "Открой Mochi Study"
            }
            views.setTextViewText(R.id.widget_title, title)
            views.setTextViewText(R.id.widget_detail, detail)
            val intent = Intent(context, MainActivity::class.java)
            val pending = PendingIntent.getActivity(
                context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_root, pending)
            manager.updateAppWidget(id, views)
        }
    }
}
