package fastcampus.part2.chapter7

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import fastcampus.part2.chapter7.databinding.ActivitySettingBinding

class SettingActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySettingBinding
    private val locationPermissionRequest =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            when {
                // ACCESS_BACKGROUND_LOCATION 권한을 허용한 경우
                permissions.getOrDefault(Manifest.permission.ACCESS_BACKGROUND_LOCATION, false) -> {
                    // foregroundService 실행(UpdateWeatherService로)
                    ContextCompat.startForegroundService(
                        this,
                        Intent(this, UpdateWeatherService::class.java)
                    )
                }
                // ACCESS_BACKGROUND_LOCATION 권한을 허용하지 않은 경우
                else -> {
                    Toast.makeText(this, "위치 권한이 필요합니다.", Toast.LENGTH_SHORT).show()
                }
            }
        }

    override fun onStart() {
        super.onStart()
        // SDK 버전이 33 이상인 경우
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // ACCESS_BACKGROUND_LOCATION(백그라운드 위치) 권한과 POST_NOTIFICATIONS(알림) 권한을 요청한다.
            locationPermissionRequest.launch(
                arrayOf(
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION,
                    Manifest.permission.POST_NOTIFICATIONS
                )
            )
        }
        // SDK 버전이 29 이상인 경우
        else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // ACCESS_BACKGROUND_LOCATION(백그라운드 위치) 권한을 요청한다.
            locationPermissionRequest.launch(
                arrayOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
            )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingBinding.inflate(layoutInflater)
        setContentView(binding.root)
        // 권한 설정하기 버튼 클릭시
        binding.settingButton.setOnClickListener {
            // 사용자가 직접 앱에 권한을 주는 intent를 띄움
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", packageName, null)
            }
            startActivity(intent)
        }
    }
}
