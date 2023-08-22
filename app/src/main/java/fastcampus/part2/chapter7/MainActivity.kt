package fastcampus.part2.chapter7

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.isVisible
import com.google.android.gms.location.*
import com.google.android.gms.tasks.CancellationToken
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.gms.tasks.OnTokenCanceledListener
import fastcampus.part2.chapter7.databinding.ActivityMainBinding
import fastcampus.part2.chapter7.databinding.ItemFirstBinding
import fastcampus.part2.chapter7.databinding.ItemForecastBinding
import java.util.*


class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var lifecycleState: String
    private val locationPermissionRequest =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            when {
                permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true -> {
                    updateLocation()
                }

                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true -> {
                    updateLocation()
                }

                permissions[Manifest.permission.ACCESS_FINE_LOCATION] == false -> {
                    showDialog()
                }

                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == false -> {
                    showDialog()
                }
            }
        }

    private fun showDialog() {
        AlertDialog.Builder(this).apply {
            setTitle("위치 정보를 얻기위해서는 위치 권한이 필요합니다")
            setPositiveButton("허용") { _, _ ->
                val findLocation = ActivityCompat.shouldShowRequestPermissionRationale(
                    this@MainActivity,
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
                val courseLocation = ActivityCompat.shouldShowRequestPermissionRationale(
                    this@MainActivity,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
                if (findLocation && courseLocation) {
                    locationPermissionRequest.launch(
                        arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION,
                        )
                    )
                } else {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", packageName, null)
                    }
                    startActivity(intent)
                    finish()
                }
            }
            setNegativeButton("취소") { _, _ ->
                hideProgress()
            }
            show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        lifecycleState = lifecycle.currentState.toString()
        // 위치 권한을 요청한다.
        updateLocation()
    }

    override fun onResume() {
        super.onResume()
        if (lifecycleState == "CREATED") {
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                updateLocation()
            } else {
                showDialog()
            }
        }
    }

    override fun onStop() {
        super.onStop()
        lifecycleState = lifecycle.currentState.toString()
        binding.firstData.removeAllViews()
        binding.childForecastLayout.removeAllViews()
    }

    private fun updateLocation() {
        // 위치 API를 사용하기 위해 FusedLocationClient 객체 생성
        // ACCESS_COARSE_LOCATION,ACCESS_FINE_LOCATION 권한이 없는 경우
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) !=
            PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            // ACCESS_COARSE_LOCATION,ACCESS_FINE_LOCATION 권한을 요청한다.
            locationPermissionRequest.launch(
                arrayOf(
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
            )
            return
        }
        showProgress()
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        // 현재 위치 얻어오기
        fusedLocationClient.getCurrentLocation(
            CurrentLocationRequest.Builder()
                .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
                .build(),
            object : CancellationToken() {
                override fun onCanceledRequested(p0: OnTokenCanceledListener) =
                    CancellationTokenSource().token

                override fun isCancellationRequested() = false
            })
            .addOnSuccessListener { location ->
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        Geocoder(this, Locale.KOREA).getFromLocation(
                            location.latitude,
                            location.longitude,
                            1
                        ) {
                            val dong = when (it.size) {
                                0 -> "이름없는 동"
                                else -> it[0].thoroughfare
                            }
                            // UI 관련 처리는 runOnUiThread에서 한다.
                            runOnUiThread {
                                binding.locationTextView.text = dong
                            }
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                // Rest API - retrofit 결과 콜백처리
                WeatherRepository.getVillageForecast(this,
                    longitude = location.longitude,
                    latitude = location.latitude,
                    // 응답 성공 콜백 처리
                    successCallback = { list ->
                        hideProgress()
                        // 인자로 온 list의 첫번째 Forecast 객체
                        val currentForecast = list.first()
                        // 받아온 첫번째 데이터 linearLayout(ViewGroup)에 동적으로 뷰 추가
                        binding.firstData.apply {
                            val firstItemView = ItemFirstBinding.inflate(layoutInflater)
                            // 1시간 기온 값 내용 설정
                            firstItemView.temperatureTextView.text =
                                getString(R.string.temperature_text, currentForecast.temperature)
                            // 하늘 상태 정보 설정
                            firstItemView.skyTextView.text = currentForecast.weather
                            // 강수 확률 값 내용 설정
                            firstItemView.precipitationPercentTextView.text =
                                getString(
                                    R.string.precipitation_precent_text,
                                    currentForecast.precipitationPercent
                                )
                            firstItemView.precipitationTextView.text =
                                if (currentForecast.precipitation == "강수없음") currentForecast.precipitation
                                else "시간당 강수량: ${currentForecast.precipitation}"
                            addView(firstItemView.root)
                        }
                        // LinearLayout(ViewGroup)에 동적으로 뷰 추가
                        binding.childForecastLayout.apply {
                            list.forEachIndexed { index, forecast ->
                                // 현재 index가 0이면 리턴
                                if (index == 0) return@forEachIndexed
                                val itemView = ItemForecastBinding.inflate(layoutInflater)
                                // 예보 시간 추출
                                val time = forecast.forecastTime
                                val hour = time.substring(0..1)
                                val minute = time.substring(2 until time.length)
                                val date = forecast.forecastDate
                                val year = date.substring(0..3)
                                val month = date.substring(4..5)
                                val day = date.substring(6..7)
                                itemView.dateTextView.text = "$year.$month.$day"
                                // 예보 시간을 내용 설정
                                itemView.timeTextView.text = "$hour:$minute"
                                // 하늘 상태 혹은 강우 형태 내용 설정
                                itemView.weatherTextView.text = forecast.weather
                                // 1시간 기온 값 내용 설정
                                itemView.temperatureTextView.text =
                                    getString(R.string.temperature_text, forecast.temperature)
                                // 강우 확률 내용 설정
                                itemView.precipitationPercentageTextView.text =
                                    getString(
                                        R.string.precipitation_precent_text,
                                        forecast.precipitationPercent
                                    )
                                // 시간당 강우량 내용 설정
                                itemView.precipitationTextView.text =
                                    if (forecast.precipitation == "강수없음") forecast.precipitation
                                    else "시간당 강수량: ${forecast.precipitation}"
                                // LinearLayout(ViewGroup)에 itemView 추가
                                addView(itemView.root)
                            }
                        }
                    },
                    // 응답 에러 스택을 띄움
                    failureCallback = {
                        it.printStackTrace()
                    })
            }.addOnFailureListener {
                it.printStackTrace()
            }
//     마지막으로 확인된 위치 정보 얻어오기
//        fusedLocationClient.lastLocation.addOnSuccessListener{
//            Thread {
//                try {
//                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
//                        Geocoder(this, Locale.KOREA).getFromLocation(it.latitude, it.longitude, 1) {
//                            // UI 관련 처리는 runOnUiThread에서 한다.
//                            runOnUiThread {
//                                // thoroughfare은 동을 뜻한다.
//                                binding.locationTextView.text = it[0]?.thoroughfare.orEmpty()
//                            }
//                        }
//                    }
//                } catch (e: Exception) {
//                    e.printStackTrace()
//                }
//            }.start()
//            // Rest API - retrofit 결과 콜백처리
//            WeatherRepository.getVillageForecast(this,
//                longitude = it.longitude,
//                latitude = it.latitude,
//                // 응답 성공 콜백 처리
//                successCallback = { list ->
//                    hideProgress()
//                    // 인자로 온 list의 첫번째 Forecast 객체
//                    val currentForecast = list.first()
//                    // 1시간 기온 값 내용 설정
//                    binding.temperatureTextView.text =
//                        getString(R.string.temperature_text, currentForecast.temperature)
//                    // 하늘 상태 정보 설정
//                    binding.skyTextView.text = currentForecast.weather
//                    // 강수 확률 값 내용 설정
//                    binding.precipitationTextView.text =
//                        getString(R.string.precipitation_text, currentForecast.precipitation)
//                    // LinearLayout(ViewGroup) 설정
//                    binding.childForecastLayout.apply {
//                        list.forEachIndexed { index, forecast ->
//                            // 현재 index가 0이면 리턴
//                            if (index == 0) return@forEachIndexed
//                            val itemView = ItemForecastBinding.inflate(layoutInflater)
//                            // 예보 시간 추출
//                            val time = forecast.forecastTime
//                            val hour = time.substring(0..1)
//                            val minute = time.substring(2 until time.length)
//                            val date = forecast.forecastDate
//                            val year = date.substring(0..3)
//                            val month = date.substring(4..5)
//                            val day = date.substring(6..7)
//                            itemView.dateTextView.text = "$year.$month.$day"
//                            // 예보 시간을 내용 설정
//                            itemView.timeTextView.text = "$hour:$minute"
//                            // 하늘 상태 혹은 강우 형태 내용 설정
//                            itemView.weatherTextView.text = forecast.weather
//                            // 1시간 기온 값 내용 설정
//                                itemView.temperatureTextView.text =
//                                    getString(R.string.temperature_text, forecast.temperature)

//                                itemView.precipitationPercentageTextView.text =
//                                    getString(
//                                        R.string.precipitation_precent_text,
//                                        forecast.precipitationPercent
//                                    )
//                                 // 시간당 강우량 내용 설정
//                                itemView.precipitationTextView.text =
//                                    if(forecast.precipitation=="강수없음") forecast.precipitation
//                                    else "시간당 강수량: ${forecast.precipitation}"
//                            // LinearLayout(ViewGroup)에 itemView 추가
//                            addView(itemView.root)
//                        }
//                    }
//                },
//                // 응답 에러 스택을 띄움
//                failureCallback = {
//                    it.printStackTrace()
//                })
//        }
    }

    // 진행 프로그래스바를 보이게 설정
    private fun showProgress() {
        binding.progressBarLayout.isVisible = true
    }

    // 진행 프로그래스바를 보이지 않게 설정
    private fun hideProgress() {
        binding.progressBarLayout.isVisible = false
    }
}