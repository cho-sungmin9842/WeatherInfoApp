package fastcampus.part2.chapter7

import com.google.gson.annotations.SerializedName

data class WeatherEntity(
    @SerializedName("response")
    val response: WeatherResponse
)
data class WeatherResponse(
    @SerializedName("header")
    val header: WeatherHeader,
    @SerializedName("body")
    val body: WeatherBody,
)
data class WeatherHeader(
    @SerializedName("resultCode")
    val resultCode: String,
    @SerializedName("resultMsg")
    val resultMessage: String
)
data class WeatherBody(
    @SerializedName("items")
    val items: ForecastEntityList
)
data class ForecastEntityList(
    @SerializedName("item")
    val forecastEntities: List<ForecastEntity>
)
data class ForecastEntity(
    @SerializedName("baseDate")
    val baseDate: String,   // 발표일
    @SerializedName("baseTime")
    val baseTime: String,   // 발표시간
    @SerializedName("category")
    val category: Category?,    // 자료구분문자
    @SerializedName("fcstDate")
    val forecastDate: String,   // 예보일
    @SerializedName("fcstTime")
    val forecastTime: String,   // 예보시간
   @SerializedName("fcstValue")
    val forecastValue: String,  // 예보값
    @SerializedName("nx")
    val nx: Int,    // 예보지점 x좌표
    @SerializedName("ny")
    val ny: Int,    // 예보지점 y좌표
)