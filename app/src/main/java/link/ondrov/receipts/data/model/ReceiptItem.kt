package link.ondrov.receipts.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "receipt_items",
    foreignKeys = [
        ForeignKey(
            entity = Receipt::class,
            parentColumns = ["id"],
            childColumns = ["receiptId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("receiptId")]
)
data class ReceiptItem(
    @PrimaryKey val id: String,
    val receiptId: String, // REQUIRED: Links this item to a specific Receipt
    val name: String,
    val quantity: Double,
    val unit: String?,
    val price: Double
)