package link.ondrov.receipts.data.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import link.ondrov.receipts.data.model.Category
import link.ondrov.receipts.data.model.Receipt
import link.ondrov.receipts.data.model.ReceiptItem
import link.ondrov.receipts.data.model.ReceiptWithDetails

@Dao
interface ReceiptDao {
    @Transaction
    @Query("SELECT * FROM receipts ORDER BY dateTimestamp DESC")
    fun getAllReceipts(): Flow<List<ReceiptWithDetails>>

    @Transaction
    @Query("SELECT * FROM receipts WHERE id = :id")
    fun getReceiptById(id: String): Flow<ReceiptWithDetails?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReceipt(receipt: Receipt)

    @Update
    suspend fun updateReceipt(receipt: Receipt)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItems(items: List<ReceiptItem>)

    @Query("DELETE FROM receipt_items WHERE receiptId = :receiptId")
    suspend fun deleteItemsByReceiptId(receiptId: String)

    // Categories
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: Category)

    @Query("SELECT * FROM categories")
    fun getAllCategories(): Flow<List<Category>>

    @Delete
    suspend fun deleteReceipt(receipt: Receipt)

    @Transaction
    suspend fun insertReceiptWithItems(receipt: Receipt, items: List<ReceiptItem>) {
        insertReceipt(receipt)
        insertItems(items)
    }

    @Transaction
    suspend fun replaceItems(receiptId: String, items: List<ReceiptItem>) {
        deleteItemsByReceiptId(receiptId)
        insertItems(items)
    }
}