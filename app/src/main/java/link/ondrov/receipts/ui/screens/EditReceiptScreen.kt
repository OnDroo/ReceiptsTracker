package link.ondrov.receipts.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import link.ondrov.receipts.data.model.ReceiptItem
import link.ondrov.receipts.data.model.ReceiptWithDetails
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditReceiptScreen(
    originalReceipt: ReceiptWithDetails,
    onDismiss: () -> Unit,
    onSave: (ReceiptWithDetails) -> Unit
) {
    // --- FORM STATE ---
    var storeName by remember { mutableStateOf(originalReceipt.receipt.storeName) }
    var currency by remember { mutableStateOf(originalReceipt.receipt.currency) }

    // Date & Time State
    var selectedDateMs by remember { mutableLongStateOf(originalReceipt.receipt.dateTimestamp) }

    // Items State (Mutable List)
    // We clone the list so we don't modify the original until Save is clicked
    val items = remember { mutableStateListOf(*originalReceipt.items.toTypedArray()) }

    val calculatedTotal = items.sumOf { it.price }

    // Picker Dialog States
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    // Formatter
    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }
    val timeFormat = remember { SimpleDateFormat("h:mm a", Locale.getDefault()) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        topBar = {
            TopAppBar(
                title = { Text("Edit Receipt", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Cancel")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
            )
        },
        floatingActionButton = {
            LargeFloatingActionButton(
                onClick = {
                    // SAVE LOGIC
                    val updatedReceipt = originalReceipt.receipt.copy(
                        storeName = storeName,
                        totalAmount = calculatedTotal,
                        currency = currency,
                        dateTimestamp = selectedDateMs
                    )
                    // Pass back the updated object + list
                    onSave(
                        ReceiptWithDetails(
                            receipt = updatedReceipt,
                            category = originalReceipt.category,
                            items = items.toList()
                        )
                    )
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Check, contentDescription = "Save", modifier = Modifier.size(32.dp))
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp),
            contentPadding = PaddingValues(bottom = 100.dp), // Space for FAB
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // --- SECTION 1: HEADER INFO ---
            item {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("General Info", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)

                    // Store Name
                    OutlinedTextField(
                        value = storeName,
                        onValueChange = { storeName = it },
                        label = { Text("Store Name") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next)
                    )

                    // Date & Time Row
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        // Date Picker Trigger
                        PickableField(
                            value = dateFormat.format(Date(selectedDateMs)),
                            label = "Date",
                            icon = { Icon(Icons.Default.CalendarToday, null) },
                            onClick = { showDatePicker = true },
                            modifier = Modifier.weight(1f)
                        )

                        // Time Picker Trigger
                        PickableField(
                            value = timeFormat.format(Date(selectedDateMs)),
                            label = "Time",
                            icon = { Icon(Icons.Default.Schedule, null) },
                            onClick = { showTimePicker = true },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Amount & Currency Row
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = "%.2f".format(calculatedTotal),
                            onValueChange = { },
                            readOnly = true,
                            label = { Text("Total") },
                            modifier = Modifier.weight(2f),
                            shape = RoundedCornerShape(16.dp),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next)
                        )

                        OutlinedTextField(
                            value = currency,
                            onValueChange = { currency = it.uppercase() },
                            label = { Text("Currency") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(16.dp),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters, imeAction = ImeAction.Done)
                        )
                    }
                }
            }

            // --- SECTION 2: ITEMS ---
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Items", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)

                    TextButton(onClick = {
                        // Add empty item
                        items.add(
                            ReceiptItem(
                                id = UUID.randomUUID().toString(),
                                receiptId = originalReceipt.receipt.id,
                                name = "",
                                quantity = 1.0,
                                unit = null,
                                price = 0.0
                            )
                        )
                    }) {
                        Icon(Icons.Default.Add, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Add Item")
                    }
                }
            }

            itemsIndexed(items) { index, item ->
                EditItemRow(
                    item = item,
                    currencySymbol = currency, // Pass symbol for context
                    onUpdate = { updated -> items[index] = updated },
                    onDelete = { items.removeAt(index) }
                )
            }
        }
    }

    // --- DIALOGS ---

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedDateMs
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { newDate ->
                        // Preserve time from old timestamp
                        val oldCal = Calendar.getInstance().apply { timeInMillis = selectedDateMs }
                        val newCal = Calendar.getInstance().apply { timeInMillis = newDate }

                        newCal.set(Calendar.HOUR_OF_DAY, oldCal.get(Calendar.HOUR_OF_DAY))
                        newCal.set(Calendar.MINUTE, oldCal.get(Calendar.MINUTE))

                        selectedDateMs = newCal.timeInMillis
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showTimePicker) {
        val cal = Calendar.getInstance().apply { timeInMillis = selectedDateMs }
        val timePickerState = rememberTimePickerState(
            initialHour = cal.get(Calendar.HOUR_OF_DAY),
            initialMinute = cal.get(Calendar.MINUTE)
        )

        TimePickerDialog(
            onDismiss = { showTimePicker = false },
            onConfirm = {
                cal.set(Calendar.HOUR_OF_DAY, timePickerState.hour)
                cal.set(Calendar.MINUTE, timePickerState.minute)
                selectedDateMs = cal.timeInMillis
                showTimePicker = false
            }
        ) {
            TimePicker(state = timePickerState)
        }
    }
}

