package com.fams.app.ui.screens.teacher

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.fams.app.domain.model.*
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateQuizScreen(
    subjects: List<Pair<String, String>>,   // subjectId to subjectName
    sectionId: String,
    teacherId: String,
    onDismiss: () -> Unit,
    onConfirm: (Quiz) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var selectedSubject by remember { mutableStateOf(subjects.firstOrNull()) }
    var timerMode by remember { mutableStateOf(TimerMode.TOTAL) }
    var totalMinutes by remember { mutableStateOf("30") }
    var perQuestionSeconds by remember { mutableStateOf("60") }
    val questions = remember { mutableStateListOf<QuizQuestion>() }
    var showAddQuestion by remember { mutableStateOf(false) }
    var editingQuestion by remember { mutableStateOf<QuizQuestion?>(null) }
    var titleError by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Create Quiz") },
                navigationIcon = {
                    IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, null) }
                },
                actions = {
                    Button(
                        onClick = {
                            if (title.isBlank()) { titleError = true; return@Button }
                            if (questions.isEmpty()) return@Button
                            val s = selectedSubject ?: return@Button
                            val quiz = Quiz(
                                title = title.trim(),
                                subjectId = s.first,
                                subjectName = s.second,
                                sectionId = sectionId,
                                teacherId = teacherId,
                                isAvailable = false,
                                timerMode = timerMode,
                                totalTimeSeconds = (totalMinutes.toIntOrNull() ?: 30) * 60,
                                perQuestionSeconds = perQuestionSeconds.toIntOrNull() ?: 60,
                                questions = questions.toList()
                            )
                            onConfirm(quiz)
                        },
                        modifier = Modifier.padding(end = 8.dp),
                        enabled = title.isNotBlank() && questions.isNotEmpty()
                    ) { Text("Save") }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── Quiz Info ──────────────────────────────────────────────────
            item {
                Text("Quiz Details", style = MaterialTheme.typography.titleMedium)
            }

            item {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it; titleError = false },
                    label = { Text("Quiz Title *") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = titleError,
                    supportingText = if (titleError) ({ Text("Title is required") }) else null
                )
            }

            // Subject picker
            item {
                Text("Subject", style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(4.dp))
                subjects.forEach { (id, name) ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = selectedSubject?.first == id,
                            onClick = { selectedSubject = id to name }
                        )
                        Text(name, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            // ── Timer Settings ─────────────────────────────────────────────
            item {
                HorizontalDivider()
                Spacer(Modifier.height(4.dp))
                Text("Timer Settings", style = MaterialTheme.typography.titleMedium)
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = timerMode == TimerMode.TOTAL,
                        onClick = { timerMode = TimerMode.TOTAL },
                        label = { Text("Total Timer") },
                        leadingIcon = if (timerMode == TimerMode.TOTAL) ({
                            Icon(Icons.Default.Check, null, Modifier.size(16.dp))
                        }) else null
                    )
                    FilterChip(
                        selected = timerMode == TimerMode.PER_QUESTION,
                        onClick = { timerMode = TimerMode.PER_QUESTION },
                        label = { Text("Per Question") },
                        leadingIcon = if (timerMode == TimerMode.PER_QUESTION) ({
                            Icon(Icons.Default.Check, null, Modifier.size(16.dp))
                        }) else null
                    )
                }
            }

            item {
                if (timerMode == TimerMode.TOTAL) {
                    OutlinedTextField(
                        value = totalMinutes,
                        onValueChange = { totalMinutes = it },
                        label = { Text("Total Time (minutes)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        leadingIcon = { Icon(Icons.Default.Timer, null) },
                        supportingText = { Text("e.g. 30 = 30 minutes for the whole quiz") }
                    )
                } else {
                    OutlinedTextField(
                        value = perQuestionSeconds,
                        onValueChange = { perQuestionSeconds = it },
                        label = { Text("Time per Question (seconds)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        leadingIcon = { Icon(Icons.Default.Timer, null) },
                        supportingText = { Text("e.g. 60 = 1 minute per question") }
                    )
                }
            }

            // ── Questions ──────────────────────────────────────────────────
            item {
                HorizontalDivider()
                Spacer(Modifier.height(4.dp))
                Row(modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically) {
                    Text("Questions (${questions.size})", style = MaterialTheme.typography.titleMedium)
                    Button(onClick = { showAddQuestion = true }) {
                        Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Add Question")
                    }
                }
                if (questions.isEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Card(colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer)) {
                        Text("Add at least one question to save the quiz.",
                            modifier = Modifier.padding(12.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer)
                    }
                }
            }

            itemsIndexed(questions) { index, question ->
                QuestionCard(
                    index = index,
                    question = question,
                    onEdit = { editingQuestion = question },
                    onDelete = { questions.removeAt(index) },
                    onMoveUp = { if (index > 0) { questions.add(index - 1, questions.removeAt(index)) } },
                    onMoveDown = { if (index < questions.size - 1) { questions.add(index + 1, questions.removeAt(index)) } }
                )
            }
        }
    }

    if (showAddQuestion) {
        QuestionEditorDialog(
            question = null,
            onDismiss = { showAddQuestion = false },
            onConfirm = { q ->
                questions.add(q)
                showAddQuestion = false
            }
        )
    }

    if (editingQuestion != null) {
        QuestionEditorDialog(
            question = editingQuestion,
            onDismiss = { editingQuestion = null },
            onConfirm = { updated ->
                val idx = questions.indexOfFirst { it.id == updated.id }
                if (idx >= 0) questions[idx] = updated
                editingQuestion = null
            }
        )
    }
}

// ── Question Card ─────────────────────────────────────────────────────────────

@Composable
fun QuestionCard(
    index: Int,
    question: QuizQuestion,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Question number badge
                    Badge(containerColor = MaterialTheme.colorScheme.primary) {
                        Text("${index + 1}", style = MaterialTheme.typography.labelMedium)
                    }
                    // Type chip
                    AssistChip(
                        onClick = {},
                        label = {
                            Text(when (question.type) {
                                QuestionType.MCQ -> "MCQ"
                                QuestionType.TRUE_FALSE -> "T/F"
                                QuestionType.SHORT_ANSWER -> "Short"
                            }, style = MaterialTheme.typography.labelSmall)
                        }
                    )
                }
                Row {
                    IconButton(onClick = onMoveUp, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.KeyboardArrowUp, null, modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onMoveDown, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.KeyboardArrowDown, null, modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Edit, null, modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Delete, null, modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.error)
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            Text(question.text, style = MaterialTheme.typography.bodyMedium)

            when (question.type) {
                QuestionType.MCQ -> {
                    Spacer(Modifier.height(4.dp))
                    question.options.forEachIndexed { i, opt ->
                        val isCorrect = opt == question.correctAnswer
                        Row(verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(
                                if (isCorrect) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                null,
                                modifier = Modifier.size(16.dp),
                                tint = if (isCorrect) MaterialTheme.colorScheme.primary
                                       else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(opt, style = MaterialTheme.typography.bodySmall,
                                color = if (isCorrect) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                QuestionType.TRUE_FALSE -> {
                    Spacer(Modifier.height(4.dp))
                    Text("Answer: ${question.correctAnswer}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary)
                }
                QuestionType.SHORT_ANSWER -> {
                    Spacer(Modifier.height(4.dp))
                    Text("Short answer — graded manually",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

// ── Question Editor Dialog ────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuestionEditorDialog(
    question: QuizQuestion?,
    onDismiss: () -> Unit,
    onConfirm: (QuizQuestion) -> Unit
) {
    var questionText by remember { mutableStateOf(question?.text ?: "") }
    var questionType by remember { mutableStateOf(question?.type ?: QuestionType.MCQ) }
    var correctAnswer by remember { mutableStateOf(question?.correctAnswer ?: "") }
    val options = remember {
        mutableStateListOf<String>().also { list ->
            if (question?.options?.isNotEmpty() == true) list.addAll(question.options)
            else { list.add(""); list.add(""); list.add(""); list.add("") }
        }
    }
    var textError by remember { mutableStateOf(false) }
    var answerError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (question == null) "Add Question" else "Edit Question") },
        text = {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Question type selector
                Text("Question Type", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(
                        QuestionType.MCQ to "MCQ",
                        QuestionType.TRUE_FALSE to "True/False",
                        QuestionType.SHORT_ANSWER to "Short Answer"
                    ).forEach { (type, label) ->
                        FilterChip(
                            selected = questionType == type,
                            onClick = {
                                questionType = type
                                correctAnswer = ""
                                if (type == QuestionType.TRUE_FALSE) {
                                    options.clear()
                                    options.addAll(listOf("True", "False"))
                                } else if (type == QuestionType.MCQ && options.size < 2) {
                                    options.clear()
                                    options.addAll(listOf("", "", "", ""))
                                }
                            },
                            label = { Text(label, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }

                // Question text
                OutlinedTextField(
                    value = questionText,
                    onValueChange = { questionText = it; textError = false },
                    label = { Text("Question *") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 4,
                    isError = textError,
                    supportingText = if (textError) ({ Text("Question text is required") }) else null
                )

                // Type-specific inputs
                when (questionType) {
                    QuestionType.MCQ -> {
                        Text("Answer Options (mark correct one)", style = MaterialTheme.typography.labelLarge)
                        options.forEachIndexed { i, opt ->
                            Row(verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                RadioButton(
                                    selected = correctAnswer == opt && opt.isNotBlank(),
                                    onClick = { if (opt.isNotBlank()) correctAnswer = opt }
                                )
                                OutlinedTextField(
                                    value = opt,
                                    onValueChange = { newVal ->
                                        if (correctAnswer == options[i]) correctAnswer = newVal
                                        options[i] = newVal
                                    },
                                    label = { Text("Option ${i + 1}") },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true
                                )
                                if (options.size > 2) {
                                    IconButton(onClick = {
                                        if (correctAnswer == options[i]) correctAnswer = ""
                                        options.removeAt(i)
                                    }, modifier = Modifier.size(32.dp)) {
                                        Icon(Icons.Default.Close, null,
                                            modifier = Modifier.size(16.dp),
                                            tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }
                        if (options.size < 6) {
                            TextButton(onClick = { options.add("") }) {
                                Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Add Option")
                            }
                        }
                        if (answerError) {
                            Text("Select the correct answer by tapping a radio button",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error)
                        }
                    }

                    QuestionType.TRUE_FALSE -> {
                        Text("Correct Answer", style = MaterialTheme.typography.labelLarge)
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            listOf("True", "False").forEach { answer ->
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    RadioButton(
                                        selected = correctAnswer == answer,
                                        onClick = { correctAnswer = answer; answerError = false }
                                    )
                                    Text(answer)
                                }
                            }
                        }
                        if (answerError) {
                            Text("Select True or False",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error)
                        }
                    }

                    QuestionType.SHORT_ANSWER -> {
                        Card(colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                            Text("Short answer questions are graded manually by the teacher after submission.",
                                modifier = Modifier.padding(12.dp),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                var valid = true
                if (questionText.isBlank()) { textError = true; valid = false }
                if (questionType != QuestionType.SHORT_ANSWER && correctAnswer.isBlank()) {
                    answerError = true; valid = false
                }
                if (questionType == QuestionType.MCQ) {
                    val nonEmpty = options.filter { it.isNotBlank() }
                    if (nonEmpty.size < 2) { valid = false }
                }
                if (!valid) return@Button

                val finalOptions = when (questionType) {
                    QuestionType.MCQ -> options.filter { it.isNotBlank() }
                    QuestionType.TRUE_FALSE -> listOf("True", "False")
                    QuestionType.SHORT_ANSWER -> emptyList()
                }

                onConfirm(QuizQuestion(
                    id = question?.id ?: UUID.randomUUID().toString(),
                    text = questionText.trim(),
                    type = questionType,
                    options = finalOptions,
                    correctAnswer = correctAnswer.trim()
                ))
            }) { Text("Save Question") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
