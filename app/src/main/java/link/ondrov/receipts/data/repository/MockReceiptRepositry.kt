package link.ondrov.receipts.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import link.ondrov.receipts.data.model.Category
import link.ondrov.receipts.data.model.Receipt
import link.ondrov.receipts.data.model.ReceiptItem
import link.ondrov.receipts.data.model.ReceiptMetadata
import link.ondrov.receipts.data.model.ReceiptWithDetails
import java.util.concurrent.TimeUnit

/**
 * Implementation of the repository that just returns hard-coded mock data.
 * This is used for designing and previewing the app.
 */
class MockReceiptRepository : ReceiptRepository {

    // Helper to get time relative to now
    private val now = System.currentTimeMillis()
    private fun daysAgo(days: Long): Long = now - TimeUnit.DAYS.toMillis(days)

    // 1. Define Categories (Simulating a DB table)
    private val catGroceries = Category("c1", "Groceries", "shopping_cart")
    private val catDining = Category("c2", "Dining", "restaurant")
    private val catTech = Category("c3", "Tech", "tech")
    private val catTransport = Category("c4", "Transport", "bus")

    private val mockCategories = mutableListOf(catGroceries, catDining, catTech, catTransport)

    // 2. Define Mock Data (Changed to MutableList to support delete/save in memory)
    private val mockData = mutableListOf(
        // --- THIS WEEK ---
        ReceiptWithDetails(
            receipt = Receipt(
                id = "1",
                storeName = "SuperMart",
                totalAmount = 45.80,
                dateTimestamp = daysAgo(0),
                currency = "EUR",
                categoryId = "c1",
                imageFileName = "mock_receipt_1.jpg", // NEW Field
                metadata = ReceiptMetadata("Card", "TXN-123") // NEW Field
            ),
            category = catGroceries,
            items = listOf(
                ReceiptItem("2_1", "1", "Mlieko polotučné", 2.0, "ks", 1.98), // Added receiptId "1"
                ReceiptItem("2_2", "1", "Kuracie prsia", 0.8, "kg", 5.20),
                ReceiptItem("2_3", "1", "Syr Eidam", 1.0, "ks", 4.80)
            )
        ),
        ReceiptWithDetails(
            receipt = Receipt(
                id = "2",
                storeName = "Starbucks",
                totalAmount = 12.50,
                dateTimestamp = daysAgo(1),
                currency = "EUR",
                categoryId = "c2",
                imageFileName = "mock_receipt_2.jpg",
                metadata = ReceiptMetadata("Apple Pay", "TXN-456")
            ),
            category = catDining,
            items = emptyList()
        ),
        ReceiptWithDetails(
            receipt = Receipt(
                id = "3",
                storeName = "Uber Trip",
                totalAmount = 24.00,
                dateTimestamp = daysAgo(2),
                currency = "EUR",
                categoryId = "c4",
                imageFileName = "mock_receipt_3.jpg",
                metadata = null
            ),
            category = catTransport,
            items = emptyList()
        ),

        // --- LAST WEEK (For Trend Calculation) ---
        ReceiptWithDetails(
            receipt = Receipt(
                id = "4",
                storeName = "Best Buy",
                totalAmount = 150.00,
                dateTimestamp = daysAgo(8),
                currency = "USD",
                categoryId = "c3",
                imageFileName = "mock_receipt_4.jpg",
                metadata = ReceiptMetadata("Visa", "TXN-789")
            ),
            category = catTech,
            items = emptyList()
        ),
        ReceiptWithDetails(
            receipt = Receipt(
                id = "5",
                storeName = "Whole Foods",
                totalAmount = 85.20,
                dateTimestamp = daysAgo(9),
                currency = "USD",
                categoryId = "c1",
                imageFileName = "mock_receipt_5.jpg",
                metadata = null
            ),
            category = catGroceries,
            items = listOf(
                ReceiptItem("1_1", "5", "Organic Milk", 1.0, "x", 3.50),
                ReceiptItem("1_2", "5", "Whole Wheat Bread", 1.0, "x", 2.75),
                ReceiptItem("1_3", "5", "Apples (Gala)", 2.1, "lbs", 4.18),
                ReceiptItem("1_4", "5", "Free-Range Eggs", 1.0, "x", 5.20),
                ReceiptItem("1_5", "5", "Vanilla Ice Cream", 2.0, "x", 13.00)
            )
        ),

        // --- OLDER ---
        ReceiptWithDetails(
            receipt = Receipt(
                id = "6",
                storeName = "IKEA",
                totalAmount = 210.50,
                dateTimestamp = daysAgo(25),
                currency = "EUR",
                categoryId = null,
                imageFileName = "mock_receipt_6.jpg",
                metadata = null
            ),
            category = null, // Simulating Uncategorized
            items = emptyList()
        )
    )

    override fun getAllReceipts(): Flow<List<ReceiptWithDetails>> {
        // Return the mock list as a Flow
        return flowOf(mockData)
    }

    override fun getReceiptById(id: String): Flow<ReceiptWithDetails?> {
        // Find the receipt in the mock list and return it as a Flow
        return flowOf(mockData.find { it.receipt.id == id })
    }

    // --- NEW: Implement Missing Interface Methods ---

    override suspend fun saveReceipt(receipt: Receipt, items: List<ReceiptItem>) {
        // For Mock: We can add it to the list so the UI updates in Preview
        // Note: In a real mock we might handle categories better, but this is sufficient.
        val existingIndex = mockData.indexOfFirst { it.receipt.id == receipt.id }

        val newEntry = ReceiptWithDetails(
            receipt = receipt,
            category = null, // Mock doesn't strictly handle category assignment logic here
            items = items
        )

        if (existingIndex != -1) {
            mockData[existingIndex] = newEntry
        } else {
            mockData.add(newEntry)
        }
    }

    override suspend fun updateReceipt(receipt: Receipt, items: List<ReceiptItem>) {
        val index = mockData.indexOfFirst { it.receipt.id == receipt.id }
        if (index != -1) {
            val updatedEntry = ReceiptWithDetails(
                receipt = receipt,
                category = mockCategories.find { it.id == receipt.categoryId },
                items = items
            )
            mockData[index] = updatedEntry
        }
    }

    override suspend fun deleteReceipt(receipt: Receipt) {
        // Remove from local list
        mockData.removeAll { it.receipt.id == receipt.id }
    }

    override fun getAllCategories(): Flow<List<Category>> {
        return flowOf(listOf(
            Category("cat_groceries", "Groceries", "shopping_cart"),
            Category("cat_tech", "Tech", "tech")
        ))
    }
}