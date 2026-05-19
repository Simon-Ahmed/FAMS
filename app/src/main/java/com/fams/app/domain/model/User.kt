package com.fams.app.domain.model

data class User(
    val uid: String = "",
    val fullName: String = "",
    val email: String = "",
    val phone: String = "",
    val role: UserRole = UserRole.STUDENT,
    val sectionId: String = "",
    val specialization: String = "",
    val studentId: String = "",
    val teacherId: String = "",
    val generatedPassword: String = ""  // stored so coordinator can view credentials
)

enum class UserRole(val value: String) {
    COORDINATOR("coordinator"),
    TEACHER("teacher"),
    STUDENT("student"),
    CLASS_REP("class_rep");

    companion object {
        fun fromString(value: String): UserRole =
            values().firstOrNull { it.value == value } ?: STUDENT
    }
}
