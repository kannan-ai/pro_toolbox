package com.example.utilityhub.features.media

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.utilityhub.data.prefs.ThemeManager
import com.example.utilityhub.ui.components.CameraPreview
import com.example.utilityhub.ui.components.TutorialOverlay
import com.example.utilityhub.ui.components.TutorialStep
import com.example.utilityhub.ui.theme.CategoryCreative
import com.example.utilityhub.ui.theme.PrimaryAmber
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun FileTransferScreen(
    themeManager: ThemeManager,
    hasSeenTutorial: Boolean = true,
    onMarkTutorialSeen: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    val deviceName by themeManager.deviceName.collectAsState(initial = android.os.Build.MODEL)
    
    var isDiscovering by remember { mutableStateOf(false) }
    var transferMode by remember { mutableStateOf("IDLE") } // "IDLE", "SEND", "RECEIVE"
    var selectedFileUri by remember { mutableStateOf<Uri?>(null) }
    var discoveredPeers by remember { mutableStateOf(listOf<PeerDevice>()) }
    var isStatusTrackingEnabled by remember { mutableStateOf(true) }
    
    var showRenameDialog by remember { mutableStateOf(false) }
    var showQrDialog by remember { mutableStateOf(false) }
    var showScanner by remember { mutableStateOf(false) }

    var showTutorialManual by remember { mutableStateOf(false) }
    val showTutorial = !hasSeenTutorial || showTutorialManual

    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            selectedFileUri = it
            transferMode = "SEND"
            isDiscovering = true
        }
    }

    // Discovery Animation & Peer Simulation
    LaunchedEffect(isDiscovering, isStatusTrackingEnabled) {
        if (isDiscovering && isStatusTrackingEnabled) {
            discoveredPeers = emptyList()
            delay(2000.milliseconds) // Scanning time
            discoveredPeers = listOf(
                PeerDevice("Elite-Pad-X", "Tablet", true),
                PeerDevice("Pro-Phone-S24", "Mobile", false),
                PeerDevice("Studio-Workstation", "Desktop", true)
            )
        } else {
            discoveredPeers = emptyList()
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "Fast Transfer", 
                        style = MaterialTheme.typography.headlineSmall, 
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Wireless P2P Protocol", 
                        style = MaterialTheme.typography.labelSmall, 
                        color = Color.Gray
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Visibility", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    Spacer(Modifier.width(8.dp))
                    Switch(
                        checked = isStatusTrackingEnabled,
                        onCheckedChange = { isStatusTrackingEnabled = it },
                        modifier = Modifier.scale(0.7f),
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = PrimaryAmber,
                            checkedTrackColor = PrimaryAmber.copy(alpha = 0.3f)
                        )
                    )
                    Spacer(Modifier.width(8.dp))
                    IconButton(onClick = { showTutorialManual = true }) {
                        Icon(Icons.AutoMirrored.Filled.Help, null, tint = PrimaryAmber)
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Device Info Card
            Card(
                modifier = Modifier.fillMaxWidth().clickable { showRenameDialog = true },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(color = PrimaryAmber.copy(alpha = 0.1f), shape = CircleShape, modifier = Modifier.size(40.dp)) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Smartphone, null, tint = PrimaryAmber, modifier = Modifier.size(20.dp))
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Visible as", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                        Text(deviceName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                    }
                    Icon(Icons.Default.Edit, null, modifier = Modifier.size(16.dp), tint = Color.Gray)
                }
            }

            Spacer(Modifier.height(24.dp))

            if (transferMode == "IDLE") {
                IdleLayout(
                    onSend = { 
                        if (isStatusTrackingEnabled) {
                            filePicker.launch("*/*") 
                        } else {
                            Toast.makeText(context, "Please enable Visibility to share.", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onReceive = { 
                        if (isStatusTrackingEnabled) {
                            transferMode = "RECEIVE"
                            isDiscovering = true
                        } else {
                            Toast.makeText(context, "Please enable Visibility to receive.", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            } else {
                ActiveTransferLayout(
                    mode = transferMode,
                    isDiscovering = isDiscovering,
                    isVisibilityEnabled = isStatusTrackingEnabled,
                    selectedFileUri = selectedFileUri,
                    peers = discoveredPeers,
                    onScanQr = { showScanner = true },
                    onShowQr = { showQrDialog = true },
                    onCancel = {
                        transferMode = "IDLE"
                        isDiscovering = false
                        selectedFileUri = null
                    }
                )
            }
        }

        if (showTutorial) {
            TutorialOverlay(
                steps = listOf(
                    TutorialStep(
                        "No Data Costs",
                        "Transfer large videos and files without using any mobile data or internet.",
                        Icons.Default.CloudOff
                    ),
                    TutorialStep(
                        "Dual Connectivity",
                        "Works using high-speed Wi-Fi Direct or stable Bluetooth fallback.",
                        Icons.Default.SettingsInputAntenna
                    ),
                    TutorialStep(
                        "Secure P2P",
                        "All transfers are point-to-point and encrypted. Your files stay between devices.",
                        Icons.Default.Https
                    )
                ),
                onDismiss = {
                    onMarkTutorialSeen()
                    showTutorialManual = false
                },
                onSkip = {
                    onMarkTutorialSeen()
                    showTutorialManual = false
                }
            )
        }

        if (showRenameDialog) {
            var newName by remember { mutableStateOf(deviceName) }
            AlertDialog(
                onDismissRequest = { showRenameDialog = false },
                modifier = Modifier.widthIn(max = 560.dp),
                title = { Text("Rename Device") },
                text = {
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        label = { Text("Device Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                confirmButton = {
                    Button(onClick = {
                        if (newName.isNotBlank()) {
                            scope.launch { themeManager.setDeviceName(newName) }
                            showRenameDialog = false
                        }
                    }) { Text("Save") }
                },
                dismissButton = {
                    TextButton(onClick = { showRenameDialog = false }) { Text("Cancel") }
                }
            )
        }

        if (showQrDialog) {
            QrDialog(deviceName = deviceName, onDismiss = { showQrDialog = false })
        }

        if (showScanner) {
            Dialog(onDismissRequest = { showScanner = false }) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        CameraPreview { result ->
                            showScanner = false
                            Toast.makeText(context, "Connected to: $result", Toast.LENGTH_LONG).show()
                        }
                        IconButton(
                            onClick = { showScanner = false },
                            modifier = Modifier.align(Alignment.TopEnd).padding(16.dp)
                        ) {
                            Icon(Icons.Default.Close, null, tint = Color.White)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun IdleLayout(onSend: () -> Unit, onReceive: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(contentAlignment = Alignment.Center) {
            // Animated background rings
            RadarAnimation()
            
            Surface(
                color = PrimaryAmber.copy(alpha = 0.1f),
                shape = CircleShape,
                modifier = Modifier.size(140.dp),
                border = BorderStroke(1.dp, PrimaryAmber.copy(alpha = 0.2f))
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.RocketLaunch, null, tint = PrimaryAmber, modifier = Modifier.size(64.dp))
                }
            }
        }

        Spacer(Modifier.height(48.dp))

        Text(
            "Share Files Instantly",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Text(
            "Send and receive high-res media offline",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(48.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            TransferActionCard(
                title = "Send",
                desc = "Select files to share",
                icon = Icons.Default.FileUpload,
                color = PrimaryAmber,
                modifier = Modifier.weight(1f),
                onClick = onSend
            )
            TransferActionCard(
                title = "Receive",
                desc = "Wait for incoming",
                icon = Icons.Default.FileDownload,
                color = Color(0xFF4CAF50),
                modifier = Modifier.weight(1f),
                onClick = onReceive
            )
        }
    }
}

@Composable
fun ActiveTransferLayout(
    mode: String,
    isDiscovering: Boolean,
    isVisibilityEnabled: Boolean,
    selectedFileUri: Uri?,
    peers: List<PeerDevice>,
    onScanQr: () -> Unit,
    onShowQr: () -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    var selectedPeers by remember { mutableStateOf(setOf<PeerDevice>()) }

    Column(modifier = Modifier.fillMaxSize()) {
        // Status Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = if (mode == "SEND") PrimaryAmber else Color(0xFF4CAF50),
                    shape = CircleShape,
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            if (mode == "SEND") Icons.Default.FileUpload else Icons.Default.FileDownload, 
                            null, 
                            tint = Color.White, 
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Spacer(Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        if (mode == "SEND") "Sending Files" else "Receiving Mode",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    if (selectedFileUri != null) {
                        Text(
                            selectedFileUri.lastPathSegment ?: "Unknown file",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.Gray,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    } else {
                        Text(
                            "Waiting for sender...",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.Gray
                        )
                    }
                }
                TextButton(onClick = onCancel) {
                    Text("Cancel", color = MaterialTheme.colorScheme.error)
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // Multi-device Send Button
        AnimatedVisibility(visible = selectedPeers.isNotEmpty() && mode == "SEND") {
            Button(
                onClick = {
                    Toast.makeText(context, "Sending to ${selectedPeers.size} devices...", Toast.LENGTH_SHORT).show()
                    selectedPeers = emptySet()
                    onCancel()
                },
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryAmber)
            ) {
                Icon(Icons.Default.Send, null)
                Spacer(Modifier.width(8.dp))
                Text("Send to ${selectedPeers.size} Devices")
            }
        }

        // QR Option Button
        OutlinedButton(
            onClick = if (mode == "SEND") onScanQr else onShowQr,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(if (mode == "SEND") Icons.Default.QrCodeScanner else Icons.Default.QrCode, null)
            Spacer(Modifier.width(12.dp))
            Text(if (mode == "SEND") "Scan QR to Connect" else "Show QR to Connect")
        }

        Spacer(Modifier.height(16.dp))

        // Discovery Radar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp),
            contentAlignment = Alignment.Center
        ) {
            RadarAnimation(active = isDiscovering && isVisibilityEnabled)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    if (!isVisibilityEnabled) "Visibility is OFF" 
                    else if (mode == "SEND") "Searching for nearby devices..." 
                    else "Visible to everyone nearby",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.Gray
                )
                if (isVisibilityEnabled) {
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        DiscoveryPill("Wi-Fi", true)
                        DiscoveryPill("Bluetooth", true)
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        Text(
            "DISCOVERED DEVICES",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = Color.Gray,
            letterSpacing = 1.sp
        )

        Spacer(Modifier.height(12.dp))

        if (peers.isEmpty()) {
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(peers) { peer ->
                    PeerItem(
                        peer = peer,
                        isSelected = selectedPeers.contains(peer),
                        onClick = {
                            if (mode == "SEND") {
                                selectedPeers = if (selectedPeers.contains(peer)) {
                                    selectedPeers - peer
                                } else {
                                    selectedPeers + peer
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun TransferActionCard(title: String, desc: String, icon: ImageVector, color: Color, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Card(
        modifier = modifier.clickable { onClick() },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                color = color.copy(alpha = 0.1f),
                shape = CircleShape,
                modifier = Modifier.size(56.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, null, tint = color, modifier = Modifier.size(28.dp))
                }
            }
            Spacer(Modifier.height(16.dp))
            Text(title, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleMedium)
            Text(desc, style = MaterialTheme.typography.labelSmall, color = Color.Gray, textAlign = TextAlign.Center)
        }
    }
}

@Composable
fun PeerItem(peer: PeerDevice, isSelected: Boolean, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) PrimaryAmber.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            1.dp, 
            if (isSelected) PrimaryAmber else MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                shape = CircleShape,
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        if (peer.type == "Tablet") Icons.Default.TabletAndroid else Icons.Default.Smartphone, 
                        null, 
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(peer.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                Text(peer.type, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            }
            if (peer.isSaved) {
                Icon(Icons.Default.Verified, null, tint = Color(0xFF2196F3), modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
            }
            if (isSelected) {
                Icon(Icons.Default.CheckCircle, null, tint = PrimaryAmber)
            } else {
                Icon(Icons.Default.ChevronRight, null, tint = Color.Gray.copy(alpha = 0.5f))
            }
        }
    }
}

@Composable
fun DiscoveryPill(label: String, active: Boolean) {
    Surface(
        color = if (active) Color(0xFF4CAF50).copy(alpha = 0.1f) else Color.Gray.copy(alpha = 0.1f),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, if (active) Color(0xFF4CAF50).copy(alpha = 0.2f) else Color.Transparent)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(if (active) Color(0xFF4CAF50) else Color.Gray))
            Text(
                label, 
                style = MaterialTheme.typography.labelSmall, 
                fontWeight = FontWeight.Bold, 
                color = if (active) Color(0xFF4CAF50) else Color.Gray,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun RadarAnimation(active: Boolean = true) {
    if (!active) return
    val infiniteTransition = rememberInfiniteTransition(label = "radar")

    Box(modifier = Modifier.size(200.dp)) {
        repeat(2) { index ->
            val delayValue = index * 1000
            val animatedRadius by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = 200f,
                animationSpec = infiniteRepeatable(
                    animation = tween(2000, delayMillis = delayValue, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                ),
                label = "radius_$index"
            )
            val animatedAlpha by infiniteTransition.animateFloat(
                initialValue = 0.4f,
                targetValue = 0f,
                animationSpec = infiniteRepeatable(
                    animation = tween(2000, delayMillis = delayValue, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                ),
                label = "alpha_$index"
            )
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(animatedRadius.dp)
                    .clip(CircleShape)
                    .background(PrimaryAmber.copy(alpha = animatedAlpha))
            )
        }
    }
}

@Composable
fun QrDialog(deviceName: String, onDismiss: () -> Unit) {
    val bitmap = remember(deviceName) {
        generateQrCode(deviceName)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.widthIn(max = 560.dp),
        title = { Text("Connect via QR", fontWeight = FontWeight.Bold) },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text("Scan this code on the sending device", style = MaterialTheme.typography.bodySmall, color = Color.Gray, textAlign = TextAlign.Center)
                Spacer(Modifier.height(24.dp))
                bitmap?.let {
                    Image(
                        bitmap = it.asImageBitmap(),
                        contentDescription = "Connection QR",
                        modifier = Modifier.size(200.dp).background(Color.White).padding(8.dp)
                    )
                }
                Spacer(Modifier.height(16.dp))
                Text(deviceName, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleMedium)
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) { Text("Done") }
        }
    )
}

fun generateQrCode(content: String): android.graphics.Bitmap? {
    return try {
        val writer = QRCodeWriter()
        val bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, 512, 512)
        val width = bitMatrix.width
        val height = bitMatrix.height
        val bitmap = android.graphics.Bitmap.createBitmap(width, height, android.graphics.Bitmap.Config.RGB_565)
        for (x in 0 until width) {
            for (y in 0 until height) {
                bitmap.setPixel(x, y, if (bitMatrix.get(x, y)) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
            }
        }
        bitmap
    } catch (_: Exception) {
        null
    }
}

data class PeerDevice(val name: String, val type: String, val isSaved: Boolean)
