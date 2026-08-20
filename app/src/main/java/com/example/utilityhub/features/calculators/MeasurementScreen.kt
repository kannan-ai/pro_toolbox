package com.example.utilityhub.features.calculators

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.utilityhub.ui.HistoryViewModel
import com.example.utilityhub.ui.theme.PrimaryAmber
import java.util.Locale

enum class MeasurementCategory(val label: String, val icon: String, val color: Color) {
    LENGTH("Length", "📏", Color(0xFF2196F3)),
    WEIGHT("Weight", "⚖️", Color(0xFFFF9800)),
    AREA("Area", "🗺️", Color(0xFF4CAF50)),
    VOLUME("Volume", "🧪", Color(0xFFE91E63)),
    TEMP("Temp", "🌡️", Color(0xFFF44336)),
    SPEED("Speed", "🚀", Color(0xFF9C27B0)),
    POWER("Power", "⚡", Color(0xFFFFC107)),
    PRESSURE("Pressure", "🎈", Color(0xFF00BCD4)),
    DATA("Data", "💾", Color(0xFF607D8B)),
    TIME("Time", "⏰", Color(0xFF795548))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeasurementScreen(historyViewModel: HistoryViewModel) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val focusRequester = remember { FocusRequester() }
    
    var selectedCategory by remember { mutableStateOf(MeasurementCategory.LENGTH) }
    var inputValue by remember { mutableStateOf("") }
    var fromUnit by remember { mutableStateOf("m") }
    var toUnit by remember { mutableStateOf("cm") }
    var result by remember { mutableStateOf("0") }

    val categoryUnits = mapOf(
        MeasurementCategory.LENGTH to mapOf(
            "m" to 1.0, "cm" to 0.01, "mm" to 0.001, "km" to 1000.0, "inch" to 0.0254, 
            "feet" to 0.3048, "yard" to 0.9144, "miles" to 1609.344, "nautical mile" to 1852.0
        ),
        MeasurementCategory.WEIGHT to mapOf(
            "kg" to 1.0, "g" to 0.001, "mg" to 0.000001, "lbs" to 0.453592, 
            "oz" to 0.0283495, "stone" to 6.35029, "tonne" to 1000.0
        ),
        MeasurementCategory.AREA to mapOf(
            "sq m" to 1.0, "sq cm" to 0.0001, "sq km" to 1000000.0, "sq inch" to 0.00064516,
            "sq feet" to 0.092903, "acre" to 4046.86, "hectare" to 10000.0, "sq mile" to 2589988.11
        ),
        MeasurementCategory.VOLUME to mapOf(
            "litre" to 1.0, "ml" to 0.001, "gal" to 3.78541, "cup" to 0.236588,
            "fl oz" to 0.0295735, "pint" to 0.473176, "quart" to 0.946353
        ),
        MeasurementCategory.TEMP to mapOf(
            "Celsius" to 1.0, "Fahrenheit" to 1.0, "Kelvin" to 1.0
        ),
        MeasurementCategory.SPEED to mapOf(
            "km/h" to 1.0, "mph" to 1.60934, "m/s" to 3.6, "knot" to 1.852
        ),
        MeasurementCategory.POWER to mapOf(
            "watt" to 1.0, "kilowatt" to 1000.0, "horsepower" to 745.7, "btu/hr" to 0.29307
        ),
        MeasurementCategory.PRESSURE to mapOf(
            "pascal" to 1.0, "bar" to 100000.0, "psi" to 6894.76, "atm" to 101325.0
        ),
        MeasurementCategory.DATA to mapOf(
            "B" to 1.0, "KB" to 1024.0, "MB" to 1024.0 * 1024, 
            "GB" to 1024.0 * 1024 * 1024, "TB" to 1024.0 * 1024 * 1024 * 1024
        ),
        MeasurementCategory.TIME to mapOf(
            "sec" to 1.0, "min" to 60.0, "hour" to 3600.0, "day" to 86400.0, "week" to 604800.0
        )
    )

