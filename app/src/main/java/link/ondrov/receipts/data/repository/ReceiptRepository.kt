package link.ondrov.receipts.data.repository

import link.ondrov.receipts.data.model.ReceiptWithDetails
import kotlinx.coroutines.flow.Flow
import link.ondrov.receipts.data.model.Category
import link.ondrov.receipts.data.model.Receipt
import link.ondrov.receipts.data.model.ReceiptItem

/**
 * Interface defining data functions.
 * This acts as the "contract" for your ViewModels.
 */
interface ReceiptRepository {
    // Basic Receipt CRUD
    fun getAllReceipts(): Flow<List<ReceiptWithDetails>>
    fun getReceiptById(id: String): Flow<ReceiptWithDetails?>

    // Create
    suspend fun saveReceipt(receipt: Receipt, items: List<ReceiptItem>)

    // Update
    suspend fun updateReceipt(receipt: Receipt, items: List<ReceiptItem>)

    // Delete
    suspend fun deleteReceipt(receipt: Receipt)

    // Category Management
    fun getAllCategories(): Flow<List<Category>>
}