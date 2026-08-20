package com.example.utilityhub.ui

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.graphics.toColorInt
import com.example.utilityhub.R
import com.example.utilityhub.navigation.allScreens
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: SettingsViewModel, onNavigateToSupport: () -> Unit, onBack: () -> Unit) {
    val context = LocalContext.current
    
    val voicePermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.setVoiceWakeEnabled(true)
            Toast.makeText(context, "Voice Wake Enabled!", Toast.LENGTH_SHORT).show()
        } else {
            viewModel.setVoiceWakeEnabled(false)
            Toast.makeText(context, "Audio permission is required for Voice Wake.", Toast.LENGTH_LONG).show()
        }
    }

    val isDarkModePref by viewModel.isDarkMode.collectAsState()
    val isDarkMode = isDarkModePref ?: isSystemInDarkTheme()
    val pinnedScreens by viewModel.pinnedScreens.collectAsState()
    val appLanguage by viewModel.appLanguage.collectAsState()
    val isIncognito by viewModel.isIncognitoMode.collectAsState()
    val accentColorName by viewModel.accentColor.collectAsState()
    val isSwaraEnabled by viewModel.isSwaraEnabled.collectAsState()
    val isVoiceWakeEnabled by viewModel.isVoiceWakeEnabled.collectAsState()
    val hideComingSoon by viewModel.hideComingSoon.collectAsState()
    val isGalleryAccessEnabled by viewModel.isGalleryAccessEnabled.collectAsState()
    val isOledStealth by viewModel.isOledStealth.collectAsState()
    val isVaultLocked by viewModel.isVaultLocked.collectAsState()

    val appLanguages = mapOf(
        "en" to "English",
        "ml" to "Malayalam (മലയാളം)",
        "hi" to "Hindi (हिन्दी)",
        "ta" to "Tamil (தமிழ்)",
        "te" to "Telugu (తెలుగు)"
    )
    
    var langExpanded by remember { mutableStateOf(false) }

    // Group States
    var displayExpanded by remember { mutableStateOf(true) }
    var privacyExpanded by remember { mutableStateOf(false) }
    var supportExpanded by remember { mutableStateOf(false) }
    var pinExpanded by remember { mutableStateOf(false) }
    var updatesExpanded by remember { mutableStateOf(false) }
    var devExpanded by remember { mutableStateOf(false) }

    var tapCount by remember { mutableIntStateOf(0) }
    var isDevModeEnabled by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Spacer(Modifier.height(16.dp))

        // 1. Display & Interface
        ExpandableSettingsGroup(
            title = "Display & Interface",
            icon = Icons.Default.Palette,
            isExpanded = displayExpanded,
            onToggle = { displayExpanded = !displayExpanded }
        ) {
            var showCustomPicker by remember { mutableStateOf(false) }

            // Dark Mode Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.DarkMode, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(12.dp))
                    Text(stringResource(R.string.label_dark_mode))
                }
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Refined Reset Button with Label
                    TextButton(
                        onClick = { viewModel.resetTheme(); Toast.makeText(context, "Theme reset", Toast.LENGTH_SHORT).show() },
                        modifier = Modifier.height(32.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp)
                    ) {
                        Icon(Icons.Default.RestartAlt, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Reset Themes", style = MaterialTheme.typography.labelSmall)
                    }
                    
                    IconButton(
                        onClick = { showCustomPicker = true },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.ColorLens, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                    }
                    Switch(
                        checked = isDarkMode,
                        onCheckedChange = { viewModel.toggleDarkMode(it) },
                        modifier = Modifier.scale(0.8f)
                    )
                }
            }

            if (showCustomPicker) {
                CustomColorPickerDialog(
                    initialColor = accentColorName,
                    onDismiss = { showCustomPicker = false },
                    onColorSelected = { viewModel.setAccentColor(it); showCustomPicker = false }
                )
            }

            if (isDarkMode) {
                SettingsToggleItem(
                    label = "OLED Stealth",
                    subtitle = "Pure black for OLED",
                    icon = Icons.Default.Adjust,
                    checked = isOledStealth,
                    onCheckedChange = { viewModel.setOledStealth(it) }
                )
            }

            SettingsToggleItem(
                label = "Hide placeholders",
                subtitle = "Hide 'Coming Soon' tiles",
                icon = Icons.Default.CleaningServices,
                checked = hideComingSoon,
                onCheckedChange = { viewModel.setHideComingSoon(it) }
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
            
            Text("Language", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            Spacer(Modifier.height(4.dp))
            ExposedDropdownMenuBox(
                expanded = langExpanded,
                onExpandedChange = { langExpanded = !langExpanded }
            ) {
                OutlinedTextField(
                    value = appLanguages[appLanguage] ?: "English",
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = langExpanded) },
                    modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, true).fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    textStyle = MaterialTheme.typography.bodySmall
                )
                ExposedDropdownMenu(expanded = langExpanded, onDismissRequest = { langExpanded = false }) {
                    appLanguages.forEach { (code, name) ->
                        DropdownMenuItem(text = { Text(name, style = MaterialTheme.typography.bodySmall) }, onClick = { viewModel.setLanguage(code); langExpanded = false })
                    }
                }
            }
        }

        // 2. Security & Privacy
        ExpandableSettingsGroup(
            title = "Security & Privacy",
            icon = Icons.Default.Security,
            isExpanded = privacyExpanded,
            onToggle = { privacyExpanded = !privacyExpanded }
        ) {
            SettingsToggleItem(
                label = "Incognito Mode",
                subtitle = "Stop recording history",
                icon = Icons.Default.VisibilityOff,
                checked = isIncognito,
                onCheckedChange = { viewModel.setIncognitoMode(it) }
            )
            SettingsToggleItem(
                label = "The Vault Lock",
                subtitle = "Biometric gallery lock",
                icon = Icons.Default.Fingerprint,
                checked = isVaultLocked,
                onCheckedChange = { viewModel.setVaultLocked(it) }
            )
            SettingsToggleItem(
                label = "Gallery Access",
                subtitle = "Enable file management",
                icon = Icons.Default.Storage,
                checked = isGalleryAccessEnabled,
                onCheckedChange = { viewModel.setGalleryAccess(it) }
            )
        }

        // 3. AI & Support
        ExpandableSettingsGroup(
            title = "AI & Support",
            icon = Icons.Default.SmartToy,
            isExpanded = supportExpanded,
            onToggle = { supportExpanded = !supportExpanded }
        ) {
            SettingsToggleItem(
                label = "Swara Bot",
                subtitle = "Offline AI assistant",
                icon = Icons.Default.ChatBubbleOutline,
                checked = isSwaraEnabled,
                onCheckedChange = { viewModel.setSwaraEnabled(it) }
            )
            if (isSwaraEnabled) {
                SettingsToggleItem(
                    label = "Voice Wake",
                    subtitle = "Say 'Hey Swara'",
                    icon = Icons.Default.Mic,
                    checked = isVoiceWakeEnabled,
                    onCheckedChange = { enabled ->
                        if (enabled) {
                            if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                                viewModel.setVoiceWakeEnabled(true)
                            } else {
                                voicePermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            }
                        } else {
                            viewModel.setVoiceWakeEnabled(false)
                        }
                    }
                )
            }
            
            TextButton(
                onClick = onNavigateToSupport,
                modifier = Modifier.fillMaxWidth(),
                enabled = isSwaraEnabled
            ) {
                Icon(Icons.Default.SupportAgent, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Chat with Swara")
            }
        }

        // 4. Pin Tools (Grid) - Redesigned to match image
        val toolsToPin = allScreens.filter { it.route != "settings" && it.route != "history" && it.route != "home" }
        ExpandableSettingsGroup(
            title = "PIN DASHBOARD TOOLS (${pinnedScreens.size}/${toolsToPin.size} Active)",
            icon = Icons.Default.PushPin,
            isExpanded = pinExpanded,
            onToggle = { pinExpanded = !pinExpanded }
        ) {
            CustomSettingsGrid(columns = 2) {
                toolsToPin.forEach { screen ->
                    val isPinned = pinnedScreens.contains(screen.route)
                    val icon = when(screen.route) {
                        "text_studio" -> Icons.Default.Translate
                        "text_to_audio" -> Icons.Default.AudioFile
                        "currency" -> Icons.Default.CurrencyExchange
                        "quick_calc" -> Icons.Default.Percent
                        "qr" -> Icons.Default.QrCodeScanner
                        "measurement" -> Icons.Default.Straighten
                        "password" -> Icons.Default.Password
                        "media_studio" -> Icons.Default.Brush
                        "creations" -> Icons.Default.Folder
                        "video_player" -> Icons.Default.Movie
                        "music_player" -> Icons.Default.Headset
                        "system_health" -> Icons.Default.Dns
                        "smart_price_hub" -> Icons.Default.LocalOffer
                        "file_transfer" -> Icons.Default.RocketLaunch
                        else -> Icons.Default.Apps
                    }

                    // Pill-shaped item from image
                    Surface(
                        onClick = { viewModel.togglePin(screen.route) },
                        modifier = Modifier
                            .padding(4.dp)
                            .fillMaxWidth(),
                        shape = CircleShape, // Heavily rounded corners like in the image
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        border = if (isPinned) BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)) else null
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                icon, 
                                null, 
                                tint = if (isPinned) MaterialTheme.colorScheme.primary else Color.Gray,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                stringResource(screen.titleRes), 
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f),
                                fontWeight = if (isPinned) FontWeight.Bold else FontWeight.Normal
                            )
                            Switch(
                                checked = isPinned,
                                onCheckedChange = { viewModel.togglePin(screen.route) },
                                modifier = Modifier.scale(0.6f),
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = MaterialTheme.colorScheme.primary
                                )
                            )
                        }
                    }
                }
            }
        }

        // 5. Recent Updates
        ExpandableSettingsGroup(
            title = "What's New",
            icon = Icons.Default.NewReleases,
            isExpanded = updatesExpanded,
            onToggle = { updatesExpanded = !updatesExpanded }
        ) {
            val updates = listOf(
                "Universal Casting: Stream video & audio",
                "Fast Transfer: P2P sharing protocol",
                "Swara Bio-Brain: Cognitive intelligence",
                "The Vault: Biometric security hub",
                "Smart Price Hub: AI price comparison",
                "OLED Stealth Mode: Pure black theme"
            )
            updates.forEach { update ->
                Row(modifier = Modifier.padding(vertical = 4.dp)) {
                    Icon(Icons.Default.Check, null, tint = Color(0xFF4CAF50), modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(update, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        if (isDevModeEnabled) {
            ExpandableSettingsGroup(
                title = "Developer Options",
                icon = Icons.Default.BugReport,
                isExpanded = devExpanded,
                onToggle = { devExpanded = !devExpanded }
            ) {
                Text("DB Status: Healthy (Primary Hub)", style = MaterialTheme.typography.bodySmall)
                Text("Swara Brain: Active (v4.2)", style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = { Toast.makeText(context, "System Self-Heal Initiated...", Toast.LENGTH_LONG).show() },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.1f), contentColor = Color.Red)
                ) {
                    Text("Force Self-Heal")
                }
            }
        }
        
        Text(
            "v2.3.0 Elite Edition • Private Hub",
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
                .clickable {
                    tapCount++
                    if (tapCount >= 7) {
                        if (!isDevModeEnabled) {
                            isDevModeEnabled = true
                            Toast.makeText(context, "Developer Options Unlocked!", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.labelSmall,
            color = Color.Gray.copy(alpha = 0.5f)
        )
        
        Spacer(Modifier.height(40.dp))
    }
}

@Composable
fun ExpandableSettingsGroup(
    title: String,
    icon: ImageVector,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggle() }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                    shape = CircleShape,
                    modifier = Modifier.size(32.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                    }
                }
                Spacer(Modifier.width(16.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    null,
                    modifier = Modifier.size(16.dp),
                    tint = Color.Gray
                )
            }
            
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 16.dp)
                ) {
                    content()
                }
            }
        }
    }
}

