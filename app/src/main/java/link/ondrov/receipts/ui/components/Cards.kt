package link.ondrov.receipts.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import link.ondrov.receipts.ui.viewmodels.CategoryUiData

// --- SUMMARY ROW ---
@Composable
fun DashboardSummary(
    totalSpending: String,
    receiptCount: Int,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        StatCard(Modifier.weight(1f), "Total Spending", totalSpending, onClick = onClick)
        StatCard(Modifier.weight(1f), "Receipts Scanned", receiptCount.toString(), onClick = onClick)
    }
}

@Composable
fun StatCard(modifier: Modifier = Modifier, title: String, value: String, onClick: () -> Unit) {
    Card(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .clickable { onClick() },
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

// --- TRENDS CHART ---
@Composable
fun SpendingTrendsCard(
    graphData: List<Double>,
    timeLabel: String,
    totalFormatted: String,
    onClick: () -> Unit
) {
    val graphColor = MaterialTheme.colorScheme.primary
    val graphFillColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
    val cardBg = MaterialTheme.colorScheme.surfaceContainerHigh
    val textColor = MaterialTheme.colorScheme.onSurfaceVariant

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp)) // Clip shape for ripple effect
            .clickable { onClick() },
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
                        drawTrendChart(graphData, graphColor, graphFillColor)
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

// --- PIE CHART ---
@Composable
fun CategoryBreakdownCard(
    breakdown: List<CategoryUiData>,
    totalFormatted: String,
    onClick: () -> Unit
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
        modifier = Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text("Spending by Category", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface)
            Spacer(modifier = Modifier.height(24.dp))

            if (breakdown.isEmpty()) {
                Text("No spending data.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(140.dp)) {
                        Canvas(modifier = Modifier.size(140.dp)) {
                            drawPieChart(breakdown, themePalette)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Total", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(totalFormatted, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
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
            Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(color))
            Spacer(modifier = Modifier.width(8.dp))
            Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
        }
        Text(pct, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
    }
}

// --- DRAWING LOGIC (Internal to Cards) ---

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawTrendChart(
    data: List<Double>,
    color: Color,
    fillColor: Color
) {
    val width = size.width
    val height = size.height
    val maxVal = (data.maxOrNull() ?: 1.0) * 1.2
    val safeMax = if (maxVal == 0.0) 1.0 else maxVal
    val stepX = if (data.size > 1) width / (data.size - 1) else width
    val path = Path()

    val firstY = height - ((data.firstOrNull() ?: 0.0) / safeMax * height).toFloat()
    path.moveTo(0f, firstY)

    if (data.size > 1) {
        for (i in 0 until data.size - 1) {
            val p1x = i * stepX
            val p1y = height - ((data[i] / safeMax) * height).toFloat()
            val p2x = (i + 1) * stepX
            val p2y = height - ((data[i + 1] / safeMax) * height).toFloat()
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
            colors = listOf(fillColor, Color.Transparent),
            startY = 0f,
            endY = height
        )
    )

    drawPath(
        path = path,
        color = color,
        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
    )
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawPieChart(
    breakdown: List<CategoryUiData>,
    palette: List<Color>
) {
    val strokeWidth = 16.dp.toPx()
    val radius = (size.minDimension - strokeWidth) / 2
    val circumference = (2 * Math.PI * radius).toFloat()
    val strokeCapAngle = (strokeWidth / circumference) * 360f
    val visualGapAngle = 4f
    val totalGapAdjustment = strokeCapAngle + visualGapAngle

    var currentStartAngle = -90f

    breakdown.forEach { item ->
        val sweepAngleFull = item.percentage * 360f
        if (sweepAngleFull > totalGapAdjustment) {
            val drawSweep = sweepAngleFull - totalGapAdjustment
            val startOffset = (visualGapAngle / 2f) + (strokeCapAngle / 2f)

            drawArc(
                color = palette[item.colorIndex % palette.size],
                startAngle = currentStartAngle + startOffset,
                sweepAngle = drawSweep,
                useCenter = false,
                style = Stroke(strokeWidth, cap = StrokeCap.Round)
            )
        }
        currentStartAngle += sweepAngleFull
    }
}