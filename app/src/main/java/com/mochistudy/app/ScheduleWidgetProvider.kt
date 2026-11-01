package com.mochistudy.app

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
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
                var bestTime = "99:99"
                for (i in 0 until lessons.length()) {
                    val item = lessons.getJSONObject(i)
                    val date = item.optString("date")
                    val time = item.optString("time")
                    if (date == today && time.isNotBlank() && time >= now && time < bestTime) {
                        bestTime = time
                        title = item.optString("title", "Занятие")
                        detail = "$time · ${item.optString("room", "аудитория не указана")}" 
                    }
                }
                if (bestTime == "99:99" && raw != null) {
                    title = "На сегодня пар больше нет 🌸"
                    detail = "Хорошего дня!"
                }
            } catch (_: Exception) { }
            views.setTextViewText(R.id.widget_title, title)
            views.setTextViewText(R.id.widget_detail, detail)
            val intent = Intent(context, MainActivity::class.java)
            val pending = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            views.setOnClickPendingIntent(R.id.widget_root, pending)
            manager.updateAppWidget(id, views)
        }
    }
}
