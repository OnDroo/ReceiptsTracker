package link.ondrov.receipts.data.model

import androidx.room.Embedded
import androidx.room.Relation

data class ReceiptWithDetails(
    @Embedded val receipt: Receipt,

    @Relation(
        parentColumn = "categoryId",
        entityColumn = "id"
    )
    val category: Category?,

    @Relation(
        parentColumn = "id",
        entityColumn = "receiptId"
    )
    val items: List<ReceiptItem>
)