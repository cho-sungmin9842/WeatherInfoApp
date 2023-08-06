package fastcampus.part2.chapter7

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.IBinder
import android.widget.RemoteViews
import android.widget.RemoteViews.RemoteView
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationToken
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.gms.tasks.OnTokenCanceledListener

class UpdateWeatherService : Service() {
    // 알림 채널명 상수 정의
    companion object {
        const val NOTIFICATION_CHANNEL = "widget_refresh_channel"
    }

    // BindService 하지 않음
    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    // 서비스 시작될 시 호출됨
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // 알림채널을 생성한다.
        createChannel()
        // foreground 서비스를 시작함
        startForeground(1, createNotification())
        // appWidgetManager 객체 생성
        val appWidgetManager = AppWidgetManager.getInstance(this)
        // ACCESS_BACKGROUND_LOCATION 권한이 없는 경우
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // SettingActivity로 이동하는 PendingIntent 객체 생성 및 설정
            val pendingIntent = Intent(this, SettingActivity::class.java).let {
                PendingIntent.getActivity(this, 2, it, PendingIntent.FLAG_IMMUTABLE)
            }
            // RemoteViews 객체 생성 및 설정
            RemoteViews(packageName, R.layout.widget_weather).apply {
                setTextViewText(R.id.temperatureTextView, "권한없음")
                setTextViewText(R.id.weatherTextView, "")
                // 클릭시 SettingActivity를 실행한다.(pendingIntent)
                setOnClickPendingIntent(R.id.temperatureTextView, pendingIntent)
            }.also { remoteViews ->
                val appWidgetName = ComponentName(this, WeatherAppWidgetProvider::class.java)
                // RemoteViews로 해당 위젯을 업데이트한다.
                appWidgetManager.updateAppWidget(appWidgetName, remoteViews)
            }
            // 서비스를 종료함
            stopSelf()
            return super.onStartCommand(intent, flags, startId)
        }
        // 마지막으로 저장된 위치 정보
        LocationServices.getFusedLocationProviderClient(this)
            .getCurrentLocation(CurrentLocationRequest.Builder()
                .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
                .build(),
                object : CancellationToken() {
                    override fun onCanceledRequested(p0: OnTokenCanceledListener) =
                        CancellationTokenSource().token

                    override fun isCancellationRequested() = false
                })
            .addOnSuccessListener {
                WeatherRepository.getVillageForecast(
                    context = this,
                    longitude = it.longitude,
                    latitude = it.latitude,
                    // 성공 처리
                    successCallback = { forecastList ->
                        // UpdateWeatherService 서비스를 실행하는 pendingIntent
                        val pendingServiceIntent =
                            Intent(this, UpdateWeatherService::class.java).let { intent ->
                                PendingIntent.getService(
                                    this,
                                    1,
                                    intent,
                                    PendingIntent.FLAG_IMMUTABLE
                                )
                            }
                        // forecastList의 첫번째 Forecast를 가져온다(현재 날씨 대한 정보)
                        val currentForecast = forecastList.first()
                        RemoteViews(packageName, R.layout.widget_weather).apply {
                            setTextViewText(
                                R.id.temperatureTextView,
                                getString(R.string.temperature_text, currentForecast.temperature)
                            )
                            setTextViewText(R.id.weatherTextView, currentForecast.weather)
                            // 클릭시 UpdateWeatherService 서비스를 시작한다.
                            setOnClickPendingIntent(R.id.temperatureTextView, pendingServiceIntent)
                        }.also { remoteViews ->
                            val appWidgetName =
                                ComponentName(this, WeatherAppWidgetProvider::class.java)
                            // RemoteViews로 해당 위젯을 업데이트한다.
                            appWidgetManager.updateAppWidget(appWidgetName, remoteViews)
                        }
                        // 서비스를 종료함
                        stopSelf()
                    },
                    // 실패 처리
                    failureCallback = {
                        val pendingServiceIntent =
                            Intent(this, UpdateWeatherService::class.java).let { intent ->
                                PendingIntent.getService(
                                    this,
                                    1,
                                    intent,
                                    PendingIntent.FLAG_IMMUTABLE
                                )
                            }
                        // RemoteViews 객체 생성 및 설정
                        RemoteViews(packageName, R.layout.widget_weather).apply {
                            setTextViewText(R.id.temperatureTextView, "에러")
                            setTextViewText(R.id.weatherTextView, "")
                            // 클릭시 UpdateWeatherService 서비스를 시작한다.
                            setOnClickPendingIntent(R.id.temperatureTextView, pendingServiceIntent)
                        }.also { remoteViews ->
                            val appWidgetName =
                                ComponentName(this, WeatherAppWidgetProvider::class.java)
                            // RemoteViews로 해당 위젯을 업데이트한다.
                            appWidgetManager.updateAppWidget(appWidgetName, remoteViews)
                        }
                        // 서비스를 종료함
                        stopSelf()
                    }
                )
            }
        return super.onStartCommand(intent, flags, startId)
    }

    // 알림 채널 생성
    private fun createChannel() {
        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL,
            "날씨앱",
            NotificationManager.IMPORTANCE_LOW
        )
        channel.description = "위젯을 업데이트하는 채널"
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
    }

    // 알림 생성 및 알림 반환
    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL)
            .setSmallIcon(R.drawable.ic_launcher_background)
            .setContentTitle("날씨앱")
            .setContentText("날씨 업데이트")
            .build()
    }

    // 서비스 제거 라이프사이클 콜백
    override fun onDestroy() {
        super.onDestroy()
        // foreground 서비스를 종료함. 알림이 취소되고 보여지지 않음
        stopForeground(STOP_FOREGROUND_REMOVE)
    }
}