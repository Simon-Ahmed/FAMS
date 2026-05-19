package com.fams.app.ui.screens.teacher

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.fams.app.data.repository.AcademicRepository
import com.fams.app.data.repository.AuthRepository
import com.fams.app.domain.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

data class TeacherUiState(
    val currentUser: User? = null,
    val schedule: List<ScheduleEntry> = emptyList(),
    val todayClasses: List<ScheduleEntry> = emptyList(),
    val materials: List<Material> = emptyList(),
    val assignments: List<Assignment> = emptyList(),
    val submissions: List<Submission> = emptyList(),
    val quizzes: List<Quiz> = emptyList(),
    val quizSessions: List<QuizSession> = emptyList(),
    val grades: List<Grade> = emptyList(),
    val complaints: List<Complaint> = emptyList(),
    val announcements: List<Announcement> = emptyList(),
    val notifications: List<AppNotification> = emptyList(),
    val chatThreads: List<ChatThread> = emptyList(),
    val messages: List<ChatMessage> = emptyList(),
    val sectionStudents: List<User> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    // Navigation state for quick actions
    val quickActionTab: Int? = null,       // which bottom tab to jump to
    val quickActionSubTab: Int? = null,    // which sub-tab inside Classes
    val quickActionSectionId: String? = null
)

