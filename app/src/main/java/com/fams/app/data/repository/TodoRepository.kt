package com.fams.app.data.repository

import com.fams.app.domain.model.TodoItem
import com.fams.app.di.FirebaseModule
import kotlinx.coroutines.tasks.await

class TodoRepository {
    private val db = FirebaseModule.firestore

    suspend fun getTodos(userId: String): Result<List<TodoItem>> {
        return try {
            val snap = db.collection("todos").whereEqualTo("userId", userId).get().await()
            Result.success(snap.documents.map { doc ->
                TodoItem(id = doc.id,
                    userId = doc.getString("userId") ?: "",
                    title = doc.getString("title") ?: "",
                    done = doc.getBoolean("done") ?: false,
                    timestamp = doc.getLong("timestamp") ?: 0L)
            }.sortedByDescending { it.timestamp })
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun addTodo(todo: TodoItem): Result<String> {
        return try {
            val ref = db.collection("todos").add(mapOf(
                "userId" to todo.userId,
                "title" to todo.title,
                "done" to todo.done,
                "timestamp" to todo.timestamp
            )).await()
            Result.success(ref.id)
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun toggleDone(id: String, done: Boolean): Result<Unit> {
        return try { db.collection("todos").document(id).update("done", done).await(); Result.success(Unit) }
        catch (e: Exception) { Result.failure(e) }
    }

    suspend fun deleteTodo(id: String): Result<Unit> {
        return try { db.collection("todos").document(id).delete().await(); Result.success(Unit) }
        catch (e: Exception) { Result.failure(e) }
    }
}
