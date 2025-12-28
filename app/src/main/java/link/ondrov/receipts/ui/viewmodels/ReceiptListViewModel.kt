package link.ondrov.receipts.ui.viewmodels

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import link.ondrov.receipts.ReceiptApplication
import link.ondrov.receipts.data.model.Category
import link.ondrov.receipts.data.repository.ReceiptRepository
import link.ondrov.receipts.ui.utils.CurrencyUtils
import link.ondrov.receipts.util.DateUtils
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class ReceiptListUiState(
    val isLoading: Boolean = true,
    val groupedReceipts: Map<String, List<TransactionUiData>> = emptyMap(),
    val totalResultsCount: Int = 0,
    val availableCategories: List<Category> = emptyList()
)

class ReceiptListViewModel(
    private val application: Application,
    private val repository: ReceiptRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery

    private val _selectedCategory = MutableStateFlow<String?>(null)
    val selectedCategory = _selectedCategory

    // 1. Load Real Data
    private val _allReceipts = repository.getAllReceipts()
    private val _allCategories = repository.getAllCategories()

    val uiState: StateFlow<ReceiptListUiState> = combine(
        _allReceipts,
        _allCategories,
        _searchQuery,
        _selectedCategory
    ) { receipts, categories, query, categoryId ->

        // 2. Updated Search Logic (Store Name OR Item Name)
        val filtered = receipts.filter { receiptWithDetails ->
            val matchesSearch = if (query.isBlank()) true else {
                // Check Store Name
                receiptWithDetails.receipt.storeName.contains(query, ignoreCase = true) ||
                        // Check ANY Item Name
                        receiptWithDetails.items.any { it.name.contains(query, ignoreCase = true) }
            }

            // 3. Category Logic (Compare IDs)
            val matchesCategory = categoryId == null || receiptWithDetails.category?.id == categoryId

            matchesSearch && matchesCategory
        }

        // Sort & Group
        val sorted = filtered.sortedByDescending { it.receipt.dateTimestamp }
        val grouped = sorted.groupBy { item ->
            SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(Date(item.receipt.dateTimestamp))
        }.mapValues { (_, list) ->
            list.map { item ->

                val hits = if (query.isNotBlank()) {
                    item.items.filter {
                        it.name.contains(query, ignoreCase = true)
                    }.map { matchedItem ->
                        // Format: "Milk (2x)" or just "Milk"
                        if (matchedItem.quantity > 1.0) {
                            "${matchedItem.name} (${matchedItem.quantity.toInt()}x)"
                        } else {
                            matchedItem.name
                        }
                    }
                } else {
                    emptyList()
                }

                TransactionUiData(
                    id = item.receipt.id,
                    storeName = item.receipt.storeName,
                    dateString = DateUtils.formatDateTime(application, item.receipt.dateTimestamp),
                    amountFormatted = CurrencyUtils.format(item.receipt.totalAmount, item.receipt.currency),
                    iconId = item.category?.iconId ?: "qr_code",
                    matchedItems = hits
                )
            }
        }

        ReceiptListUiState(
            isLoading = false,
            groupedReceipts = grouped,
            totalResultsCount = filtered.size,
            availableCategories = categories
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ReceiptListUiState())

    fun onSearchQueryChange(newQuery: String) {
        _searchQuery.value = newQuery
    }

    fun onCategoryFilterSelect(categoryId: String?) {
        _selectedCategory.value = if (_selectedCategory.value == categoryId) null else categoryId
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = (this[APPLICATION_KEY] as ReceiptApplication)

                ReceiptListViewModel(
                    app,
                    app.container.receiptRepository
                )
            }
        }
    }
}