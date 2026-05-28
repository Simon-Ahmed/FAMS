package com.fams.app.ui.screens.classrep

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.fams.app.data.repository.AcademicRepository
import com.fams.app.data.repository.AuthRepository
import com.fams.app.domain.model.*
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

data class ClassRepUiState(
    val currentUser: User? = null,
    val schedule: List<ScheduleEntry> = emptyList(),
    val todayClasses: List<ScheduleEntry> = emptyList(),
    val teacherAttendance: List<TeacherAttendanceRecord> = emptyList(),
    val announcements: List<Announcement> = emptyList(),
    val notifications: List<AppNotification> = emptyList(),
    val chatThreads: List<ChatThread> = emptyList(),
    val messages: List<ChatMessage> = emptyList(),
    val availableUsers: List<User> = emptyList(),
    val materials: List<Material> = emptyList(),
    val assignments: List<Assignment> = emptyList(),
    val mySubmissions: Map<String, Submission> = emptyMap(),
    val quizzes: List<Quiz> = emptyList(),
    val grades: List<Grade> = emptyList(),
    val attendanceRecords: List<AttendanceRecord> = emptyList(),
    val activeQuiz: Quiz? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

class ClassRepViewModel(
    private val repo: AcademicRepository = AcademicRepository(),
    private val authRepo: AuthRepository = AuthRepository()
) : ViewModel() {

    private val _state = MutableStateFlow(ClassRepUiState())
    val state: StateFlow<ClassRepUiState> = _state.asStateFlow()

    private val dayFormat = SimpleDateFormat("EEEE", Locale.getDefault())
    private fun todayName() = dayFormat.format(Date())

    init { loadCurrentUser() }

    private fun loadCurrentUser() {
        viewModelScope.launch {
            val user = authRepo.getCurrentUser() ?: return@launch
            _state.value = _state.value.copy(currentUser = user)
            loadAll(user)
        }
    }

    private fun loadAll(user: User) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            val schedule = repo.getScheduleForSection(user.sectionId).getOrElse { emptyList() }
            val today = schedule.filter { it.day.equals(todayName(), ignoreCase = true) }
            val teacherAtt = repo.getTeacherAttendanceForSection(user.sectionId).getOrElse { emptyList() }
            val announcements = repo.getAnnouncements(user.uid, "class_rep", user.sectionId).getOrElse { emptyList() }
            val notifications = repo.getNotifications(user.uid).getOrElse { emptyList() }
            val chatThreads = repo.getChatThreads(user.uid).getOrElse { emptyList() }
            val materials = repo.getMaterialsForSection(user.sectionId).getOrElse { emptyList() }
            val assignments = repo.getAssignmentsForSection(user.sectionId).getOrElse { emptyList() }
            val grades = repo.getGradesForStudent(user.uid).getOrElse { emptyList() }
            val attendance = repo.getAttendanceForStudent(user.uid).getOrElse { emptyList() }
            val quizzes = repo.getQuizzesForSection(user.sectionId).getOrElse { emptyList() }
            val submissionsMap = mutableMapOf<String, Submission>()
            assignments.forEach { a ->
                repo.getStudentSubmission(a.id, user.uid).getOrNull()?.let { submissionsMap[a.id] = it }
            }
            _state.value = _state.value.copy(
                schedule = schedule, todayClasses = today,
                teacherAttendance = teacherAtt, announcements = announcements,
                notifications = notifications, chatThreads = chatThreads,
                materials = materials, assignments = assignments,
                mySubmissions = submissionsMap, grades = grades,
                attendanceRecords = attendance, quizzes = quizzes, isLoading = false
            )
        }
    }

    fun markTeacherAttendance(teacherId: String, teacherName: String, subjectId: String,
                              subjectName: String, status: AttendanceStatus) {
        viewModelScope.launch {
            val user = _state.value.currentUser ?: return@launch
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            val record = TeacherAttendanceRecord(teacherId = teacherId, teacherName = teacherName,
                sectionId = user.sectionId, subjectId = subjectId, subjectName = subjectName,
                date = today, status = status, markedByRepId = user.uid)
            repo.submitTeacherAttendance(record).fold(
                onSuccess = {
                    loadAll(user)
                    _state.value = _state.value.copy(successMessage = "Teacher attendance marked")
                },
                onFailure = { _state.value = _state.value.copy(errorMessage = it.message) }
            )
        }
    }

    fun sendMessage(receiverId: String, receiverName: String, message: String) {
        viewModelScope.launch {
            val user = _state.value.currentUser ?: return@launch
            val threadId = repo.chatThreadId(user.uid, receiverId)
            val msg = ChatMessage(senderId = user.uid, senderName = user.fullName,
                receiverId = receiverId, message = message)
            repo.sendMessage(threadId, msg, receiverName)
            loadMessages(receiverId)
        }
    }

    fun loadMessages(otherUserId: String) {
        viewModelScope.launch {
            val user = _state.value.currentUser ?: return@launch
            val threadId = repo.chatThreadId(user.uid, otherUserId)
            val msgs = repo.getMessages(threadId).getOrElse { emptyList() }
            _state.value = _state.value.copy(messages = msgs)
        }
    }

    fun startQuiz(quiz: Quiz) { _state.value = _state.value.copy(activeQuiz = quiz) }

    fun submitQuiz(answers: Map<String, String>) {
        viewModelScope.launch {
            val user = _state.value.currentUser ?: return@launch
            val quiz = _state.value.activeQuiz ?: return@launch
            var score = 0.0
            quiz.questions.forEach { q ->
                if (q.type != QuestionType.SHORT_ANSWER &&
                    answers[q.id]?.trim()?.lowercase() == q.correctAnswer.trim().lowercase()) score++
            }
            val session = QuizSession(quizId = quiz.id, studentId = user.uid,
                studentName = user.fullName, answers = answers,
                score = score, maxScore = quiz.questions.size.toDouble())
            repo.submitQuizSession(session)
            _state.value = _state.value.copy(activeQuiz = null,
                successMessage = "Quiz submitted! Score: ${score.toInt()}/${quiz.questions.size}")
        }
    }

    fun submitAssignment(assignmentId: String, textResponse: String) {
        viewModelScope.launch {
            val user = _state.value.currentUser ?: return@launch
            val sub = Submission(assignmentId = assignmentId, studentId = user.uid,
                studentName = user.fullName, textResponse = textResponse)
            repo.submitAssignment(sub).fold(
                onSuccess = { loadAll(user); _state.value = _state.value.copy(successMessage = "Submitted") },
                onFailure = { _state.value = _state.value.copy(errorMessage = it.message) }
            )
        }
    }

    fun markNotificationRead(id: String) {
        viewModelScope.launch {
            repo.markNotificationRead(id)
            val user = _state.value.currentUser ?: return@launch
            val notifs = repo.getNotifications(user.uid).getOrElse { emptyList() }
            _state.value = _state.value.copy(notifications = notifs)
        }
    }

    fun postAnnouncement(announcement: Announcement) {
        viewModelScope.launch {
            try {
                Firebase.firestore.collection("announcements")
                    .add(announcement)
                    .await()
                _state.update { it.copy(successMessage = "Announcement posted successfully!") }
            } catch (e: Exception) {
                _state.update { it.copy(errorMessage = e.message) }
            }
        }
    }

    fun replyToAnnouncement(announcementId: String, reply: AnnouncementReply) {
        viewModelScope.launch {
            try {
                Firebase.firestore.collection("announcements")
                    .document(announcementId)
                    .update("replies", FieldValue.arrayUnion(reply))
                    .await()
                _state.update { it.copy(successMessage = "Reply added!") }
            } catch (e: Exception) {
                _state.update { it.copy(errorMessage = e.message) }
            }
        }
    }

    fun loadAvailableUsers() {
        viewModelScope.launch {
            // Class reps can chat with: teachers, coordinator
            val db = com.fams.app.di.FirebaseModule.firestore
            val snap = db.collection("users").get().await()
            val me = _state.value.currentUser?.uid ?: ""
            val users = snap.documents.mapNotNull { doc ->
                val role = doc.getString("role") ?: ""
                if (role in listOf("teacher", "coordinator") && doc.id != me) {
                    com.fams.app.data.repository.UserRepository().mapDocToUser(doc.id, doc.data)
                } else null
            }.sortedBy { it.fullName }
            _state.value = _state.value.copy(availableUsers = users)
        }
    }

    fun clearMessages() { _state.value = _state.value.copy(errorMessage = null, successMessage = null) }

    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = ClassRepViewModel() as T
        }
    }
}