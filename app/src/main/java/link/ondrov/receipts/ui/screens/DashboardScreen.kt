package link.ondrov.receipts.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import link.ondrov.receipts.ui.viewmodels.DashboardViewModel
import android.app.Activity
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.net.toUri
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import link.ondrov.receipts.ui.components.*
import link.ondrov.receipts.ui.theme.ReceiptsTheme
import link.ondrov.receipts.util.FileUtils
import link.ondrov.receipts.ui.viewmodels.CategoryUiData
import link.ondrov.receipts.ui.viewmodels.DashboardUiState
import link.ondrov.receipts.ui.viewmodels.TransactionUiData

@Composable
fun DashboardRoute(
    onOpenReceipts: () -> Unit,
    onReceiptClick: (String) -> Unit,
    onNewScan: (String, String) -> Unit,
    onSettingsClick: () -> Unit,
    viewModel: DashboardViewModel = viewModel(factory = DashboardViewModel.Factory)
) {
    val state by viewModel.uiState.collectAsState()
    val currentFilter by viewModel.selectedFilter.collectAsState()

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val scannerOptions = remember {
        GmsDocumentScannerOptions.Builder()
            .setGalleryImportAllowed(true)
            .setPageLimit(1)
            .setResultFormats(GmsDocumentScannerOptions.RESULT_FORMAT_JPEG)
            .setScannerMode(GmsDocumentScannerOptions.SCANNER_MODE_FULL)
            .build()
    }
    val scanner = remember { GmsDocumentScanning.getClient(scannerOptions) }

    val scannerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val scanResult = GmsDocumentScanningResult.fromActivityResultIntent(result.data)
            scanResult?.pages?.firstOrNull()?.let { page ->
                scope.launch(Dispatchers.IO) {
                    val permanentFile = FileUtils.copyUriToInternalStorage(context, page.imageUri)
                    if (permanentFile != null) {
                        val receiptId = permanentFile.nameWithoutExtension
                        val encodedUri = Uri.encode(permanentFile.toUri().toString())
                        withContext(Dispatchers.Main) {
                            onNewScan(receiptId, encodedUri)
                        }
                    }
                }
            }
        }
    }

    DashboardContent(
        state = state,
        selectedFilter = currentFilter,
        onFilterSelected = viewModel::onFilterSelected,
        onOpenReceipts = onOpenReceipts,
        onReceiptClick = onReceiptClick,
        onSettingsClick = onSettingsClick,
        onScanClick = {
            scanner.getStartScanIntent(context as Activity)
                .addOnSuccessListener { scannerLauncher.launch(IntentSenderRequest.Builder(it).build()) }
        }
    )
}

@Composable
fun DashboardContent(
    state: DashboardUiState,
    selectedFilter: String,
    onFilterSelected: (String) -> Unit,
    onOpenReceipts: () -> Unit,
    onReceiptClick: (String) -> Unit,
    onSettingsClick: () -> Unit,
    onScanClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        floatingActionButton = {
            LargeFloatingActionButton(
                onClick = onScanClick,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(28.dp)
            ) {
                Icon(Icons.Outlined.QrCodeScanner, "Scan Receipt", Modifier.size(36.dp))
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item { DashboardTopBar(onSettingsClick = onSettingsClick) }
            item { FilterSection(selectedFilter, onFilterSelected) }
            item { DashboardSummary(state.totalSpendingFormatted, state.receiptCount) }
            item {
                SpendingTrendsCard(
                    graphData = state.graphData,
                    timeLabel = state.dateRangeLabel,
                    totalFormatted = state.totalSpendingFormatted
                )
            }
            item {
                CategoryBreakdownCard(
                    breakdown = state.categoryBreakdown,
                    totalFormatted = state.totalSpendingFormatted
                )
            }
            item {
                TransactionsHeader(
                    selectedFilter = selectedFilter,
                    onOpenReceipts = onOpenReceipts
                )
            }

            if (state.recentTransactions.isEmpty()) {
                item {
                    EmptyTransactionState()
                }
            } else {
                items(state.recentTransactions) { transaction ->
                    TransactionItem(
                        item = transaction,
                        onClick = { onReceiptClick(transaction.id) }
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(100.dp)) }
        }
    }
}

@Composable
fun DashboardTopBar(onSettingsClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "Dashboard",
            style = MaterialTheme.typography.headlineLarge.copy(
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        )

        IconButton(onClick = onSettingsClick) {
            Icon(
                imageVector = Icons.Filled.Settings,
                contentDescription = "Settings",
                tint = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

@Composable
private fun TransactionsHeader(
    selectedFilter: String,
    onOpenReceipts: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onOpenReceipts() }
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Transactions ($selectedFilter)",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground,
        )
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = "Go to Receipts",
            tint = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun EmptyTransactionState() {
    Text(
        text = "No transactions found.",
        modifier = Modifier.padding(vertical = 20.dp),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Preview(showBackground = true)
@Composable
fun DashboardPreview() {
    ReceiptsTheme {
        // 1. Create a Fake State to visualize
        val mockState = DashboardUiState(
            isLoading = false,
            currencySymbol = "€",
            totalSpendingFormatted = "€1,240.00", // Total converted to EUR
            receiptCount = 12,
            dateRangeLabel = "Dec 22 - Dec 28",

            // Fake Graph Data (7 points for "This Week")
            graphData = listOf(10.0, 50.0, 20.0, 120.0, 80.0, 30.0, 60.0),

            // Fake Categories
            categoryBreakdown = listOf(
                CategoryUiData(
                    name = "Groceries",
                    totalFormatted = "€600.00",
                    percentage = 0.5f,
                    colorIndex = 0
                ),
                CategoryUiData(
                    name = "Tech",
                    totalFormatted = "€400.00",
                    percentage = 0.35f,
                    colorIndex = 1
                ),
                CategoryUiData(
                    name = "Utilities",
                    totalFormatted = "€240.00",
                    percentage = 0.15f,
                    colorIndex = 2
                )
            ),

            // Fake Transactions (showing ORIGINAL currencies as requested)
            recentTransactions = listOf(
                TransactionUiData(
                    id = "1",
                    storeName = "Tesco",
                    dateString = "Dec 28, 4:30 PM",
                    amountFormatted = "€24.50",
                    iconId = "shopping_cart"
                ),
                TransactionUiData(
                    id = "2",
                    storeName = "Alza",
                    dateString = "Dec 27, 10:15 AM",
                    amountFormatted = "1 200 Kč", // CZK example
                    iconId = "tech"
                ),
                TransactionUiData(
                    id = "3",
                    storeName = "Starbucks",
                    dateString = "Dec 26, 9:00 AM",
                    amountFormatted = "$12.00", // USD example
                    iconId = "utilities"
                )
            )
        )

        // 2. Render the "Dumb" View
        DashboardContent(
            state = mockState,
            selectedFilter = "This Week",
            onFilterSelected = {}, // No-op for preview
            onOpenReceipts = {},
            onReceiptClick = {},
            onSettingsClick = {},
            onScanClick = {}
        )
    }
}