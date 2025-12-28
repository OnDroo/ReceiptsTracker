package link.ondrov.receipts.data.model

import android.content.Context
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.File

@Entity(tableName = "receipts")
data class Receipt(
    @PrimaryKey val id: String,
    val storeName: String,
    val totalAmount: Double,
    val dateTimestamp: Long,
    val currency: String,
    val categoryId: String?,
    val imageFileName: String,

    @Embedded(prefix = "meta_")
    val metadata: ReceiptMetadata?
) {
    fun getFile(context: Context): File = File(context.filesDir, imageFileName)
}