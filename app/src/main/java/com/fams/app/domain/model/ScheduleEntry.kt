package com.fams.app.domain.model

data class ScheduleEntry(
    val id: String = "",
    val scheduleId: String = "",
    val sectionId: String = "",
    val sectionName: String = "",
    val subjectId: String = "",
    val subjectName: String = "",
    val teacherId: String = "",
    val teacherName: String = "",
    val roomId: String = "",
    val roomName: String = "",
    val timeSlotId: String = "",
    val day: String = "",
    val startTime: String = "",
    val endTime: String = ""
)

data class Schedule(
    val id: String = "",
    val status: ScheduleStatus = ScheduleStatus.DRAFT,
    val generatedAt: Long = 0L,
    val publishedAt: Long = 0L,
    val entries: List<ScheduleEntry> = emptyList()
)

enum class ScheduleStatus(val value: String) {
    DRAFT("draft"),
    PUBLISHED("published");

    companion object {
        fun from(value: String): ScheduleStatus =
            values().firstOrNull { it.value == value } ?: DRAFT
    }
}
