package com.fams.app.data.repository

import com.fams.app.di.FirebaseModule
import com.fams.app.domain.model.Subject
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class SubjectRepository(
    private val firestore: FirebaseFirestore = FirebaseModule.firestore
) {
    suspend fun getAll(): Result<List<Subject>> {
        return try {
            val snap = firestore.collection("subjects").get().await()
            Result.success(snap.documents.map { doc ->
                Subject(
                    id = doc.id,
                    name = doc.getString("name") ?: "",
                    description = doc.getString("description") ?: "",
                    creditHours = (doc.getLong("creditHours") ?: doc.getLong("hoursPerWeek") ?: 3L).toInt()
                )
            }.sortedBy { it.name })
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun add(name: String, description: String, creditHours: Int): Result<Subject> {
        return try {
            val data = mapOf("name" to name, "description" to description, "creditHours" to creditHours)
            val ref = firestore.collection("subjects").add(data).await()
            Result.success(Subject(id = ref.id, name = name, description = description, creditHours = creditHours))
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun delete(id: String): Result<Unit> {
        return try {
            firestore.collection("subjects").document(id).delete().await()
            Result.success(Unit)
        } catch (e: Exception) { Result.failure(e) }
    }
}
