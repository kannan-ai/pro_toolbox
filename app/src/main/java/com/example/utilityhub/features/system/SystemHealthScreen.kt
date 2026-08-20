package com.example.utilityhub.features.system

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.os.SystemClock
import android.widget.Toast
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.utilityhub.ui.theme.PrimaryAmber
import kotlinx.coroutines.delay
import java.io.File
import java.util.Locale
import kotlin.math.abs

@Composable
fun SystemHealthScreen() {
    val context = LocalContext.current
    
    // Battery States
    var batteryLevel by remember { mutableIntStateOf(0) }
    var batteryTemp by remember { mutableFloatStateOf(0f) }
    var batteryHealth by remember { mutableStateOf("Unknown") }
    var batteryVoltage by remember { mutableIntStateOf(0) }
    var currentNow by remember { mutableIntStateOf(0) }
    var chargeTimeRemaining by remember { mutableLongStateOf(-1L) }
    var powerSource by remember { mutableStateOf("Battery") }
    var isCharging by remember { mutableStateOf(false) }
    
    // Storage States
    var totalStorage by remember { mutableLongStateOf(0L) }
    var availableStorage by remember { mutableLongStateOf(0L) }
    
    // RAM States
    var totalRam by remember { mutableLongStateOf(0L) }
    var availableRam by remember { mutableLongStateOf(0L) }

    // System Stats
    var uptimeMillis by remember { mutableLongStateOf(0L) }

    val infiniteTransition = rememberInfiniteTransition(label = "charging")
    val alphaAnim by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    fun updateStats() {
        val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        
        intent?.let {
            batteryLevel = it.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            batteryTemp = it.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) / 10f
            batteryVoltage = it.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0)
            
            val status = it.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
            isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
            
            val healthInt = it.getIntExtra(BatteryManager.EXTRA_HEALTH, BatteryManager.BATTERY_HEALTH_UNKNOWN)
            batteryHealth = when (healthInt) {
                BatteryManager.BATTERY_HEALTH_GOOD -> "Good"
                BatteryManager.BATTERY_HEALTH_OVERHEAT -> "Overheat"
                BatteryManager.BATTERY_HEALTH_DEAD -> "Dead"
                BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "Over Voltage"
                else -> "Normal"
            }
            
            val plugged = it.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1)
            powerSource = when (plugged) {
                BatteryManager.BATTERY_PLUGGED_AC -> "AC Adapter"
                BatteryManager.BATTERY_PLUGGED_USB -> "USB Cable"
                BatteryManager.BATTERY_PLUGGED_WIRELESS -> "Wireless"
                else -> "Battery"
            }
        }
        
        currentNow = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW) / 1000 // Convert to mA

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            chargeTimeRemaining = batteryManager.computeChargeTimeRemaining()
        }

        // Storage
        val path: File = Environment.getDataDirectory()
        val stat = StatFs(path.path)
        totalStorage = stat.blockCountLong * stat.blockSizeLong
        availableStorage = stat.availableBlocksLong * stat.blockSizeLong

        // RAM
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memInfo)
        totalRam = memInfo.totalMem
        availableRam = memInfo.availMem

        // Uptime
        uptimeMillis = SystemClock.elapsedRealtime()
    }

    LaunchedEffect(Unit) {
        while (true) {
            updateStats()
            delay(2000)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // 1. Pro Battery Card
        val batteryColor = when {
            batteryLevel > 60 -> Color(0xFF4CAF50)
            batteryLevel > 20 -> Color(0xFFFFC107)
            else -> Color(0xFFF44336)
        }

        HealthCard(
            title = "Battery Doctor",
            icon = if (isCharging) Icons.Default.BatteryChargingFull else Icons.Default.BatteryFull,
            iconColor = batteryColor,
            onClick = {
                val intent = Intent(Intent.ACTION_POWER_USAGE_SUMMARY)
                try { context.startActivity(intent) } catch (_: Exception) {
                    Toast.makeText(context, "Discharge Rate: ${abs(currentNow)}mA", Toast.LENGTH_SHORT).show()
                }
            }
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text("Current Status", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "$batteryLevel%",
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.ExtraBold
                            )
                            if (isCharging) {
                                Icon(
                                    Icons.Default.Bolt, 
                                    null, 
                                    tint = PrimaryAmber, 
                                    modifier = Modifier.size(24.dp).alpha(alphaAnim)
                                )
                            }
                        }
                        
                        // Dynamic Time Remaining (Sub-header style)
                        val standbyHours = (batteryLevel * 0.4).toInt()
                        val timeText = if (isCharging && chargeTimeRemaining > 0) {
                            val mins = chargeTimeRemaining / 1000 / 60
                            if (mins >= 60) "${mins / 60}h ${mins % 60}m to full" else "$mins mins to full"
                        } else if (!isCharging) {
                            "~ $standbyHours hours remaining"
                        } else {
                            "Calculating..."
                        }
                        
                        Text(
                            text = timeText,
                            style = MaterialTheme.typography.labelMedium,
                            color = PrimaryAmber,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    Column(horizontalAlignment = Alignment.End) {
                        Surface(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = if (isCharging) powerSource.uppercase() else "DISCHARGING",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                // Animated Progress Bar - Improved contrast track
                Box(modifier = Modifier.fillMaxWidth().height(10.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant)) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(batteryLevel / 100f)
                            .fillMaxHeight()
                            .background(
                                if (isCharging) {
                                    Brush.horizontalGradient(listOf(batteryColor.copy(alpha = 0.7f), batteryColor))
                                } else {
                                    Brush.horizontalGradient(listOf(batteryColor.copy(alpha = 0.5f), batteryColor))
                                }
                            )
                    )
                }

                // Technical Grid (Focused on Physical Status)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    MetricItem(
                        "Health", 
                        batteryHealth, 
                        Icons.Default.Favorite, 
                        if (batteryHealth == "Good") Color(0xFF4CAF50) else Color(0xFFE91E63)
                    )
                    
                    val tempColor = when {
                        batteryTemp > 45 -> Color(0xFFF44336)
                        batteryTemp > 38 -> Color(0xFFFF9800)
                        else -> Color(0xFF2196F3)
                    }
                    MetricItem("Temp", "$batteryTemp°C", Icons.Default.Thermostat, tempColor)
                    
                    MetricItem(
                        if (isCharging) "Charging Rate" else "Current Draw", 
                        "${abs(currentNow)} mA", 
                        if (isCharging) Icons.Default.Bolt else Icons.Default.Speed, 
                        Color(0xFF00BCD4)
                    )
                }
            }
        }

        // 2. Memory & System Hub
        HealthCard(
            title = "System Hub",
            icon = Icons.Default.Memory,
            iconColor = Color(0xFF9C27B0),
            onClick = {
                Toast.makeText(context, "RAM: ${formatSize(availableRam)} free of ${formatSize(totalRam)}", Toast.LENGTH_SHORT).show()
            }
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                UsageSection("RAM Usage", formatSize(totalRam - availableRam), formatSize(totalRam), (totalRam - availableRam).toFloat() / totalRam.coerceAtLeast(1L), Color(0xFF9C27B0))
                
                val totalNominal = getNominalStorage(totalStorage)
                UsageSection("Storage", formatSize(totalStorage - availableStorage), "$totalNominal GB", (totalStorage - availableStorage).toFloat() / totalStorage.coerceAtLeast(1L), Color(0xFF2196F3))
                
                HorizontalDivider(modifier = Modifier.alpha(0.1f))
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if (uptimeMillis > 7 * 24 * 3600 * 1000L) Icons.Default.Warning else Icons.Default.Schedule, 
                        null, 
                        tint = if (uptimeMillis > 7 * 24 * 3600 * 1000L) Color.Red else Color.Gray, 
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text(
                            "System Uptime: ${formatUptime(uptimeMillis)}",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (uptimeMillis > 7 * 24 * 3600 * 1000L) Color.Red else Color.Gray
                        )
                        if (uptimeMillis > 7 * 24 * 3600 * 1000L) {
                            Text("Restart recommended for better performance", style = MaterialTheme.typography.labelSmall, color = Color.Red.copy(alpha = 0.7f))
                        }
                    }
                }
            }
        }

        // 3. Expert Care Section
        HealthCard(
            title = "Battery Care Expert",
            icon = Icons.Default.TipsAndUpdates,
            iconColor = Color(0xFFFFC107)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                TipItem("Keep your battery between 20% and 80% for long-term health.", Icons.Default.BatteryChargingFull, Color(0xFF4CAF50))
                TipItem("Unplug if your phone feels hot while charging.", Icons.Default.Thermostat, Color(0xFFF44336))
                TipItem("Use original chargers to prevent voltage surges and excessive wear.", Icons.Default.ElectricBolt, Color(0xFF00BCD4))
                TipItem("Use Dark Mode and lower brightness to save power cycles.", Icons.Default.DarkMode, Color(0xFF9C27B0))
            }
        }
        
        Spacer(modifier = Modifier.height(40.dp))
    }
}

