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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import link.ondrov.receipts.ui.theme.ReceiptsTheme
import link.ondrov.receipts.data.model.ReceiptWithDetails
import link.ondrov.receipts.ui.viewmodels.DashboardViewModel
import java.util.Calendar
import java.util.Locale
import android.app.Activity
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import link.ondrov.receipts.util.FileUtils
import link.ondrov.receipts.ui.utils.CurrencyUtils

// 1. The "Connector"
@Composable
fun DashboardScreen(
    onOpenReceipts: () -> Unit,
    onReceiptClick: (String) -> Unit,
    onNewScan: (String, String) -> Unit,
    viewModel: DashboardViewModel = viewModel(factory = DashboardViewModel.Factory)
) {
    // Collect the computed/filtered results directly
    val receipts by viewModel.filteredReceipts.collectAsState()
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
        receipts = receipts,
        selectedFilter = currentFilter, // Pass state down
        onFilterSelected = viewModel::onFilterSelected, // Pass event up
        onOpenReceipts = onOpenReceipts,
        onReceiptClick = onReceiptClick, // <--- PASS DOWN
        onScanClick = {
            scanner.getStartScanIntent(context as Activity)
                .addOnSuccessListener { scannerLauncher.launch(IntentSenderRequest.Builder(it).build()) }
        }
    )
}

