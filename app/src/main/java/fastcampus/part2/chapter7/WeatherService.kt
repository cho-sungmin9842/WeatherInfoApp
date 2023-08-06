package fastcampus.part2.chapter7

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface WeatherService {
    // 3일간의 예보 정보를 받아옴
    @GET("/1360000/VilageFcstInfoService_2.0/getVilageFcst?pageNo=1&numOfRows=1000&dataType=json")
    fun getVillageForecast(
        @Query("serviceKey") serviceKey: String,
        @Query("base_date") baseDate: String,
        @Query("base_time") baseTime: String,
        @Query("nx") nx: Int,
        @Query("ny") ny: Int
    ): Call<WeatherEntity>
}
