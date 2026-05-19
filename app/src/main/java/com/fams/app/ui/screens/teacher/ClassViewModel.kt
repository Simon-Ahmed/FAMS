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

data class ClassUiState(
    val currentUser: User? = null,
    val entry: ScheduleEntry? = null,
    val students: List<User> = emptyList(),
    val attendanceMap: Map<String, AttendanceStatus> = emptyMap(),
    val materials: List<Material> = emptyList(),
    val assignments: List<Assignment> = emptyList(),
    val selectedAssignment: Assignment? = null,
    val submissions: List<Submission> = emptyList(),
    val quizzes: List<Quiz> = emptyList(),
    val grades: List<Grade> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

class ClassViewModel(
    private val repo: AcademicRepository = AcademicRepository(),
    private val authRepo: AuthRepository = AuthRepository()
) : ViewModel() {

    private val _state = MutableStateFlow(ClassUiState())
    val state: StateFlow<ClassUiState> = _state.asStateFlow()

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    fun loadClass(entry: ScheduleEntry) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, entry = entry)
            val user = authRepo.getCurrentUser()
            val students = repo.getStudentsForSection(entry.sectionId).getOrElse { emptyList() }
            val materials = repo.getMaterialsForSection(entry.sectionId).getOrElse { emptyList() }
                .filter { it.subjectId == entry.subjectId }
            val assignments = repo.getAssignmentsForSection(entry.sectionId).getOrElse { emptyList() }
                .filter { it.subjectId == entry.subjectId }
            val quizzes = repo.getQuizzesForTeacher(user?.uid ?: "").getOrElse { emptyList() }
                .filter { it.sectionId == entry.sectionId && it.subjectId == entry.subjectId }
            val grades = repo.getGradesForSection(entry.sectionId).getOrElse { emptyList() }
                .filter { it.subjectId == entry.subjectId }

            _state.value = _state.value.copy(
                currentUser = user, students = students, materials = materials,
                assignments = assignments, quizzes = quizzes, grades = grades,
                isLoading = false
            )
        }
    }

    fun updateAttendance(studentId: String, status: AttendanceStatus) {
        val current = _state.value.attendanceMap.toMutableMap()
        current[studentId] = status
        _state.value = _state.value.copy(attendanceMap = current)
    }

    fun submitAttendance() {
        val entry = _state.value.entry ?: return
        val user = _state.value.currentUser ?: return
        val map = _state.value.attendanceMap
        if (map.isEmpty()) {
            _state.value = _state.value.copy(errorMessage = "Mark attendance for at least one student")
            return
        }
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            val today = dateFormat.format(Date())
            val sessionId = "${entry.subjectId}_${entry.sectionId}_$today"
            val records = _state.value.students.map { student ->
                AttendanceRecord(
                    studentId = student.uid, studentName = student.fullName,
                    teacherId = user.uid, sectionId = entry.sectionId,
                    subjectId = entry.subjectId, subjectName = entry.subjectName,
                    date = today, status = map[student.uid] ?: AttendanceStatus.ABSENT,
                    sessionId = sessionId
                )
            }
            repo.submitAttendance(records).fold(
                onSuccess = {
                    _state.value = _state.value.copy(isLoading = false,
                        successMessage = "Attendance submitted for ${records.size} students")
                },
                onFailure = { _state.value = _state.value.copy(isLoading = false, errorMessage = it.message) }
            )
        }
    }

    fun addMaterial(title: String, fileUrl: String, fileName: String) {
        val entry = _state.value.entry ?: return
        val user = _state.value.currentUser ?: return
        viewModelScope.launch {
            repo.addMaterial(Material(title = title, subjectId = entry.subjectId,
                subjectName = entry.subjectName, sectionId = entry.sectionId,
                uploadedBy = user.uid, uploaderName = user.fullName,
                fileUrl = fileUrl, fileName = fileName)).fold(
                onSuccess = { loadClass(entry) },
                onFailure = { _state.value = _state.value.copy(errorMessage = it.message) }
            )
        }
    }

    fun deleteMaterial(id: String) {
        val entry = _state.value.entry ?: return
        viewModelScope.launch { repo.deleteMaterial(id); loadClass(entry) }
    }

    fun createAssignment(title: String, description: String, deadlineMs: Long) {
        val entry = _state.value.entry ?: return
        val user = _state.value.currentUser ?: return
        viewModelScope.launch {
            repo.createAssignment(Assignment(title = title, description = description,
                subjectId = entry.subjectId, subjectName = entry.subjectName,
                sectionId = entry.sectionId, teacherId = user.uid, deadline = deadlineMs)).fold(
                onSuccess = { loadClass(entry) },
                onFailure = { _state.value = _state.value.copy(errorMessage = it.message) }
            )
        }
    }

    fun loadSubmissions(assignment: Assignment) {
        viewModelScope.launch {
            _state.value = _state.value.copy(selectedAssignment = assignment, isLoading = true)
            val subs = repo.getSubmissionsForAssignment(assignment.id).getOrElse { emptyList() }
            _state.value = _state.value.copy(submissions = subs, isLoading = false)
        }
    }

    fun gradeSubmission(submissionId: String, grade: Double, feedback: String) {
        val entry = _state.value.entry ?: return
        viewModelScope.launch {
            repo.gradeSubmission(submissionId, grade, feedback).fold(
                onSuccess = {
                    _state.value.selectedAssignment?.let { loadSubmissions(it) }
                    _state.value = _state.value.copy(successMessage = "Grade saved")
                },
                onFailure = { _state.value = _state.value.copy(errorMessage = it.message) }
            )
        }
    }

    fun createQuiz(title: String, questions: List<QuizQuestion>, timerMode: TimerMode,
                   totalSecs: Int, perQSecs: Int) {
        val entry = _state.value.entry ?: return
        val user = _state.value.currentUser ?: return
        viewModelScope.launch {
            repo.createQuiz(Quiz(title = title, subjectId = entry.subjectId,
                subjectName = entry.subjectName, sectionId = entry.sectionId,
                teacherId = user.uid, isAvailable = false, timerMode = timerMode,
                totalTimeSeconds = totalSecs, perQuestionSeconds = perQSecs,
                questions = questions)).fold(
                onSuccess = { loadClass(entry) },
                onFailure = { _state.value = _state.value.copy(errorMessage = it.message) }
            )
        }
    }

    fun toggleQuizAvailability(quizId: String, available: Boolean) {
        val entry = _state.value.entry ?: return
        viewModelScope.launch {
            repo.setQuizAvailability(quizId, available)
            loadClass(entry)
        }
    }

    fun saveGrade(studentId: String, studentName: String, value: Double) {
        val entry = _state.value.entry ?: return
        val user = _state.value.currentUser ?: return
        viewModelScope.launch {
            repo.saveGrade(Grade(studentId = studentId, studentName = studentName,
                subjectId = entry.subjectId, subjectName = entry.subjectName,
                teacherId = user.uid, value = value), entry.sectionId).fold(
                onSuccess = {
                    loadClass(entry)
                    _state.value = _state.value.copy(successMessage = "Grade saved")
                },
                onFailure = { _state.value = _state.value.copy(errorMessage = it.message) }
            )
        }
    }

    fun clearMessages() {
        _state.value = _state.value.copy(errorMessage = null, successMessage = null)
    }

    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = ClassViewModel() as T
        }
    }
}
