package com.fams.app.domain.model

data class TimeSlot(
    val id: String = "",
    val day: String = "",        // "Monday", "Tuesday", etc.
    val startTime: String = "",  // "08:00"
    val endTime: String = ""     // "09:30"
) {
    fun label(): String = "$day $startTime–$endTime"
}
