package fastcampus.part2.chapter7

data class Forecast(
    val forecastDate: String,   // 예보일
    val forecastTime: String,   // 예보 시간
    var temperature: Double = 0.0,  // 1시간 기온
    var sky: String = "",   // 하늘 상태
    var precipitationPercent: Int = 0, // 강수 확률
    var precipitation: String = "", // 시간당 강수량
    var precipitationType: String = ""  // 강수 형태
) {
    val weather: String
        get() {
            // 강수형태가 빈문자열이거나 없음이라면 하늘 상태를 반환하고
            // 강수형태가 빈문자열이 아니고 없음이 아니라면 강수 형태를 반환
            return if (precipitationType == "" || precipitationType == "없음") sky else precipitationType
        }
}