// --- SUB-COMPONENTS ---

@Composable
fun EditItemRow(
    item: ReceiptItem,
    currencySymbol: String,
    onUpdate: (ReceiptItem) -> Unit,
    onDelete: () -> Unit
) {
    var name by remember(item.id) { mutableStateOf(item.name) }

    var qtyText by remember(item.id) {
        mutableStateOf(if (item.quantity == 0.0) "" else item.quantity.toString().removeSuffix(".0"))
    }

    var priceText by remember(item.id) {
        mutableStateOf(if (item.price == 0.0) "" else item.price.toString().removeSuffix(".0"))
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Row 1: Name and Delete
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        onUpdate(item.copy(name = it))
                    },
                    placeholder = { Text("Item Name") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = Color.Transparent,
                        focusedBorderColor = MaterialTheme.colorScheme.primary
                    )
                )

                IconButton(onClick = onDelete) {
                    Icon(Icons.Outlined.Delete, "Remove item", tint = MaterialTheme.colorScheme.error)
                }
            }

            HorizontalDivider(Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.surfaceVariant)

            // Row 2: Qty and Price
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                // QUANTITY
                OutlinedTextField(
                    value = qtyText,
                    onValueChange = { newText ->
                        // Allow digits and one dot only
                        if (newText.isEmpty() || newText.matches(Regex("^\\d*\\.?\\d*$"))) {
                            qtyText = newText
                            val doubleVal = newText.toDoubleOrNull() ?: 0.0
                            onUpdate(item.copy(quantity = doubleVal))
                        }
                    },
                    label = { Text("Qty") },
                    placeholder = { Text("0") }, // Placeholder shows when empty
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )

                // PRICE
                OutlinedTextField(
                    value = priceText,
                    onValueChange = { newText ->
                        if (newText.isEmpty() || newText.matches(Regex("^\\d*\\.?\\d*$"))) {
                            priceText = newText
                            val doubleVal = newText.toDoubleOrNull() ?: 0.0
                            onUpdate(item.copy(price = doubleVal))
                        }
                    },
                    label = { Text("Price ($currencySymbol)") },
                    placeholder = { Text("0.00") },
                    modifier = Modifier.weight(1.5f),
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
            }
        }
    }
}

@Composable
fun PickableField(
    value: String,
    label: String,
    icon: @Composable () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = {}, // Read only
        label = { Text(label) },
        readOnly = true,
        trailingIcon = {
            IconButton(onClick = onClick) { icon() }
        },
        modifier = modifier.clickable { onClick() }, // Whole field clickable
        enabled = false, // Disable typing
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
            disabledTextColor = MaterialTheme.colorScheme.onSurface,
            disabledBorderColor = MaterialTheme.colorScheme.outline,
            disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
            disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
    )
}

@Composable
fun TimePickerDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    content: @Composable () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text("OK") }
        },
        text = { content() }
    )
}