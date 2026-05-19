package com.fams.app.data.repository

import com.fams.app.di.FirebaseModule
import com.fams.app.domain.model.*
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class ScheduleDataRepository(
    private val firestore: FirebaseFirestore = FirebaseModule.firestore
) {

    // ── Subjects ──────────────────────────────────────────────────────────────

    suspend fun getSubjects(): Result<List<Subject>> {
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

    suspend fun addSubject(name: String, description: String = "", creditHours: Int): Result<Subject> {
        return try {
            val data = mapOf("name" to name, "description" to description, "creditHours" to creditHours)
            val ref = firestore.collection("subjects").add(data).await()
            Result.success(Subject(id = ref.id, name = name, description = description, creditHours = creditHours))
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun deleteSubject(id: String): Result<Unit> {
        return try {
            firestore.collection("subjects").document(id).delete().await()
            Result.success(Unit)
        } catch (e: Exception) { Result.failure(e) }
    }

    // ── Rooms ─────────────────────────────────────────────────────────────────

    suspend fun getRooms(): Result<List<Room>> {
        return try {
            val snap = firestore.collection("rooms").get().await()
            Result.success(snap.documents.map { doc ->
                Room(
                    id = doc.id,
                    name = doc.getString("name") ?: "",
                    capacity = (doc.getLong("capacity") ?: 30L).toInt()
                )
            }.sortedBy { it.name })
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun addRoom(name: String, capacity: Int): Result<Room> {
        return try {
            val data = mapOf("name" to name, "capacity" to capacity)
            val ref = firestore.collection("rooms").add(data).await()
            Result.success(Room(id = ref.id, name = name, capacity = capacity))
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun deleteRoom(id: String): Result<Unit> {
        return try {
            firestore.collection("rooms").document(id).delete().await()
            Result.success(Unit)
        } catch (e: Exception) { Result.failure(e) }
    }

    // ── Time Slots ────────────────────────────────────────────────────────────

    suspend fun getTimeSlots(): Result<List<TimeSlot>> {
        return try {
            val snap = firestore.collection("timeSlots").get().await()
            val dayOrder = listOf("Monday","Tuesday","Wednesday","Thursday","Friday","Saturday","Sunday")
            val list = snap.documents.map { doc ->
                TimeSlot(
                    id = doc.id,
                    day = doc.getString("day") ?: "",
                    startTime = doc.getString("startTime") ?: "",
                    endTime = doc.getString("endTime") ?: ""
                )
            }
            Result.success(list.sortedWith(Comparator { a, b ->
                val d = dayOrder.indexOf(a.day).compareTo(dayOrder.indexOf(b.day))
                if (d != 0) d else a.startTime.compareTo(b.startTime)
            }))
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun addTimeSlot(day: String, startTime: String, endTime: String): Result<TimeSlot> {
        return try {
            val data = mapOf("day" to day, "startTime" to startTime, "endTime" to endTime)
            val ref = firestore.collection("timeSlots").add(data).await()
            Result.success(TimeSlot(id = ref.id, day = day, startTime = startTime, endTime = endTime))
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun deleteTimeSlot(id: String): Result<Unit> {
        return try {
            firestore.collection("timeSlots").document(id).delete().await()
            Result.success(Unit)
        } catch (e: Exception) { Result.failure(e) }
    }

    // ── Teacher-Subject Mappings ───────────────────────────────────────────────

    suspend fun getTeacherSubjects(): Result<List<TeacherSubject>> {
        return try {
            val snap = firestore.collection("teacherSubjects").get().await()
            Result.success(snap.documents.map { doc ->
                TeacherSubject(
                    id = doc.id,
                    teacherId = doc.getString("teacherId") ?: "",
                    subjectId = doc.getString("subjectId") ?: ""
                )
            })
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun assignTeacherSubject(teacherId: String, subjectId: String): Result<Unit> {
        return try {
            val data = mapOf("teacherId" to teacherId, "subjectId" to subjectId)
            firestore.collection("teacherSubjects").add(data).await()
            Result.success(Unit)
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun removeTeacherSubject(id: String): Result<Unit> {
        return try {
            firestore.collection("teacherSubjects").document(id).delete().await()
            Result.success(Unit)
        } catch (e: Exception) { Result.failure(e) }
    }

    // ── Teacher Unavailability ────────────────────────────────────────────────

    suspend fun getTeacherUnavailabilities(): Result<List<TeacherUnavailability>> {
        return try {
            val snap = firestore.collection("teacherUnavailability").get().await()
            Result.success(snap.documents.map { doc ->
                TeacherUnavailability(
                    id = doc.id,
                    teacherId = doc.getString("teacherId") ?: "",
                    timeSlotId = doc.getString("timeSlotId") ?: ""
                )
            })
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun addUnavailability(teacherId: String, timeSlotId: String): Result<Unit> {
        return try {
            val data = mapOf("teacherId" to teacherId, "timeSlotId" to timeSlotId)
            firestore.collection("teacherUnavailability").add(data).await()
            Result.success(Unit)
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun removeUnavailability(id: String): Result<Unit> {
        return try {
            firestore.collection("teacherUnavailability").document(id).delete().await()
            Result.success(Unit)
        } catch (e: Exception) { Result.failure(e) }
    }

    // ── Published Schedule ────────────────────────────────────────────────────

    suspend fun saveSchedule(entries: List<ScheduleEntry>, status: ScheduleStatus): Result<String> {
        return try {
            val scheduleData = mapOf(
                "status" to status.value,
                "generatedAt" to System.currentTimeMillis(),
                "publishedAt" to if (status == ScheduleStatus.PUBLISHED) System.currentTimeMillis() else 0L
            )
            val scheduleRef = firestore.collection("schedules").add(scheduleData).await()
            val scheduleId = scheduleRef.id

            val batch = firestore.batch()
            entries.forEach { entry ->
                val entryRef = firestore.collection("schedules")
                    .document(scheduleId).collection("entries").document()
                batch.set(entryRef, mapOf(
                    "scheduleId" to scheduleId,
                    "sectionId" to entry.sectionId,
                    "subjectId" to entry.subjectId,
                    "teacherId" to entry.teacherId,
                    "roomId" to entry.roomId,
                    "timeSlotId" to entry.timeSlotId,
                    "sectionName" to entry.sectionName,
                    "subjectName" to entry.subjectName,
                    "teacherName" to entry.teacherName,
                    "roomName" to entry.roomName,
                    "day" to entry.day,
                    "startTime" to entry.startTime,
                    "endTime" to entry.endTime
                ))
            }
            batch.commit().await()
            Result.success(scheduleId)
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun getPublishedSchedule(): Result<List<ScheduleEntry>> {
        return try {
            val schedSnap = firestore.collection("schedules")
                .whereEqualTo("status", ScheduleStatus.PUBLISHED.value)
                .get().await()

            if (schedSnap.isEmpty) return Result.success(emptyList())

            val latestSchedule = schedSnap.documents.maxByOrNull {
                it.getLong("publishedAt") ?: 0L
            } ?: return Result.success(emptyList())

            val entriesSnap = firestore.collection("schedules")
                .document(latestSchedule.id).collection("entries").get().await()

            Result.success(entriesSnap.documents.map { doc ->
                ScheduleEntry(
                    id = doc.id,
                    scheduleId = latestSchedule.id,
                    sectionId = doc.getString("sectionId") ?: "",
                    subjectId = doc.getString("subjectId") ?: "",
                    teacherId = doc.getString("teacherId") ?: "",
                    roomId = doc.getString("roomId") ?: "",
                    timeSlotId = doc.getString("timeSlotId") ?: "",
                    sectionName = doc.getString("sectionName") ?: "",
                    subjectName = doc.getString("subjectName") ?: "",
                    teacherName = doc.getString("teacherName") ?: "",
                    roomName = doc.getString("roomName") ?: "",
                    day = doc.getString("day") ?: "",
                    startTime = doc.getString("startTime") ?: "",
                    endTime = doc.getString("endTime") ?: ""
                )
            })
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun deletePublishedSchedule(): Result<Unit> {
        return try {
            val schedSnap = firestore.collection("schedules")
                .whereEqualTo("status", ScheduleStatus.PUBLISHED.value)
                .get().await()

            if (schedSnap.isEmpty) return Result.success(Unit)

            val latestSchedule = schedSnap.documents.maxByOrNull {
                it.getLong("publishedAt") ?: 0L
            } ?: return Result.success(Unit)

            val entriesSnap = firestore.collection("schedules")
                .document(latestSchedule.id)
                .collection("entries").get().await()

            val batch = firestore.batch()
            entriesSnap.documents.forEach { batch.delete(it.reference) }
            batch.delete(firestore.collection("schedules").document(latestSchedule.id))
            batch.commit().await()
            Result.success(Unit)
        } catch (e: Exception) { Result.failure(e) }
    }
}
