package com.example.utilityhub.features.calculators

import android.content.ClipData
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.utilityhub.data.api.RetrofitInstance
import com.example.utilityhub.data.db.CachedCurrency
import com.example.utilityhub.data.db.CurrencyCacheDao
import com.example.utilityhub.features.text.LanguageSelector
import com.example.utilityhub.ui.HistoryViewModel
import com.example.utilityhub.ui.theme.PrimaryAmber
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CurrencyScreen(
    historyViewModel: HistoryViewModel, 
    currencyCacheDao: CurrencyCacheDao,
    initialAmount: String = "1",
    initialFrom: String = "USD",
    initialTo: String = "INR"
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboard.current
    var amount by remember { mutableStateOf(initialAmount.ifBlank { "1" }) }
    var fromCurrency by remember { mutableStateOf(initialFrom.ifBlank { "USD" }) }
    var toCurrency by remember { mutableStateOf(initialTo.ifBlank { "INR" }) }
    var result by remember { mutableStateOf("0.00") }
    var isFetching by remember { mutableStateOf(false) }
    var lastUpdated by remember { mutableStateOf("Never") }
    var isOfflineData by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val gson = remember { Gson() }

    val currencies = mapOf(
        "USD" to "🇺🇸 USD - US Dollar",
        "INR" to "🇮🇳 INR - Indian Rupee",
        "EUR" to "🇪🇺 EUR - Euro",
        "GBP" to "🇬🇧 GBP - British Pound",
        "AED" to "🇦🇪 AED - UAE Dirham",
        "SAR" to "🇸🇦 SAR - Saudi Riyal",
        "QAR" to "🇶🇦 QAR - Qatari Riyal",
        "KWD" to "🇰🇼 KWD - Kuwaiti Dinar",
        "BHD" to "🇧🇭 BHD - Bahraini Dinar",
        "OMR" to "🇴🇲 OMR - Omani Rial",
        "JPY" to "🇯🇵 JPY - Japanese Yen",
        "CNY" to "🇨🇳 CNY - Chinese Yuan",
        "CAD" to "🇨🇦 CAD - Canadian Dollar",
        "AUD" to "🇦🇺 AUD - Australian Dollar",
        "SGD" to "🇸🇬 SGD - Singapore Dollar",
        "CHF" to "🇨🇭 CHF - Swiss Franc",
        "MYR" to "🇲🇾 MYR - Malaysian Ringgit",
        "THB" to "🇹🇭 THB - Thai Baht",
        "IDR" to "🇮🇩 IDR - Indonesian Rupiah",
        "KRW" to "🇰🇷 KRW - South Korean Won",
        "BRL" to "🇧🇷 BRL - Brazilian Real",
        "MXN" to "🇲🇽 MXN - Mexican Peso",
        "SEK" to "🇸🇪 SEK - Swedish Krona",
        "NOK" to "🇳🇴 NOK - Norwegian Krone",
        "TRY" to "🇹🇷 TRY - Turkish Lira",
        "RUB" to "🇷🇺 RUB - Russian Ruble",
        "ZAR" to "🇿🇦 ZAR - South African Rand"
    )

    fun convert() {
        val amt = amount.toDoubleOrNull() ?: 0.0
        if (amt == 0.0) {
            result = "0.00"
            return
        }
        isFetching = true
        scope.launch {
            try {
                // 1. Try Network
                val response = RetrofitInstance.currencyApi.getLatestRates(fromCurrency)
                val rate = response.rates[toCurrency] ?: 1.0
                val total = amt * rate
                result = String.format(Locale.getDefault(), "%.2f", total)
                lastUpdated = "Just now"
                isOfflineData = false
                
                // Save to Cache
                withContext(Dispatchers.IO) {
                    val json = gson.toJson(response.rates)
                    currencyCacheDao.insertCache(CachedCurrency(fromCurrency, json))
                }

                if (amount.isNotBlank() && amount != "1") {
                    historyViewModel.addHistory("Currency", "$amt $fromCurrency", "$result $toCurrency")
                }
            } catch (_: Exception) {
                // 2. Fallback to Cache
                withContext(Dispatchers.IO) {
                    val cache = currencyCacheDao.getCache(fromCurrency)
                    if (cache != null) {
                        val type = object : TypeToken<Map<String, Double>>() {}.type
                        val rates: Map<String, Double> = gson.fromJson(cache.ratesJson, type)
                        val rate = rates[toCurrency] ?: 1.0
                        val total = amt * rate
                        
                        val sdf = SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault())
                        val dateStr = sdf.format(Date(cache.timestamp))
                        
                        withContext(Dispatchers.Main) {
                            result = String.format(Locale.getDefault(), "%.2f", total)
                            lastUpdated = "Cached ($dateStr)"
                            isOfflineData = true
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "No network and no cached data for $fromCurrency", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            } finally {
                isFetching = false
            }
        }
    }

    // Auto-calculate on mount and changes
    LaunchedEffect(amount, fromCurrency, toCurrency) {
        convert()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .imePadding()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Conversion Canvas
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Amount Field with Clear
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Amount") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(12.dp),
                    trailingIcon = {
                        if (amount.isNotEmpty()) {
                            IconButton(onClick = { amount = "" }) {
                                Icon(Icons.Default.Close, "Clear")
                            }
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                    )
                )

                // Currency Selectors with Swap
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        LanguageSelector("From", fromCurrency, currencies) { 
                            fromCurrency = it 
                        }
                    }

                    // Circular Swap Button
                    Surface(
                        onClick = {
                            val temp = fromCurrency
                            fromCurrency = toCurrency
                            toCurrency = temp
                        },
                        color = PrimaryAmber.copy(alpha = 0.1f),
                        shape = CircleShape,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.SwapHoriz,
                                contentDescription = "Swap",
                                tint = PrimaryAmber,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    Box(modifier = Modifier.weight(1f)) {
                        LanguageSelector("To", toCurrency, currencies) { 
                            toCurrency = it 
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (isFetching) {
                            CircularProgressIndicator(modifier = Modifier.size(12.dp), strokeWidth = 2.dp, color = PrimaryAmber)
                            Spacer(Modifier.width(8.dp))
                        }
                        Column {
                            Text("Last updated: $lastUpdated", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                            if (isOfflineData) {
                                Text("⚠️ Using Offline Data", style = MaterialTheme.typography.labelSmall, color = Color(0xFFFF5722), fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    TextButton(onClick = { convert() }) {
                        Text("Refresh", style = MaterialTheme.typography.labelSmall, color = PrimaryAmber)
                    }
                }
            }
        }

        // Attractive Result Display
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "Conversion Result",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "$result $toCurrency",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Spacer(Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            scope.launch {
                                val clipData = ClipData.newPlainText("Currency Result", "$result $toCurrency")
                                clipboardManager.setClipEntry(ClipEntry(clipData))
                            }
                            Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                        },
                        colors = IconButtonDefaults.iconButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Default.ContentCopy, "Copy")
                    }
                }
            }
        }

        // Privacy Info
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Info, null, tint = PrimaryAmber, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(12.dp))
                Text(
                    "Live rates are fetched in real-time and cached for offline use. No conversion data is shared externally.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
