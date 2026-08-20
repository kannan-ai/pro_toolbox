package com.example.utilityhub.features.media

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cast
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Speaker
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.mediarouter.media.MediaControlIntent
import androidx.mediarouter.media.MediaRouteSelector
import androidx.mediarouter.media.MediaRouter
import com.example.utilityhub.ui.theme.PrimaryAmber
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

data class CastDevice(
    val id: String,
    val name: String,
    val description: String?,
    val isConnected: Boolean,
    val route: MediaRouter.RouteInfo
)

object CastManager {
    private val selector = MediaRouteSelector.Builder()
        .addControlCategory(MediaControlIntent.CATEGORY_REMOTE_PLAYBACK)
        .addControlCategory(MediaControlIntent.CATEGORY_LIVE_AUDIO)
        .addControlCategory(MediaControlIntent.CATEGORY_LIVE_VIDEO)
        .build()

    fun getAvailableRoutes(context: Context): Flow<List<CastDevice>> = callbackFlow {
        val router = MediaRouter.getInstance(context)
        
        // Start Multicast Lock for Discovery
        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as android.net.wifi.WifiManager
        val multicastLock = wifiManager.createMulticastLock("CastDiscoveryLock")
        multicastLock.setReferenceCounted(true)
        multicastLock.acquire()

        val callback = object : MediaRouter.Callback() {
            override fun onRouteAdded(router: MediaRouter, route: MediaRouter.RouteInfo) {
                updateRoutes(router)
            }
            override fun onRouteRemoved(router: MediaRouter, route: MediaRouter.RouteInfo) {
                updateRoutes(router)
            }
            override fun onRouteChanged(router: MediaRouter, route: MediaRouter.RouteInfo) {
                updateRoutes(router)
            }
            
            private fun updateRoutes(router: MediaRouter) {
                val devices = router.routes.filter { 
                    it.matchesSelector(selector) && !it.isDefault && it.isEnabled
                }.map {
                    CastDevice(
                        id = it.id,
                        name = it.name,
                        description = it.description ?: if (it.deviceType == MediaRouter.RouteInfo.DEVICE_TYPE_TV) "Smart TV" else "Wireless Device",
                        isConnected = it.isSelected,
                        route = it
                    )
                }
                trySend(devices)
            }
        }

        // Use PERFORM_ACTIVE_SCAN for faster, more aggressive discovery
        router.addCallback(
            selector, 
            callback, 
            MediaRouter.CALLBACK_FLAG_REQUEST_DISCOVERY or MediaRouter.CALLBACK_FLAG_PERFORM_ACTIVE_SCAN
        )
        
        // Initial send
        val initialDevices = router.routes.filter { it.matchesSelector(selector) && !it.isDefault }.map {
            CastDevice(
                id = it.id,
                name = it.name,
                description = it.description,
                isConnected = it.isSelected,
                route = it
            )
        }
        trySend(initialDevices)

        awaitClose {
            router.removeCallback(callback)
            if (multicastLock.isHeld) multicastLock.release()
        }
    }

    fun selectRoute(context: Context, route: MediaRouter.RouteInfo) {
        val router = MediaRouter.getInstance(context)
        router.selectRoute(route)
    }

    fun unselectRoute(context: Context) {
        val router = MediaRouter.getInstance(context)
        router.unselect(MediaRouter.UNSELECT_REASON_DISCONNECTED)
    }
}

@Composable
fun CastHubDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val devices by CastManager.getAvailableRoutes(context).collectAsState(initial = emptyList())
    
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.widthIn(max = 560.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Cast, null, tint = PrimaryAmber)
                Spacer(Modifier.width(12.dp))
                Text("Cast Hub", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "Select a nearby device to stream audio or video.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
                
                if (devices.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(120.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp, color = PrimaryAmber)
                            Spacer(Modifier.height(12.dp))
                            Text("Searching for devices...", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().heightIn(max = 300.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(devices) { device ->
                            Card(
                                onClick = {
                                    CastManager.selectRoute(context, device.route)
                                    Toast.makeText(context, "Connecting to ${device.name}...", Toast.LENGTH_SHORT).show()
                                    onDismiss()
                                },
                                colors = CardDefaults.cardColors(
                                    containerColor = if (device.isConnected) 
                                        PrimaryAmber.copy(alpha = 0.1f) 
                                    else 
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                ),
                                border = if (device.isConnected) 
                                    BorderStroke(1.dp, PrimaryAmber) 
                                else null
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp).fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (device.description?.contains("TV", true) == true) 
                                            Icons.Default.Tv 
                                        else 
                                            Icons.Default.Speaker,
                                        null,
                                        tint = if (device.isConnected) PrimaryAmber else Color.Gray,
                                        modifier = Modifier.size(32.dp)
                                    )
                                    Spacer(Modifier.width(16.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(device.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                        device.description?.let { 
                                            Text(it, style = MaterialTheme.typography.labelSmall, color = Color.Gray) 
                                        }
                                    }
                                    if (device.isConnected) {
                                        Icon(Icons.Default.CheckCircle, null, tint = PrimaryAmber, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                }

                if (devices.any { it.isConnected }) {
                    TextButton(
                        onClick = { 
                            CastManager.unselectRoute(context)
                            onDismiss()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Disconnect Current Device", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(24.dp)
    )
}
