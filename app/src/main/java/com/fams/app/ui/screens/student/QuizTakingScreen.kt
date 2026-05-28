package com.fams.app.ui.screens.student

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.fams.app.domain.model.*
import kotlinx.coroutines.delay

@Composable
fun QuizTakingScreen(
    quiz: Quiz,
    onSubmit: (Map<String, String>) -> Unit,
    quizSessions: List<QuizSession> = emptyList(),
    currentUserId: String = ""
) {
    val answers = remember { mutableStateMapOf<String, String>() }
    var currentIndex by remember { mutableIntStateOf(0) }
    var showConfirmSubmit by remember { mutableStateOf(false) }
    var showConfirmExit by remember { mutableStateOf(false) }
    var submitted by remember { mutableStateOf(false) }
    var myScore by remember { mutableStateOf(0.0) }
    var maxScore by remember { mutableStateOf(0.0) }

    var timeLeft by remember {
        mutableIntStateOf(
            if (quiz.timerMode == TimerMode.TOTAL) quiz.totalTimeSeconds
            else quiz.perQuestionSeconds
        )
    }

    // Timer
    LaunchedEffect(currentIndex, quiz.timerMode, submitted) {
        if (submitted) return@LaunchedEffect
        if (quiz.timerMode == TimerMode.PER_QUESTION) timeLeft = quiz.perQuestionSeconds
        while (timeLeft > 0 && !submitted) {
            delay(1000)
            timeLeft--
            if (timeLeft == 0 && !submitted) {
                if (quiz.timerMode == TimerMode.PER_QUESTION) {
                    if (currentIndex < quiz.questions.size - 1) currentIndex++
                    else {
                        // Auto-submit
                        val score = calculateScore(quiz, answers)
                        myScore = score
                        maxScore = quiz.questions.size.toDouble()
                        submitted = true
                        onSubmit(answers.toMap())
                    }
                } else {
                    val score = calculateScore(quiz, answers)
                    myScore = score
                    maxScore = quiz.questions.size.toDouble()
                    submitted = true
                    onSubmit(answers.toMap())
                }
            }
        }
    }

    BackHandler {
        if (!submitted) showConfirmExit = true
    }

    // ── Results Screen ────────────────────────────────────────────────────────
    if (submitted) {
        val percentage = if (maxScore > 0) (myScore / maxScore * 100).toInt() else 0
        val leaderboard = quizSessions.sortedByDescending { it.score }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                Spacer(Modifier.height(16.dp))
                Icon(
                    imageVector = if (percentage >= 50) Icons.Default.EmojiEvents else Icons.Default.SentimentDissatisfied,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = if (percentage >= 50) MaterialTheme.colorScheme.primary
                           else MaterialTheme.colorScheme.error
                )
                Spacer(Modifier.height(8.dp))
                Text("Quiz Submitted!", style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center)
                Text(quiz.title, style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center)
            }

            item {
                Card(modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (percentage >= 50)
                            MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.errorContainer
                    )) {
                    Column(modifier = Modifier.padding(24.dp).fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Your Score", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(8.dp))
                        Text("${myScore.toInt()} / ${maxScore.toInt()}",
                            style = MaterialTheme.typography.displayMedium,
                            color = if (percentage >= 50) MaterialTheme.colorScheme.onPrimaryContainer
                                    else MaterialTheme.colorScheme.onErrorContainer)
                        Text("$percentage%",
                            style = MaterialTheme.typography.titleLarge,
                            color = if (percentage >= 50) MaterialTheme.colorScheme.onPrimaryContainer
                                    else MaterialTheme.colorScheme.onErrorContainer)
                        Spacer(Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { (percentage / 100f).coerceIn(0f, 1f) },
                            modifier = Modifier.fillMaxWidth(),
                            color = if (percentage >= 50) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            if (leaderboard.isNotEmpty()) {
                item {
                    Text("Section Leaderboard", style = MaterialTheme.typography.titleMedium)
                }
                itemsIndexed(leaderboard.take(10)) { index, session ->
                    val isMe = session.studentId == currentUserId
                    Card(modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isMe) MaterialTheme.colorScheme.primaryContainer
                                             else MaterialTheme.colorScheme.surface
                        )) {
                        Row(modifier = Modifier.padding(12.dp).fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically) {
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically) {
                                Text("#${index + 1}",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = when (index) {
                                        0 -> androidx.compose.ui.graphics.Color(0xFFFFD700)
                                        1 -> androidx.compose.ui.graphics.Color(0xFFC0C0C0)
                                        2 -> androidx.compose.ui.graphics.Color(0xFFCD7F32)
                                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                                    })
                                Text(
                                    if (isMe) "${session.studentName} (You)"
                                    else session.studentName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (isMe) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Text("${session.score.toInt()}/${session.maxScore.toInt()}",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(80.dp)) }
        }
        return
    }

    // ── Quiz Taking Screen ────────────────────────────────────────────────────

    if (quiz.questions.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("This quiz has no questions yet.")
                Spacer(Modifier.height(16.dp))
                Button(onClick = {
                    submitted = true
                    onSubmit(emptyMap())
                }) { Text("Exit") }
            }
        }
        return
    }

    val question = quiz.questions[currentIndex]
    val progress = (currentIndex + 1).toFloat() / quiz.questions.size

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        // Header
        Row(modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically) {
            Text(quiz.title, style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f))
            Card(colors = CardDefaults.cardColors(
                containerColor = if (timeLeft < 30) MaterialTheme.colorScheme.errorContainer
                                 else MaterialTheme.colorScheme.primaryContainer)) {
                Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Icons.Default.Timer, null, modifier = Modifier.size(16.dp))
                    Text("${timeLeft / 60}:${(timeLeft % 60).toString().padStart(2, '0')}",
                        style = MaterialTheme.typography.titleMedium)
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
        Text("Question ${currentIndex + 1} of ${quiz.questions.size}",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)

        Spacer(Modifier.height(24.dp))

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(question.text, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(16.dp))

                when (question.type) {
                    QuestionType.MCQ -> {
                        question.options.forEach { option ->
                            Row(verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()) {
                                RadioButton(selected = answers[question.id] == option,
                                    onClick = { answers[question.id] = option })
                                Text(option, modifier = Modifier.padding(start = 8.dp))
                            }
                        }
                    }
                    QuestionType.TRUE_FALSE -> {
                        listOf("True", "False").forEach { option ->
                            Row(verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()) {
                                RadioButton(selected = answers[question.id] == option,
                                    onClick = { answers[question.id] = option })
                                Text(option, modifier = Modifier.padding(start = 8.dp))
                            }
                        }
                    }
                    QuestionType.SHORT_ANSWER -> {
                        OutlinedTextField(
                            value = answers[question.id] ?: "",
                            onValueChange = { answers[question.id] = it },
                            label = { Text("Your answer") },
                            modifier = Modifier.fillMaxWidth(), maxLines = 4
                        )
                    }
                }
            }
        }

        Spacer(Modifier.weight(1f))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (currentIndex > 0) {
                OutlinedButton(onClick = { currentIndex-- }, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.ArrowBack, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Previous")
                }
            }
            if (currentIndex < quiz.questions.size - 1) {
                Button(onClick = { currentIndex++ }, modifier = Modifier.weight(1f)) {
                    Text("Next")
                    Spacer(Modifier.width(4.dp))
                    Icon(Icons.Default.ArrowForward, null, modifier = Modifier.size(16.dp))
                }
            } else {
                Button(onClick = { showConfirmSubmit = true }, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Submit Quiz")
                }
            }
        }
    }

    if (showConfirmSubmit) {
        AlertDialog(onDismissRequest = { showConfirmSubmit = false },
            title = { Text("Submit Quiz?") },
            text = { Text("You answered ${answers.size} of ${quiz.questions.size} questions. Submit now?") },
            confirmButton = {
                Button(onClick = {
                    showConfirmSubmit = false
                    val score = calculateScore(quiz, answers)
                    myScore = score
                    maxScore = quiz.questions.size.toDouble()
                    submitted = true
                    onSubmit(answers.toMap())
                }) { Text("Submit") }
            },
            dismissButton = { TextButton(onClick = { showConfirmSubmit = false }) { Text("Continue") } }
        )
    }

    if (showConfirmExit) {
        AlertDialog(onDismissRequest = { showConfirmExit = false },
            title = { Text("Exit Quiz?") },
            text = { Text("Your progress will be lost if you exit without submitting.") },
            confirmButton = {
                Button(onClick = {
                    showConfirmExit = false
                    val score = calculateScore(quiz, answers)
                    myScore = score
                    maxScore = quiz.questions.size.toDouble()
                    submitted = true
                    onSubmit(answers.toMap())
                }, colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error)) { Text("Submit & Exit") }
            },
            dismissButton = { TextButton(onClick = { showConfirmExit = false }) { Text("Continue Quiz") } }
        )
    }
}

private fun calculateScore(quiz: Quiz, answers: Map<String, String>): Double {
    var score = 0.0
    quiz.questions.forEach { q ->
        if (q.type != QuestionType.SHORT_ANSWER) {
            if (answers[q.id]?.trim()?.lowercase() == q.correctAnswer.trim().lowercase()) score++
        }
    }
    return score
}
