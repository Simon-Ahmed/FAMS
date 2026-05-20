package com.fams.app.ui.screens.student

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.fams.app.data.repository.AcademicRepository
import com.fams.app.data.repository.NotesRepository
import com.fams.app.data.repository.TodoRepository
import com.fams.app.data.repository.UniversityInfoRepository
import com.fams.app.data.repository.AuthRepository
import com.fams.app.domain.model.*
import com.fams.app.domain.model.ChatMessage
import com.fams.app.domain.model.ChatThread
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

data class StudentUiState(
    val currentUser: User? = null,
    val schedule: List<ScheduleEntry> = emptyList(),
    val todayClasses: List<ScheduleEntry> = emptyList(),
    val materials: List<Material> = emptyList(),
    val assignments: List<Assignment> = emptyList(),
    val mySubmissions: Map<String, Submission> = emptyMap(),
    val quizzes: List<Quiz> = emptyList(),
    val grades: List<Grade> = emptyList(),
    val attendanceRecords: List<AttendanceRecord> = emptyList(),
    val complaints: List<Complaint> = emptyList(),
    val announcements: List<Announcement> = emptyList(),
    val notifications: List<AppNotification> = emptyList(),
    val chatThreads: List<ChatThread> = emptyList(),
    val messages: List<ChatMessage> = emptyList(),
    val notes: List<com.fams.app.domain.model.Note> = emptyList(),
    val todos: List<com.fams.app.domain.model.TodoItem> = emptyList(),
    val uniInfo: List<com.fams.app.domain.model.UniversityInfo> = emptyList(),
    val activeQuiz: Quiz? = null,
    val quizSessions: List<QuizSession> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

class StudentViewModel(
    private val repo: AcademicRepository = AcademicRepository(),
    private val authRepo: AuthRepository = AuthRepository()
) : ViewModel() {

    private val notesRepo = NotesRepository()
    private val todoRepo = TodoRepository()
    private val uniRepo = UniversityInfoRepository()

    private val _state = MutableStateFlow(StudentUiState())
    val state: StateFlow<StudentUiState> = _state.asStateFlow()

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
            val materials = repo.getMaterialsForSection(user.sectionId).getOrElse { emptyList() }
            val assignments = repo.getAssignmentsForSection(user.sectionId).getOrElse { emptyList() }
            val grades = repo.getGradesForStudent(user.uid).getOrElse { emptyList() }
            val attendance = repo.getAttendanceForStudent(user.uid).getOrElse { emptyList() }
            val complaints = repo.getComplaintsForStudent(user.uid).getOrElse { emptyList() }
            val announcements = repo.getAnnouncements(user.uid, "student", user.sectionId).getOrElse { emptyList() }
            val notifications = repo.getNotifications(user.uid).getOrElse { emptyList() }
            val quizzes = repo.getQuizzesForSection(user.sectionId).getOrElse { emptyList() }
            val chatThreads = repo.getChatThreads(user.uid).getOrElse { emptyList() }
            val notes = notesRepo.getNotes(user.uid).getOrElse { emptyList() }
            val todos = todoRepo.getTodos(user.uid).getOrElse { emptyList() }
            val uniInfo = uniRepo.getInfo().getOrElse { emptyList() }

            // Load my submissions for each assignment
            val submissionsMap = mutableMapOf<String, Submission>()
            assignments.forEach { a ->
                repo.getStudentSubmission(a.id, user.uid).getOrNull()?.let {
                    submissionsMap[a.id] = it
                }
            }

            // Load quiz sessions for this student across all quizzes
            val quizSessions = mutableListOf<QuizSession>()
            quizzes.forEach { quiz ->
                repo.getQuizSessions(quiz.id).getOrNull()
                    ?.filter { it.studentId == user.uid }
                    ?.let { quizSessions.addAll(it) }
            }

            _state.value = _state.value.copy(
                schedule = schedule, todayClasses = today,
                materials = materials, assignments = assignments,
                mySubmissions = submissionsMap, grades = grades,
                attendanceRecords = attendance, complaints = complaints,
                announcements = announcements, notifications = notifications,
                quizzes = quizzes, quizSessions = quizSessions,
                chatThreads = chatThreads, notes = notes, todos = todos, uniInfo = uniInfo, isLoading = false
            )
        }
    }

    fun addNote(title: String, body: String) {
        viewModelScope.launch {
            val user = _state.value.currentUser ?: return@launch
            val note = com.fams.app.domain.model.Note(userId = user.uid, title = title, body = body, createdAt = System.currentTimeMillis())
            notesRepo.addNote(note).fold(
                onSuccess = { loadNotes(user.uid) },
                onFailure = { _state.value = _state.value.copy(errorMessage = it.message) }
            )
        }
    }

    private fun loadNotes(userId: String) {
        viewModelScope.launch {
            val notes = notesRepo.getNotes(userId).getOrElse { emptyList() }
            _state.value = _state.value.copy(notes = notes)
        }
    }

    fun addTodo(title: String) {
        viewModelScope.launch {
            val user = _state.value.currentUser ?: return@launch
            val todo = com.fams.app.domain.model.TodoItem(userId = user.uid, title = title, timestamp = System.currentTimeMillis())
            todoRepo.addTodo(todo).fold(
                onSuccess = { loadTodos(user.uid) },
                onFailure = { _state.value = _state.value.copy(errorMessage = it.message) }
            )
        }
    }

    private fun loadTodos(userId: String) {
        viewModelScope.launch {
            val todos = todoRepo.getTodos(userId).getOrElse { emptyList() }
            _state.value = _state.value.copy(todos = todos)
        }
    }

    fun toggleTodo(id: String, done: Boolean) {
        viewModelScope.launch {
            val user = _state.value.currentUser ?: return@launch
            todoRepo.toggleDone(id, done).fold(
                onSuccess = { loadTodos(user.uid) },
                onFailure = { _state.value = _state.value.copy(errorMessage = it.message) }
            )
        }
    }

    fun deleteTodo(id: String) {
        viewModelScope.launch {
            val user = _state.value.currentUser ?: return@launch
            todoRepo.deleteTodo(id).fold(
                onSuccess = { loadTodos(user.uid) },
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

    fun submitAssignment(assignmentId: String, textResponse: String) {
        viewModelScope.launch {
            val user = _state.value.currentUser ?: return@launch
            val sub = Submission(assignmentId = assignmentId, studentId = user.uid,
                studentName = user.fullName, textResponse = textResponse)
            repo.submitAssignment(sub).fold(
                onSuccess = {
                    loadAll(user)
                    _state.value = _state.value.copy(successMessage = "Assignment submitted")
                },
                onFailure = { _state.value = _state.value.copy(errorMessage = it.message) }
            )
        }
    }

    fun submitComplaint(category: ComplaintCategory, title: String, description: String, targetTeacherId: String) {
        viewModelScope.launch {
            val user = _state.value.currentUser ?: return@launch
            val complaint = Complaint(studentId = user.uid, studentName = user.fullName,
                category = category, title = title, description = description,
                targetTeacherId = targetTeacherId)
            repo.submitComplaint(complaint).fold(
                onSuccess = {
                    loadAll(user)
                    _state.value = _state.value.copy(successMessage = "Complaint submitted")
                },
                onFailure = { _state.value = _state.value.copy(errorMessage = it.message) }
            )
        }
    }

    fun startQuiz(quiz: Quiz) {
        _state.value = _state.value.copy(activeQuiz = quiz)
    }

    fun submitQuiz(answers: Map<String, String>) {
        viewModelScope.launch {
            val user = _state.value.currentUser ?: return@launch
            val quiz = _state.value.activeQuiz ?: return@launch
            var score = 0.0
            quiz.questions.forEach { q ->
                if (q.type != QuestionType.SHORT_ANSWER) {
                    if (answers[q.id]?.trim()?.lowercase() == q.correctAnswer.trim().lowercase()) score++
                }
            }
            val session = QuizSession(quizId = quiz.id, studentId = user.uid,
                studentName = user.fullName, answers = answers,
                score = score, maxScore = quiz.questions.size.toDouble())
            repo.submitQuizSession(session).fold(
                onSuccess = {
                    // Reload all sessions for this quiz to show leaderboard
                    val allSessions = repo.getQuizSessions(quiz.id).getOrElse { emptyList() }
                    val updatedSessions = (_state.value.quizSessions
                        .filter { it.quizId != quiz.id } + allSessions)
                    _state.value = _state.value.copy(
                        activeQuiz = null,
                        quizSessions = updatedSessions,
                        successMessage = "Quiz submitted! Score: ${score.toInt()}/${quiz.questions.size}"
                    )
                },
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

    fun replyToAnnouncement(announcementId: String, message: String) {
        viewModelScope.launch {
            val user = _state.value.currentUser ?: return@launch
            repo.replyToAnnouncement(announcementId,
                AnnouncementReply(authorId = user.uid, authorName = user.fullName, message = message))
            loadAll(user)
        }
    }

    fun clearMessages() {
        _state.value = _state.value.copy(errorMessage = null, successMessage = null)
    }

    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = StudentViewModel() as T
        }
    }
}
