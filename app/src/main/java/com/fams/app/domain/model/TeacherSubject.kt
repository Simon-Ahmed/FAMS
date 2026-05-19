package com.fams.app.domain.model

/** Maps which subjects a teacher is qualified to teach */
data class TeacherSubject(
    val id: String = "",
    val teacherId: String = "",
    val subjectId: String = ""
)

/** A time slot the teacher is NOT available */
data class TeacherUnavailability(
    val id: String = "",
    val teacherId: String = "",
    val timeSlotId: String = ""
)

/** Scheduling constraints set by the coordinator */
data class ScheduleConstraints(
    val maxClassesPerDayPerSection: Int = 4,
    val maxConsecutiveClassesPerTeacher: Int = 3
)
