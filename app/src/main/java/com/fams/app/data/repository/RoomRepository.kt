package com.fams.app.data.repository

import com.fams.app.di.FirebaseModule
import com.fams.app.domain.model.Room
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class RoomRepository(
    private val firestore: FirebaseFirestore = FirebaseModule.firestore
) {
    suspend fun getAll(): Result<List<Room>> = try {
        val snap = firestore.collection("rooms").get().await()
        Result.success(snap.documents.map { doc ->
            Room(
                id = doc.id,
                name = doc.getString("name") ?: "",
                capacity = (doc.getLong("capacity") ?: 30).toInt()
            )
        }.sortedBy { it.name })
    } catch (e: Exception) { Result.failure(e) }

    suspend fun add(name: String, capacity: Int): Result<Room> = try {
        val data = mapOf("name" to name, "capacity" to capacity)
        val ref = firestore.collection("rooms").add(data).await()
        Result.success(Room(id = ref.id, name = name, capacity = capacity))
    } catch (e: Exception) { Result.failure(e) }

    suspend fun delete(id: String): Result<Unit> = try {
        firestore.collection("rooms").document(id).delete().await()
        Result.success(Unit)
    } catch (e: Exception) { Result.failure(e) }
}
