package link.ondrov.receipts.ui.screens

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material3.*
import androidx.compose.material3.ToggleFloatingActionButtonDefaults.animateIcon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import link.ondrov.receipts.data.model.ReceiptItem
import link.ondrov.receipts.data.model.ReceiptWithDetails
import link.ondrov.receipts.ui.components.ExpressiveLoadingState
import link.ondrov.receipts.ui.viewmodels.ReceiptDetailViewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReceiptDetailScreen(
    receiptId: String,
    imageUriArg: String? = null,
    onBack: () -> Unit,
    onNavigateToEdit: (String) -> Unit,
    viewModel: ReceiptDetailViewModel = viewModel(factory = ReceiptDetailViewModel.Factory)
) {
    val receiptState by viewModel.uiState.collectAsState()

    val isAnalyzing = receiptState == null
    var errorMsg by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(receiptId) {
        if (receiptState == null && imageUriArg != null) {
            val uri = Uri.parse(Uri.decode(imageUriArg))
            val file = File(uri.path ?: "")
            viewModel.analyzeAndSave(file, onError = { errorMsg = it })
        }
    }

    if (errorMsg != null) {
        ErrorState(msg = errorMsg!!, onBack = onBack)
    } else if (isAnalyzing) {
        ExpressiveLoadingState(message = "Extracting data from receipt")
    } else {
        ReceiptDetailsContent(
            receipt = receiptState!!,
            onBack = onBack,
            onEdit = { onNavigateToEdit(receiptId) },
            onDelete = {
                viewModel.deleteReceipt { onBack() }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ReceiptDetailsContent(
    receipt: ReceiptWithDetails,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showFullScreenImage by remember { mutableStateOf(false) }

    // FAB Menu State
    var fabMenuExpanded by remember { mutableStateOf(false) }

    // Close menu on Back Press
    BackHandler(fabMenuExpanded) { fabMenuExpanded = false }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        topBar = {
            TopAppBar(
                title = { Text("Receipt Details", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
            )
        },
        floatingActionButton = {
            FloatingActionButtonMenu(
                expanded = fabMenuExpanded,
                button = {
                    ToggleFloatingActionButton(
                        checked = fabMenuExpanded,
                        onCheckedChange = { fabMenuExpanded = !fabMenuExpanded },
                        containerSize = ToggleFloatingActionButtonDefaults.containerSize(
                            initialSize = 96.dp, // Large
                            finalSize = 96.dp    // Large (Prevent shrinking)
                        ),
                        // 2. Force corner radius to stay Large (28dp) in BOTH states
                        containerCornerRadius = ToggleFloatingActionButtonDefaults.containerCornerRadius(
                            initialSize = 28.dp,
                            finalSize = 28.dp
                        ),
                        containerColor = ToggleFloatingActionButtonDefaults.containerColor(
                            initialColor = MaterialTheme.colorScheme.primary,
                            finalColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        val imageVector by remember {
                            derivedStateOf {
                                if (checkedProgress > 0.5f) Icons.Filled.Close else Icons.Outlined.Edit
                            }
                        }
                        Icon(
                            painter = rememberVectorPainter(imageVector),
                            contentDescription = if (fabMenuExpanded) "Close Menu" else "Actions",
                            modifier = Modifier.animateIcon(
                                checkedProgress = { checkedProgress },
                                size = ToggleFloatingActionButtonDefaults.iconSize(
                                    initialSize = 36.dp,
                                    finalSize = 36.dp
                                ),
                                color = ToggleFloatingActionButtonDefaults.iconColor(
                                    initialColor = MaterialTheme.colorScheme.onPrimary,
                                    finalColor = MaterialTheme.colorScheme.onPrimary
                                )
                            )
                        )
                    }
                }
            ) {
                FloatingActionButtonMenuItem(
                    onClick = {
                        fabMenuExpanded = false
                        showDeleteDialog = true
                    },
                    icon = { Icon(Icons.Outlined.Delete, null) },
                    text = { Text("Delete") },
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                )

                FloatingActionButtonMenuItem(
                    onClick = {
                        fabMenuExpanded = false
                        scope.launch {
                            saveImageToGallery(context, receipt.receipt.getFile(context))
                        }
                    },
                    icon = { Icon(Icons.Outlined.Download, null) },
                    text = { Text("Download") },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )

                FloatingActionButtonMenuItem(
                    onClick = {
                        fabMenuExpanded = false
                        onEdit()
                    },
                    icon = { Icon(Icons.Outlined.Edit, null) },
                    text = { Text("Edit") },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp),
            contentPadding = PaddingValues(bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            item { ReceiptHeaderCard(receipt) }

            item {
                SegmentedControl(
                    options = listOf("Items", "Raw Image"),
                    selectedIndex = selectedTab,
                    onOptionSelected = { selectedTab = it }
                )
            }

            item {
                Crossfade(targetState = selectedTab, label = "TabSwitch") { index ->
                    if (index == 0) {
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            Text("Purchased Items", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            receipt.items.forEach { item ->
                                ReceiptItemRowDisplay(item, receipt.receipt.currency)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    } else {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(500.dp)
                                .clip(RoundedCornerShape(24.dp))
                                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                                .clickable { showFullScreenImage = true }
                        ) {
                            AsyncImage(
                                model = receipt.receipt.getFile(context),
                                contentDescription = "Receipt Scan",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Fit
                            )

                            Surface(
                                modifier = Modifier.align(Alignment.TopEnd).padding(16.dp),
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
                            ) {
                                Icon(Icons.Default.ZoomIn, null, Modifier.padding(8.dp))
                            }
                        }
                    }
                }
            }
        }

        // --- DIALOGS ---
        if (showDeleteDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                icon = { Icon(Icons.Default.DeleteForever, null) },
                title = { Text("Delete Receipt?") },
                text = { Text("This action cannot be undone. The receipt and its image will be permanently removed.") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showDeleteDialog = false
                            onDelete()
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Delete")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
                }
            )
        }

        if (showFullScreenImage) {
            FullScreenImageDialog(
                imageModel = receipt.receipt.getFile(context),
                onDismiss = { showFullScreenImage = false }
            )
        }
    }
}

// --- HELPER: Save Image Logic ---
suspend fun saveImageToGallery(context: Context, sourceFile: File) = withContext(Dispatchers.IO) {
    if (!sourceFile.exists()) return@withContext

    try {
        val fileName = "Receipt_${System.currentTimeMillis()}.jpg"

        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/Receipts")
        }

        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

        if (uri != null) {
            resolver.openOutputStream(uri).use { output ->
                sourceFile.inputStream().use { input ->
                    input.copyTo(output!!)
                }
            }
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Saved to Gallery", Toast.LENGTH_SHORT).show()
            }
        }
    } catch (e: Exception) {
        withContext(Dispatchers.Main) {
            Toast.makeText(context, "Failed to save image", Toast.LENGTH_SHORT).show()
        }
        e.printStackTrace()
    }
}

// --- Components (Image Dialog, Cards, etc.) ---

@Composable
fun FullScreenImageDialog(
    imageModel: Any?,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            var scale by remember { mutableFloatStateOf(1f) }
            var offset by remember { mutableStateOf(Offset.Zero) }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            scale = (scale * zoom).coerceIn(1f, 4f)
                            if (scale > 1f) {
                                val maxTranslateX = (size.width * (scale - 1)) / 2
                                val maxTranslateY = (size.height * (scale - 1)) / 2
                                val newX = (offset.x + pan.x).coerceIn(-maxTranslateX, maxTranslateX)
                                val newY = (offset.y + pan.y).coerceIn(-maxTranslateY, maxTranslateY)
                                offset = Offset(newX, newY)
                            } else {
                                offset = Offset.Zero
                            }
                        }
                    }
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        translationX = offset.x
                        translationY = offset.y
                    }
            ) {
                AsyncImage(
                    model = imageModel,
                    contentDescription = "Full Screen Receipt",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
            }

            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(16.dp)
                    .statusBarsPadding()
                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
            ) {
                Icon(Icons.Default.Close, "Close", tint = Color.White)
            }
        }
    }
}

