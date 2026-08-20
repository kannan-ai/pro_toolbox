package com.example.utilityhub.features.qr

import android.content.ContentValues
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.utilityhub.ui.theme.PrimaryAmber
import com.google.zxing.*
import com.google.zxing.common.HybridBinarizer
import com.journeyapps.barcodescanner.BarcodeEncoder
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

@Composable
fun QRScreen() {
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Generate, 1: Scan
    var input by remember { mutableStateOf("") }
    var qrBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var scannedText by remember { mutableStateOf<String?>(null) }
    
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val scrollState = rememberScrollState()

    fun generate(text: String) {
        if (text.isBlank()) {
            qrBitmap = null
            return
        }
        try {
            val encoder = BarcodeEncoder()
            val hints = mutableMapOf<EncodeHintType, Any>()
            hints[EncodeHintType.ERROR_CORRECTION] = com.google.zxing.qrcode.decoder.ErrorCorrectionLevel.H
            hints[EncodeHintType.MARGIN] = 1
            
            qrBitmap = encoder.encodeBitmap(text, BarcodeFormat.QR_CODE, 512, 512, hints)
        } catch (_: Exception) {
            qrBitmap = null
        }
    }

    val scanPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            try {
                val inputStream: InputStream? = context.contentResolver.openInputStream(it)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                
                val maxDimension = 1024
                val scaledBitmap = if (bitmap.width > maxDimension || bitmap.height > maxDimension) {
                    val scale = maxDimension.toFloat() / maxOf(bitmap.width, bitmap.height)
                    Bitmap.createScaledBitmap(bitmap, (bitmap.width * scale).toInt(), (bitmap.height * scale).toInt(), true)
                } else {
                    bitmap
                }

                val intArray = IntArray(scaledBitmap.width * scaledBitmap.height)
                scaledBitmap.getPixels(intArray, 0, scaledBitmap.width, 0, 0, scaledBitmap.width, scaledBitmap.height)
                
                val source = RGBLuminanceSource(scaledBitmap.width, scaledBitmap.height, intArray)
                val binaryBitmap = BinaryBitmap(HybridBinarizer(source))
                val reader = MultiFormatReader()
                
                val hints = mutableMapOf<DecodeHintType, Any>()
                hints[DecodeHintType.TRY_HARDER] = true
                hints[DecodeHintType.POSSIBLE_FORMATS] = listOf(BarcodeFormat.QR_CODE)
                
                val result = reader.decode(binaryBitmap, hints)
                
                scannedText = result.text
                Toast.makeText(context, "QR Code Read Successfully", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Could not read QR Code", Toast.LENGTH_SHORT).show()
                e.printStackTrace()
            }
        }
    }

    fun saveQrToGallery(bitmap: Bitmap) {
        val filename = "QR_${System.currentTimeMillis()}.png"
        val contentResolver = context.contentResolver

        try {
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/UtilityHub")
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }
            }

            val imageUri: Uri? = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            
            imageUri?.let { uri ->
                contentResolver.openOutputStream(uri)?.use { outputStream ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                }
            }

            if ((Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) && imageUri != null) {
                contentValues.clear()
                contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                contentResolver.update(imageUri, contentValues, null, null)
            }

            Toast.makeText(context, "Saved to Gallery", Toast.LENGTH_SHORT).show()
        } catch (_: Exception) {
            Toast.makeText(context, "Failed to save", Toast.LENGTH_SHORT).show()
        }
    }

    fun shareQrCode(bitmap: Bitmap) {
        try {
            val cachePath = File(context.cacheDir, "images")
            cachePath.mkdirs()
            val stream = FileOutputStream("$cachePath/qr_code.png")
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            stream.close()

            val newFile = File(cachePath, "qr_code.png")
            val contentUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", newFile)

            if (contentUri != null) {
                val shareIntent = Intent().apply {
                    action = Intent.ACTION_SEND
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    setDataAndType(contentUri, context.contentResolver.getType(contentUri))
                    putExtra(Intent.EXTRA_STREAM, contentUri)
                    type = "image/png"
                }
                context.startActivity(Intent.createChooser(shareIntent, "Share QR Code"))
            }
        } catch (_: Exception) {
            Toast.makeText(context, "Sharing failed", Toast.LENGTH_SHORT).show()
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = selectedTab) {
            Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Generate") })
            Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Scan") })
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .imePadding()
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            if (selectedTab == 0) {
                // Generate Tab
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = input,
                                onValueChange = { input = it },
                                label = { Text("Text or URL to Generate") },
                                modifier = Modifier.fillMaxWidth().height(120.dp),
                                shape = RoundedCornerShape(12.dp),
                                trailingIcon = {
                                    if (input.isNotEmpty()) {
                                        IconButton(onClick = { input = ""; qrBitmap = null }) {
                                            Icon(Icons.Default.Close, "Clear")
                                        }
                                    }
                                },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                                )
                            )
                            IconButton(
                                onClick = {
                                    val clip = clipboardManager.getText()
                                    if (clip != null) input = clip.text
                                },
                                modifier = Modifier.align(Alignment.TopEnd).padding(top = 8.dp, end = 48.dp)
                            ) {
                                Icon(Icons.Outlined.ContentPaste, "Paste", tint = PrimaryAmber)
                            }
                        }

                        Button(
                            onClick = { generate(input) },
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryAmber),
                            enabled = input.isNotBlank()
                        ) {
                            Icon(Icons.Default.QrCode, null)
                            Spacer(Modifier.width(8.dp))
                            Text("Generate QR Code", fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Generated QR Result
                qrBitmap?.let { bitmap ->
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        Card(
                            modifier = Modifier.size(280.dp),
                            shape = RoundedCornerShape(24.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White)
                        ) {
                            Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
                                Image(
                                    bitmap = bitmap.asImageBitmap(),
                                    contentDescription = "Generated QR Code",
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                        
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Button(
                                onClick = { saveQrToGallery(bitmap) },
                                modifier = Modifier.weight(1f).height(56.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryAmber)
                            ) {
                                Icon(Icons.Default.Download, null)
                                Spacer(Modifier.width(8.dp))
                                Text("Download", fontWeight = FontWeight.Bold)
                            }
                            
                            Button(
                                onClick = { shareQrCode(bitmap) },
                                modifier = Modifier.weight(1f).height(56.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Icon(Icons.Default.Share, null)
                                Spacer(Modifier.width(8.dp))
                                Text("Share", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                } ?: Box(
                    modifier = Modifier.height(280.dp).fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.QrCode, null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f))
                        Spacer(Modifier.height(12.dp))
                        Text("Generated QR will appear here", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                    }
                }
            } else {
                // Scan Tab
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text("Select a QR image from your gallery to extract information.", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                        
                        Button(
                            onClick = { scanPickerLauncher.launch("image/*") },
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryAmber)
                        ) {
                            Icon(Icons.Default.PhotoLibrary, null)
                            Spacer(Modifier.width(8.dp))
                            Text("Pick Image from Gallery", fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Scanned Result Card
                if (scannedText != null) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                    ) {
                        Row(
                            modifier = Modifier.padding(20.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Scanned Content", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                Text(scannedText!!, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Medium)
                            }
                            IconButton(
                                onClick = {
                                    clipboardManager.setText(AnnotatedString(scannedText!!))
                                    Toast.makeText(context, "Copied!", Toast.LENGTH_SHORT).show()
                                },
                                colors = IconButtonDefaults.iconButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Icon(Icons.Default.ContentCopy, "Copy")
                            }
                        }
                    }
                }
            }

            // Feature Highlight
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
                        "All QR processing happens locally. Your URLs and text remain private on your device.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
