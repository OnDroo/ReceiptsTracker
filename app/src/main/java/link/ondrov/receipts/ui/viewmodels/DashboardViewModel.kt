package link.ondrov.receipts.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import link.ondrov.receipts.ReceiptApplication
import link.ondrov.receipts.data.model.ReceiptWithDetails
import link.ondrov.receipts.data.repository.ReceiptRepository
import java.util.Calendar

class DashboardViewModel(
    private val repository: ReceiptRepository
) : ViewModel() {

    // 1. Raw Data from Repo
    private val _allReceipts = MutableStateFlow<List<ReceiptWithDetails>>(emptyList())

    // 2. The Filter State (Default: This Week)
    private val _selectedFilter = MutableStateFlow("This Week")
    val selectedFilter: StateFlow<String> = _selectedFilter.asStateFlow()

    // 3. The Resulting State (Combined)
    // We use combine so whenever receipts OR filter changes, this updates automatically.
    val filteredReceipts: StateFlow<List<ReceiptWithDetails>> = combine(
        _allReceipts,
        _selectedFilter
    ) { receipts, filter ->
        filterReceipts(receipts, filter)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        loadReceipts()
    }

    private fun loadReceipts() {
        viewModelScope.launch {
            repository.getAllReceipts().collect { _allReceipts.value = it }
        }
    }

    // 4. Intent: Function for UI to call
    fun onFilterSelected(newFilter: String) {
        _selectedFilter.value = newFilter
    }

    // 5. The Logic (Moved from UI to here)
    private fun filterReceipts(list: List<ReceiptWithDetails>, filter: String): List<ReceiptWithDetails> {
        if (filter == "All Time") return list

        val cal = Calendar.getInstance()
        // Reset to start of day
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)

        when (filter) {
            "This Week" -> {
                cal.firstDayOfWeek = Calendar.MONDAY
                cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            }
            "This Month" -> cal.set(Calendar.DAY_OF_MONTH, 1)
            "This Year" -> cal.set(Calendar.DAY_OF_YEAR, 1)
        }

        val startTime = cal.timeInMillis
        return list.filter { it.receipt.dateTimestamp >= startTime }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = (this[APPLICATION_KEY] as ReceiptApplication)
                val repository = application.container.receiptRepository
                DashboardViewModel(repository)
            }
        }
    }
}