@Composable
fun ErrorState(msg: String, onBack: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Default.Error, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(48.dp))
        Spacer(Modifier.height(16.dp))
        Text("Error", style = MaterialTheme.typography.headlineSmall)
        Text(msg, modifier = Modifier.padding(16.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
        Button(onClick = onBack) { Text("Go Back") }
    }
}

@Composable
fun ReceiptHeaderCard(data: ReceiptWithDetails) {
    val dateStr = remember(data.receipt.dateTimestamp) {
        SimpleDateFormat("MMM dd, yyyy • h:mm a", Locale.getDefault())
            .format(Date(data.receipt.dateTimestamp))
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 32.dp, horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Outlined.Storefront, null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(20.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(data.receipt.storeName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(dateStr, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "${data.receipt.currency}${String.format("%.2f", data.receipt.totalAmount)}",
                style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text("Total Amount", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun ReceiptItemRowDisplay(item: ReceiptItem, currency: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(48.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.ShoppingBag, null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(24.dp))
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(item.name, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium))
            val unitStr = item.unit ?: "x"
            Text("${item.quantity} $unitStr", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(
            "$currency${String.format("%.2f", item.price)}",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
        )
    }
}

@Composable
fun SegmentedControl(options: List<String>, selectedIndex: Int, onOptionSelected: (Int) -> Unit) {
    val backgroundColor = MaterialTheme.colorScheme.surfaceContainerHighest
    val selectedColor = MaterialTheme.colorScheme.primary
    Row(
        modifier = Modifier.fillMaxWidth().height(50.dp).clip(RoundedCornerShape(50)).background(backgroundColor).padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        options.forEachIndexed { index, label ->
            val isSelected = index == selectedIndex
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(50))
                    .background(if (isSelected) selectedColor else Color.Transparent)
                    .clickable { onOptionSelected(index) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}