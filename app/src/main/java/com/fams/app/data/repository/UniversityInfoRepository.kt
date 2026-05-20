package com.fams.app.data.repository

import com.fams.app.domain.model.UniversityInfo
import com.fams.app.di.FirebaseModule
import kotlinx.coroutines.tasks.await

class UniversityInfoRepository {
    private val db = FirebaseModule.firestore

    suspend fun getInfo(): Result<List<UniversityInfo>> {
        return try {
            val snap = db.collection("universityInfo").get().await()
            Result.success(snap.documents.map { doc ->
                UniversityInfo(id = doc.id,
                    title = doc.getString("title") ?: "",
                    body = doc.getString("body") ?: "",
                    updatedAt = doc.getLong("updatedAt") ?: 0L)
            }.sortedByDescending { it.updatedAt })
        } catch (e: Exception) { Result.failure(e) }
    }
}
