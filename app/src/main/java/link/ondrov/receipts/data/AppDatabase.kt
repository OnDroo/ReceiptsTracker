package link.ondrov.receipts.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import link.ondrov.receipts.data.dao.ReceiptDao
import link.ondrov.receipts.data.model.Category
import link.ondrov.receipts.data.model.Receipt
import link.ondrov.receipts.data.model.ReceiptItem

@Database(
    entities = [Receipt::class, ReceiptItem::class, Category::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun receiptDao(): ReceiptDao

    companion object {
        @Volatile
        private var Instance: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return Instance ?: synchronized(this) {
                Room.databaseBuilder(context, AppDatabase::class.java, "receipts_db")
                    .fallbackToDestructiveMigration(false)
                    .addCallback(ReceiptDatabaseCallback(context))
                    .build()
                    .also { Instance = it }
            }
        }

        private class ReceiptDatabaseCallback(
            private val context: Context
        ) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                Instance?.let { database ->
                    CoroutineScope(Dispatchers.IO).launch {
                        val dao = database.receiptDao()
                        // Define Default Categories
                        val defaults = listOf(
                            Category("cat_groceries", "Groceries", "shopping_cart"),
                            Category("cat_dining", "Dining Out", "restaurant"),
                            Category("cat_transport", "Transport", "bus"),
                            Category("cat_tech", "Electronics", "tech"),
                            Category("cat_health", "Health", "health"),
                            Category("cat_utilities", "Bills & Utils", "utilities"),
                            Category("cat_shopping", "Shopping", "gift")
                        )
                        defaults.forEach { dao.insertCategory(it) }
                    }
                }
            }
        }
    }
}