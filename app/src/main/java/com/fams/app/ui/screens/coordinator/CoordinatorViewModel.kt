package com.fams.app.ui.screens.coordinator

import android.content.Context
import android.net.Uri
import androidx.compose.ui.geometry.isEmpty
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.fams.app.data.repository.AcademicRepository
import com.fams.app.data.repository.BulkImportRepository
import com.fams.app.data.repository.ScheduleDataRepository
import com.fams.app.data.repository.SectionRepository
import com.fams.app.data.repository.UserRepository
import com.fams.app.domain.model.AttendanceRecord
import com.fams.app.domain.model.Section
import com.fams.app.domain.model.Subject
import com.fams.app.domain.model.TeacherAttendanceRecord
import com.fams.app.domain.model.TeacherSubject
import com.fams.app.domain.model.User
import com.fams.app.domain.model.UserRole
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class CoordinatorUiState(
    val students: List<User> = emptyList(),
    val teachers: List<User> = emptyList(),
    val sections: List<Section> = emptyList(),
    val subjects: List<Subject> = emptyList(),
    val teacherSubjects: List<TeacherSubject> = emptyList(),
    val attendanceRecords: List<AttendanceRecord> = emptyList(),
    val teacherAttendanceRecords: List<TeacherAttendanceRecord> = emptyList(),
    val attendanceSectionId: String = "All",
    val attendanceDate: String = "",
    val isLoading: Boolean = false,
    val attendanceLoading: Boolean = false,
    val errorMessage: String? = null,
    val attendanceError: String? = null,
    val successMessage: String? = null,
    val generatedCredential: Pair<String, String>? = null,
    val importFailedRows: List<String> = emptyList(),
    val scheduleStatus: String = "Not Published",
    val pendingComplaints: Int = 0,
    val attendanceRate: Float = 0f,
    val maxStudentsPerSection: Int = 30,
    // Filter State
    val searchQuery: String = "",
    val selectedSectionId: String = "All"
)

