package fastcampus.part2.chapter7

import java.time.LocalDateTime
import java.time.LocalTime

data class BaseDateTime(val baseDate: String, val baseTime: String) {
    companion object {
        // BaseDateTime 객체 타입을 반환
        fun getBaseDateTime(): BaseDateTime {
            // 현재 날짜 및 시간 정보를 가진 객체(LocalDateTime)
            var dateTime = LocalDateTime.now()
            // baseTime을 설정(시,분)
            val baseTime = when (dateTime.toLocalTime()) {
                // 현재 시간이 0시 0분에서 2시 30분이라면 하루전으로 baseDate를 설정하고 2300 반환
                in LocalTime.of(0, 0)..LocalTime.of(2, 30) -> {
                    dateTime = dateTime.minusDays(1)
                    "2300"
                }
                // 현재 시간이 2시 30분에서 5시 30분이라면 0200 반환
                in LocalTime.of(2, 30)..LocalTime.of(5, 30) -> "0200"
                // 현재 시간이 5시 30분에서 8시 30분이라면 0500 반환
                in LocalTime.of(5, 30)..LocalTime.of(8, 30) -> "0500"
                // 현재 시간이 8시 30분에서 11시 30분이라면 0800 반환
                in LocalTime.of(8, 30)..LocalTime.of(11, 30) -> "0800"
                // 현재 시간이 11시 30분에서 14시 30분이라면 1100 반환
                in LocalTime.of(11, 30)..LocalTime.of(14, 30) -> "1100"
                // 현재 시간이 14시 30분에서 17시 30분이라면 1400 반환
                in LocalTime.of(14, 30)..LocalTime.of(17, 30) -> "1400"
                // 현재 시간이 17시 30분에서 20시 30분이라면 1700 반환
                in LocalTime.of(17, 30)..LocalTime.of(20, 30) -> "1700"
                // 현재 시간이 20시 30분에서 23시 30분이라면 2000 반환
                in LocalTime.of(20, 30)..LocalTime.of(23, 30) -> "2000"
                // 현재 시간이 23시 31분에서 23시 59분이라면 2300 반환
                else -> "2300"
            }
            // baseDate를 설정(년,월,일)
            val baseDate = String.format(
                "%04d%02d%02d",
                dateTime.year,
                dateTime.monthValue,
                dateTime.dayOfMonth
            )
            // baseDate,baseTime을 인자로 하는 BaseDateTime 객체를 반환
            return BaseDateTime(baseDate, baseTime)
        }
    }
}
