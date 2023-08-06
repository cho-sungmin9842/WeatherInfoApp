package fastcampus.part2.chapter7

import android.app.ForegroundServiceStartNotAllowedException
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.RemoteViews
import androidx.core.content.ContextCompat

class WeatherAppWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        // 앱 위젯 클릭시 이벤트 등록 (appWidgetId은 앱 위젯이 등록될 때마다 부여받는 ID)
        appWidgetIds.forEach { appWidgetId ->
            // PendingIntent를 통해 ForegroundService를 실행하게 된다.
            val pendingIntent = Intent(context, UpdateWeatherService::class.java).let { intent ->
                PendingIntent.getForegroundService(context, 1, intent, PendingIntent.FLAG_IMMUTABLE)
            }
            // RemoteViews 생성자로 views를 리스너 설정
            val views = RemoteViews(context.packageName, R.layout.widget_weather).apply {
                setOnClickPendingIntent(R.id.temperatureTextView, pendingIntent)
            }
            // 해당 위젯을 업데이트한다.
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
        val serviceIntent = Intent(context, UpdateWeatherService::class.java)
        // SDK 31 이상의 경우
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                // serviceIntent를 foregroundService로 시작함
                ContextCompat.startForegroundService(context, serviceIntent)
            } catch (e: ForegroundServiceStartNotAllowedException) {
                e.printStackTrace()
            }
        } else {
            // serviceIntent를 foregroundService로 시작함
            ContextCompat.startForegroundService(context, serviceIntent)
        }
    }
}