// 2. The "UI Renderer"
@Composable
fun DashboardContent(
    receipts: List<ReceiptWithDetails>, // Already filtered by ViewModel
    selectedFilter: String,             // Current filter state from ViewModel
    onFilterSelected: (String) -> Unit, // Callback to ViewModel
    onOpenReceipts: () -> Unit,
    onReceiptClick: (String) -> Unit,
    onScanClick: () -> Unit
) {
    val totalSpending = remember(receipts) {
        receipts.sumOf {
            CurrencyUtils.toEur(it.receipt.totalAmount, it.receipt.currency)
        }
    }
    val receiptCount = remember(receipts) { receipts.size }
    val recentTransactions = remember(receipts) { receipts }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        floatingActionButton = {
            LargeFloatingActionButton(
                onClick = onScanClick,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.QrCodeScanner,
                    contentDescription = "Scan Receipt",
                    modifier = Modifier.size(36.dp)
                )
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
                    text = "Dashboard",
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                )
            }

            // 2. Filter Section (Stateless)
            // Just displays the selected string and fires the callback
            item {
                FilterSection(
                    selectedFilter = selectedFilter,
                    onFilterSelected = onFilterSelected
                )
            }

            item { SummaryRow(totalSpending, receiptCount) }

            // 3. Components use the data directly
            item { SpendingTrendsCard(receipts, selectedFilter) }

            item { CategoryBreakdownCard(receipts) }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
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

            if (recentTransactions.isEmpty()) {
                item {
                    Text(
                        "No transactions found for this period.",
                        modifier = Modifier.padding(vertical = 20.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                items(recentTransactions) { receipt ->
                    TransactionItem(
                        details = receipt,
                        onClick = { onReceiptClick(receipt.receipt.id) }
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
    totalSpending: Double,
    receiptCount: Int
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        StatCard(
            modifier = Modifier.weight(1f),
            title = "Total Spending",
            value = CurrencyUtils.formatEur(totalSpending),
        )
        StatCard(
            modifier = Modifier.weight(1f),
            title = "Receipts Scanned",
            value = receiptCount.toString()
        )
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
    receipts: List<ReceiptWithDetails>,
    timeLabel: String
) {
    val graphColor = MaterialTheme.colorScheme.primary
    val graphFillColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
    val cardBg = MaterialTheme.colorScheme.surfaceContainerHigh
    val textColor = MaterialTheme.colorScheme.onSurfaceVariant

    // 1. Determine Date Range and Data Points based on Filter
    val (chartData, dateRangeLabel) = remember(receipts, timeLabel) {
        val cal = Calendar.getInstance()
        val now = System.currentTimeMillis()

        // Helper to format dates
        val dateFormat = java.text.SimpleDateFormat("MMM dd", Locale.US)
        val yearFormat = java.text.SimpleDateFormat("yyyy", Locale.US)

        when (timeLabel) {
            "This Week" -> {
                // Logic: 7 Days (Sun-Sat or Mon-Sun depending on locale, simplifying to last 7 days from filter)
                // Assuming filteredReceipts already contains only this week's data.
                // We map them to 7 buckets (Day of Week)
                val buckets = DoubleArray(7)

                cal.firstDayOfWeek = Calendar.MONDAY
                cal.timeInMillis = now
                cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
                val start = cal.timeInMillis
                cal.add(Calendar.DAY_OF_WEEK, 6)
                val end = cal.timeInMillis

                receipts.forEach {
                    val rCal = Calendar.getInstance()
                    rCal.timeInMillis = it.receipt.dateTimestamp
                    val dayConstant = rCal.get(Calendar.DAY_OF_WEEK) // e.g., Sunday = 1, Monday = 2

                    // 2. LOGIC: Dynamic mapping based on your setting above
                    // If first day is Mon (2) and current day is Mon (2) -> index 0
                    // If first day is Mon (2) and current day is Sun (1) -> index 6
                    val index = (dayConstant - cal.firstDayOfWeek + 7) % 7

                    val amountInEur = CurrencyUtils.toEur(it.receipt.totalAmount, it.receipt.currency)

                    if (index in 0..6) buckets[index] += amountInEur
                }

                Pair(buckets.toList(), "${dateFormat.format(start)} - ${dateFormat.format(end)}")
            }
            "This Month" -> {
                // Logic: Days in Month (1..31)
                cal.timeInMillis = now
                val maxDays = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
                val buckets = DoubleArray(maxDays)

                cal.set(Calendar.DAY_OF_MONTH, 1)
                val start = cal.timeInMillis
                cal.set(Calendar.DAY_OF_MONTH, maxDays)
                val end = cal.timeInMillis

                receipts.forEach {
                    cal.timeInMillis = it.receipt.dateTimestamp
                    val day = cal.get(Calendar.DAY_OF_MONTH) // 1..31
                    if (day - 1 in buckets.indices) buckets[day - 1] += it.receipt.totalAmount
                }

                Pair(buckets.toList(), "${dateFormat.format(start)} - ${dateFormat.format(end)}")
            }
            "This Year" -> {
                // Logic: 12 Months
                val buckets = DoubleArray(12)
                cal.timeInMillis = now
                cal.set(Calendar.DAY_OF_YEAR, 1)
                val start = cal.timeInMillis
                cal.set(Calendar.MONTH, 11)
                cal.set(Calendar.DAY_OF_MONTH, 31)
                cal.timeInMillis

                receipts.forEach {
                    cal.timeInMillis = it.receipt.dateTimestamp
                    val month = cal.get(Calendar.MONTH) // 0..11
                    if (month in 0..11) buckets[month] += it.receipt.totalAmount
                }

                Pair(buckets.toList(), "${yearFormat.format(start)}")
            }
            else -> { // "All Time"
                // Group by Year. Find min and max year in data.
                if (receipts.isEmpty()) {
                    Pair(listOf(0.0), "No Data")
                } else {
                    val minTs = receipts.minOf { it.receipt.dateTimestamp }
                    val maxTs = receipts.maxOf { it.receipt.dateTimestamp }

                    cal.timeInMillis = minTs
                    val minYear = cal.get(Calendar.YEAR)
                    cal.timeInMillis = maxTs
                    val maxYear = cal.get(Calendar.YEAR)

                    val span = maxYear - minYear + 1
                    val buckets = DoubleArray(span) { 0.0 }

                    receipts.forEach {
                        cal.timeInMillis = it.receipt.dateTimestamp
                        val y = cal.get(Calendar.YEAR)
                        buckets[y - minYear] += it.receipt.totalAmount
                    }
                    Pair(buckets.toList(), "${yearFormat.format(minTs)} - ${yearFormat.format(maxTs)}")
                }
            }
        }
    }

    val periodTotal = receipts.sumOf {
        CurrencyUtils.toEur(it.receipt.totalAmount, it.receipt.currency)
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text("Spending Trends", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface)
            Spacer(modifier = Modifier.height(8.dp))

            Text(
                CurrencyUtils.formatEur(periodTotal),
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )

            // Label for the Period
            Text(timeLabel, style = MaterialTheme.typography.bodyMedium, color = textColor)

            Spacer(modifier = Modifier.height(24.dp))

            // --- Dynamic Graph ---
            Box(
                modifier = Modifier.fillMaxWidth().height(120.dp)
            ) {
                if (periodTotal == 0.0) {
                    Text("No data for this period", modifier = Modifier.align(Alignment.Center), style = MaterialTheme.typography.bodySmall)
                } else {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val width = size.width
                        val height = size.height

                        // Avoid flatline if all values are 0 or equal
                        val maxVal = (chartData.maxOrNull() ?: 1.0) * 1.2
                        // Ensure we don't divide by 0 if maxVal is 0
                        val safeMax = if (maxVal == 0.0) 1.0 else maxVal

                        val pointsCount = chartData.size
                        // If we only have 1 point (e.g. 1 year of data), we can't draw a curve.
                        // We need at least 2 points for a line.
                        val stepX = if (pointsCount > 1) width / (pointsCount - 1) else width

                        val path = Path()

                        // Calculate starting Y
                        val firstY = height - ((chartData.firstOrNull() ?: 0.0) / safeMax * height).toFloat()
                        path.moveTo(0f, firstY)

                        if (pointsCount > 1) {
                            for (i in 0 until pointsCount - 1) {
                                val p1x = i * stepX
                                val p1y = height - ((chartData[i] / safeMax) * height).toFloat()
                                val p2x = (i + 1) * stepX
                                val p2y = height - ((chartData[i + 1] / safeMax) * height).toFloat()

                                // Cubic Bezier for smooth curve
                                val cx1 = (p1x + p2x) / 2
                                val cy1 = p1y
                                val cx2 = (p1x + p2x) / 2
                                val cy2 = p2y
                                path.cubicTo(cx1, cy1, cx2, cy2, p2x, p2y)
                            }
                        } else {
                            // Single point case: just draw a line across
                            path.lineTo(width, firstY)
                        }

                        // Draw Fill
                        val fillPath = Path().apply {
                            addPath(path)
                            lineTo(width, height)
                            lineTo(0f, height)
                            close()
                        }

                        drawPath(
                            path = fillPath,
                            brush = Brush.verticalGradient(
                                colors = listOf(graphFillColor, Color.Transparent),
                                startY = 0f,
                                endY = height
                            )
                        )

                        // Draw Stroke
                        drawPath(
                            path = path,
                            color = graphColor,
                            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // --- Date Range Label (Bottom) ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.DateRange, // Requires androidx.compose.material.icons.filled.DateRange
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = dateRangeLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun CategoryBreakdownCard(receipts: List<ReceiptWithDetails>) {
    val totalAmount = receipts.sumOf {
        CurrencyUtils.toEur(it.receipt.totalAmount, it.receipt.currency)
    }
    val themePalette = listOf(
        MaterialTheme.colorScheme.primary,
        MaterialTheme.colorScheme.secondary,
        MaterialTheme.colorScheme.tertiary,
        MaterialTheme.colorScheme.error,
        MaterialTheme.colorScheme.primaryContainer,
        MaterialTheme.colorScheme.secondaryContainer
    )

    val categoryStats = remember(receipts) {
        val grouped = receipts.groupBy { it.category }
        val result = grouped.map { (category, receiptsList) ->
            val sum = receiptsList.sumOf {
                CurrencyUtils.toEur(it.receipt.totalAmount, it.receipt.currency)
            }
            val name = category?.name ?: "Uncategorized"
            Triple(name, sum, Color.Transparent)
        }

        // Sort Descending
        val sorted = result.sortedByDescending { it.second }

        // --- NEW GROUPING LOGIC ---
        // Thick rounded charts need ~6-7% minimum to look good.
        // We group anything smaller than that into "Others".
        val thresholdPercent = 0.07 // 7%
        val minAmount = totalAmount * thresholdPercent

        val (bigCategories, smallCategories) = sorted.partition { it.second >= minAmount }

        val finalData = if (smallCategories.isNotEmpty()) {
            val othersSum = smallCategories.sumOf { it.second }
            // Only add "Others" if it has value
            if (othersSum > 0) {
                bigCategories + Triple("Others", othersSum, Color.Transparent)
            } else {
                bigCategories
            }
        } else {
            sorted
        }

        // Apply Colors
        finalData.mapIndexed { index, (name, sum, _) ->
            val color = themePalette[index % themePalette.size]
            Triple(name, sum, color)
        }
    }

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

            if (totalAmount == 0.0) {
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

                            // The cap adds half a stroke width on EACH end.
                            // Total cap length = 1 stroke width.
                            val strokeCapAngle = (strokeWidth / circumference) * 360f
                            val visualGapAngle = 4f
                            val totalGapAdjustment = strokeCapAngle + visualGapAngle

                            var currentStartAngle = -90f

                            categoryStats.forEach { (_, amount, color) ->
                                val sweepAngleFull = ((amount / totalAmount) * 360f).toFloat()

                                // Only draw if the slice is visually large enough to support the rounded caps
                                if (sweepAngleFull > totalGapAdjustment) {
                                    val drawSweep = sweepAngleFull - totalGapAdjustment
                                    val startOffset = (visualGapAngle / 2f) + (strokeCapAngle / 2f)

                                    drawArc(
                                        color = color,
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
                                CurrencyUtils.formatEur(totalAmount),                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(24.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        categoryStats.forEach { (name, amount, color) ->
                            val percentage = (amount / totalAmount) * 100
                            // Round to 1 decimal place if needed, or Int
                            val pctString = if (percentage < 1) "<1%" else "${percentage.toInt()}%"
                            LegendItem(
                                color = color,
                                label = name,
                                pct = pctString
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
fun TransactionItem(
    details: ReceiptWithDetails,
    onClick: () -> Unit
) {
    val dateString = remember(details.receipt.dateTimestamp) {
        java.text.SimpleDateFormat("MMM dd, h:mm a", Locale.getDefault())
            .format(java.util.Date(details.receipt.dateTimestamp))
    }

    val iconVector = link.ondrov.receipts.ui.utils.IconRegistry.getIcon(details.category?.iconId ?: "qr_code")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.secondaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = iconVector,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = details.receipt.storeName,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = dateString,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Text(
            text = "${details.receipt.totalAmount} ${details.receipt.currency}",
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Preview(showBackground = true)
@Composable
fun DashboardPreview() {
    // 1. Mock Categories
    val previewCatGroceries = link.ondrov.receipts.data.model.Category("c1", "Groceries", "shopping_cart")
    val previewCatTech = link.ondrov.receipts.data.model.Category("c2", "Tech", "tech")
    val previewCatRandom = link.ondrov.receipts.data.model.Category("c3", "Random", "utilities")

    // 2. Mock Data
    // We use consistent offsets to ensure data appears in specific ranges
    val now = System.currentTimeMillis()
    val dayMillis = 24 * 60 * 60 * 1000L

    val previewData = listOf(
        // Today
        ReceiptWithDetails(
            receipt = link.ondrov.receipts.data.model.Receipt("1", "Preview Mart", 120.00, now, "USD", "c1", "img1", null),
            category = previewCatGroceries,
            items = emptyList()
        ),
        // 15 days ago (e.g., Nov 22) - Should NOT show in "This Week" or "This Month" (if Dec)
        ReceiptWithDetails(
            receipt = link.ondrov.receipts.data.model.Receipt("2", "Tech Store", 450.00, now - (15 * dayMillis), "USD", "c2", "img2", null),
            category = previewCatTech,
            items = emptyList()
        ),
        // 2 days ago (Shows in "This Week")
        ReceiptWithDetails(
            receipt = link.ondrov.receipts.data.model.Receipt("3", "Alza", 35.00, now - (2 * dayMillis), "USD", "c3", "img3", null),
            category = previewCatRandom,
            items = emptyList()
        )
    )

    ReceiptsTheme {
        // 3. State Wrapper for Preview
        // We replicate the filtering logic LOCALLY just for the preview
        val (selectedFilter, setFilter) = remember { mutableStateOf("This Week") }

        val filteredPreviewData = remember(selectedFilter) {
            val cal = Calendar.getInstance()
            cal.firstDayOfWeek = Calendar.MONDAY

            // Reset to start of today
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)

            when (selectedFilter) {
                "This Week" -> cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
                "This Month" -> cal.set(Calendar.DAY_OF_MONTH, 1)
                "This Year" -> cal.set(Calendar.DAY_OF_YEAR, 1)
                // "All Time" doesn't need a start time
            }

            val startTime = if (selectedFilter == "All Time") 0L else cal.timeInMillis
            previewData.filter { it.receipt.dateTimestamp >= startTime }
        }

        DashboardContent(
            receipts = filteredPreviewData, // Pass the filtered list!
            selectedFilter = selectedFilter,
            onFilterSelected = setFilter,
            onOpenReceipts = {},
            onReceiptClick = {},
            onScanClick = {}
        )
    }
}