    fun convert() {
        val input = inputValue.toDoubleOrNull() ?: 0.0
        
        if (selectedCategory == MeasurementCategory.TEMP) {
            val res = when {
                fromUnit == "Celsius" && toUnit == "Fahrenheit" -> (input * 9/5) + 32
                fromUnit == "Celsius" && toUnit == "Kelvin" -> input + 273.15
                fromUnit == "Fahrenheit" && toUnit == "Celsius" -> (input - 32) * 5/9
                fromUnit == "Fahrenheit" && toUnit == "Kelvin" -> (input - 32) * 5/9 + 273.15
                fromUnit == "Kelvin" && toUnit == "Celsius" -> input - 273.15
                fromUnit == "Kelvin" && toUnit == "Fahrenheit" -> (input - 273.15) * 9/5 + 32
                else -> input
            }
            result = String.format(Locale.getDefault(), "%.2f", res).trimEnd('0').trimEnd('.')
        } else {
            val units = categoryUnits[selectedCategory] ?: emptyMap()
            val fromFactor = units[fromUnit] ?: 1.0
            val toFactor = units[toUnit] ?: 1.0
            val res = (input * fromFactor) / toFactor
            result = if (res < 0.0001 && res != 0.0) {
                String.format(Locale.getDefault(), "%.8f", res)
            } else {
                String.format(Locale.getDefault(), "%.6f", res).trimEnd('0').trimEnd('.')
            }
        }
        
        if (inputValue.isNotBlank()) {
            historyViewModel.addHistory("Measure", "$inputValue $fromUnit", "$result $toUnit")
        }
    }

    LaunchedEffect(selectedCategory) {
        val units = categoryUnits[selectedCategory]?.keys?.toList() ?: emptyList()
        if (units.isNotEmpty()) {
            fromUnit = units[0]
            toUnit = if (units.size > 1) units[1] else units[0]
            convert()
        }
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        PrimaryScrollableTabRow(
            selectedTabIndex = selectedCategory.ordinal,
            containerColor = MaterialTheme.colorScheme.surface,
            edgePadding = 16.dp,
            divider = {},
            indicator = {
                TabRowDefaults.PrimaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(selectedCategory.ordinal),
                    color = PrimaryAmber
                )
            }
        ) {
            MeasurementCategory.entries.forEach { category ->
                Tab(
                    selected = selectedCategory == category,
                    onClick = { selectedCategory = category },
                    text = { 
                        Text(
                            category.label, 
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = if (selectedCategory == category) FontWeight.Bold else FontWeight.Medium,
                            color = if (selectedCategory == category) selectedCategory.color else Color.Gray
                        )
                    }
                )
            }
        }

        LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .imePadding()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Conversion Canvas Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    // Input Field with Clear Button
                    OutlinedTextField(
                        value = inputValue,
                        onValueChange = { inputValue = it; convert() },
                        label = { Text("Enter Value") },
                        modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(12.dp),
                        trailingIcon = {
                            if (inputValue.isNotEmpty()) {
                                IconButton(onClick = { inputValue = ""; result = "0"; convert() }) {
                                    Icon(Icons.Default.Close, "Clear")
                                }
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                        )
                    )

                    // Unit Selectors with Swap Button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        UnitSelector(
                            label = "From", 
                            selected = fromUnit, 
                            units = categoryUnits[selectedCategory]?.keys?.toList() ?: emptyList()
                        ) {
                            fromUnit = it
                            convert()
                        }

                        // Circular Swap Button
                        Surface(
                            onClick = {
                                val tempUnit = fromUnit
                                fromUnit = toUnit
                                toUnit = tempUnit
                                convert()
                            },
                            color = selectedCategory.color.copy(alpha = 0.1f),
                            shape = CircleShape,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.SwapHoriz, 
                                    contentDescription = "Swap", 
                                    tint = selectedCategory.color,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }

                        UnitSelector(
                            label = "To", 
                            selected = toUnit, 
                            units = categoryUnits[selectedCategory]?.keys?.toList() ?: emptyList()
                        ) {
                            toUnit = it
                            convert()
                        }
                    }
                }
            }

            // Results Display Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                border = BorderStroke(1.dp, selectedCategory.color.copy(alpha = 0.2f))
            ) {
                LaunchedEffect(Unit) {
                    focusRequester.requestFocus()
                }

                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "Result", 
                        style = MaterialTheme.typography.titleSmall,
                        color = selectedCategory.color,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "$result $toUnit", 
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Spacer(Modifier.width(8.dp))
                        IconButton(
                            onClick = {
                                clipboardManager.setText(AnnotatedString("$result $toUnit"))
                                Toast.makeText(context, "Result copied!", Toast.LENGTH_SHORT).show()
                            },
                            colors = IconButtonDefaults.iconButtonColors(contentColor = selectedCategory.color)
                        ) {
                            Icon(Icons.Default.ContentCopy, "Copy")
                        }
                    }
                }
            }

            // Category Suggestion Info
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Info, null, tint = selectedCategory.color, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(12.dp))
                    Text(
                        "Showing conversion for ${selectedCategory.label}. Result updates automatically as you type.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RowScope.UnitSelector(label: String, selected: String, units: List<String>, onSelected: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = Modifier.weight(1f)
    ) {
        OutlinedTextField(
            value = selected,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, true),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface
            )
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            units.forEach { unit ->
                DropdownMenuItem(
                    text = { Text(unit) },
                    onClick = {
                        onSelected(unit)
                        expanded = false
                    }
                )
            }
        }
    }
}
