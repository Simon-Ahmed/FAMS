package com.fams.app.ui.screens.coordinator

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.fams.app.data.repository.ScheduleConfigRepository
import com.fams.app.data.repository.ScheduleDataRepository
import com.fams.app.data.repository.SectionRepository
import com.fams.app.data.repository.UserRepository
import com.fams.app.domain.model.*
import com.fams.app.domain.scheduler.SchedulingEngine
import com.fams.app.domain.scheduler.SchedulingInput
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.ceil

data class ScheduleUiState(
    val subjects: List<Subject> = emptyList(),
    val rooms: List<Room> = emptyList(),
    val timeSlots: List<TimeSlot> = emptyList(),
    val teachers: List<User> = emptyList(),
    val sections: List<Section> = emptyList(),
    val teacherSubjects: List<TeacherSubject> = emptyList(),
    val unavailabilities: List<TeacherUnavailability> = emptyList(),
    val generatedEntries: List<ScheduleEntry> = emptyList(),
    val unscheduledWarnings: List<String> = emptyList(),
    val publishedEntries: List<ScheduleEntry> = emptyList(),
    val config: ScheduleConfig = ScheduleConfig(),
    val requirements: List<SubjectRequirement> = emptyList(),
    val maxClassesPerDay: Int = 4,
    val isLoading: Boolean = false,
    val isGenerating: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

class ScheduleViewModel(
    private val repo: ScheduleDataRepository = ScheduleDataRepository(),
    private val userRepo: UserRepository = UserRepository(),
    private val sectionRepo: SectionRepository = SectionRepository(),
    private val configRepo: ScheduleConfigRepository = ScheduleConfigRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(ScheduleUiState())
    val uiState: StateFlow<ScheduleUiState> = _uiState.asStateFlow()

    init { loadAll() }

    fun loadAll() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val subjects = repo.getSubjects().getOrElse { emptyList() }
            val rooms = repo.getRooms().getOrElse { emptyList() }
            val timeSlots = repo.getTimeSlots().getOrElse { emptyList() }
            val teachers = userRepo.getAllTeachers().getOrElse { emptyList() }
            val sections = sectionRepo.getAllSections().getOrElse { emptyList() }
            val teacherSubjects = repo.getTeacherSubjects().getOrElse { emptyList() }
            val unavailabilities = repo.getTeacherUnavailabilities().getOrElse { emptyList() }
            val published = repo.getPublishedSchedule().getOrElse { emptyList() }
            val config = configRepo.getConfig().getOrElse { ScheduleConfig() }

            // Compute teacher requirements for each subject
            val requirements = computeRequirements(
                subjects, sections, teacherSubjects, config.maxTeacherCreditHoursPerWeek
            )

            _uiState.value = _uiState.value.copy(
                subjects = subjects, rooms = rooms, timeSlots = timeSlots,
                teachers = teachers, sections = sections,
                teacherSubjects = teacherSubjects, unavailabilities = unavailabilities,
                publishedEntries = published, config = config,
                requirements = requirements, isLoading = false
            )
        }
    }

    /**
     * For each subject, calculates:
     * - How many sections one teacher can cover = floor(maxTeacherHours / subject.creditHours)
     * - Minimum teachers needed = ceil(sectionCount / sectionsPerTeacher)
     * - How many are currently assigned
     */
    private fun computeRequirements(
        subjects: List<Subject>,
        sections: List<Section>,
        teacherSubjects: List<TeacherSubject>,
        maxTeacherHours: Int
    ): List<SubjectRequirement> {
        val sectionCount = sections.size
        return subjects.map { subject ->
            val sectionsPerTeacher = if (subject.creditHours > 0)
                (maxTeacherHours / subject.creditHours).coerceAtLeast(1) else 1
            val minNeeded = ceil(sectionCount.toDouble() / sectionsPerTeacher).toInt()
            val assigned = teacherSubjects.count { it.subjectId == subject.id }
            SubjectRequirement(
                subject = subject,
                sectionCount = sectionCount,
                sectionsPerTeacher = sectionsPerTeacher,
                minTeachersNeeded = minNeeded,
                assignedTeacherCount = assigned,
                isSufficient = assigned >= minNeeded
            )
        }
    }

    // ── Config ────────────────────────────────────────────────────────────────

    fun saveConfig(maxStudentsPerSection: Int, maxTeacherHours: Int) {
        viewModelScope.launch {
            val config = ScheduleConfig(maxStudentsPerSection, maxTeacherHours)
            configRepo.saveConfig(config).fold(
                onSuccess = {
                    loadAll()
                    _uiState.value = _uiState.value.copy(successMessage = "Configuration saved")
                },
                onFailure = { _uiState.value = _uiState.value.copy(errorMessage = it.message) }
            )
        }
    }

    // ── Auto-create sections from student count ───────────────────────────────

    fun autoCreateSectionsFromStudents(totalStudents: Int) {
        viewModelScope.launch {
            val maxPerSection = _uiState.value.config.maxStudentsPerSection
            if (maxPerSection <= 0) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Set max students per section in Config first.")
                return@launch
            }
            val sectionsNeeded = ceil(totalStudents.toDouble() / maxPerSection).toInt()
            _uiState.value = _uiState.value.copy(isLoading = true)
            var created = 0
            for (i in 0 until sectionsNeeded) {
                val name = "Section ${'A' + i}"
                val result = sectionRepo.createSection(name, maxPerSection)
                if (result.isSuccess) created++
            }
            loadAll()
            _uiState.value = _uiState.value.copy(
                successMessage = "$created sections created (Section A–${'A' + sectionsNeeded - 1})"
            )
        }
    }

    // ── Subjects ──────────────────────────────────────────────────────────────

    fun addSubject(name: String, description: String, creditHours: Int) {
        viewModelScope.launch {
            repo.addSubject(name, description, creditHours).fold(
                onSuccess = { loadAll() },
                onFailure = { _uiState.value = _uiState.value.copy(errorMessage = it.message) }
            )
        }
    }

    fun deleteSubject(id: String) {
        viewModelScope.launch { repo.deleteSubject(id); loadAll() }
    }

    fun deleteSubjects(ids: List<String>) {
        viewModelScope.launch {
            ids.forEach { repo.deleteSubject(it) }
            loadAll()
            _uiState.value = _uiState.value.copy(successMessage = "${ids.size} subjects deleted")
        }
    }

    // ── Rooms ─────────────────────────────────────────────────────────────────

    fun addRoom(name: String, capacity: Int) {
        viewModelScope.launch {
            repo.addRoom(name, capacity).fold(
                onSuccess = { loadAll() },
                onFailure = { _uiState.value = _uiState.value.copy(errorMessage = it.message) }
            )
        }
    }

    fun deleteRoom(id: String) {
        viewModelScope.launch { repo.deleteRoom(id); loadAll() }
    }

    fun deleteRooms(ids: List<String>) {
        viewModelScope.launch {
            ids.forEach { repo.deleteRoom(it) }
            loadAll()
            _uiState.value = _uiState.value.copy(successMessage = "${ids.size} rooms deleted")
        }
    }

    // ── Time Slots ────────────────────────────────────────────────────────────

    fun addTimeSlot(day: String, startTime: String, endTime: String) {
        viewModelScope.launch {
            repo.addTimeSlot(day, startTime, endTime).fold(
                onSuccess = { loadAll() },
                onFailure = { _uiState.value = _uiState.value.copy(errorMessage = it.message) }
            )
        }
    }

    fun autoGenerateTimeSlots(days: List<String>, startHour: Int, endHour: Int, durationMinutes: Int) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            var created = 0
            for (day in days) {
                var currentMinutes = startHour * 60
                val endMinutes = endHour * 60
                while (currentMinutes + durationMinutes <= endMinutes) {
                    val startStr = "%02d:%02d".format(currentMinutes / 60, currentMinutes % 60)
                    val endMin = currentMinutes + durationMinutes
                    val endStr = "%02d:%02d".format(endMin / 60, endMin % 60)
                    val result = repo.addTimeSlot(day, startStr, endStr)
                    if (result.isSuccess) created++
                    currentMinutes += durationMinutes
                }
            }
            loadAll()
            _uiState.value = _uiState.value.copy(successMessage = "$created time slots generated")
        }
    }

    fun deleteTimeSlot(id: String) {
        viewModelScope.launch { repo.deleteTimeSlot(id); loadAll() }
    }

    fun deleteTimeSlots(ids: List<String>) {
        viewModelScope.launch {
            ids.forEach { repo.deleteTimeSlot(it) }
            loadAll()
            _uiState.value = _uiState.value.copy(successMessage = "${ids.size} time slots deleted")
        }
    }

    // ── Unavailability ────────────────────────────────────────────────────────

    fun addUnavailability(teacherId: String, timeSlotId: String) {
        viewModelScope.launch { repo.addUnavailability(teacherId, timeSlotId); loadAll() }
    }

    fun removeUnavailability(id: String) {
        viewModelScope.launch { repo.removeUnavailability(id); loadAll() }
    }

    fun removeUnavailabilities(ids: List<String>) {
        viewModelScope.launch {
            ids.forEach { repo.removeUnavailability(it) }
            loadAll()
            _uiState.value = _uiState.value.copy(successMessage = "${ids.size} unavailabilities removed")
        }
    }

    // ── Teacher-Subject Assignment ────────────────────────────────────────────

    fun assignTeacherToSubject(teacherId: String, subjectId: String) {
        viewModelScope.launch {
            val exists = _uiState.value.teacherSubjects.any {
                it.teacherId == teacherId && it.subjectId == subjectId
            }
            if (exists) return@launch
            repo.assignTeacherSubject(teacherId, subjectId).fold(
                onSuccess = { loadAll() },
                onFailure = { _uiState.value = _uiState.value.copy(errorMessage = it.message) }
            )
        }
    }

    fun removeTeacherSubject(id: String) {
        viewModelScope.launch { repo.removeTeacherSubject(id); loadAll() }
    }

    fun removeTeacherSubjects(ids: List<String>) {
        viewModelScope.launch {
            ids.forEach { repo.removeTeacherSubject(it) }
            loadAll()
            _uiState.value = _uiState.value.copy(successMessage = "${ids.size} assignments removed")
        }
    }

    // ── Constraints ───────────────────────────────────────────────────────────

    fun setMaxClassesPerDay(value: Int) {
        _uiState.value = _uiState.value.copy(maxClassesPerDay = value)
    }

    // ── Generate Schedule ─────────────────────────────────────────────────────

    fun generateSchedule() {
        val state = _uiState.value
        // Validate requirements
        val insufficientSubjects = state.requirements.filter { !it.isSufficient }
        if (insufficientSubjects.isNotEmpty()) {
            val msg = insufficientSubjects.joinToString("\n") {
                "${it.subject.name}: needs ${it.minTeachersNeeded} teachers, has ${it.assignedTeacherCount}"
            }
            _uiState.value = state.copy(
                errorMessage = "Insufficient teachers:\n$msg"
            )
            return
        }
        if (state.sections.isEmpty()) {
            _uiState.value = state.copy(errorMessage = "No sections found. Create sections first.")
            return
        }
        if (state.rooms.isEmpty()) {
            _uiState.value = state.copy(errorMessage = "Add at least one room first.")
            return
        }
        if (state.timeSlots.isEmpty()) {
            _uiState.value = state.copy(errorMessage = "Add time slots first.")
            return
        }

        viewModelScope.launch {
            _uiState.value = state.copy(isGenerating = true, errorMessage = null)
            val input = SchedulingInput(
                sections = state.sections,
                subjects = state.subjects,
                teachers = state.teachers,
                rooms = state.rooms,
                timeSlots = state.timeSlots,
                teacherSubjects = state.teacherSubjects,
                unavailabilities = state.unavailabilities,
                maxClassesPerDay = state.maxClassesPerDay,
                maxTeacherCreditHoursPerWeek = state.config.maxTeacherCreditHoursPerWeek
            )
            val result = SchedulingEngine.generate(input)
            _uiState.value = _uiState.value.copy(
                isGenerating = false,
                generatedEntries = result.entries,
                unscheduledWarnings = result.unscheduled,
                successMessage = if (result.success)
                    "Schedule generated: ${result.entries.size} sessions"
                else
                    "Partial schedule: ${result.unscheduled.size} items unscheduled"
            )
        }
    }

    // ── Publish ───────────────────────────────────────────────────────────────

    fun publishSchedule() {
        val entries = _uiState.value.generatedEntries
        if (entries.isEmpty()) {
            _uiState.value = _uiState.value.copy(errorMessage = "Generate a schedule first.")
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            repo.saveSchedule(entries, ScheduleStatus.PUBLISHED).fold(
                onSuccess = {
                    loadAll()
                    _uiState.value = _uiState.value.copy(
                        generatedEntries = emptyList(),
                        successMessage = "Schedule published!"
                    )
                },
                onFailure = {
                    _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = it.message)
                }
            )
        }
    }

    fun deletePublishedSchedule() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            repo.deletePublishedSchedule().fold(
                onSuccess = {
                    loadAll()
                    _uiState.value = _uiState.value.copy(successMessage = "Published schedule deleted")
                },
                onFailure = {
                    _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = it.message)
                }
            )
        }
    }

    fun clearMessages() {
        _uiState.value = _uiState.value.copy(errorMessage = null, successMessage = null)
    }

    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = ScheduleViewModel() as T
        }
    }
}
