package com.fams.app.domain.model

// ── Attendance ────────────────────────────────────────────────────────────────

data class AttendanceRecord(
    val id: String = "",
    val studentId: String = "",
    val studentName: String = "",
    val teacherId: String = "",
    val sectionId: String = "",
    val subjectId: String = "",
    val subjectName: String = "",
    val date: String = "",           // "2024-01-15"
    val status: AttendanceStatus = AttendanceStatus.ABSENT,
    val sessionId: String = ""
)

data class TeacherAttendanceRecord(
    val id: String = "",
    val teacherId: String = "",
    val teacherName: String = "",
    val sectionId: String = "",
    val subjectId: String = "",
    val subjectName: String = "",
    val date: String = "",
    val status: AttendanceStatus = AttendanceStatus.ABSENT,
    val markedByRepId: String = ""
)

enum class AttendanceStatus(val value: String) {
    PRESENT("present"),
    ABSENT("absent"),
    LATE("late");
    companion object {
        fun from(v: String) = values().firstOrNull { it.value == v } ?: ABSENT
    }
}

// ── Materials ─────────────────────────────────────────────────────────────────

data class Material(
    val id: String = "",
    val title: String = "",
    val subjectId: String = "",
    val subjectName: String = "",
    val sectionId: String = "",
    val uploadedBy: String = "",
    val uploaderName: String = "",
    val fileUrl: String = "",
    val fileName: String = "",
    val uploadedAt: Long = 0L
)

// ── Assignments ───────────────────────────────────────────────────────────────

data class Assignment(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val subjectId: String = "",
    val subjectName: String = "",
    val sectionId: String = "",
    val teacherId: String = "",
    val deadline: Long = 0L,
    val createdAt: Long = 0L
)

data class Submission(
    val id: String = "",
    val assignmentId: String = "",
    val studentId: String = "",
    val studentName: String = "",
    val textResponse: String = "",
    val fileUrl: String = "",
    val fileName: String = "",
    val submittedAt: Long = 0L,
    val grade: Double? = null,
    val feedback: String = ""
)

// ── Quizzes ───────────────────────────────────────────────────────────────────

data class Quiz(
    val id: String = "",
    val title: String = "",
    val subjectId: String = "",
    val subjectName: String = "",
    val sectionId: String = "",
    val teacherId: String = "",
    val isAvailable: Boolean = false,
    val timerMode: TimerMode = TimerMode.TOTAL,
    val totalTimeSeconds: Int = 1800,
    val perQuestionSeconds: Int = 60,
    val createdAt: Long = 0L,
    val questions: List<QuizQuestion> = emptyList()
)

data class QuizQuestion(
    val id: String = "",
    val text: String = "",
    val type: QuestionType = QuestionType.MCQ,
    val options: List<String> = emptyList(),
    val correctAnswer: String = ""
)

enum class QuestionType(val value: String) {
    MCQ("mcq"), TRUE_FALSE("true_false"), SHORT_ANSWER("short_answer");
    companion object { fun from(v: String) = values().firstOrNull { it.value == v } ?: MCQ }
}

enum class TimerMode(val value: String) {
    TOTAL("total"), PER_QUESTION("per_question");
    companion object { fun from(v: String) = values().firstOrNull { it.value == v } ?: TOTAL }
}

data class QuizSession(
    val id: String = "",
    val quizId: String = "",
    val studentId: String = "",
    val studentName: String = "",
    val answers: Map<String, String> = emptyMap(),
    val score: Double = 0.0,
    val maxScore: Double = 0.0,
    val submittedAt: Long = 0L,
    val completed: Boolean = false
)

// ── Grades ────────────────────────────────────────────────────────────────────

data class Grade(
    val id: String = "",
    val studentId: String = "",
    val studentName: String = "",
    val subjectId: String = "",
    val subjectName: String = "",
    val teacherId: String = "",
    val value: Double = 0.0,
    val letterGrade: String = "",
    val recordedAt: Long = 0L
)

// ── Complaints ────────────────────────────────────────────────────────────────

data class Complaint(
    val id: String = "",
    val studentId: String = "",
    val studentName: String = "",
    val category: ComplaintCategory = ComplaintCategory.OTHER,
    val title: String = "",
    val description: String = "",
    val status: ComplaintStatus = ComplaintStatus.OPEN,
    val responses: List<ComplaintResponse> = emptyList(),
    val createdAt: Long = 0L,
    val targetTeacherId: String = ""
)

data class ComplaintResponse(
    val responderId: String = "",
    val responderName: String = "",
    val message: String = "",
    val timestamp: Long = 0L
)

enum class ComplaintCategory(val value: String) {
    GRADES("grades"), ATTENDANCE("attendance"), ASSIGNMENTS("assignments"), OTHER("other");
    companion object { fun from(v: String) = values().firstOrNull { it.value == v } ?: OTHER }
}

enum class ComplaintStatus(val value: String) {
    OPEN("open"), IN_PROGRESS("in_progress"), RESOLVED("resolved");
    companion object { fun from(v: String) = values().firstOrNull { it.value == v } ?: OPEN }
}

// ── Announcements ─────────────────────────────────────────────────────────────

data class Announcement(
    val id: String = "",
    val title: String = "",
    val body: String = "",
    val authorId: String = "",
    val authorName: String = "",
    val targetRole: String = "all",   // "all", "student", "teacher", sectionId
    val createdAt: Long = 0L,
    val replies: List<AnnouncementReply> = emptyList()
)

data class AnnouncementReply(
    val authorId: String = "",
    val authorName: String = "",
    val message: String = "",
    val timestamp: Long = 0L
)

// ── Notifications ─────────────────────────────────────────────────────────────

data class AppNotification(
    val id: String = "",
    val userId: String = "",
    val title: String = "",
    val body: String = "",
    val type: String = "",
    val referenceId: String = "",
    val isRead: Boolean = false,
    val createdAt: Long = 0L
)

// ── Chat ──────────────────────────────────────────────────────────────────────

data class ChatMessage(
    val id: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val receiverId: String = "",
    val message: String = "",
    val timestamp: Long = 0L,
    val isRead: Boolean = false
)

data class ChatThread(
    val id: String = "",           // sorted uid1_uid2
    val participantIds: List<String> = emptyList(),
    val participantNames: Map<String, String> = emptyMap(),
    val lastMessage: String = "",
    val lastMessageAt: Long = 0L,
    val unreadCount: Int = 0
)

// ── Notes / To‑Do / University Info ─────────────────────────────────────────

data class Note(
    val id: String = "",
    val userId: String = "",
    val title: String = "",
    val body: String = "",
    val createdAt: Long = 0L
)

data class TodoItem(
    val id: String = "",
    val userId: String = "",
    val title: String = "",
    val done: Boolean = false,
    val timestamp: Long = 0L
)

data class UniversityInfo(
    val id: String = "",
    val title: String = "",
    val body: String = "",
    val updatedAt: Long = 0L
)
