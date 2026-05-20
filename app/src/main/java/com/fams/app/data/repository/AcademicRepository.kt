package com.fams.app.data.repository

import com.fams.app.di.FirebaseModule
import com.fams.app.domain.model.*
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

class AcademicRepository(
    private val db: FirebaseFirestore = FirebaseModule.firestore
) {
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private fun today() = dateFormat.format(Date())

    // ── Schedule entries for a teacher ───────────────────────────────────────

    suspend fun getScheduleForTeacher(teacherId: String): Result<List<ScheduleEntry>> {
        return try {
            val snap = db.collection("schedules")
                .whereEqualTo("status", "published").get().await()
            if (snap.isEmpty) return Result.success(emptyList())
            val latest = snap.documents.maxByOrNull { it.getLong("publishedAt") ?: 0L }
                ?: return Result.success(emptyList())
            val entries = db.collection("schedules").document(latest.id)
                .collection("entries").whereEqualTo("teacherId", teacherId).get().await()
            Result.success(entries.documents.map { mapEntry(it.id, latest.id, it.data) })
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun getScheduleForSection(sectionId: String): Result<List<ScheduleEntry>> {
        return try {
            val snap = db.collection("schedules")
                .whereEqualTo("status", "published").get().await()
            if (snap.isEmpty) return Result.success(emptyList())
            val latest = snap.documents.maxByOrNull { it.getLong("publishedAt") ?: 0L }
                ?: return Result.success(emptyList())
            val entries = db.collection("schedules").document(latest.id)
                .collection("entries").whereEqualTo("sectionId", sectionId).get().await()
            Result.success(entries.documents.map { mapEntry(it.id, latest.id, it.data) })
        } catch (e: Exception) { Result.failure(e) }
    }

    private fun mapEntry(id: String, scheduleId: String, data: Map<String, Any>?): ScheduleEntry {
        data ?: return ScheduleEntry(id = id, scheduleId = scheduleId)
        return ScheduleEntry(
            id = id, scheduleId = scheduleId,
            sectionId = data["sectionId"] as? String ?: "",
            sectionName = data["sectionName"] as? String ?: "",
            subjectId = data["subjectId"] as? String ?: "",
            subjectName = data["subjectName"] as? String ?: "",
            teacherId = data["teacherId"] as? String ?: "",
            teacherName = data["teacherName"] as? String ?: "",
            roomId = data["roomId"] as? String ?: "",
            roomName = data["roomName"] as? String ?: "",
            timeSlotId = data["timeSlotId"] as? String ?: "",
            day = data["day"] as? String ?: "",
            startTime = data["startTime"] as? String ?: "",
            endTime = data["endTime"] as? String ?: ""
        )
    }

    // ── Attendance ────────────────────────────────────────────────────────────

    suspend fun getStudentsForSection(sectionId: String): Result<List<User>> {
        return try {
            val snap = db.collection("users").get().await()
            val students = snap.documents.mapNotNull { doc ->
                val data = doc.data ?: return@mapNotNull null
                val role = data["role"] as? String ?: ""
                val sec = data["sectionId"] as? String ?: ""
                if ((role == "student" || role == "class_rep") && sec == sectionId) {
                    User(uid = doc.id,
                        fullName = data["fullName"] as? String ?: "",
                        email = data["email"] as? String ?: "",
                        role = UserRole.fromString(role),
                        sectionId = sec)
                } else null
            }.sortedBy { it.fullName }
            Result.success(students)
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun submitAttendance(records: List<AttendanceRecord>): Result<Unit> {
        return try {
            val batch = db.batch()
            records.forEach { r ->
                val ref = db.collection("attendance").document()
                batch.set(ref, mapOf(
                    "studentId" to r.studentId, "studentName" to r.studentName,
                    "teacherId" to r.teacherId, "sectionId" to r.sectionId,
                    "subjectId" to r.subjectId, "subjectName" to r.subjectName,
                    "date" to r.date, "status" to r.status.value, "sessionId" to r.sessionId
                ))
            }
            batch.commit().await()
            Result.success(Unit)
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun getAttendanceForStudent(studentId: String): Result<List<AttendanceRecord>> {
        return try {
            val snap = db.collection("attendance")
                .whereEqualTo("studentId", studentId).get().await()
            Result.success(snap.documents.map { doc ->
                AttendanceRecord(
                    id = doc.id,
                    studentId = doc.getString("studentId") ?: "",
                    studentName = doc.getString("studentName") ?: "",
                    teacherId = doc.getString("teacherId") ?: "",
                    sectionId = doc.getString("sectionId") ?: "",
                    subjectId = doc.getString("subjectId") ?: "",
                    subjectName = doc.getString("subjectName") ?: "",
                    date = doc.getString("date") ?: "",
                    status = AttendanceStatus.from(doc.getString("status") ?: ""),
                    sessionId = doc.getString("sessionId") ?: ""
                )
            })
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun getAttendanceForSection(sectionId: String, date: String? = null): Result<List<AttendanceRecord>> {
        return try {
            // Explicitly type it as Query
            var query: com.google.firebase.firestore.Query = db.collection("attendance")
            if (sectionId != "All") query = query.whereEqualTo("sectionId", sectionId)
            if (!date.isNullOrBlank()) query = query.whereEqualTo("date", date)
            val snap = query.get().await()
            Result.success(snap.documents.map { doc ->
                AttendanceRecord(
                    id = doc.id,
                    studentId = doc.getString("studentId") ?: "",
                    studentName = doc.getString("studentName") ?: "",
                    teacherId = doc.getString("teacherId") ?: "",
                    sectionId = doc.getString("sectionId") ?: "",
                    subjectId = doc.getString("subjectId") ?: "",
                    subjectName = doc.getString("subjectName") ?: "",
                    date = doc.getString("date") ?: "",
                    status = AttendanceStatus.from(doc.getString("status") ?: ""),
                    sessionId = doc.getString("sessionId") ?: ""
                )
            })
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun submitTeacherAttendance(record: TeacherAttendanceRecord): Result<Unit> {
        return try {
            db.collection("teacherAttendance").add(mapOf(
                "teacherId" to record.teacherId, "teacherName" to record.teacherName,
                "sectionId" to record.sectionId, "subjectId" to record.subjectId,
                "subjectName" to record.subjectName, "date" to record.date,
                "status" to record.status.value, "markedByRepId" to record.markedByRepId
            )).await()
            Result.success(Unit)
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun getTeacherAttendanceForSection(sectionId: String, date: String? = null): Result<List<TeacherAttendanceRecord>> {
        return try {
            var query: com.google.firebase.firestore.Query = db.collection("teacherAttendance")
            if (sectionId != "All") query = query.whereEqualTo("sectionId", sectionId)
            if (!date.isNullOrBlank()) query = query.whereEqualTo("date", date)
            val snap = query.get().await()
            Result.success(snap.documents.map { doc ->
                TeacherAttendanceRecord(
                    id = doc.id,
                    teacherId = doc.getString("teacherId") ?: "",
                    teacherName = doc.getString("teacherName") ?: "",
                    sectionId = doc.getString("sectionId") ?: "",
                    subjectId = doc.getString("subjectId") ?: "",
                    subjectName = doc.getString("subjectName") ?: "",
                    date = doc.getString("date") ?: "",
                    status = AttendanceStatus.from(doc.getString("status") ?: ""),
                    markedByRepId = doc.getString("markedByRepId") ?: ""
                )
            })
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun getTeacherAttendanceForSection(sectionId: String): Result<List<TeacherAttendanceRecord>> {
        return try {
            val snap = db.collection("teacherAttendance")
                .whereEqualTo("sectionId", sectionId).get().await()
            Result.success(snap.documents.map { doc ->
                TeacherAttendanceRecord(
                    id = doc.id,
                    teacherId = doc.getString("teacherId") ?: "",
                    teacherName = doc.getString("teacherName") ?: "",
                    sectionId = doc.getString("sectionId") ?: "",
                    subjectId = doc.getString("subjectId") ?: "",
                    subjectName = doc.getString("subjectName") ?: "",
                    date = doc.getString("date") ?: "",
                    status = AttendanceStatus.from(doc.getString("status") ?: ""),
                    markedByRepId = doc.getString("markedByRepId") ?: ""
                )
            })
        } catch (e: Exception) { Result.failure(e) }
    }

    // ── Materials ─────────────────────────────────────────────────────────────

    suspend fun getMaterialsForSection(sectionId: String): Result<List<Material>> {
        return try {
            val snap = db.collection("materials")
                .whereEqualTo("sectionId", sectionId).get().await()
            Result.success(snap.documents.map { doc ->
                Material(id = doc.id,
                    title = doc.getString("title") ?: "",
                    subjectId = doc.getString("subjectId") ?: "",
                    subjectName = doc.getString("subjectName") ?: "",
                    sectionId = doc.getString("sectionId") ?: "",
                    uploadedBy = doc.getString("uploadedBy") ?: "",
                    uploaderName = doc.getString("uploaderName") ?: "",
                    fileUrl = doc.getString("fileUrl") ?: "",
                    fileName = doc.getString("fileName") ?: "",
                    uploadedAt = doc.getLong("uploadedAt") ?: 0L)
            }.sortedByDescending { it.uploadedAt })
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun addMaterial(material: Material): Result<String> {
        return try {
            val ref = db.collection("materials").add(mapOf(
                "title" to material.title, "subjectId" to material.subjectId,
                "subjectName" to material.subjectName, "sectionId" to material.sectionId,
                "uploadedBy" to material.uploadedBy, "uploaderName" to material.uploaderName,
                "fileUrl" to material.fileUrl, "fileName" to material.fileName,
                "uploadedAt" to System.currentTimeMillis()
            )).await()
            Result.success(ref.id)
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun deleteMaterial(id: String): Result<Unit> {
        return try {
            db.collection("materials").document(id).delete().await()
            Result.success(Unit)
        } catch (e: Exception) { Result.failure(e) }
    }

    // ── Assignments ───────────────────────────────────────────────────────────

    suspend fun getAssignmentsForSection(sectionId: String): Result<List<Assignment>> {
        return try {
            val snap = db.collection("assignments")
                .whereEqualTo("sectionId", sectionId).get().await()
            Result.success(snap.documents.map { doc ->
                Assignment(id = doc.id,
                    title = doc.getString("title") ?: "",
                    description = doc.getString("description") ?: "",
                    subjectId = doc.getString("subjectId") ?: "",
                    subjectName = doc.getString("subjectName") ?: "",
                    sectionId = doc.getString("sectionId") ?: "",
                    teacherId = doc.getString("teacherId") ?: "",
                    deadline = doc.getLong("deadline") ?: 0L,
                    createdAt = doc.getLong("createdAt") ?: 0L)
            }.sortedByDescending { it.createdAt })
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun createAssignment(assignment: Assignment): Result<String> {
        return try {
            val ref = db.collection("assignments").add(mapOf(
                "title" to assignment.title, "description" to assignment.description,
                "subjectId" to assignment.subjectId, "subjectName" to assignment.subjectName,
                "sectionId" to assignment.sectionId, "teacherId" to assignment.teacherId,
                "deadline" to assignment.deadline, "createdAt" to System.currentTimeMillis()
            )).await()
            Result.success(ref.id)
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun getSubmissionsForAssignment(assignmentId: String): Result<List<Submission>> {
        return try {
            val snap = db.collection("submissions")
                .whereEqualTo("assignmentId", assignmentId).get().await()
            Result.success(snap.documents.map { doc ->
                Submission(id = doc.id,
                    assignmentId = doc.getString("assignmentId") ?: "",
                    studentId = doc.getString("studentId") ?: "",
                    studentName = doc.getString("studentName") ?: "",
                    textResponse = doc.getString("textResponse") ?: "",
                    fileUrl = doc.getString("fileUrl") ?: "",
                    fileName = doc.getString("fileName") ?: "",
                    submittedAt = doc.getLong("submittedAt") ?: 0L,
                    grade = doc.getDouble("grade"),
                    feedback = doc.getString("feedback") ?: "")
            })
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun submitAssignment(submission: Submission): Result<String> {
        return try {
            val ref = db.collection("submissions").add(mapOf(
                "assignmentId" to submission.assignmentId,
                "studentId" to submission.studentId,
                "studentName" to submission.studentName,
                "textResponse" to submission.textResponse,
                "fileUrl" to submission.fileUrl,
                "fileName" to submission.fileName,
                "submittedAt" to System.currentTimeMillis()
            )).await()
            Result.success(ref.id)
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun gradeSubmission(submissionId: String, grade: Double, feedback: String): Result<Unit> {
        return try {
            db.collection("submissions").document(submissionId)
                .update(mapOf("grade" to grade, "feedback" to feedback)).await()
            Result.success(Unit)
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun getStudentSubmission(assignmentId: String, studentId: String): Result<Submission?> {
        return try {
            val snap = db.collection("submissions")
                .whereEqualTo("assignmentId", assignmentId)
                .whereEqualTo("studentId", studentId).get().await()
            if (snap.isEmpty) return Result.success(null)
            val doc = snap.documents.first()
            Result.success(Submission(id = doc.id,
                assignmentId = doc.getString("assignmentId") ?: "",
                studentId = doc.getString("studentId") ?: "",
                textResponse = doc.getString("textResponse") ?: "",
                fileUrl = doc.getString("fileUrl") ?: "",
                submittedAt = doc.getLong("submittedAt") ?: 0L,
                grade = doc.getDouble("grade"),
                feedback = doc.getString("feedback") ?: ""))
        } catch (e: Exception) { Result.failure(e) }
    }

    // ── Grades ────────────────────────────────────────────────────────────────

    suspend fun getGradesForStudent(studentId: String): Result<List<Grade>> {
        return try {
            val snap = db.collection("grades").whereEqualTo("studentId", studentId).get().await()
            Result.success(snap.documents.map { doc ->
                Grade(id = doc.id, studentId = doc.getString("studentId") ?: "",
                    studentName = doc.getString("studentName") ?: "",
                    subjectId = doc.getString("subjectId") ?: "",
                    subjectName = doc.getString("subjectName") ?: "",
                    teacherId = doc.getString("teacherId") ?: "",
                    value = doc.getDouble("value") ?: 0.0,
                    letterGrade = doc.getString("letterGrade") ?: "",
                    recordedAt = doc.getLong("recordedAt") ?: 0L)
            })
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun getGradesForSection(sectionId: String): Result<List<Grade>> {
        return try {
            val snap = db.collection("grades").whereEqualTo("sectionId", sectionId).get().await()
            Result.success(snap.documents.map { doc ->
                Grade(id = doc.id, studentId = doc.getString("studentId") ?: "",
                    studentName = doc.getString("studentName") ?: "",
                    subjectId = doc.getString("subjectId") ?: "",
                    subjectName = doc.getString("subjectName") ?: "",
                    teacherId = doc.getString("teacherId") ?: "",
                    value = doc.getDouble("value") ?: 0.0,
                    letterGrade = doc.getString("letterGrade") ?: "",
                    recordedAt = doc.getLong("recordedAt") ?: 0L)
            })
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun saveGrade(grade: Grade, sectionId: String): Result<Unit> {
        return try {
            val existing = db.collection("grades")
                .whereEqualTo("studentId", grade.studentId)
                .whereEqualTo("subjectId", grade.subjectId).get().await()
            val letter = when {
                grade.value >= 90 -> "A+"; grade.value >= 85 -> "A"; grade.value >= 80 -> "A-"
                grade.value >= 75 -> "B+"; grade.value >= 70 -> "B"; grade.value >= 65 -> "B-"
                grade.value >= 60 -> "C+"; grade.value >= 55 -> "C"; grade.value >= 50 -> "C-"
                grade.value >= 45 -> "D"; else -> "F"
            }
            val data = mapOf("studentId" to grade.studentId, "studentName" to grade.studentName,
                "subjectId" to grade.subjectId, "subjectName" to grade.subjectName,
                "teacherId" to grade.teacherId, "sectionId" to sectionId,
                "value" to grade.value, "letterGrade" to letter,
                "recordedAt" to System.currentTimeMillis())
            if (existing.isEmpty) db.collection("grades").add(data).await()
            else db.collection("grades").document(existing.documents.first().id).set(data).await()
            Result.success(Unit)
        } catch (e: Exception) { Result.failure(e) }
    }

    // ── Complaints ────────────────────────────────────────────────────────────

    suspend fun submitComplaint(complaint: Complaint): Result<String> {
        return try {
            val ref = db.collection("complaints").add(mapOf(
                "studentId" to complaint.studentId, "studentName" to complaint.studentName,
                "category" to complaint.category.value, "title" to complaint.title,
                "description" to complaint.description, "status" to ComplaintStatus.OPEN.value,
                "targetTeacherId" to complaint.targetTeacherId,
                "createdAt" to System.currentTimeMillis(), "responses" to emptyList<Any>()
            )).await()
            Result.success(ref.id)
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun getComplaintsForStudent(studentId: String): Result<List<Complaint>> {
        return try {
            val snap = db.collection("complaints").whereEqualTo("studentId", studentId).get().await()
            Result.success(snap.documents.map { mapComplaint(it.id, it.data) })
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun getComplaintsForTeacher(teacherId: String): Result<List<Complaint>> {
        return try {
            val snap = db.collection("complaints").whereEqualTo("targetTeacherId", teacherId).get().await()
            Result.success(snap.documents.map { mapComplaint(it.id, it.data) })
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun respondToComplaint(complaintId: String, response: ComplaintResponse, newStatus: ComplaintStatus): Result<Unit> {
        return try {
            val doc = db.collection("complaints").document(complaintId).get().await()
            @Suppress("UNCHECKED_CAST")
            val existing = (doc.get("responses") as? List<Map<String, Any>>) ?: emptyList()
            val newResp = mapOf("responderId" to response.responderId,
                "responderName" to response.responderName, "message" to response.message,
                "timestamp" to System.currentTimeMillis())
            db.collection("complaints").document(complaintId)
                .update(mapOf("responses" to existing + newResp, "status" to newStatus.value)).await()
            Result.success(Unit)
        } catch (e: Exception) { Result.failure(e) }
    }

    private fun mapComplaint(id: String, data: Map<String, Any>?): Complaint {
        data ?: return Complaint(id = id)
        @Suppress("UNCHECKED_CAST")
        val responses = (data["responses"] as? List<Map<String, Any>>)?.map {
            ComplaintResponse(responderId = it["responderId"] as? String ?: "",
                responderName = it["responderName"] as? String ?: "",
                message = it["message"] as? String ?: "",
                timestamp = it["timestamp"] as? Long ?: 0L)
        } ?: emptyList()
        return Complaint(id = id, studentId = data["studentId"] as? String ?: "",
            studentName = data["studentName"] as? String ?: "",
            category = ComplaintCategory.from(data["category"] as? String ?: ""),
            title = data["title"] as? String ?: "", description = data["description"] as? String ?: "",
            status = ComplaintStatus.from(data["status"] as? String ?: ""),
            responses = responses, createdAt = data["createdAt"] as? Long ?: 0L,
            targetTeacherId = data["targetTeacherId"] as? String ?: "")
    }

    // ── Announcements ─────────────────────────────────────────────────────────

    suspend fun getAnnouncements(userId: String, role: String, sectionId: String): Result<List<Announcement>> {
        return try {
            val snap = db.collection("announcements").get().await()
            val all = snap.documents.map { doc ->
                @Suppress("UNCHECKED_CAST")
                val replies = (doc.get("replies") as? List<Map<String, Any>>)?.map {
                    AnnouncementReply(authorId = it["authorId"] as? String ?: "",
                        authorName = it["authorName"] as? String ?: "",
                        message = it["message"] as? String ?: "",
                        timestamp = it["timestamp"] as? Long ?: 0L)
                } ?: emptyList()
                Announcement(id = doc.id, title = doc.getString("title") ?: "",
                    body = doc.getString("body") ?: "", authorId = doc.getString("authorId") ?: "",
                    authorName = doc.getString("authorName") ?: "",
                    targetRole = doc.getString("targetRole") ?: "all",
                    createdAt = doc.getLong("createdAt") ?: 0L, replies = replies)
            }.filter { a -> a.targetRole == "all" || a.targetRole == role || a.targetRole == sectionId }
                .sortedByDescending { it.createdAt }
            Result.success(all)
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun postAnnouncement(announcement: Announcement): Result<String> {
        return try {
            val ref = db.collection("announcements").add(mapOf(
                "title" to announcement.title, "body" to announcement.body,
                "authorId" to announcement.authorId, "authorName" to announcement.authorName,
                "targetRole" to announcement.targetRole,
                "createdAt" to System.currentTimeMillis(), "replies" to emptyList<Any>()
            )).await()
            Result.success(ref.id)
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun replyToAnnouncement(announcementId: String, reply: AnnouncementReply): Result<Unit> {
        return try {
            val doc = db.collection("announcements").document(announcementId).get().await()
            @Suppress("UNCHECKED_CAST")
            val existing = (doc.get("replies") as? List<Map<String, Any>>) ?: emptyList()
            val newReply = mapOf("authorId" to reply.authorId, "authorName" to reply.authorName,
                "message" to reply.message, "timestamp" to System.currentTimeMillis())
            db.collection("announcements").document(announcementId)
                .update("replies", existing + newReply).await()
            Result.success(Unit)
        } catch (e: Exception) { Result.failure(e) }
    }

    // ── Notifications ─────────────────────────────────────────────────────────

    suspend fun getNotifications(userId: String): Result<List<AppNotification>> {
        return try {
            val snap = db.collection("notifications").whereEqualTo("userId", userId).get().await()
            Result.success(snap.documents.map { doc ->
                AppNotification(id = doc.id, userId = doc.getString("userId") ?: "",
                    title = doc.getString("title") ?: "", body = doc.getString("body") ?: "",
                    type = doc.getString("type") ?: "", referenceId = doc.getString("referenceId") ?: "",
                    isRead = doc.getBoolean("isRead") ?: false,
                    createdAt = doc.getLong("createdAt") ?: 0L)
            }.sortedByDescending { it.createdAt })
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun markNotificationRead(id: String): Result<Unit> {
        return try {
            db.collection("notifications").document(id).update("isRead", true).await()
            Result.success(Unit)
        } catch (e: Exception) { Result.failure(e) }
    }

    // ── Chat ──────────────────────────────────────────────────────────────────

    fun chatThreadId(uid1: String, uid2: String) = listOf(uid1, uid2).sorted().joinToString("_")

    suspend fun getChatThreads(userId: String): Result<List<ChatThread>> {
        return try {
            val snap = db.collection("chatThreads").whereArrayContains("participantIds", userId).get().await()
            Result.success(snap.documents.map { doc ->
                @Suppress("UNCHECKED_CAST")
                val ids = doc.get("participantIds") as? List<String> ?: emptyList()
                @Suppress("UNCHECKED_CAST")
                val names = doc.get("participantNames") as? Map<String, String> ?: emptyMap()
                ChatThread(id = doc.id, participantIds = ids, participantNames = names,
                    lastMessage = doc.getString("lastMessage") ?: "",
                    lastMessageAt = doc.getLong("lastMessageAt") ?: 0L)
            }.sortedByDescending { it.lastMessageAt })
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun getMessages(threadId: String): Result<List<ChatMessage>> {
        return try {
            val snap = db.collection("chatThreads").document(threadId).collection("messages").get().await()
            Result.success(snap.documents.map { doc ->
                ChatMessage(id = doc.id, senderId = doc.getString("senderId") ?: "",
                    senderName = doc.getString("senderName") ?: "",
                    receiverId = doc.getString("receiverId") ?: "",
                    message = doc.getString("message") ?: "",
                    timestamp = doc.getLong("timestamp") ?: 0L,
                    isRead = doc.getBoolean("isRead") ?: false)
            }.sortedBy { it.timestamp })
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun sendMessage(threadId: String, message: ChatMessage, otherUserName: String): Result<Unit> {
        return try {
            val threadRef = db.collection("chatThreads").document(threadId)
            val threadDoc = threadRef.get().await()
            if (!threadDoc.exists()) {
                threadRef.set(mapOf("participantIds" to listOf(message.senderId, message.receiverId),
                    "participantNames" to mapOf(message.senderId to message.senderName,
                        message.receiverId to otherUserName),
                    "lastMessage" to message.message, "lastMessageAt" to System.currentTimeMillis())).await()
            } else {
                threadRef.update(mapOf("lastMessage" to message.message,
                    "lastMessageAt" to System.currentTimeMillis())).await()
            }
            threadRef.collection("messages").add(mapOf("senderId" to message.senderId,
                "senderName" to message.senderName, "receiverId" to message.receiverId,
                "message" to message.message, "timestamp" to System.currentTimeMillis(),
                "isRead" to false)).await()
            Result.success(Unit)
        } catch (e: Exception) { Result.failure(e) }
    }

    // ── Quizzes ───────────────────────────────────────────────────────────────

    suspend fun getQuizzesForSection(sectionId: String): Result<List<Quiz>> {
        return try {
            val snap = db.collection("quizzes").whereEqualTo("sectionId", sectionId).get().await()
            Result.success(snap.documents.map { mapQuiz(it.id, it.data) })
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun getQuizzesForTeacher(teacherId: String): Result<List<Quiz>> {
        return try {
            val snap = db.collection("quizzes").whereEqualTo("teacherId", teacherId).get().await()
            Result.success(snap.documents.map { mapQuiz(it.id, it.data) })
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun createQuiz(quiz: Quiz): Result<String> {
        return try {
            val questions = quiz.questions.map { q ->
                mapOf("id" to q.id, "text" to q.text, "type" to q.type.value,
                    "options" to q.options, "correctAnswer" to q.correctAnswer)
            }
            val ref = db.collection("quizzes").add(mapOf("title" to quiz.title,
                "subjectId" to quiz.subjectId, "subjectName" to quiz.subjectName,
                "sectionId" to quiz.sectionId, "teacherId" to quiz.teacherId,
                "isAvailable" to quiz.isAvailable, "timerMode" to quiz.timerMode.value,
                "totalTimeSeconds" to quiz.totalTimeSeconds,
                "perQuestionSeconds" to quiz.perQuestionSeconds,
                "questions" to questions, "createdAt" to System.currentTimeMillis())).await()
            Result.success(ref.id)
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun setQuizAvailability(quizId: String, available: Boolean): Result<Unit> {
        return try {
            db.collection("quizzes").document(quizId).update("isAvailable", available).await()
            Result.success(Unit)
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun submitQuizSession(session: QuizSession): Result<Unit> {
        return try {
            db.collection("quizSessions").add(mapOf("quizId" to session.quizId,
                "studentId" to session.studentId, "studentName" to session.studentName,
                "answers" to session.answers, "score" to session.score,
                "maxScore" to session.maxScore, "submittedAt" to System.currentTimeMillis(),
                "completed" to true)).await()
            Result.success(Unit)
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun getQuizSessions(quizId: String): Result<List<QuizSession>> {
        return try {
            val snap = db.collection("quizSessions").whereEqualTo("quizId", quizId).get().await()
            Result.success(snap.documents.map { doc ->
                @Suppress("UNCHECKED_CAST")
                val answers = doc.get("answers") as? Map<String, String> ?: emptyMap()
                QuizSession(id = doc.id, quizId = doc.getString("quizId") ?: "",
                    studentId = doc.getString("studentId") ?: "",
                    studentName = doc.getString("studentName") ?: "",
                    answers = answers, score = doc.getDouble("score") ?: 0.0,
                    maxScore = doc.getDouble("maxScore") ?: 0.0,
                    submittedAt = doc.getLong("submittedAt") ?: 0L,
                    completed = doc.getBoolean("completed") ?: false)
            }.sortedByDescending { it.score })
        } catch (e: Exception) { Result.failure(e) }
    }

    private fun mapQuiz(id: String, data: Map<String, Any>?): Quiz {
        data ?: return Quiz(id = id)
        @Suppress("UNCHECKED_CAST")
        val questions = (data["questions"] as? List<Map<String, Any>>)?.map { q ->
            @Suppress("UNCHECKED_CAST")
            QuizQuestion(id = q["id"] as? String ?: "", text = q["text"] as? String ?: "",
                type = QuestionType.from(q["type"] as? String ?: ""),
                options = q["options"] as? List<String> ?: emptyList(),
                correctAnswer = q["correctAnswer"] as? String ?: "")
        } ?: emptyList()
        return Quiz(id = id, title = data["title"] as? String ?: "",
            subjectId = data["subjectId"] as? String ?: "",
            subjectName = data["subjectName"] as? String ?: "",
            sectionId = data["sectionId"] as? String ?: "",
            teacherId = data["teacherId"] as? String ?: "",
            isAvailable = data["isAvailable"] as? Boolean ?: false,
            timerMode = TimerMode.from(data["timerMode"] as? String ?: ""),
            totalTimeSeconds = (data["totalTimeSeconds"] as? Long ?: 1800L).toInt(),
            perQuestionSeconds = (data["perQuestionSeconds"] as? Long ?: 60L).toInt(),
            createdAt = data["createdAt"] as? Long ?: 0L, questions = questions)
    }
}

