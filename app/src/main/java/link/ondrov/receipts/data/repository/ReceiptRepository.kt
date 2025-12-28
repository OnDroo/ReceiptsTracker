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

    /**
     * Get all receipts as a Flow, so the UI can update automatically.
     */
    fun getAllReceipts(): Flow<List<ReceiptWithDetails>>

    /**
     * Get a single receipt by its ID as a Flow.
     */
    fun getReceiptById(id: String): Flow<ReceiptWithDetails?>

    suspend fun saveReceipt(receipt: Receipt, items: List<ReceiptItem>)
    suspend fun deleteReceipt(receipt: Receipt)
    fun getAllCategories(): Flow<List<Category>>
}