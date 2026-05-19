package com.fams.app.data.repository

import com.fams.app.di.FirebaseModule
import com.fams.app.domain.model.Section
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class SectionRepository(
    private val firestore: FirebaseFirestore = FirebaseModule.firestore
) {

    suspend fun getAllSections(): Result<List<Section>> = try {
        val snapshot = firestore.collection("sections").get().await()
        val sections = snapshot.documents.mapNotNull { doc ->
            Section(
                id = doc.id,
                name = doc.getString("name") ?: "",
                maxCapacity = (doc.getLong("maxCapacity") ?: 30).toInt(),
                studentCount = (doc.getLong("studentCount") ?: 0).toInt()
            )
        }
        Result.success(sections.sortedBy { it.name })
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun createSection(name: String, maxCapacity: Int): Result<Section> = try {
        val data = mapOf(
            "name" to name,
            "maxCapacity" to maxCapacity,
            "studentCount" to 0
        )
        val ref = firestore.collection("sections").add(data).await()
        Result.success(Section(id = ref.id, name = name, maxCapacity = maxCapacity))
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun updateSection(id: String, name: String, maxCapacity: Int): Result<Unit> = try {
        firestore.collection("sections").document(id)
            .update(mapOf("name" to name, "maxCapacity" to maxCapacity)).await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun deleteSection(id: String): Result<Unit> = try {
        firestore.collection("sections").document(id).delete().await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun incrementStudentCount(sectionId: String): Result<Unit> = try {
        val ref = firestore.collection("sections").document(sectionId)
        firestore.runTransaction { tx ->
            val snap = tx.get(ref)
            val current = (snap.getLong("studentCount") ?: 0).toInt()
            tx.update(ref, "studentCount", current + 1)
        }.await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }
}
