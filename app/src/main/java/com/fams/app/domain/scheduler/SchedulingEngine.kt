package com.fams.app.domain.scheduler

import com.fams.app.domain.model.*

/**
 * Constraint-based greedy scheduler with conflict detection.
 *
 * Input:
 *   - sections, subjects, teachers, rooms, timeSlots
 *   - teacherSubjects: which teacher can teach which subject
 *   - unavailabilities: blocked slots per teacher
 *   - maxClassesPerDay: max sessions a section can have in one day
 *   - maxConsecutivePerTeacher: max back-to-back sessions for a teacher
 *
 * Output:
 *   - List<ScheduleEntry> or SchedulingResult with conflicts listed
 */

data class SchedulingInput(
    val sections: List<Section>,
    val subjects: List<Subject>,
    val teachers: List<User>,
    val rooms: List<Room>,
    val timeSlots: List<TimeSlot>,
    val teacherSubjects: List<TeacherSubject>,
    val unavailabilities: List<TeacherUnavailability>,
    val maxClassesPerDay: Int = 4,
    val maxConsecutivePerTeacher: Int = 3,
    val maxTeacherCreditHoursPerWeek: Int = 12
)

data class SchedulingResult(
    val entries: List<ScheduleEntry>,
    val unscheduled: List<String>,  // descriptions of what couldn't be scheduled
    val success: Boolean
)

object SchedulingEngine {

    fun generate(input: SchedulingInput): SchedulingResult {
        val entries = mutableListOf<ScheduleEntry>()
        val unscheduled = mutableListOf<String>()

        // Tracking sets for conflict detection
        val teacherSlotUsed = mutableSetOf<String>()
        val roomSlotUsed = mutableSetOf<String>()
        val sectionSlotUsed = mutableSetOf<String>()
        val sectionDayCount = mutableMapOf<String, Int>()
        // Track total credit hours assigned per teacher across all subjects
        val teacherCreditHoursUsed = mutableMapOf<String, Int>()

        // Build lookup maps
        val subjectTeachers = buildSubjectTeacherMap(input)
        val unavailableSet = input.unavailabilities
            .map { "${it.teacherId}_${it.timeSlotId}" }.toSet()

        // Sort time slots by day then start time for balanced distribution
        val sortedSlots = input.timeSlots.sortedWith(
            Comparator { a, b ->
                val dayCompare = dayIndex(a.day).compareTo(dayIndex(b.day))
                if (dayCompare != 0) dayCompare else a.startTime.compareTo(b.startTime)
            }
        )

        // For each section, schedule all required subject sessions
        for (section in input.sections.sortedBy { it.name }) {
            for (subject in input.subjects) {
                val sessionsNeeded = subject.creditHours
                var sessionsScheduled = 0

                // Shuffle slots to distribute across the week
                val shuffledSlots = sortedSlots.shuffled()

                for (slot in shuffledSlots) {
                    if (sessionsScheduled >= sessionsNeeded) break

                    val sectionSlotKey = "${section.id}_${slot.id}"
                    val sectionDayKey = "${section.id}_${slot.day}"

                    // Check section not already busy in this slot
                    if (sectionSlotKey in sectionSlotUsed) continue

                    // Check section hasn't exceeded max classes per day
                    val dayCount = sectionDayCount[sectionDayKey] ?: 0
                    if (dayCount >= input.maxClassesPerDay) continue

                    // Find an available teacher for this subject in this slot
                    val availableTeachers = subjectTeachers[subject.id] ?: continue
                    val teacher = availableTeachers.firstOrNull { t ->
                        val teacherSlotKey = "${t.uid}_${slot.id}"
                        val unavailKey = "${t.uid}_${slot.id}"
                        // Check slot not used, not unavailable, and credit hours not exceeded
                        val usedHours = teacherCreditHoursUsed[t.uid] ?: 0
                        teacherSlotKey !in teacherSlotUsed &&
                        unavailKey !in unavailableSet &&
                        (usedHours + subject.creditHours) <= input.maxTeacherCreditHoursPerWeek
                    } ?: continue

                    // Find an available room in this slot
                    val room = input.rooms.firstOrNull { r ->
                        val roomSlotKey = "${r.id}_${slot.id}"
                        roomSlotKey !in roomSlotUsed &&
                        r.capacity >= (section.maxCapacity)
                    } ?: input.rooms.firstOrNull { r ->
                        // Fallback: any available room
                        "${r.id}_${slot.id}" !in roomSlotUsed
                    } ?: continue

                    // All constraints satisfied — create entry
                    val entry = ScheduleEntry(
                        id = "",
                        scheduleId = "",
                        sectionId = section.id,
                        subjectId = subject.id,
                        teacherId = teacher.uid,
                        roomId = room.id,
                        timeSlotId = slot.id,
                        sectionName = section.name,
                        subjectName = subject.name,
                        teacherName = teacher.fullName,
                        roomName = room.name,
                        day = slot.day,
                        startTime = slot.startTime,
                        endTime = slot.endTime
                    )
                    entries.add(entry)

                    // Mark resources as used
                    teacherSlotUsed.add("${teacher.uid}_${slot.id}")
                    roomSlotUsed.add("${room.id}_${slot.id}")
                    sectionSlotUsed.add(sectionSlotKey)
                    sectionDayCount[sectionDayKey] = dayCount + 1
                    // Accumulate credit hours for this teacher
                    teacherCreditHoursUsed[teacher.uid] =
                        (teacherCreditHoursUsed[teacher.uid] ?: 0) + subject.creditHours
                    sessionsScheduled++
                }

                if (sessionsScheduled < sessionsNeeded) {
                    unscheduled.add(
                        "${section.name} — ${subject.name}: " +
                        "scheduled $sessionsScheduled/$sessionsNeeded sessions"
                    )
                }
            }
        }

        return SchedulingResult(
            entries = entries,
            unscheduled = unscheduled,
            success = unscheduled.isEmpty()
        )
    }

    /** Builds a map of subjectId -> list of teachers who can teach it */
    private fun buildSubjectTeacherMap(input: SchedulingInput): Map<String, List<User>> {
        val map = mutableMapOf<String, MutableList<User>>()
        val teacherById = input.teachers.associateBy { it.uid }
        for (ts in input.teacherSubjects) {
            val teacher = teacherById[ts.teacherId] ?: continue
            map.getOrPut(ts.subjectId) { mutableListOf() }.add(teacher)
        }
        return map
    }

    private fun dayIndex(day: String) = when (day) {
        "Monday" -> 0; "Tuesday" -> 1; "Wednesday" -> 2
        "Thursday" -> 3; "Friday" -> 4; "Saturday" -> 5
        else -> 6
    }
}
