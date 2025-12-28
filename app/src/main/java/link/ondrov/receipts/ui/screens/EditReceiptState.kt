package link.ondrov.receipts.ui.screens

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.derivedStateOf
import link.ondrov.receipts.data.model.ReceiptItem
import link.ondrov.receipts.data.model.ReceiptWithDetails

class EditReceiptState(initialData: ReceiptWithDetails) {
    // Immutable ID
    val id = initialData.receipt.id

    // Mutable fields
    var storeName by mutableStateOf(initialData.receipt.storeName)
    // Keep date as Long (timestamp) for logic, bind to UI as formatted string if needed
    var dateTimestamp by mutableStateOf(initialData.receipt.dateTimestamp)

    // Mutable List for editing items
    val items = mutableStateListOf<ReceiptItem>().apply {
        addAll(initialData.items)
    }

    // Auto-calculate total
    val totalAmount by derivedStateOf {
        items.sumOf { it.price }
    }

    // Helper to map back to DB Model
    fun toReceiptWithDetails(original: ReceiptWithDetails): ReceiptWithDetails {
        val updatedReceipt = original.receipt.copy(
            storeName = storeName,
            totalAmount = totalAmount,
            dateTimestamp = dateTimestamp
        )
        return original.copy(
            receipt = updatedReceipt,
            items = items.toList()
        )
    }
}