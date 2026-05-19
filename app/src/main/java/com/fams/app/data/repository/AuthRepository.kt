package com.fams.app.data.repository

import com.fams.app.di.FirebaseModule
import com.fams.app.domain.model.User
import com.fams.app.domain.model.UserRole
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class AuthRepository(
    private val auth: FirebaseAuth = FirebaseModule.auth,
    private val firestore: FirebaseFirestore = FirebaseModule.firestore
) {

    suspend fun getCurrentUser(): User? {
        val firebaseUser = auth.currentUser ?: return null
        return fetchUserProfile(firebaseUser.uid)
    }

    suspend fun signIn(email: String, password: String): Result<User> {
        return try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            val uid = result.user?.uid ?: return Result.failure(Exception("Authentication failed"))
            val user = fetchUserProfile(uid)
                ?: return Result.failure(Exception("User profile not found. Contact your coordinator."))
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(mapAuthException(e))
        }
    }

    fun signOut() = auth.signOut()

    private suspend fun fetchUserProfile(uid: String): User? {
        val doc = firestore.collection("users").document(uid).get().await()
        if (!doc.exists()) return null
        return User(
            uid = uid,
            fullName = doc.getString("fullName") ?: "",
            email = doc.getString("email") ?: "",
            phone = doc.getString("phone") ?: "",
            role = when (doc.getString("role")) {
                "coordinator" -> UserRole.COORDINATOR
                "teacher" -> UserRole.TEACHER
                "class_rep" -> UserRole.CLASS_REP
                else -> UserRole.STUDENT
            },
            sectionId = doc.getString("sectionId") ?: "",
            specialization = doc.getString("specialization") ?: "",
            studentId = doc.getString("studentId") ?: "",
            teacherId = doc.getString("teacherId") ?: ""
        )
    }

    private fun mapAuthException(e: Exception): Exception {
        val message = e.message ?: ""
        return when {
            "INVALID_EMAIL" in message -> Exception("Invalid email address format.")
            "WRONG_PASSWORD" in message ||
            "INVALID_CREDENTIAL" in message -> Exception("Incorrect email or password.")
            "USER_NOT_FOUND" in message -> Exception("No account found with this email.")
            "USER_DISABLED" in message -> Exception("This account has been disabled.")
            "NETWORK_ERROR" in message ||
            "network" in message.lowercase() -> Exception("Network error. Check your connection.")
            "TOO_MANY_REQUESTS" in message -> Exception("Too many attempts. Please try again later.")
            else -> Exception("Login failed: ${e.message}")
        }
    }
}
