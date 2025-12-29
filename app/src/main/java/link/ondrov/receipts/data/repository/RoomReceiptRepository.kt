package link.ondrov.receipts.data.repository

import kotlinx.coroutines.flow.Flow
import link.ondrov.receipts.data.dao.ReceiptDao
import link.ondrov.receipts.data.model.Category
import link.ondrov.receipts.data.model.Receipt
import link.ondrov.receipts.data.model.ReceiptItem
import link.ondrov.receipts.data.model.ReceiptWithDetails

class RoomReceiptRepository(private val receiptDao: ReceiptDao) : ReceiptRepository {
    override fun getAllReceipts(): Flow<List<ReceiptWithDetails>> {
        return receiptDao.getAllReceipts()
    }

    override fun getReceiptById(id: String): Flow<ReceiptWithDetails?> {
        return receiptDao.getReceiptById(id)
    }

    override suspend fun saveReceipt(receipt: Receipt, items: List<ReceiptItem>) {
        receiptDao.insertReceiptWithItems(receipt, items)
    }

    override suspend fun updateReceipt(receipt: Receipt, items: List<ReceiptItem>) {
        receiptDao.updateReceipt(receipt)
        receiptDao.replaceItems(receipt.id, items)
    }

    override suspend fun deleteReceipt(receipt: Receipt) {
        receiptDao.deleteReceipt(receipt)
    }

    override fun getAllCategories(): Flow<List<Category>> {
        return receiptDao.getAllCategories()
    }
}