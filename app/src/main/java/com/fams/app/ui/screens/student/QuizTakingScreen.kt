package com.fams.app.ui.screens.student

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.fams.app.domain.model.*
import kotlinx.coroutines.delay

@Composable
fun QuizTakingScreen(quiz: Quiz, onSubmit: (Map<String, String>) -> Unit) {
    val answers = remember { mutableStateMapOf<String, String>() }
    var currentIndex by remember { mutableIntStateOf(0) }
    var showConfirmSubmit by remember { mutableStateOf(false) }
    var showConfirmExit by remember { mutableStateOf(false) }
    var timeLeft by remember {
        mutableIntStateOf(
            if (quiz.timerMode == TimerMode.TOTAL) quiz.totalTimeSeconds
            else quiz.perQuestionSeconds
        )
    }

    // Timer
    LaunchedEffect(currentIndex, quiz.timerMode) {
        if (quiz.timerMode == TimerMode.PER_QUESTION) timeLeft = quiz.perQuestionSeconds
        while (timeLeft > 0) {
            delay(1000)
            timeLeft--
            if (timeLeft == 0) {
                if (quiz.timerMode == TimerMode.PER_QUESTION) {
                    if (currentIndex < quiz.questions.size - 1) currentIndex++
                    else onSubmit(answers.toMap())
                } else {
                    onSubmit(answers.toMap())
                }
            }
        }
    }

    BackHandler { showConfirmExit = true }

    if (quiz.questions.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("This quiz has no questions yet.")
                Spacer(Modifier.height(16.dp))
                Button(onClick = { onSubmit(emptyMap()) }) { Text("Exit") }
            }
        }
        return
    }

    val question = quiz.questions[currentIndex]
    val progress = (currentIndex + 1).toFloat() / quiz.questions.size

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        // Header
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically) {
            Text(quiz.title, style = MaterialTheme.typography.titleMedium)
            // Timer
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

        // Question
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

        // Navigation
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
                Button(onClick = { onSubmit(answers.toMap()) }) { Text("Submit") }
            },
            dismissButton = { TextButton(onClick = { showConfirmSubmit = false }) { Text("Continue") } }
        )
    }

    if (showConfirmExit) {
        AlertDialog(onDismissRequest = { showConfirmExit = false },
            title = { Text("Exit Quiz?") },
            text = { Text("Your progress will be lost if you exit without submitting.") },
            confirmButton = {
                Button(onClick = { onSubmit(answers.toMap()) },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) {
                    Text("Submit & Exit")
                }
            },
            dismissButton = { TextButton(onClick = { showConfirmExit = false }) { Text("Continue Quiz") } }
        )
    }
}