class CoordinatorViewModel(
    private val userRepository: UserRepository = UserRepository(),
    private val sectionRepository: SectionRepository = SectionRepository(),
    private val scheduleRepository: ScheduleDataRepository = ScheduleDataRepository(),
    private val academicRepository: AcademicRepository = AcademicRepository(),
    private val bulkImportRepository: BulkImportRepository = BulkImportRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(CoordinatorUiState(attendanceDate = todayDate()))
    val uiState: StateFlow<CoordinatorUiState> = _uiState.asStateFlow()

    init { loadAll() }

    fun loadAll() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val students = userRepository.getAllStudents().getOrElse { emptyList() }
            val teachers = userRepository.getAllTeachers().getOrElse { emptyList() }
            val sections = sectionRepository.getAllSections().getOrElse { emptyList() }
            val subjects = scheduleRepository.getSubjects().getOrElse { emptyList() }
            val teacherSubjects = scheduleRepository.getTeacherSubjects().getOrElse { emptyList() }

            val scheduleStatus = try {
                val snap = com.fams.app.di.FirebaseModule.firestore
                    .collection("schedules").whereEqualTo("status", "published").get().await()
                if (snap.isEmpty) "Not Published" else "Published"
            } catch (_: Exception) { "Unknown" }

            val pendingComplaints = try {
                val snap = com.fams.app.di.FirebaseModule.firestore
                    .collection("complaints").whereEqualTo("status", "open").get().await()
                snap.size()
            } catch (_: Exception) { 0 }

            val attendanceRate = try {
                val snap = com.fams.app.di.FirebaseModule.firestore
                    .collection("attendance").get().await()
                if (snap.isEmpty) 0f else {
                    val present = snap.documents.count { it.getString("status") == "present" }
                    (present.toFloat() / snap.size()) * 100f
                }
            } catch (_: Exception) { 0f }

            val maxStudentsPerSection = try {
                val doc = com.fams.app.di.FirebaseModule.firestore
                    .collection("config").document("sections").get().await()
                (doc.getLong("maxStudentsPerSection") ?: 30L).toInt()
            } catch (_: Exception) { 30 }

            _uiState.value = _uiState.value.copy(
                students = students,
                teachers = teachers,
                sections = sections,
                subjects = subjects,
                teacherSubjects = teacherSubjects,
                scheduleStatus = scheduleStatus,
                pendingComplaints = pendingComplaints,
                attendanceRate = attendanceRate,
                maxStudentsPerSection = maxStudentsPerSection,
                isLoading = false
            )
            loadAttendanceSummary()
        }
    }

    private fun todayDate(): String = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())

    fun updateAttendanceSection(sectionId: String) {
        _uiState.value = _uiState.value.copy(attendanceSectionId = sectionId)
        loadAttendanceSummary()
    }

    fun updateAttendanceDate(date: String) {
        _uiState.value = _uiState.value.copy(attendanceDate = date)
        loadAttendanceSummary()
    }

    fun loadAttendanceSummary() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(attendanceLoading = true, attendanceError = null)
            val state = _uiState.value
            val dateFilter = state.attendanceDate.takeIf { it.isNotBlank() }
            var failureMessage: String? = null
            val studentAttendance = academicRepository.getAttendanceForSection(state.attendanceSectionId, dateFilter)
                .getOrElse {
                    failureMessage = it.message
                    emptyList()
                }
            val teacherAttendance = academicRepository.getTeacherAttendanceForSection(state.attendanceSectionId, dateFilter)
                .getOrElse {
                    failureMessage = failureMessage ?: it.message
                    emptyList()
                }
            _uiState.value = _uiState.value.copy(
                attendanceRecords = studentAttendance,
                teacherAttendanceRecords = teacherAttendance,
                attendanceLoading = false,
                attendanceError = failureMessage
            )
        }
    }

    // ── Search & Filter Logic ────────────────────────────────────────────────

    fun updateSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
    }

    fun updateSelectedSection(sectionId: String) {
        _uiState.value = _uiState.value.copy(selectedSectionId = sectionId)
    }

    val filteredStudents: List<User>
        get() {
            val state = _uiState.value
            return state.students.filter { student ->
                val matchesSearch = student.fullName.contains(state.searchQuery, ignoreCase = true) ||
                        (student.studentId?.contains(state.searchQuery, ignoreCase = true) == true)

                val matchesSection = if (state.selectedSectionId == "All") true
                else student.sectionId == state.selectedSectionId

                matchesSearch && matchesSection
            }.sortedBy { it.fullName }
        }

    // ── Add Users ─────────────────────────────────────────────────────────────

    fun addStudent(fullName: String, email: String, phone: String, studentId: String, sectionId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            userRepository.createUser(
                fullName = fullName, email = email, phone = phone,
                role = UserRole.STUDENT, studentId = studentId, sectionId = sectionId
            ).fold(
                onSuccess = { password ->
                    if (sectionId.isNotBlank()) sectionRepository.incrementStudentCount(sectionId)
                    loadAll()
                    _uiState.value = _uiState.value.copy(generatedCredential = Pair(email, password))
                },
                onFailure = {
                    _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = it.message)
                }
            )
        }
    }

    fun addTeacher(fullName: String, email: String, phone: String, teacherId: String, specialization: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            userRepository.createUser(
                fullName = fullName, email = email, phone = phone,
                role = UserRole.TEACHER, teacherId = teacherId, specialization = specialization
            ).fold(
                onSuccess = { password ->
                    loadAll()
                    _uiState.value = _uiState.value.copy(generatedCredential = Pair(email, password))
                },
                onFailure = {
                    _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = it.message)
                }
            )
        }
    }

    // ── Sections ──────────────────────────────────────────────────────────────

    fun addSection(name: String, maxCapacity: Int) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            sectionRepository.createSection(name, maxCapacity).fold(
                onSuccess = {
                    loadAll()
                    _uiState.value = _uiState.value.copy(successMessage = "Section '$name' created")
                },
                onFailure = {
                    _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = it.message)
                }
            )
        }
    }

    fun autoGenerateSections(count: Int, maxCapacity: Int) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            var created = 0
            for (i in 0 until count) {
                val name = "Section ${'A' + i}"
                if (sectionRepository.createSection(name, maxCapacity).isSuccess) created++
            }
            loadAll()
            _uiState.value = _uiState.value.copy(successMessage = "$created sections generated")
        }
    }

    fun deleteSection(id: String) {
        viewModelScope.launch { sectionRepository.deleteSection(id); loadAll() }
    }

    fun deleteSections(ids: List<String>) {
        viewModelScope.launch {
            ids.forEach { sectionRepository.deleteSection(it) }
            loadAll()
            _uiState.value = _uiState.value.copy(successMessage = "${ids.size} sections deleted")
        }
    }

    fun setMaxStudentsPerSection(max: Int) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(maxStudentsPerSection = max)
            try {
                com.fams.app.di.FirebaseModule.firestore
                    .collection("config").document("sections")
                    .set(mapOf("maxStudentsPerSection" to max)).await()
            } catch (_: Exception) {}
        }
    }

    fun assignTeacherToSubject(teacherId: String, subjectId: String) {
        viewModelScope.launch {
            if (_uiState.value.teacherSubjects.any { it.teacherId == teacherId && it.subjectId == subjectId }) return@launch
            scheduleRepository.assignTeacherSubject(teacherId, subjectId).fold(
                onSuccess = {
                    loadAll()
                    _uiState.value = _uiState.value.copy(successMessage = "Teacher assigned to subject")
                },
                onFailure = { _uiState.value = _uiState.value.copy(errorMessage = it.message) }
            )
        }
    }

    fun autoAssignStudentsToSections() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            val unassigned = _uiState.value.students
                .filter { it.sectionId.isBlank() }
                .sortedBy { it.fullName.lowercase() }

            if (unassigned.isEmpty()) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    successMessage = "All students already have sections assigned."
                )
                return@launch
            }

            val sections = sectionRepository.getAllSections()
                .getOrElse { emptyList() }.sortedBy { it.name }

            if (sections.isEmpty()) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "No sections found. Create sections first."
                )
                return@launch
            }

            val sectionFill = sections.associate { it.id to it.studentCount }.toMutableMap()
            var assigned = 0

            for (student in unassigned) {
                val target = sections.firstOrNull { (sectionFill[it.id] ?: 0) < it.maxCapacity } ?: break
                userRepository.moveStudentToSection(student.uid, target.id)
                sectionFill[target.id] = (sectionFill[target.id] ?: 0) + 1
                sectionRepository.incrementStudentCount(target.id)
                assigned++
            }

            loadAll()
            _uiState.value = _uiState.value.copy(
                successMessage = "$assigned students assigned to sections alphabetically."
            )
        }
    }

    // ── Student Management ────────────────────────────────────────────────────

    fun deleteUsers(uids: List<String>) {
        viewModelScope.launch {
            uids.forEach { uid ->
                try {
                    com.fams.app.di.FirebaseModule.firestore
                        .collection("users").document(uid).delete().await()
                } catch (_: Exception) {}
            }
            loadAll()
            _uiState.value = _uiState.value.copy(successMessage = "${uids.size} users deleted")
        }
    }

    fun promoteToClassRep(uid: String) {
        viewModelScope.launch {
            userRepository.promoteToClassRep(uid)
            loadAll()
            _uiState.value = _uiState.value.copy(successMessage = "Student promoted to Class Rep")
        }
    }

    fun demoteToStudent(uid: String) {
        viewModelScope.launch {
            userRepository.demoteToStudent(uid)
            loadAll()
            _uiState.value = _uiState.value.copy(successMessage = "Class Rep demoted to Student")
        }
    }

    fun moveStudentToSection(uid: String, sectionId: String) {
        viewModelScope.launch {
            userRepository.moveStudentToSection(uid, sectionId)
            loadAll()
            _uiState.value = _uiState.value.copy(successMessage = "Student moved to new section")
        }
    }

    // ── Bulk Import ───────────────────────────────────────────────────────────

    fun bulkImportStudents(context: Context, uri: Uri, coordinatorEmail: String, coordinatorPassword: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            val result = bulkImportRepository.importStudents(context, uri, coordinatorEmail, coordinatorPassword)
            delay(1500)
            loadAll()
            val msg = buildString {
                append("${result.successCount} students imported.")
                if (result.failedRows.isNotEmpty()) append(" ${result.failedRows.size} failed.")
            }
            _uiState.value = _uiState.value.copy(successMessage = msg, importFailedRows = result.failedRows)
        }
    }

    fun bulkImportTeachers(context: Context, uri: Uri, coordinatorEmail: String, coordinatorPassword: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            val result = bulkImportRepository.importTeachers(context, uri, coordinatorEmail, coordinatorPassword)
            delay(1500)
            loadAll()
            val msg = buildString {
                append("${result.successCount} teachers imported.")
                if (result.failedRows.isNotEmpty()) append(" ${result.failedRows.size} failed.")
            }
            _uiState.value = _uiState.value.copy(successMessage = msg, importFailedRows = result.failedRows)
        }
    }

    fun clearImportErrors() {
        _uiState.value = _uiState.value.copy(importFailedRows = emptyList())
    }

    // ── Messages ──────────────────────────────────────────────────────────────

    fun clearMessages() {
        _uiState.value = _uiState.value.copy(
            errorMessage = null,
            successMessage = null,
            generatedCredential = null
        )
    }

    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                CoordinatorViewModel() as T
        }
    }
}