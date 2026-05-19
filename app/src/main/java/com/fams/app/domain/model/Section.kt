package com.fams.app.domain.model

data class Section(
    val id: String = "",
    val name: String = "",       // e.g. "Section A"
    val maxCapacity: Int = 30,
    val studentCount: Int = 0
)
