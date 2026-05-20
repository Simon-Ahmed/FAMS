package com.fams.app.data.repository

import com.fams.app.domain.model.Note
import com.fams.app.di.FirebaseModule
import kotlinx.coroutines.tasks.await

class NotesRepository {
    private val db = FirebaseModule.firestore

    suspend fun getNotes(userId: String): Result<List<Note>> {
        return try {
            val snap = db.collection("notes").whereEqualTo("userId", userId).get().await()
            Result.success(snap.documents.map { doc ->
                Note(id = doc.id,
                    userId = doc.getString("userId") ?: "",
                    title = doc.getString("title") ?: "",
                    body = doc.getString("body") ?: "",
                    createdAt = doc.getLong("createdAt") ?: 0L)
            }.sortedByDescending { it.createdAt })
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun addNote(note: Note): Result<String> {
        return try {
            val ref = db.collection("notes").add(mapOf(
                "userId" to note.userId,
                "title" to note.title,
                "body" to note.body,
                "createdAt" to note.createdAt
            )).await()
            Result.success(ref.id)
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun deleteNote(id: String): Result<Unit> {
        return try { db.collection("notes").document(id).delete().await(); Result.success(Unit) }
        catch (e: Exception) { Result.failure(e) }
    }
}
