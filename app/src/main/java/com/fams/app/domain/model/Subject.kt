package com.fams.app.domain.model

data class Subject(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val creditHours: Int = 3  // credit hours = sessions per week in the schedule
)
