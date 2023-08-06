package fastcampus.part2.chapter7

import android.content.Context
import android.util.Log
import android.widget.Toast
import com.google.gson.GsonBuilder
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object WeatherRepository {
    // gson 객체 생성 및 설정
    var gson = GsonBuilder().setLenient().create()
    // retrofit 객체 생성 및 설정
    private val retrofit = Retrofit.Builder()
        .baseUrl("http://apis.data.go.kr/")
        .addConverterFactory(GsonConverterFactory.create(gson))
        .build()
    // retrofit 객체로 WeatherService 객체 생성
    private val service = retrofit.create(WeatherService::class.java)
    fun getVillageForecast(
        context: Context,
        longitude: Double,
        latitude: Double,
        successCallback: (List<Forecast>) -> Unit,
        failureCallback: (Throwable) -> Unit,
    ) {
        // baseDate와 baseTime 추출한 값 받아오기
        val baseDateTime = BaseDateTime.getBaseDateTime()
        // 위도,경도 값으로 point 객체로 변환하여 반환
        val point = GeoPointConverter().convert(lat = latitude, lon = longitude)
        service.getVillageForecast(
            serviceKey = context.getString(R.string.serviceKey),
            baseDate = baseDateTime.baseDate,
            baseTime = baseDateTime.baseTime,
            nx = point.nx,
            ny = point.ny
        ).enqueue(object : Callback<WeatherEntity> {
            // 응답이 있을 시(성공시)
            override fun onResponse(call: Call<WeatherEntity>, response: Response<WeatherEntity>) {
                // 키는 String 타입 값은 Forecast 타입의 MutableMap 객체 생성
                val forecastDateTimeMap = mutableMapOf<String, Forecast>()
                // 응답으로 온 List<ForecastEntity> 추출
                val forecastList = response.body()?.response?.body?.items?.forecastEntities.orEmpty()
                for (forecast in forecastList) {
                    // forecastList를 순회하면서 forecastDateTimeMap의 key에
                    // forecastDate/forecastTime에 해당하는 value가 없다면 Forecast 객체를 생성함
                    if (forecastDateTimeMap["${forecast.forecastDate}/${forecast.forecastTime}"] == null) {
                        forecastDateTimeMap["${forecast.forecastDate}/${forecast.forecastTime}"] =
                            Forecast(forecast.forecastDate, forecast.forecastTime)
                    }
                    forecastDateTimeMap["${forecast.forecastDate}/${forecast.forecastTime}"]?.apply {
                        when (forecast.category) {
                            // category가 강수 확률인 경우
                            Category.POP -> precipitation = forecast.forecastValue.toInt()
                            // category가 강수 형태인 경우
                            Category.PTY -> precipitationType = transformRainType(forecast)
                            // category가 하늘 상태인 경우
                            Category.SKY -> sky = transformSky(forecast)
                            // category가 1시간 기온인 경우
                            Category.TMP -> temperature = forecast.forecastValue.toDouble()
                            else -> {}
                        }
                    }
                }
                // 아래의 sortWith 람다함수를 사용하여 정렬하지 않아도 같은 결과값이 나오는 것 같음
                // forecastDateTimeMap에서 value인 Forecast 리스트 추출(mutablelist 형태로 변환)
                val list = forecastDateTimeMap.values.toMutableList()
                // 리스트 정렬
                list.sortWith { f1, f2 ->
                    val f1DateTime = "${f1.forecastDate}${f1.forecastTime}"
                    val f2DateTime = "${f2.forecastDate}${f2.forecastTime}"
                    // compareTo 함수는 fiDateTime과 f2DateTime이 같으면 0을 반환
                    // f1DateTime과 f2DateTime이 다르면 일치하지 않은 인덱스의 내용에 차이를 정수형으로 반환
                    return@sortWith f1DateTime.compareTo(f2DateTime)
                }
                // list가 비었다면 NPE 발생
                if (list.isEmpty())
                    failureCallback(NullPointerException())
                // list가 비었지 않으면 successCallback 함수 실행 인자로 list
                else
                    successCallback(list)
            }
            // 응답이 없을 시(실패 시)
            override fun onFailure(call: Call<WeatherEntity>, t: Throwable) {
                failureCallback(t)
            }
        })
    }
    // 강수 형태를 반환
    private fun transformRainType(forecast: ForecastEntity): String {
        return when (forecast.forecastValue.toInt()) {
            0 -> "없음"
            1 -> "비"
            2 -> "비/눈"
            3 -> "눈"
            4 -> "소나기"
            else -> ""
        }
    }
    // 하늘 상태를 반환
    private fun transformSky(forecast: ForecastEntity): String {
        return when (forecast.forecastValue.toInt()) {
            1 -> "맑음"
            3 -> "구름많음"
            4 -> "흐림"
            else -> ""
        }
    }
}