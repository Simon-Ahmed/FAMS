package com.fams.app.data.repository

import com.fams.app.di.FirebaseModule
import com.fams.app.domain.model.TimeSlot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class TimeSlotRepository(
    private val firestore: FirebaseFirestore = FirebaseModule.firestore
) {
    private val dayOrder = listOf("Monday","Tuesday","Wednesday","Thursday","Friday","Saturday","Sunday")

    suspend fun getAll(): Result<List<TimeSlot>> = try {
        val snap = firestore.collection("timeSlots").get().await()
        val slots = snap.documents.map { doc ->
            TimeSlot(
                id = doc.id,
                day = doc.getString("day") ?: "",
                startTime = doc.getString("startTime") ?: "",
                endTime = doc.getString("endTime") ?: ""
            )
        }.sortedWith(compareBy({ dayOrder.indexOf(it.day) }, { it.startTime }))
        Result.success(slots)
    } catch (e: Exception) { Result.failure(e) }

    suspend fun add(day: String, startTime: String, endTime: String): Result<TimeSlot> = try {
        val data = mapOf("day" to day, "startTime" to startTime, "endTime" to endTime)
        val ref = firestore.collection("timeSlots").add(data).await()
        Result.success(TimeSlot(id = ref.id, day = day, startTime = startTime, endTime = endTime))
    } catch (e: Exception) { Result.failure(e) }

    suspend fun delete(id: String): Result<Unit> = try {
        firestore.collection("timeSlots").document(id).delete().await()
        Result.success(Unit)
    } catch (e: Exception) { Result.failure(e) }
}
