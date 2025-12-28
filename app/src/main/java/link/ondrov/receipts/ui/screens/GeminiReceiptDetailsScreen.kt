package link.ondrov.receipts.ui

import android.os.Build
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.UUID

// ================= THEME SETUP =================
// (Included here for standalone copy-paste ability, or use your existing theme file)
@Composable
fun ReceiptAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> darkColorScheme()
        else -> lightColorScheme()
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}

// ================= MAIN NAVIGATOR =================

@Composable
fun ReceiptFlowNavigator() {
    // Simulating navigation state
    var isEditing by remember { mutableStateOf(false) }
    // The Single Source of Truth for the receipt data
    var receiptData by remember { mutableStateOf(getMockDetailedReceipt()) }

    if (isEditing) {
        EditReceiptScreen(
            receipt = receiptData,
            onDismiss = { isEditing = false },
            onSave = { updatedReceipt ->
                receiptData = updatedReceipt
                isEditing = false
            }
        )
    } else {
        ReceiptDetailsScreen(
            receipt = receiptData,
            onBack = { /* Handle Back */ },
            onEdit = { isEditing = true },
            onDelete = { /* Handle Delete */ }
        )
    }
}

// ================= SCREEN 1: RECEIPT DETAILS (VIEW) =================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReceiptDetailsScreen(
    receipt: ReceiptDetailed,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Items, 1 = Raw Image

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        topBar = {
            TopAppBar(
                title = { Text("Receipt Details", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { /* Menu */ }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
            )
        },
        floatingActionButton = {
            // Stacked FABs
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                ExtendedFloatingActionButton(
                    onClick = onDelete,
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Delete")
                }

                LargeFloatingActionButton(
                    onClick = onEdit,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(32.dp))
                }
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // 1. Header Card
            item { ReceiptHeaderCard(receipt) }

            // 2. Segmented Control
            item {
                SegmentedControl(
                    options = listOf("Items", "Raw Image"),
                    selectedIndex = selectedTab,
                    onOptionSelected = { selectedTab = it }
                )
            }

            // 3. Content Area
            item {
                Crossfade(targetState = selectedTab, label = "TabSwitch") { index ->
                    if (index == 0) {
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            Text(
                                "Purchased Items",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            receipt.items.forEach { item ->
                                ReceiptItemRowDisplay(item)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            MetadataCard()
                        }
                    } else {
                        ScannedReceiptView()
                    }
                }
            }
            // Spacer for FAB
            item { Spacer(modifier = Modifier.height(140.dp)) }
        }
    }
}

// --- Details Components ---

@Composable
fun ReceiptHeaderCard(receipt: ReceiptDetailed) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
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
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Outlined.Storefront,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(receipt.storeName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("${receipt.date}, ${receipt.time}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "$${String.format("%.2f", receipt.grandTotal)}",
                style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text("Total Amount", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun SegmentedControl(options: List<String>, selectedIndex: Int, onOptionSelected: (Int) -> Unit) {
    val backgroundColor = MaterialTheme.colorScheme.surfaceContainerHighest
    val selectedColor = MaterialTheme.colorScheme.primary
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .clip(RoundedCornerShape(50))
            .background(backgroundColor)
            .padding(4.dp),
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

@Composable
fun ReceiptItemRowDisplay(item: ReceiptItemData) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Dynamic Icon Logic
        val icon = when {
            item.name.contains("Milk", true) || item.name.contains("Ice Cream", true) -> Icons.Outlined.Icecream
            item.name.contains("Bread", true) -> Icons.Outlined.BakeryDining
            item.name.contains("Apple", true) || item.name.contains("Banana", true) -> Icons.Outlined.Eco
            item.name.contains("Egg", true) -> Icons.Outlined.Egg
            else -> Icons.Outlined.ShoppingBag
        }

        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(24.dp))
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(item.name, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium))
            Text("${item.quantity} x $${item.unitPrice}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text("$${String.format("%.2f", item.totalPrice)}", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold))
    }
}

@Composable
fun MetadataCard() {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Metadata", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Payment Method", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Visa **** 1234", style = MaterialTheme.typography.bodyMedium)
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Transaction ID", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("txn_1a2b3c4d5e", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

@Composable
fun ScannedReceiptView() {
    Card(
        modifier = Modifier.fillMaxWidth().height(400.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val stroke = Stroke(width = 4.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(20f, 20f), 0f))
                drawRect(color = Color.Gray.copy(alpha = 0.3f), style = stroke)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.Receipt, "Receipt Image", modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(16.dp))
                Text("Scanned Receipt Image", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
            }
        }
    }
}

// ================= SCREEN 2: EDIT RECEIPT (EDIT) =================

class EditReceiptState(initialReceipt: ReceiptDetailed) {
    var storeName by mutableStateOf(initialReceipt.storeName)
    var storeAddress by mutableStateOf(initialReceipt.storeAddress)
    var date by mutableStateOf(initialReceipt.date)
    var time by mutableStateOf(initialReceipt.time)
    val items = mutableStateListOf<ReceiptItemData>().apply { addAll(initialReceipt.items.map { it.copy() }) }
    var taxRate by mutableStateOf(initialReceipt.taxRate)

    val subtotal by derivedStateOf { items.sumOf { it.quantity * it.unitPrice } }
    val taxAmount by derivedStateOf { subtotal * taxRate }
    val grandTotal by derivedStateOf { subtotal + taxAmount }

