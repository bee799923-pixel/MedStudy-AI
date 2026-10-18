package com.medstudy.ai

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.medstudy.ai.data.AiEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class QuickPopupActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val selectedText = intent?.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT)?.toString() ?: "No text selected"

        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    QuickPopupContent(selectedText = selectedText, onClose = { finish() })
                }
            }
        }
    }
}

@Composable
fun QuickPopupContent(selectedText: String, onClose: () -> Unit) {
    var explanation by remember { mutableStateOf("Analyzing selected text...") }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(selectedText) {
        CoroutineScope(Dispatchers.IO).launch {
            val result = AiEngine.explainMedicalText(selectedText, "Quick Explain", "Demo Mode", "")
            withContext(Dispatchers.Main) {
                explanation = result
                isLoading = false
            }
        }
    }

    Card(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("MedStudy AI — Quick Explanation", style = MaterialTheme.typography.titleLarge)
            Text("Selected: \"$selectedText\"", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
            
            Divider()

            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(androidx.compose.ui.Alignment.CenterHorizontally))
            } else {
                Box(modifier = Modifier.weight(1f)) {
                    Text(explanation, style = MaterialTheme.typography.bodyMedium)
                }
            }

            Button(onClick = onClose, modifier = Modifier.fillMaxWidth()) {
                Text("Close")
            }
        }
    }
}
