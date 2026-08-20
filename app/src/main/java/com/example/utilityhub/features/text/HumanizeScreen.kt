package com.example.utilityhub.features.text

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.utilityhub.data.api.RetrofitInstance
import com.example.utilityhub.data.api.SynonymResponse
import com.example.utilityhub.ui.theme.PrimaryAmber
import kotlinx.coroutines.launch

enum class TextMode(val label: String, val description: String) {
    STANDARD("Paraphrase", "Rephrases text for better flow"),
    HUMANIZE("Humanize", "Removes AI-sounding markers"),
    FORMAL("Formal", "Makes text more professional"),
    SIMPLE("Simple", "Makes text easier to understand")
}

@Composable
fun HumanizeScreen() {
    var input by remember { mutableStateOf("") }
    var output by remember { mutableStateOf("") }
    var selectedMode by remember { mutableStateOf(TextMode.HUMANIZE) }
    var isProcessing by remember { mutableStateOf(false) }
    var selectedWord by remember { mutableStateOf<String?>(null) }
    var synonyms by remember { mutableStateOf<List<SynonymResponse>>(emptyList()) }
    
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val clipboardManager = LocalClipboardManager.current

    val aiMarkers = mapOf(
        "furthermore" to "also",
        "moreover" to "also",
        "additionally" to "also",
        "nevertheless" to "still",
        "consequently" to "so",
        "utilize" to "use",
        "facilitate" to "help",
        "delve" to "look",
        "embark" to "start",
        "meticulous" to "careful",
        "tapestry" to "mix",
        "vibrant" to "bright",
        "comprehensive" to "full",
        "in order to" to "to",
        "due to the fact that" to "because"
    )

    val formalMap = mapOf(
        "help" to "assist",
        "use" to "utilize",
        "get" to "obtain",
        "buy" to "purchase",
        "start" to "commence",
        "end" to "terminate",
        "tell" to "inform"
    )

    val simpleMap = mapOf(
        "utilize" to "use",
        "commence" to "start",
        "terminate" to "end",
        "assistance" to "help",
        "subsequent" to "next",
        "numerous" to "many"
    )

    suspend fun backTranslate(text: String): String {
        return try {
            // Step 1: EN -> ES
            val response1 = RetrofitInstance.translationApi.translate(sl = "en", tl = "es", q = text)
            val esText = StringBuilder()
            if (response1.size() > 0 && response1[0].isJsonArray) {
                response1[0].asJsonArray.forEach { segment ->
                    if (segment.isJsonArray && segment.asJsonArray.size() > 0) {
                        esText.append(segment.asJsonArray[0].asString)
                    }
                }
            }
            
            if (esText.isEmpty()) return text

            // Step 2: ES -> EN
            val response2 = RetrofitInstance.translationApi.translate(sl = "es", tl = "en", q = esText.toString())
            val enResult = StringBuilder()
            if (response2.size() > 0 && response2[0].isJsonArray) {
                response2[0].asJsonArray.forEach { segment ->
                    if (segment.isJsonArray && segment.asJsonArray.size() > 0) {
                        enResult.append(segment.asJsonArray[0].asString)
                    }
                }
            }
            
            if (enResult.isEmpty()) text else enResult.toString()
        } catch (e: Exception) {
            text // Fallback to original
        }
    }

    fun processText() {
        if (input.isBlank()) return
        isProcessing = true
        output = ""
        
        scope.launch {
            var result = input
            
            when (selectedMode) {
                TextMode.STANDARD -> {
                    result = backTranslate(input)
                }
                TextMode.HUMANIZE -> {
                    aiMarkers.forEach { (old, new) ->
                        result = result.replace(old, new, ignoreCase = true)
                    }
                }
                TextMode.FORMAL -> {
                    formalMap.forEach { (old, new) ->
                        result = result.replace(old, new, ignoreCase = true)
                    }
                }
                TextMode.SIMPLE -> {
                    simpleMap.forEach { (old, new) ->
                        result = result.replace(old, new, ignoreCase = true)
                    }
                }
            }
            
            output = result
            isProcessing = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Mode Carousel
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Select Rewrite Mode", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                TextMode.entries.forEach { mode ->
                    val isPremium = mode == TextMode.HUMANIZE || mode == TextMode.FORMAL
                    FilterChip(
                        selected = selectedMode == mode,
                        onClick = { 
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            selectedMode = mode 
                        },
                        label = { 
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(mode.label)
                                if (isPremium) {
                                    Spacer(Modifier.width(4.dp))
                                    Icon(Icons.Default.WorkspacePremium, null, tint = Color(0xFFFFD700), modifier = Modifier.size(12.dp))
                                }
                            }
                        },
                        shape = RoundedCornerShape(20.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                            selectedLabelColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }
            }
            Text(
                text = selectedMode.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
            )
        }

        // 2. Input Section
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color.Transparent,
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    TextField(
                        value = input,
                        onValueChange = { if (it.length <= 2500) input = it },
                        placeholder = { Text("Enter text to process...") },
                        modifier = Modifier.fillMaxWidth().height(160.dp),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        )
                    )
                    
                    Row(modifier = Modifier.align(Alignment.TopEnd)) {
                        if (input.isNotEmpty()) {
                            IconButton(onClick = { input = "" }) {
                                Icon(Icons.Default.Close, null, modifier = Modifier.size(18.dp))
                            }
                        }
                        IconButton(onClick = {
                            val clip = clipboardManager.getText()
                            if (clip != null) input = clip.text
                        }) {
                            Icon(Icons.Outlined.ContentPaste, null, modifier = Modifier.size(18.dp))
                        }
                    }
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    val wordCount = if (input.isBlank()) 0 else input.trim().split("\\s+".toRegex()).size
                    Text("$wordCount words | ${input.length}/2500 ch", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                }
            }
        }

        // 3. Process Button with Gradient
        Button(
            onClick = { processText() },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(12.dp),
            contentPadding = PaddingValues(0.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp),
            enabled = !isProcessing && input.isNotBlank()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Brush.horizontalGradient(listOf(PrimaryAmber, Color(0xFFB45309)))),
                contentAlignment = Alignment.Center
            ) {
                if (isProcessing) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White, strokeWidth = 2.dp)
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AutoFixHigh, null, tint = Color.White)
                        Spacer(Modifier.width(8.dp))
                        Text("Process Text", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }

        // 4. Result Comparison Section
        if (output.isNotEmpty()) {
            Text("Rewrite Result", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.03f),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Result (Tap word for synonyms)", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                        Row {
                            IconButton(onClick = { clipboardManager.setText(AnnotatedString(output)) }) {
                                Icon(Icons.Default.ContentCopy, null, modifier = Modifier.size(20.dp))
                            }
                            IconButton(onClick = { }) {
                                Icon(Icons.Outlined.Save, null, modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                    
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        output.split(Regex("(?<=\\s)|(?=\\s)")).forEach { word ->
                            val cleanWord = word.trim().trim(',', '.', '!', '?', '(', ')')
                            if (word.isBlank()) {
                                Text(word)
                            } else {
                                Text(
                                    text = word,
                                    modifier = Modifier.clickable {
                                        if (cleanWord.isNotEmpty()) {
                                            selectedWord = cleanWord
                                            scope.launch {
                                                try {
                                                    synonyms = RetrofitInstance.datamuseApi.getSynonyms(cleanWord)
                                                } catch (e: Throwable) {
                                                    synonyms = emptyList()
                                                }
                                            }
                                        }
                                    },
                                    color = if (selectedWord == cleanWord && cleanWord.isNotEmpty()) 
                                        MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                    style = if (selectedWord == cleanWord && cleanWord.isNotEmpty())
                                        MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.ExtraBold)
                                    else MaterialTheme.typography.bodyLarge
                                )
                            }
                        }
                    }
                }
            }
        }

        selectedWord?.let { word ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Alternatives for '$word':", style = MaterialTheme.typography.titleSmall)
                        IconButton(onClick = { selectedWord = null }) {
                            Text("✕")
                        }
                    }
                    if (synonyms.isEmpty()) {
                        Text("No synonyms found for this word.", style = MaterialTheme.typography.bodySmall)
                    } else {
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            synonyms.take(10).forEach { syn ->
                                SuggestionChip(
                                    onClick = {
                                        output = output.replaceFirst(word, syn.word)
                                        selectedWord = null
                                    },
                                    label = { Text(syn.word) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FlowRow(
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    content: @Composable () -> Unit
) {
    androidx.compose.foundation.layout.FlowRow(
        modifier = modifier,
        horizontalArrangement = horizontalArrangement,
        content = { content() }
    )
}
