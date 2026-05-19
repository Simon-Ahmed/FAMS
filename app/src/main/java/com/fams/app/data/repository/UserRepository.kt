package com.fams.app.data.repository

import com.fams.app.di.FirebaseModule
import com.fams.app.domain.model.User
import com.fams.app.domain.model.UserRole
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class UserRepository(
    private val auth: FirebaseAuth = FirebaseModule.auth,
    private val firestore: FirebaseFirestore = FirebaseModule.firestore
) {

    suspend fun getAllStudents(): Result<List<User>> {
        return try {
            val snapshot = firestore.collection("users").get().await()
            val users = snapshot.documents.mapNotNull { mapDocToUser(it.id, it.data) }
                .filter { it.role == UserRole.STUDENT || it.role == UserRole.CLASS_REP }
            Result.success(users.sortedBy { it.fullName })
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun getAllTeachers(): Result<List<User>> {
        return try {
            val snapshot = firestore.collection("users").get().await()
            val users = snapshot.documents.mapNotNull { mapDocToUser(it.id, it.data) }
                .filter { it.role == UserRole.TEACHER }
            Result.success(users.sortedBy { it.fullName })
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun createUser(
        fullName: String,
        email: String,
        phone: String,
        role: UserRole,
        studentId: String = "",
        teacherId: String = "",
        specialization: String = "",
        sectionId: String = "",
        coordinatorEmail: String = "",
        coordinatorPassword: String = ""
    ): Result<String> {
        val password = generatePassword(fullName)
        return try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            val uid = result.user?.uid
                ?: return Result.failure(Exception("Failed to create account"))

            // Save profile including the generated password so coordinator can view it
            val userMap = mapOf(
                "uid" to uid,
                "fullName" to fullName,
                "email" to email,
                "phone" to phone,
                "role" to role.value,
                "studentId" to studentId,
                "teacherId" to teacherId,
                "specialization" to specialization,
                "sectionId" to sectionId,
                "generatedPassword" to password
            )
            firestore.collection("users").document(uid).set(userMap).await()

            // Sign out new user, re-sign in as coordinator
            auth.signOut()
            if (coordinatorEmail.isNotBlank() && coordinatorPassword.isNotBlank()) {
                try {
                    auth.signInWithEmailAndPassword(coordinatorEmail, coordinatorPassword).await()
                } catch (_: Exception) {}
            }
            Result.success(password)
        } catch (e: Exception) {
            if (coordinatorEmail.isNotBlank() && coordinatorPassword.isNotBlank()) {
                try { auth.signInWithEmailAndPassword(coordinatorEmail, coordinatorPassword).await() }
                catch (_: Exception) {}
            }
            Result.failure(e)
        }
    }

    suspend fun promoteToClassRep(uid: String): Result<Unit> {
        return try {
            firestore.collection("users").document(uid)
                .update("role", UserRole.CLASS_REP.value).await()
            Result.success(Unit)
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun demoteToStudent(uid: String): Result<Unit> {
        return try {
            firestore.collection("users").document(uid)
                .update("role", UserRole.STUDENT.value).await()
            Result.success(Unit)
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun moveStudentToSection(uid: String, sectionId: String): Result<Unit> {
        return try {
            firestore.collection("users").document(uid)
                .update("sectionId", sectionId).await()
            Result.success(Unit)
        } catch (e: Exception) { Result.failure(e) }
    }

    fun generatePassword(fullName: String): String {
        val namePart = fullName.replace(" ", "").take(4).lowercase()
        val number = (1000..9999).random()
        return "${namePart}@${number}"
    }

    fun mapDocToUser(id: String, data: Map<String, Any>?): User? {
        data ?: return null
        return User(
            uid = id,
            fullName = data["fullName"] as? String ?: "",
            email = data["email"] as? String ?: "",
            phone = data["phone"] as? String ?: "",
            role = when (data["role"] as? String) {
                "coordinator" -> UserRole.COORDINATOR
                "teacher" -> UserRole.TEACHER
                "class_rep" -> UserRole.CLASS_REP
                else -> UserRole.STUDENT
            },
            sectionId = data["sectionId"] as? String ?: "",
            specialization = data["specialization"] as? String ?: "",
            studentId = data["studentId"] as? String ?: "",
            teacherId = data["teacherId"] as? String ?: "",
            generatedPassword = data["generatedPassword"] as? String ?: ""
        )
    }
}
