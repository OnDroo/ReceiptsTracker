package link.ondrov.receipts.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import link.ondrov.receipts.data.model.ReceiptWithDetails
import link.ondrov.receipts.data.model.ReceiptItem
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditReceiptScreen(
    originalReceipt: ReceiptWithDetails,
    onDismiss: () -> Unit,
    onSave: (ReceiptWithDetails) -> Unit
) {
    val editState = remember(originalReceipt) { EditReceiptState(originalReceipt) }
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
                    TextButton(onClick = {
                        onSave(editState.toReceiptWithDetails(originalReceipt))
                    }) {
                        Text("Save", fontWeight = FontWeight.Bold)
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
            // --- HEADER ---
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = editState.storeName,
                    onValueChange = { editState.storeName = it },
                    label = { Text("Store Name") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            }

            // --- ITEMS ---
            Column {
                Text("ITEMS", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(8.dp))

                editState.items.forEachIndexed { index, item ->
                    EditItemRow(
                        item = item,
                        onDelete = { editState.items.removeAt(index) },
                        onUpdate = { updated -> editState.items[index] = updated }
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }

                OutlinedButton(
                    onClick = {
                        editState.items.add(ReceiptItem(UUID.randomUUID().toString(), originalReceipt.receipt.id, "", 1.0, "x", 0.0))
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Default.Add, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Add Item")
                }
            }

            // --- TOTALS ---
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("TOTALS", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                OutlinedTextField(
                    value = "${originalReceipt.receipt.currency}${String.format("%.2f", editState.totalAmount)}",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Total") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                )
            }
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
fun EditItemRow(
    item: ReceiptItem,
    onDelete: () -> Unit,
    onUpdate: (ReceiptItem) -> Unit
) {
    var name by remember(item.id) { mutableStateOf(item.name) }
    var priceStr by remember(item.id) { mutableStateOf(item.price.toString()) }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        onUpdate(item.copy(name = it))
                    },
                    label = { Text("Item Name") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                )
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = priceStr,
                    onValueChange = {
                        priceStr = it
                        val newPrice = it.toDoubleOrNull() ?: 0.0
                        onUpdate(item.copy(price = newPrice))
                    },
                    label = { Text("Price") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        }
    }
}