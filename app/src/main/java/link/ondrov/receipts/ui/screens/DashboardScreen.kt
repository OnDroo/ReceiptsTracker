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
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.outlined.GridView
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.net.toUri
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import link.ondrov.receipts.ui.theme.ReceiptsTheme
import link.ondrov.receipts.util.FileUtils
import link.ondrov.receipts.ui.viewmodels.CategoryUiData
import link.ondrov.receipts.ui.viewmodels.DashboardUiState
import link.ondrov.receipts.ui.viewmodels.TransactionUiData

@Composable
fun DashboardScreen(
    onOpenReceipts: () -> Unit,
    onReceiptClick: (String) -> Unit,
    onNewScan: (String, String) -> Unit,
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
    onScanClick: () -> Unit
) {
    Scaffold(
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
            item { TopBarSection() }

            item {
                Text(
                    "Dashboard",
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                )
            }

            item {
                FilterSection(selectedFilter, onFilterSelected)
            }

            item {
                SummaryRow(state.totalSpendingFormatted, state.receiptCount)
            }

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
                        "Transactions ($selectedFilter)",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, "Go to Receipts", tint = MaterialTheme.colorScheme.primary)
                }
            }

            if (state.recentTransactions.isEmpty()) {
                item {
                    Text(
                        "No transactions found.",
                        modifier = Modifier.padding(vertical = 20.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
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
fun TopBarSection() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Outlined.GridView,
            contentDescription = "Menu",
            tint = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.size(28.dp)
        )
        Icon(
            imageVector = Icons.Filled.Notifications,
            contentDescription = "Alerts",
            tint = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.size(28.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterSection(
    selectedFilter: String,
    onFilterSelected: (String) -> Unit
) {
    val filters = listOf("This Week", "This Month", "This Year", "All Time")

    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        items(filters) { filter ->
            val isSelected = filter == selectedFilter
            FilterChip(
                selected = isSelected,
                onClick = { onFilterSelected(filter) },
                label = { Text(filter) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                border = null,
                shape = RoundedCornerShape(50)
            )
        }
    }
}

@Composable
fun SummaryRow(
    totalSpending: String,
    receiptCount: Int
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        StatCard(Modifier.weight(1f), "Total Spending", totalSpending)
        StatCard(Modifier.weight(1f), "Receipts Scanned", receiptCount.toString())
    }
}

@Composable
fun StatCard(modifier: Modifier = Modifier, title: String, value: String) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun SpendingTrendsCard(
    graphData: List<Double>,
    timeLabel: String,
    totalFormatted: String
) {
    val graphColor = MaterialTheme.colorScheme.primary
    val graphFillColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
    val cardBg = MaterialTheme.colorScheme.surfaceContainerHigh
    val textColor = MaterialTheme.colorScheme.onSurfaceVariant

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text("Spending Trends", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface)
            Spacer(modifier = Modifier.height(8.dp))
            Text(totalFormatted, style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface)
            Text(timeLabel, style = MaterialTheme.typography.bodyMedium, color = textColor)
            Spacer(modifier = Modifier.height(24.dp))

            Box(modifier = Modifier.fillMaxWidth().height(120.dp)) {
                if (graphData.all { it == 0.0 }) {
                    Text("No data", Modifier.align(Alignment.Center), style = MaterialTheme.typography.bodySmall)
                } else {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val width = size.width
                        val height = size.height
                        val maxVal = (graphData.maxOrNull() ?: 1.0) * 1.2
                        val safeMax = if (maxVal == 0.0) 1.0 else maxVal
                        val stepX = if (graphData.size > 1) width / (graphData.size - 1) else width
                        val path = Path()

                        val firstY = height - ((graphData.firstOrNull() ?: 0.0) / safeMax * height).toFloat()
                        path.moveTo(0f, firstY)

                        if (graphData.size > 1) {
                            for (i in 0 until graphData.size - 1) {
                                val p1x = i * stepX
                                val p1y = height - ((graphData[i] / safeMax) * height).toFloat()
                                val p2x = (i + 1) * stepX
                                val p2y = height - ((graphData[i + 1] / safeMax) * height).toFloat()
                                val cx1 = (p1x + p2x) / 2
                                path.cubicTo(cx1, p1y, cx1, p2y, p2x, p2y)
                            }
                        } else {
                            path.lineTo(width, firstY)
                        }

                        val fillPath = Path().apply { addPath(path); lineTo(width, height); lineTo(0f, height); close() }
                        drawPath(
                            path = fillPath,
                            brush = Brush.verticalGradient(
                                colors = listOf(graphFillColor, Color.Transparent),
                                startY = 0f,
                                endY = height
                            )
                        )

                        drawPath(
                            path = path,
                            color = graphColor,
                            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                Icon(Icons.Default.DateRange, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.width(4.dp))
                Text(timeLabel, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
fun CategoryBreakdownCard(
    breakdown: List<CategoryUiData>,
    totalFormatted: String
) {
    val themePalette = listOf(
        MaterialTheme.colorScheme.primary,
        MaterialTheme.colorScheme.secondary,
        MaterialTheme.colorScheme.tertiary,
        MaterialTheme.colorScheme.error,
        MaterialTheme.colorScheme.primaryContainer,
        MaterialTheme.colorScheme.secondaryContainer
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text(
                "Spending by Category",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(24.dp))

            if (breakdown.isEmpty()) {
                Text("No spending data.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(140.dp)
                    ) {
                        Canvas(modifier = Modifier.size(140.dp)) {
                            val strokeWidth = 16.dp.toPx()
                            val radius = (size.minDimension - strokeWidth) / 2
                            val circumference = (2 * Math.PI * radius).toFloat()

                            // The rounded cap adds half a stroke width on EACH end.
                            // We need to account for this so colors don't overlap.
                            val strokeCapAngle = (strokeWidth / circumference) * 360f
                            val visualGapAngle = 4f
                            val totalGapAdjustment = strokeCapAngle + visualGapAngle

                            var currentStartAngle = -90f

                            breakdown.forEach { item ->
                                // Convert percentage (0.0 - 1.0) to degrees
                                val sweepAngleFull = item.percentage * 360f

                                // Only draw if the slice is large enough to support the rounded caps and gap
                                if (sweepAngleFull > totalGapAdjustment) {
                                    val drawSweep = sweepAngleFull - totalGapAdjustment

                                    // Offset the start so the gap is centered
                                    val startOffset = (visualGapAngle / 2f) + (strokeCapAngle / 2f)

                                    drawArc(
                                        color = themePalette[item.colorIndex % themePalette.size],
                                        startAngle = currentStartAngle + startOffset,
                                        sweepAngle = drawSweep,
                                        useCenter = false,
                                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                                    )
                                }
                                currentStartAngle += sweepAngleFull
                            }
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Total", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                totalFormatted,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(24.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        breakdown.forEach { item ->
                            LegendItem(
                                color = themePalette[item.colorIndex % themePalette.size],
                                label = item.name,
                                pct = "${(item.percentage * 100).toInt()}%"
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LegendItem(color: Color, label: String, pct: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Text(
            pct,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun TransactionItem(item: TransactionUiData, onClick: () -> Unit) {
    val iconVector = link.ondrov.receipts.ui.utils.IconRegistry.getIcon(item.iconId)
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }.padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(48.dp).clip(CircleShape).background(MaterialTheme.colorScheme.secondaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(iconVector, null, tint = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.size(24.dp))
        }
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(item.storeName, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold), color = MaterialTheme.colorScheme.onSurface)
            Text(item.dateString, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(item.amountFormatted, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface)
    }
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
            onScanClick = {}
        )
    }
}