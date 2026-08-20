package com.example.utilityhub.features.calculators

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.utilityhub.ui.HistoryViewModel
import com.example.utilityhub.ui.theme.PrimaryAmber

@Composable
fun PercentageScreen(historyViewModel: HistoryViewModel) {
    var pct by remember { mutableStateOf("") }
    var total by remember { mutableStateOf("") }
    var part by remember { mutableStateOf("") }

    val resultData = remember(pct, total, part) {
        val p = pct.toDoubleOrNull()
        val t = total.toDoubleOrNull()
        val v = part.toDoubleOrNull()

        when {
            // Case 1: Solve for Part (Z = X% of Y)
            p != null && t != null && part.isEmpty() -> {
                val res = (p / 100.0) * t
                Triple("Result Value", formatDouble(res), "$p% of $t")
            }
            // Case 2: Solve for Percentage (X = Z is what % of Y)
            v != null && t != null && t != 0.0 && pct.isEmpty() -> {
                val res = (v / t) * 100.0
                Triple("Percentage", "${formatDouble(res)}%", "$v of $t")
            }
            // Case 3: Solve for Total (Y = Z is X% of what)
            v != null && p != null && p != 0.0 && total.isEmpty() -> {
                val res = v / (p / 100.0)
                Triple("Total Number", formatDouble(res), "$v is $p% of")
            }
            else -> null
        }
    }

    // Add to history when a valid result is achieved
    LaunchedEffect(resultData) {
        resultData?.let { (label, value, formula) ->
            historyViewModel.addHistory("Percentage", formula, "$label: $value")
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .imePadding()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Enter any two fields to solve for the third", style = MaterialTheme.typography.labelSmall, color = Color.Gray)

                // Field 1: Percentage
                OutlinedTextField(
                    value = pct,
                    onValueChange = { pct = it },
                    label = { Text("Percentage") },
                    suffix = { Text("%") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(12.dp),
                    trailingIcon = {
                        if (pct.isNotEmpty()) {
                            IconButton(onClick = { pct = "" }) { Icon(Icons.Default.Close, null) }
                        }
                    }
                )

                // Field 2: Total Number
                OutlinedTextField(
                    value = total,
                    onValueChange = { total = it },
                    label = { Text("Total Number") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(12.dp),
                    trailingIcon = {
                        if (total.isNotEmpty()) {
                            IconButton(onClick = { total = "" }) { Icon(Icons.Default.Close, null) }
                        }
                    }
                )

                // Field 3: Result Value
                OutlinedTextField(
                    value = part,
                    onValueChange = { part = it },
                    label = { Text("Result Value") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(12.dp),
                    trailingIcon = {
                        if (part.isNotEmpty()) {
                            IconButton(onClick = { part = "" }) { Icon(Icons.Default.Close, null) }
                        }
                    }
                )

                Spacer(modifier = Modifier.height(8.dp))
                
                // Unified Result Box
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = resultData?.first ?: "Result", 
                            style = MaterialTheme.typography.labelSmall, 
                            color = MaterialTheme.colorScheme.primary, 
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = resultData?.second ?: "---",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (resultData != null) {
                            Text(
                                text = resultData.third,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Button(
                    onClick = {
                        pct = ""
                        total = ""
                        part = ""
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                ) {
                    Text("Clear All Fields")
                }
            }
        }
    }
}

private fun formatDouble(value: Double): String {
    return if (value == value.toLong().toDouble()) {
        value.toLong().toString()
    } else {
        String.format(java.util.Locale.getDefault(), "%.2f", value).trimEnd('0').trimEnd('.')
    }
}