    fun toReceiptDetailed(): ReceiptDetailed {
        return ReceiptDetailed(storeName, storeAddress, date, time, items.toList(), taxRate)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditReceiptScreen(
    receipt: ReceiptDetailed,
    onDismiss: () -> Unit,
    onSave: (ReceiptDetailed) -> Unit
) {
    val editState = remember { EditReceiptState(receipt) }
    val scrollState = rememberScrollState()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Edit Receipt") },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                },
                actions = {
                    Button(onClick = { onSave(editState.toReceiptDetailed()) }, modifier = Modifier.padding(end = 8.dp)) {
                        Text("Save")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Edit Header
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = editState.storeName, onValueChange = { editState.storeName = it },
                    label = { Text("Store Name") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)
                )
                OutlinedTextField(
                    value = editState.storeAddress, onValueChange = { editState.storeAddress = it },
                    label = { Text("Store Address") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)
                )
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    OutlinedTextField(
                        value = editState.date, onValueChange = { editState.date = it },
                        label = { Text("Date") }, modifier = Modifier.weight(1f), trailingIcon = { Icon(Icons.Default.CalendarToday, null) }, shape = RoundedCornerShape(12.dp)
                    )
                    OutlinedTextField(
                        value = editState.time, onValueChange = { editState.time = it },
                        label = { Text("Time") }, modifier = Modifier.weight(1f), trailingIcon = { Icon(Icons.Default.Schedule, null) }, shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            // Edit Items
            Column {
                Text("ITEMS", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(bottom = 8.dp))
                editState.items.forEachIndexed { index, item ->
                    EditItemRow(item = item, onDelete = { editState.items.removeAt(index) })
                    Spacer(modifier = Modifier.height(16.dp))
                }
                OutlinedButton(
                    onClick = { editState.items.add(ReceiptItemData(name = "", quantity = 1, unitPrice = 0.0)) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(50),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Default.Add, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Add Item")
                }
            }

            // Edit Totals
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("TOTALS", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                ReadOnlyField("Subtotal", editState.subtotal)
                ReadOnlyField("Tax (8%)", editState.taxAmount)
                OutlinedTextField(
                    value = "$${String.format("%.2f", editState.grandTotal)}",
                    onValueChange = {}, readOnly = true, label = { Text("Total") },
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary, unfocusedBorderColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(16.dp)
                )
            }
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
fun EditItemRow(item: ReceiptItemData, onDelete: () -> Unit) {
    var quantityStr by remember(item.id) { mutableStateOf(item.quantity.toString()) }
    var priceStr by remember(item.id) { mutableStateOf(String.format("%.2f", item.unitPrice)) }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = item.name, onValueChange = { item.name = it },
                    label = { Text("Item Name") }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp)
                )
                IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = quantityStr,
                    onValueChange = { if (it.isEmpty() || it.all { char -> char.isDigit() }) { quantityStr = it; item.quantity = it.toIntOrNull() ?: 0 } },
                    label = { Text("Qty") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), shape = RoundedCornerShape(12.dp)
                )
                OutlinedTextField(
                    value = priceStr,
                    onValueChange = { if (it.isEmpty() || it.count { c -> c == '.' } <= 1 && it.replace(".", "").all { c -> c.isDigit() }) { priceStr = it; item.unitPrice = it.toDoubleOrNull() ?: 0.0 } },
                    label = { Text("Price") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), shape = RoundedCornerShape(12.dp), prefix = { Text("$") }
                )
            }
        }
    }
}

@Composable
fun ReadOnlyField(label: String, value: Double) {
    OutlinedTextField(
        value = "$${String.format("%.2f", value)}", onValueChange = {}, readOnly = true, label = { Text(label) },
        modifier = Modifier.fillMaxWidth(), enabled = false, shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(disabledTextColor = MaterialTheme.colorScheme.onSurface, disabledBorderColor = MaterialTheme.colorScheme.outline)
    )
}

// ================= DATA MODELS =================

data class ReceiptItemData(
    val id: String = UUID.randomUUID().toString(),
    var name: String,
    var quantity: Int,
    var unitPrice: Double
) {
    val totalPrice: Double get() = quantity * unitPrice
}

data class ReceiptDetailed(
    val storeName: String,
    val storeAddress: String,
    val date: String,
    val time: String,
    val items: List<ReceiptItemData>,
    val taxRate: Double = 0.08 // 8% tax
) {
    val subtotal: Double get() = items.sumOf { it.totalPrice }
    val taxAmount: Double get() = subtotal * taxRate
    val grandTotal: Double get() = subtotal + taxAmount
}

fun getMockDetailedReceipt(): ReceiptDetailed {
    return ReceiptDetailed(
        storeName = "SuperMart",
        storeAddress = "123 Market St, Anytown",
        date = "Oct 26, 2023",
        time = "2:15 PM",
        items = listOf(
            ReceiptItemData(name = "Organic Milk", quantity = 1, unitPrice = 3.50),
            ReceiptItemData(name = "Whole Wheat Bread", quantity = 1, unitPrice = 2.75),
            ReceiptItemData(name = "Apples (Gala)", quantity = 2, unitPrice = 1.99),
            ReceiptItemData(name = "Free-Range Eggs", quantity = 1, unitPrice = 5.20),
            ReceiptItemData(name = "Vanilla Ice Cream", quantity = 2, unitPrice = 6.50),
        )
    )
}

// ================= PREVIEW =================

@Preview(showBackground = true)
@Composable
fun FullFlowPreview() {
    ReceiptAppTheme {
        ReceiptFlowNavigator()
    }
}