class TeacherViewModel(
    private val repo: AcademicRepository = AcademicRepository(),
    private val authRepo: AuthRepository = AuthRepository()
) : ViewModel() {

    private val _state = MutableStateFlow(TeacherUiState())
    val state: StateFlow<TeacherUiState> = _state.asStateFlow()

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
            val schedule = repo.getScheduleForTeacher(user.uid).getOrElse { emptyList() }
            val today = schedule.filter { it.day.equals(todayName(), ignoreCase = true) }
            val notifications = repo.getNotifications(user.uid).getOrElse { emptyList() }
            val announcements = repo.getAnnouncements(user.uid, "teacher", "").getOrElse { emptyList() }
            val complaints = repo.getComplaintsForTeacher(user.uid).getOrElse { emptyList() }
            val chatThreads = repo.getChatThreads(user.uid).getOrElse { emptyList() }
            val quizzes = repo.getQuizzesForTeacher(user.uid).getOrElse { emptyList() }
            _state.value = _state.value.copy(
                schedule = schedule, todayClasses = today,
                notifications = notifications, announcements = announcements,
                complaints = complaints, chatThreads = chatThreads,
                quizzes = quizzes, isLoading = false
            )
        }
    }

    fun loadSectionData(sectionId: String) {
        viewModelScope.launch {
            val students = repo.getStudentsForSection(sectionId).getOrElse { emptyList() }
            val materials = repo.getMaterialsForSection(sectionId).getOrElse { emptyList() }
            val assignments = repo.getAssignmentsForSection(sectionId).getOrElse { emptyList() }
            val grades = repo.getGradesForSection(sectionId).getOrElse { emptyList() }
            _state.value = _state.value.copy(
                sectionStudents = students, materials = materials,
                assignments = assignments, grades = grades
            )
        }
    }

    fun loadSubmissions(assignmentId: String) {
        viewModelScope.launch {
            val subs = repo.getSubmissionsForAssignment(assignmentId).getOrElse { emptyList() }
            _state.value = _state.value.copy(submissions = subs)
        }
    }

    fun loadQuizSessions(quizId: String) {
        viewModelScope.launch {
            val sessions = repo.getQuizSessions(quizId).getOrElse { emptyList() }
            _state.value = _state.value.copy(quizSessions = sessions)
        }
    }

    fun submitAttendance(records: List<AttendanceRecord>) {
        viewModelScope.launch {
            repo.submitAttendance(records).fold(
                onSuccess = { _state.value = _state.value.copy(successMessage = "Attendance saved") },
                onFailure = { _state.value = _state.value.copy(errorMessage = it.message) }
            )
        }
    }

    fun addMaterial(material: Material) {
        viewModelScope.launch {
            repo.addMaterial(material).fold(
                onSuccess = {
                    val user = _state.value.currentUser ?: return@fold
                    loadSectionData(material.sectionId)
                    _state.value = _state.value.copy(successMessage = "Material uploaded")
                },
                onFailure = { _state.value = _state.value.copy(errorMessage = it.message) }
            )
        }
    }

    fun deleteMaterial(id: String, sectionId: String) {
        viewModelScope.launch {
            repo.deleteMaterial(id)
            loadSectionData(sectionId)
        }
    }

    fun createAssignment(assignment: Assignment) {
        viewModelScope.launch {
            repo.createAssignment(assignment).fold(
                onSuccess = {
                    loadSectionData(assignment.sectionId)
                    _state.value = _state.value.copy(successMessage = "Assignment created")
                },
                onFailure = { _state.value = _state.value.copy(errorMessage = it.message) }
            )
        }
    }

    fun gradeSubmission(submissionId: String, grade: Double, feedback: String) {
        viewModelScope.launch {
            repo.gradeSubmission(submissionId, grade, feedback).fold(
                onSuccess = { _state.value = _state.value.copy(successMessage = "Grade saved") },
                onFailure = { _state.value = _state.value.copy(errorMessage = it.message) }
            )
        }
    }

    fun createQuiz(quiz: Quiz) {
        viewModelScope.launch {
            repo.createQuiz(quiz).fold(
                onSuccess = {
                    val user = _state.value.currentUser ?: return@fold
                    loadAll(user)
                    _state.value = _state.value.copy(successMessage = "Quiz created")
                },
                onFailure = { _state.value = _state.value.copy(errorMessage = it.message) }
            )
        }
    }

    fun setQuizAvailability(quizId: String, available: Boolean) {
        viewModelScope.launch {
            repo.setQuizAvailability(quizId, available)
            val user = _state.value.currentUser ?: return@launch
            loadAll(user)
        }
    }

    fun saveGrade(grade: Grade, sectionId: String) {
        viewModelScope.launch {
            repo.saveGrade(grade, sectionId).fold(
                onSuccess = {
                    loadSectionData(sectionId)
                    _state.value = _state.value.copy(successMessage = "Grade saved")
                },
                onFailure = { _state.value = _state.value.copy(errorMessage = it.message) }
            )
        }
    }

    fun respondToComplaint(complaintId: String, response: ComplaintResponse, status: ComplaintStatus) {
        viewModelScope.launch {
            repo.respondToComplaint(complaintId, response, status).fold(
                onSuccess = {
                    val user = _state.value.currentUser ?: return@fold
                    loadAll(user)
                    _state.value = _state.value.copy(successMessage = "Response sent")
                },
                onFailure = { _state.value = _state.value.copy(errorMessage = it.message) }
            )
        }
    }

    fun postAnnouncement(announcement: Announcement) {
        viewModelScope.launch {
            repo.postAnnouncement(announcement).fold(
                onSuccess = {
                    val user = _state.value.currentUser ?: return@fold
                    loadAll(user)
                    _state.value = _state.value.copy(successMessage = "Announcement posted")
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

    fun markNotificationRead(id: String) {
        viewModelScope.launch {
            repo.markNotificationRead(id)
            val user = _state.value.currentUser ?: return@launch
            val notifs = repo.getNotifications(user.uid).getOrElse { emptyList() }
            _state.value = _state.value.copy(notifications = notifs)
        }
    }

    fun clearMessages() {
        _state.value = _state.value.copy(errorMessage = null, successMessage = null)
    }

    fun triggerQuickAction(subTab: Int) {
        val firstSectionId = _state.value.schedule.firstOrNull()?.sectionId
        _state.value = _state.value.copy(
            quickActionTab = 2,
            quickActionSubTab = subTab,
            quickActionSectionId = firstSectionId
        )
    }

    fun clearQuickAction() {
        _state.value = _state.value.copy(
            quickActionTab = null,
            quickActionSubTab = null,
            quickActionSectionId = null
        )
    }

    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = TeacherViewModel() as T
        }
    }
}
