package com.fams.app.data.repository

import com.fams.app.di.FirebaseModule
import com.fams.app.domain.model.TeacherSubject
import com.fams.app.domain.model.TeacherUnavailability
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class TeacherSubjectRepository(
    private val firestore: FirebaseFirestore = FirebaseModule.firestore
) {
    suspend fun getTeacherSubjects(): Result<List<TeacherSubject>> = try {
        val snap = firestore.collection("teacherSubjects").get().await()
        Result.success(snap.documents.map { doc ->
            TeacherSubject(
                id = doc.id,
                teacherId = doc.getString("teacherId") ?: "",
                subjectId = doc.getString("subjectId") ?: ""
            )
        })
    } catch (e: Exception) { Result.failure(e) }

    suspend fun assign(teacherId: String, subjectId: String): Result<Unit> = try {
        // Avoid duplicates
        val existing = firestore.collection("teacherSubjects")
            .whereEqualTo("teacherId", teacherId)
            .whereEqualTo("subjectId", subjectId)
            .get().await()
        if (existing.isEmpty) {
            firestore.collection("teacherSubjects")
                .add(mapOf("teacherId" to teacherId, "subjectId" to subjectId)).await()
        }
        Result.success(Unit)
    } catch (e: Exception) { Result.failure(e) }

    suspend fun remove(id: String): Result<Unit> = try {
        firestore.collection("teacherSubjects").document(id).delete().await()
        Result.success(Unit)
    } catch (e: Exception) { Result.failure(e) }

    suspend fun getUnavailabilities(): Result<List<TeacherUnavailability>> = try {
        val snap = firestore.collection("teacherUnavailability").get().await()
        Result.success(snap.documents.map { doc ->
            TeacherUnavailability(
                id = doc.id,
                teacherId = doc.getString("teacherId") ?: "",
                timeSlotId = doc.getString("timeSlotId") ?: ""
            )
        })
    } catch (e: Exception) { Result.failure(e) }

    suspend fun addUnavailability(teacherId: String, timeSlotId: String): Result<Unit> = try {
        firestore.collection("teacherUnavailability")
            .add(mapOf("teacherId" to teacherId, "timeSlotId" to timeSlotId)).await()
        Result.success(Unit)
    } catch (e: Exception) { Result.failure(e) }

    suspend fun removeUnavailability(id: String): Result<Unit> = try {
        firestore.collection("teacherUnavailability").document(id).delete().await()
        Result.success(Unit)
    } catch (e: Exception) { Result.failure(e) }
}
