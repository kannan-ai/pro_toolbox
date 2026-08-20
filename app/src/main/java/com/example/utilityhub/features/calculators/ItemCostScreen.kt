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
fun ItemCostScreen(historyViewModel: HistoryViewModel) {
    var unitPrice by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf("") }
    var discount by remember { mutableStateOf("") }

    val subtotal = remember(unitPrice, quantity) {
        val price = unitPrice.toDoubleOrNull() ?: 0.0
        val qty = quantity.toDoubleOrNull() ?: 0.0
        price * qty
    }

    val savings = remember(subtotal, discount) {
        val disc = discount.toDoubleOrNull() ?: 0.0
        subtotal * (disc / 100.0)
    }

    val finalTotal = remember(subtotal, savings) {
        subtotal - savings
    }

    // Add to history when calculation changes and inputs are valid
    LaunchedEffect(finalTotal) {
        if (unitPrice.isNotBlank() && quantity.isNotBlank() && subtotal > 0) {
            val discStr = if (discount.isBlank()) "0" else discount
            historyViewModel.addHistory("Shopping", "$quantity x ₹$unitPrice ($discStr% off)", "₹${formatDouble(finalTotal)}")
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
                // Unit Price Input
                OutlinedTextField(
                    value = unitPrice,
                    onValueChange = { unitPrice = it },
                    label = { Text("Unit Price") },
                    suffix = { Text("₹") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(12.dp),
                    trailingIcon = {
                        if (unitPrice.isNotEmpty()) {
                            IconButton(onClick = { unitPrice = "" }) { Icon(Icons.Default.Close, null) }
                        }
                    }
                )

                // Quantity Input
                OutlinedTextField(
                    value = quantity,
                    onValueChange = { quantity = it },
                    label = { Text("Quantity") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(12.dp),
                    trailingIcon = {
                        if (quantity.isNotEmpty()) {
                            IconButton(onClick = { quantity = "" }) { Icon(Icons.Default.Close, null) }
                        }
                    }
                )

                // Discount Input
                OutlinedTextField(
                    value = discount,
                    onValueChange = { discount = it },
                    label = { Text("Discount") },
                    suffix = { Text("%") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(12.dp),
                    trailingIcon = {
                        if (discount.isNotEmpty()) {
                            IconButton(onClick = { discount = "" }) { Icon(Icons.Default.Close, null) }
                        }
                    }
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Visual Distinct Result Box (3-Tier Breakdown)
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        // Tier 1: Subtotal
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Subtotal", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("₹${formatDouble(subtotal)}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }

                        // Tier 2: Savings
                        if (savings > 0) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("You Save", style = MaterialTheme.typography.labelMedium, color = Color(0xFF4CAF50))
                                Text("- ₹${formatDouble(savings)} (${discount}%)", style = MaterialTheme.typography.labelMedium, color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold)
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f), modifier = Modifier.padding(vertical = 4.dp))

                        // Tier 3: Total to Pay
                        Column {
                            Text("Total to Pay", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                            Text(
                                text = if (subtotal == 0.0) "---" else "₹${formatDouble(finalTotal)}",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                Button(
                    onClick = {
                        unitPrice = ""
                        quantity = ""
                        discount = ""
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