@Composable
fun SettingsToggleItem(
    label: String,
    subtitle: String,
    icon: ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(12.dp))
            Column {
                Text(label, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                Text(subtitle, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.scale(0.7f)
        )
    }
}

@Composable
fun CustomSettingsGrid(columns: Int, content: @Composable () -> Unit) {
    Layout(content = content) { measurables, constraints ->
        val itemWidth = constraints.maxWidth / columns
        val itemConstraints = constraints.copy(minWidth = itemWidth, maxWidth = itemWidth)
        val placeables = measurables.map { it.measure(itemConstraints) }
        
        val rows = (placeables.size + columns - 1) / columns
        val rowHeight = if (placeables.isNotEmpty()) placeables[0].height else 0
        val gridHeight = rowHeight * rows
        
        layout(constraints.maxWidth, gridHeight) {
            var x = 0
            var y = 0
            placeables.forEachIndexed { index, placeable ->
                placeable.placeRelative(x, y)
                if ((index + 1) % columns == 0) {
                    x = 0
                    y += rowHeight
                } else {
                    x += itemWidth
                }
            }
        }
    }
}

@Composable
fun CustomColorPickerDialog(
    initialColor: String,
    onDismiss: () -> Unit,
    onColorSelected: (String) -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(1) } // 0: Standard, 1: Custom
    
    val initialColorInt = try {
        if (initialColor.startsWith("#")) initialColor.toColorInt()
        else android.graphics.Color.parseColor(initialColor)
    } catch (_: Exception) {
        android.graphics.Color.BLUE
    }

    val hsv = remember {
        val hsv = FloatArray(3)
        android.graphics.Color.colorToHSV(initialColorInt, hsv)
        mutableFloatStateOf(hsv[0]) to (mutableFloatStateOf(hsv[1]) to mutableFloatStateOf(hsv[2]))
    }
    
    var hue by hsv.first
    var saturation by hsv.second.first
    var value by hsv.second.second

    val currentColor = remember(hue, saturation, value) {
        Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, saturation, value)))
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.widthIn(max = 560.dp),
        title = { 
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Colors", fontWeight = FontWeight.Bold)
                IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, null) }
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                TabRow(selectedTabIndex = selectedTab, containerColor = Color.Transparent, contentColor = MaterialTheme.colorScheme.primary, divider = {}) {
                    Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }) {
                        Text("Standard", modifier = Modifier.padding(8.dp))
                    }
                    Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }) {
                        Text("Custom", modifier = Modifier.padding(8.dp))
                    }
                }

                if (selectedTab == 0) {
                    StandardColorGrid(onColorSelected)
                } else {
                    CustomPickerContent(
                        hue = hue,
                        saturation = saturation,
                        value = value,
                        onHsvChange = { h, s, v ->
                            hue = h
                            saturation = s
                            value = v
                        },
                        currentColor = currentColor,
                        initialColor = Color(initialColorInt)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { 
                    val hex = String.format("#%06X", (0xFFFFFF and android.graphics.Color.HSVToColor(floatArrayOf(hue, saturation, value))))
                    onColorSelected(hex)
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(12.dp)
    )
}

@Composable
fun StandardColorGrid(onColorSelected: (String) -> Unit) {
    val colors = listOf(
        "#EF4444", "#F97316", "#F59E0B", "#EAB308", "#84CC16", "#22C55E",
        "#10B981", "#14B8A6", "#06B6D4", "#0EA5E9", "#3B82F6", "#6366F1",
        "#8B5CF6", "#A855F7", "#D946EF", "#EC4899", "#F43F5E", "#E11D48",
        "#9F1239", "#881337", "#4C1D95", "#1E3A8A", "#064E3B", "#0F172A"
    )
    
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(vertical = 12.dp)
    ) {
        for (i in 0 until 4) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.offset(x = if (i % 2 == 1) 16.dp else 0.dp)
            ) {
                for (j in 0 until 6) {
                    val index = i * 6 + j
                    if (index < colors.size) {
                        val color = colors[index]
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(color.toColorInt()))
                                .clickable { onColorSelected(color) }
                                .border(1.dp, Color.White.copy(0.1f), CircleShape)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CustomPickerContent(
    hue: Float,
    saturation: Float,
    value: Float,
    onHsvChange: (Float, Float, Float) -> Unit,
    currentColor: Color,
    initialColor: Color
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(modifier = Modifier.fillMaxWidth().height(180.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SaturationValueBox(
                hue = hue,
                saturation = saturation,
                value = value,
                onSValueChange = { s, v -> onHsvChange(hue, s, v) },
                modifier = Modifier.weight(1f).fillMaxHeight()
            )
            HueBar(
                hue = hue,
                onHueChange = { h -> onHsvChange(h, saturation, value) },
                modifier = Modifier.width(30.dp).fillMaxHeight()
            )
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1.5f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                ColorComponentSlider("Red", currentColor.red) { r -> 
                    val hsvArr = FloatArray(3)
                    android.graphics.Color.RGBToHSV((r * 255).toInt(), (currentColor.green * 255).toInt(), (currentColor.blue * 255).toInt(), hsvArr)
                    onHsvChange(hsvArr[0], hsvArr[1], hsvArr[2])
                }
                ColorComponentSlider("Green", currentColor.green) { g -> 
                    val hsvArr = FloatArray(3)
                    android.graphics.Color.RGBToHSV((currentColor.red * 255).toInt(), (g * 255).toInt(), (currentColor.blue * 255).toInt(), hsvArr)
                    onHsvChange(hsvArr[0], hsvArr[1], hsvArr[2])
                }
                ColorComponentSlider("Blue", currentColor.blue) { b -> 
                    val hsvArr = FloatArray(3)
                    android.graphics.Color.RGBToHSV((currentColor.red * 255).toInt(), (currentColor.green * 255).toInt(), (b * 255).toInt(), hsvArr)
                    onHsvChange(hsvArr[0], hsvArr[1], hsvArr[2])
                }
                
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Hex:", style = MaterialTheme.typography.labelMedium, modifier = Modifier.width(40.dp))
                    Text(
                        String.format("#%06X", (0xFFFFFF and currentColor.toArgb())),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("New", style = MaterialTheme.typography.labelSmall)
                Box(modifier = Modifier.size(60.dp).background(currentColor, RoundedCornerShape(4.dp)).border(1.dp, Color.Gray, RoundedCornerShape(4.dp)))
                Spacer(Modifier.height(4.dp))
                Box(modifier = Modifier.size(60.dp).background(initialColor, RoundedCornerShape(4.dp)).border(1.dp, Color.Gray, RoundedCornerShape(4.dp)))
                Text("Current", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
fun ColorComponentSlider(label: String, value: Float, onValueChange: (Float) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label[0].toString(), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, modifier = Modifier.width(12.dp))
        Slider(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.weight(1f).height(20.dp)
        )
        Text((value * 255).toInt().toString(), style = MaterialTheme.typography.labelSmall, modifier = Modifier.width(24.dp))
    }
}

@Composable
fun SaturationValueBox(
    hue: Float,
    saturation: Float,
    value: Float,
    onSValueChange: (Float, Float) -> Unit,
    modifier: Modifier
) {
    Box(modifier = modifier
        .clip(RoundedCornerShape(4.dp))
        .pointerInput(Unit) {
            detectDragGestures { change, _ ->
                val s = (change.position.x / size.width).coerceIn(0f, 1f)
                val v = 1f - (change.position.y / size.height).coerceIn(0f, 1f)
                onSValueChange(s, v)
            }
        }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val hsvColor = android.graphics.Color.HSVToColor(floatArrayOf(hue, 1f, 1f))
            drawRect(color = Color(hsvColor))
            drawRect(brush = Brush.horizontalGradient(listOf(Color.White, Color.Transparent)))
            drawRect(brush = Brush.verticalGradient(listOf(Color.Transparent, Color.Black)))
            val x = saturation * size.width
            val y = (1f - value) * size.height
            drawCircle(color = if (value > 0.5f) Color.Black else Color.White, radius = 6.dp.toPx(), center = Offset(x, y), style = Stroke(width = 2.dp.toPx()))
        }
    }
}

@Composable
fun HueBar(
    hue: Float,
    onHueChange: (Float) -> Unit,
    modifier: Modifier
) {
    val hueColors = remember { List(361) { Color.hsv(it.toFloat(), 1f, 1f) } }
    Box(modifier = modifier
        .clip(RoundedCornerShape(4.dp))
        .pointerInput(Unit) {
            detectDragGestures { change, _ ->
                val h = (change.position.y / size.height).coerceIn(0f, 1f) * 360f
                onHueChange(h)
            }
        }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(brush = Brush.verticalGradient(hueColors))
            val y = (hue / 360f) * size.height
            drawRect(color = Color.White, topLeft = Offset(0f, y - 2.dp.toPx()), size = androidx.compose.ui.geometry.Size(size.width, 4.dp.toPx()), style = Stroke(width = 1.dp.toPx()))
        }
    }
}
