package com.fams.app.data.repository

import android.content.Context
import android.net.Uri
import com.fams.app.domain.model.Section
import com.fams.app.domain.model.UserRole
import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.ss.usermodel.WorkbookFactory

data class ImportRow(
    val fullName: String,
    val email: String,
    val phone: String,
    val studentId: String = "",
    val teacherId: String = "",
    val specialization: String = ""
)

data class ImportResult(
    val successCount: Int,
    val failedRows: List<String>
)

class BulkImportRepository(
    private val userRepository: UserRepository = UserRepository()
) {

    /**
     * Imports students from Excel, sorts them alphabetically by full name,
     * and auto-assigns them to sections in order (A fills first, then B, etc.)
     */
    suspend fun importStudents(
        context: Context,
        uri: Uri,
        coordinatorEmail: String,
        coordinatorPassword: String
    ): ImportResult {
        val rows = parseExcel(context, uri, isStudent = true)
            .filter { it.fullName.isNotBlank() && it.email.isNotBlank() }
            .sortedBy { it.fullName.lowercase() }

        var success = 0
        val failed = mutableListOf<String>()

        rows.forEach { row ->
            // Import with NO section — coordinator assigns sections separately
            val result = userRepository.createUser(
                fullName = row.fullName,
                email = row.email,
                phone = row.phone,
                role = UserRole.STUDENT,
                studentId = row.studentId,
                sectionId = "",
                coordinatorEmail = coordinatorEmail,
                coordinatorPassword = coordinatorPassword
            )
            if (result.isSuccess) success++
            else failed.add("${row.fullName} (${row.email}): ${result.exceptionOrNull()?.message}")
        }
        return ImportResult(success, failed)
    }

    suspend fun importTeachers(
        context: Context,
        uri: Uri,
        coordinatorEmail: String,
        coordinatorPassword: String
    ): ImportResult {
        val rows = parseExcel(context, uri, isStudent = false)
        var success = 0
        val failed = mutableListOf<String>()

        rows.forEach { row ->
            if (row.fullName.isBlank() || row.email.isBlank()) {
                failed.add("Row skipped: missing name or email")
                return@forEach
            }
            val result = userRepository.createUser(
                fullName = row.fullName,
                email = row.email,
                phone = row.phone,
                role = UserRole.TEACHER,
                teacherId = row.teacherId,
                specialization = row.specialization,
                coordinatorEmail = coordinatorEmail,
                coordinatorPassword = coordinatorPassword
            )
            if (result.isSuccess) success++
            else failed.add("${row.fullName} (${row.email}): ${result.exceptionOrNull()?.message}")
        }
        return ImportResult(success, failed)
    }

    private fun parseExcel(context: Context, uri: Uri, isStudent: Boolean): List<ImportRow> {
        val rows = mutableListOf<ImportRow>()
        try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return rows
            val workbook = WorkbookFactory.create(inputStream)
            val sheet = workbook.getSheetAt(0)

            for (rowIndex in 1..sheet.lastRowNum) {
                val row = sheet.getRow(rowIndex) ?: continue
                fun cell(i: Int) = row.getCell(i)?.let {
                    when (it.cellType) {
                        CellType.STRING -> it.stringCellValue.trim()
                        CellType.NUMERIC -> it.numericCellValue.toLong().toString()
                        else -> ""
                    }
                } ?: ""

                if (isStudent) {
                    rows.add(ImportRow(
                        studentId = cell(0),
                        fullName = cell(1),
                        email = cell(2),
                        phone = cell(3)
                    ))
                } else {
                    rows.add(ImportRow(
                        teacherId = cell(0),
                        fullName = cell(1),
                        email = cell(2),
                        phone = cell(3),
                        specialization = cell(4)
                    ))
                }
            }
            workbook.close()
            inputStream.close()
        } catch (_: Exception) {}
        return rows
    }
}
