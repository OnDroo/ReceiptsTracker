package link.ondrov.receipts.ui.screens

import android.net.Uri
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import link.ondrov.receipts.data.model.ReceiptItem
import link.ondrov.receipts.data.model.ReceiptWithDetails
import link.ondrov.receipts.ui.viewmodels.ReceiptDetailViewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// --- 1. The Logic Wrapper (Screen Level) ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReceiptDetailScreen(
    receiptId: String,
    imageUriArg: String? = null,
    onBack: () -> Unit,
    viewModel: ReceiptDetailViewModel = viewModel(factory = ReceiptDetailViewModel.Factory)
) {
    val receiptState by viewModel.uiState.collectAsState()

    // Logic: If data is null, we assume it's loading/analyzing
    val isAnalyzing = receiptState == null
    var errorMsg by remember { mutableStateOf<String?>(null) }

    // Trigger Analysis if needed
    LaunchedEffect(receiptId) {
        if (receiptState == null && imageUriArg != null) {
            val uri = Uri.parse(Uri.decode(imageUriArg))
            val file = File(uri.path ?: "")
            viewModel.analyzeAndSave(file, onError = { errorMsg = it })
        }
    }

    // State Switcher
    if (errorMsg != null) {
        ErrorState(msg = errorMsg!!, onBack = onBack)
    } else if (isAnalyzing) {
        ExpressiveLoadingState(message = "Extracting data from receipt")
    } else {
        // Show the actual UI
        ReceiptDetailsContent(
            receipt = receiptState!!,
            onBack = onBack,
            onEdit = { /* TODO: Implement Edit later */ },
            onDelete = {
                // Connect Delete Action
                viewModel.deleteReceipt {
                    onBack() // Navigate back after delete
                }
            }
        )
    }
}

// --- 2. The Visual UI (Your Code + Image Loader) ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReceiptDetailsContent(
    receipt: ReceiptWithDetails,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var showMenu by remember { mutableStateOf(false) }
    val context = LocalContext.current

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
                actions = {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Options")
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Edit Receipt") },
                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                            onClick = { showMenu = false; onEdit() }
                        )
                        DropdownMenuItem(
                            text = { Text("Delete Receipt") },
                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                            onClick = { showMenu = false; onDelete() }
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp),
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
                        // --- UPDATED: Actual Image Loader ---
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(500.dp) // Taller for visibility
                                .clip(RoundedCornerShape(24.dp))
                                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                        ) {
                            AsyncImage(
                                model = receipt.receipt.getFile(context),
                                contentDescription = "Receipt Scan",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Fit // Fit the whole receipt
                            )
                        }
                    }
                }
            }
            item { Spacer(modifier = Modifier.height(40.dp)) }
        }
    }
}

// --- Components ---

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