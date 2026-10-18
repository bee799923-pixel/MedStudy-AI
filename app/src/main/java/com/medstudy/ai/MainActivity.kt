package com.medstudy.ai

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.medstudy.ai.data.AiEngine
import com.medstudy.ai.data.ExportUtil
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val sharedText = if (intent?.action == Intent.ACTION_SEND && intent.type == "text/plain") {
            intent.getStringExtra(Intent.EXTRA_TEXT) ?: ""
        } else {
            ""
        }

        setContent {
            MaterialTheme(
                colorScheme = lightColorScheme()
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainDashboard(initialQuery = sharedText)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainDashboard(initialQuery: String) {
    var queryText by remember { mutableStateOf(initialQuery) }
    var selectedMode by remember { mutableStateOf("Quick Explain") }
    var selectedProvider by remember { mutableStateOf("Demo Mode") }
    var apiKey by remember { mutableStateOf("") }
    var outputResult by remember { mutableStateOf("Select a study mode and tap 'Run AI Analysis', or highlight text in any reader app and share to MedStudy AI.") }
    var isLoading by remember { mutableStateOf(false) }
    var exportOutput by remember { mutableStateOf("") }

    val scope = rememberCoroutineScope()
    val studyModes = listOf(
        "Quick Explain", "Deep Explain", "Clinical Reasoning", 
        "Compare Two Conditions", "MRCP-style Revision", "Exam Pearls", 
        "Patient Case Discussion", "Basic Pharmacology", "Anatomy & Physiology", "Research Paper"
    )
    val aiProviders = listOf("Demo Mode", "Gemini API", "OpenAI API", "OpenRouter")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("MedStudy AI — Tablet Study Companion") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { innerPadding ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = queryText,
                    onValueChange = { queryText = it },
                    label = { Text("Medical Term, Paragraph, or Question") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    var providerExpanded by remember { mutableStateOf(false) }
                    Box(modifier = Modifier.weight(1f)) {
                        OutlinedButton(onClick = { providerExpanded = true }, modifier = Modifier.fillMaxWidth()) {
                            Text("AI: $selectedProvider")
                        }
                        DropdownMenu(expanded = providerExpanded, onDismissRequest = { providerExpanded = false }) {
                            aiProviders.forEach { prov ->
                                DropdownMenuItem(
                                    text = { Text(prov) },
                                    onClick = { selectedProvider = prov; providerExpanded = false }
                                )
                            }
                        }
                    }
                    if (selectedProvider != "Demo Mode") {
                        OutlinedTextField(
                            value = apiKey,
                            onValueChange = { apiKey = it },
                            label = { Text("API Key") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }
                }

                Text("Select Study Mode:", style = MaterialTheme.typography.titleSmall)
                
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(studyModes) { mode ->
                        Button(
                            onClick = { selectedMode = mode },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (selectedMode == mode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = if (selectedMode == mode) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        ) {
                            Text(mode)
                        }
                    }
                }

                Button(
                    onClick = {
                        if (queryText.isBlank()) return@Button
                        isLoading = true
                        scope.launch {
                            outputResult = AiEngine.explainMedicalText(queryText, selectedMode, selectedProvider, apiKey)
                            isLoading = false
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading
                ) {
                    Text(if (isLoading) "Generating Medical Analysis..." else "Run AI Analysis")
                }
            }

            Column(
                modifier = Modifier
                    .weight(1.2f)
                    .fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Card(
                    modifier = Modifier
                        .weight(1.5f)
                        .fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Analysis Result ($selectedMode):", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        if (isLoading) {
                            CircularProgressIndicator(modifier = Modifier.align(androidx.compose.ui.Alignment.CenterHorizontally))
                        } else {
                            LazyColumn(modifier = Modifier.fillMaxSize()) {
                                item {
                                    Text(outputResult, style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            val sampleCard = ExportUtil.FlashcardItem(
                                front = queryText.ifBlank { "Sample Clinical Finding" },
                                back = outputResult.take(250) + "...",
                                tags = "medstudy_ai::$selectedMode"
                            )
                            exportOutput = ExportUtil.generateAnkiTsv(listOf(sampleCard))
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Export Anki TSV")
                    }
                    Button(
                        onClick = {
                            val sampleCard = ExportUtil.FlashcardItem(
                                front = queryText.ifBlank { "Sample Clinical Finding" },
                                back = outputResult.take(250) + "...",
                                tags = "medstudy_ai"
                            )
                            exportOutput = ExportUtil.generateQuizletText(listOf(sampleCard))
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Export Quizlet")
                    }
                }

                if (exportOutput.isNotBlank()) {
                    OutlinedTextField(
                        value = exportOutput,
                        onValueChange = {},
                        label = { Text("Generated Export Data (Copy to File or AnkiDroid)") },
                        modifier = Modifier
                            .weight(0.8f)
                            .fillMaxWidth()
                    )
                }
            }
        }
    }
}
