package com.fams.app.data.repository

import com.fams.app.di.FirebaseModule
import com.fams.app.domain.model.ScheduleConfig
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class ScheduleConfigRepository(
    private val firestore: FirebaseFirestore = FirebaseModule.firestore
) {
    private val docRef = firestore.collection("config").document("schedule")

    suspend fun getConfig(): Result<ScheduleConfig> {
        return try {
            val doc = docRef.get().await()
            if (!doc.exists()) return Result.success(ScheduleConfig())
            Result.success(
                ScheduleConfig(
                    maxStudentsPerSection = (doc.getLong("maxStudentsPerSection") ?: 30L).toInt(),
                    maxTeacherCreditHoursPerWeek = (doc.getLong("maxTeacherCreditHoursPerWeek") ?: 12L).toInt()
                )
            )
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun saveConfig(config: ScheduleConfig): Result<Unit> {
        return try {
            docRef.set(mapOf(
                "maxStudentsPerSection" to config.maxStudentsPerSection,
                "maxTeacherCreditHoursPerWeek" to config.maxTeacherCreditHoursPerWeek
            )).await()
            Result.success(Unit)
        } catch (e: Exception) { Result.failure(e) }
    }
}
