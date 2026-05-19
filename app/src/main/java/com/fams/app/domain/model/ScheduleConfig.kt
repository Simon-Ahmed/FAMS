package com.fams.app.domain.model

/**
 * Global scheduling configuration set by the coordinator.
 * Stored in Firestore under /config/schedule
 */
data class ScheduleConfig(
    val maxStudentsPerSection: Int = 30,
    val maxTeacherCreditHoursPerWeek: Int = 12
)

/**
 * Per-subject teacher requirement analysis result.
 */
data class SubjectRequirement(
    val subject: Subject,
    val sectionCount: Int,
    val sectionsPerTeacher: Int,       // how many sections one teacher can cover
    val minTeachersNeeded: Int,        // ceil(sectionCount / sectionsPerTeacher)
    val assignedTeacherCount: Int,     // how many teachers are currently assigned
    val isSufficient: Boolean          // assignedTeacherCount >= minTeachersNeeded
)