@Composable
fun HealthCard(title: String, icon: ImageVector, iconColor: Color, onClick: (() -> Unit)? = null, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(color = iconColor.copy(alpha = 0.1f), shape = CircleShape, modifier = Modifier.size(36.dp)) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(icon, null, tint = iconColor, modifier = Modifier.size(20.dp))
                    }
                }
                Spacer(Modifier.width(12.dp))
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            content()
        }
    }
}

@Composable
fun MetricItem(label: String, value: String, icon: ImageVector, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, null, tint = color.copy(alpha = 0.6f), modifier = Modifier.size(16.dp))
        Spacer(Modifier.height(4.dp))
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = color)
        Text(label, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
    }
}

@Composable
fun UsageSection(label: String, used: String, total: String, percentage: Float, color: Color) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            Text("$used / $total", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
        }
        LinearProgressIndicator(
            progress = { percentage },
            modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
            color = color,
            trackColor = color.copy(alpha = 0.1f)
        )
    }
}

@Composable
fun TipItem(text: String, icon: ImageVector, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Surface(color = color.copy(alpha = 0.1f), shape = CircleShape, modifier = Modifier.size(24.dp)) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = color, modifier = Modifier.size(14.dp))
            }
        }
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 18.sp
        )
    }
}

private fun formatUptime(millis: Long): String {
    val days = millis / (24 * 3600 * 1000)
    val hours = (millis % (24 * 3600 * 1000)) / (3600 * 1000)
    return if (days > 0) "$days days $hours hours" else "$hours hours"
}

private fun getNominalStorage(bytes: Long): Int {
    val gb = bytes / (1024 * 1024 * 1024).toDouble()
    val standards = listOf(32, 64, 128, 256, 512, 1024)
    return standards.firstOrNull { it >= gb } ?: gb.toInt()
}

private fun formatSize(size: Long): String {
    val kb = size / 1024
    val mb = kb / 1024
    val gb = mb.toDouble() / 1024
    return when {
        gb >= 1.0 -> "%.1f GB".format(Locale.getDefault(), gb)
        mb > 0 -> "$mb MB"
        else -> "$kb KB"
    }
}
