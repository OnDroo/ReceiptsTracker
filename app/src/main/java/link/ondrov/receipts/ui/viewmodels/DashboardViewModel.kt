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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import link.ondrov.receipts.ReceiptApplication
import link.ondrov.receipts.data.model.ReceiptWithDetails
import link.ondrov.receipts.data.repository.ReceiptRepository
import link.ondrov.receipts.ui.utils.CurrencyUtils
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

data class DashboardUiState(
    val isLoading: Boolean = true,
    val currencySymbol: String = "€",
    val totalSpendingFormatted: String = "€0.00",
    val receiptCount: Int = 0,

    // Graph Data
    val graphData: List<Double> = emptyList(),
    val dateRangeLabel: String = "",

    // Pie Chart Data
    val categoryBreakdown: List<CategoryUiData> = emptyList(),

    // List Data
    val recentTransactions: List<TransactionUiData> = emptyList()
)

data class CategoryUiData(
    val name: String,
    val totalFormatted: String,
    val percentage: Float, // 0.0 to 1.0 (for drawing arcs)
    val colorIndex: Int    // UI maps this int to a Color from its theme
)

data class TransactionUiData(
    val id: String,
    val storeName: String,
    val dateString: String,
    val amountFormatted: String,
    val iconId: String
)

class DashboardViewModel(
    private val repository: ReceiptRepository
) : ViewModel() {
    private val _allReceipts = MutableStateFlow<List<ReceiptWithDetails>>(emptyList())
    private val _selectedFilter = MutableStateFlow("This Week")

    private val _targetCurrency = MutableStateFlow("EUR")

    val uiState: StateFlow<DashboardUiState> = combine(
        _allReceipts,
        _selectedFilter,
        _targetCurrency
    ) { receipts, filter, targetCurrency ->
        generateUiState(receipts, filter, targetCurrency)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardUiState())

    val selectedFilter: StateFlow<String> = _selectedFilter

    init {
        loadReceipts()
    }

    private fun loadReceipts() {
        viewModelScope.launch {
            repository.getAllReceipts().collect { _allReceipts.value = it }
        }
    }

    fun onFilterSelected(newFilter: String) {
        _selectedFilter.value = newFilter
    }

    private fun generateUiState(
        allReceipts: List<ReceiptWithDetails>,
        filter: String,
        targetCurrency: String
    ): DashboardUiState {
        // 1. Filter by Date
        val filtered = filterReceipts(allReceipts, filter)

        // 2. Calculate Totals (Normalized to Target Currency)
        val totalAmount = filtered.sumOf {
            CurrencyUtils.toEur(it.receipt.totalAmount, it.receipt.currency)
        }
        val formattedTotal = CurrencyUtils.formatEur(totalAmount)

        // 3. Prepare Graph Data
        val (graphData, rangeLabel) = calculateGraphData(filtered, filter)

        // 4. Prepare Category Data
        val categoryData = calculateCategoryData(filtered, totalAmount)

        // 5. Prepare Recent Transactions
        val recentList = filtered.map { item ->
            val amount = CurrencyUtils.toEur(item.receipt.totalAmount, item.receipt.currency)
            TransactionUiData(
                id = item.receipt.id,
                storeName = item.receipt.storeName,
                dateString = SimpleDateFormat("MMM dd, h:mm a", Locale.getDefault())
                    .format(java.util.Date(item.receipt.dateTimestamp)),
                amountFormatted = CurrencyUtils.format(
                    item.receipt.totalAmount,
                    item.receipt.currency
                ),
                iconId = item.category?.iconId ?: "qr_code"
            )
        }

        return DashboardUiState(
            isLoading = false,
            currencySymbol = if (targetCurrency == "EUR") "€" else "$",
            totalSpendingFormatted = formattedTotal,
            receiptCount = filtered.size,
            dateRangeLabel = rangeLabel,
            graphData = graphData,
            categoryBreakdown = categoryData,
            recentTransactions = recentList
        )
    }

    private fun filterReceipts(list: List<ReceiptWithDetails>, filter: String): List<ReceiptWithDetails> {
        if (filter == "All Time") return list
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0); cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)

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

    private fun calculateGraphData(receipts: List<ReceiptWithDetails>, filter: String): Pair<List<Double>, String> {
        val cal = Calendar.getInstance()
        val now = System.currentTimeMillis()
        val dateFormat = SimpleDateFormat("MMM dd", Locale.US)
        val yearFormat = SimpleDateFormat("yyyy", Locale.US)

        // 1. Handle Empty Case immediately
        if (receipts.isEmpty()) {
            return Pair(listOf(0.0), "No Data")
        }

        return when (filter) {
            "This Week" -> {
                val buckets = DoubleArray(7)
                cal.firstDayOfWeek = Calendar.MONDAY
                cal.timeInMillis = now
                cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
                val start = cal.timeInMillis
                cal.add(Calendar.DAY_OF_WEEK, 6)
                val end = cal.timeInMillis

                receipts.forEach {
                    val rCal = Calendar.getInstance()
                    rCal.timeInMillis = it.receipt.dateTimestamp
                    val dayConstant = rCal.get(Calendar.DAY_OF_WEEK)
                    val index = (dayConstant - cal.firstDayOfWeek + 7) % 7
                    val amount = CurrencyUtils.toEur(it.receipt.totalAmount, it.receipt.currency)
                    if (index in 0..6) buckets[index] += amount
                }
                Pair(buckets.toList(), "${dateFormat.format(start)} - ${dateFormat.format(end)}")
            }
            "This Month" -> {
                cal.timeInMillis = now
                val maxDays = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
                val buckets = DoubleArray(maxDays)
                cal.set(Calendar.DAY_OF_MONTH, 1)
                val start = cal.timeInMillis
                cal.set(Calendar.DAY_OF_MONTH, maxDays)
                val end = cal.timeInMillis

                receipts.forEach {
                    cal.timeInMillis = it.receipt.dateTimestamp
                    val day = cal.get(Calendar.DAY_OF_MONTH)
                    if (day - 1 in buckets.indices) {
                        buckets[day - 1] += CurrencyUtils.toEur(it.receipt.totalAmount, it.receipt.currency)
                    }
                }
                Pair(buckets.toList(), "${dateFormat.format(start)} - ${dateFormat.format(end)}")
            }
            "This Year" -> {
                val buckets = DoubleArray(12)
                cal.timeInMillis = now
                cal.set(Calendar.DAY_OF_YEAR, 1)
                val start = cal.timeInMillis
                receipts.forEach {
                    cal.timeInMillis = it.receipt.dateTimestamp
                    val month = cal.get(Calendar.MONTH)
                    if (month in 0..11) {
                        buckets[month] += CurrencyUtils.toEur(it.receipt.totalAmount, it.receipt.currency)
                    }
                }
                Pair(buckets.toList(), yearFormat.format(start))
            }
            else -> { // "All Time" - RESTORED LOGIC
                val minTs = receipts.minOf { it.receipt.dateTimestamp }
                val maxTs = receipts.maxOf { it.receipt.dateTimestamp }

                cal.timeInMillis = minTs
                val minYear = cal.get(Calendar.YEAR)
                cal.timeInMillis = maxTs
                val maxYear = cal.get(Calendar.YEAR)

                val span = maxYear - minYear + 1
                // Ensure at least size 1 to avoid crashes
                val buckets = DoubleArray(if (span > 0) span else 1)

                receipts.forEach {
                    cal.timeInMillis = it.receipt.dateTimestamp
                    val y = cal.get(Calendar.YEAR)
                    val index = y - minYear
                    if (index in buckets.indices) {
                        buckets[index] += CurrencyUtils.toEur(it.receipt.totalAmount, it.receipt.currency)
                    }
                }
                Pair(buckets.toList(), "${yearFormat.format(minTs)} - ${yearFormat.format(maxTs)}")
            }
        }
    }

    private fun calculateCategoryData(receipts: List<ReceiptWithDetails>, totalAmount: Double): List<CategoryUiData> {
        if (totalAmount == 0.0) return emptyList()

        val grouped = receipts.groupBy { it.category }
        val rawData = grouped.map { (category, list) ->
            val sum = list.sumOf { CurrencyUtils.toEur(it.receipt.totalAmount, it.receipt.currency) }
            Triple(category?.name ?: "Uncategorized", sum, sum / totalAmount)
        }.sortedByDescending { it.second }

        // Logic: Group small categories into "Others" (Logic moved from UI to VM)
        val threshold = 0.07
        val (big, small) = rawData.partition { it.third >= threshold }

        val finalData = mutableListOf<Triple<String, Double, Double>>()
        finalData.addAll(big)

        if (small.isNotEmpty()) {
            val smallSum = small.sumOf { it.second }
            finalData.add(Triple("Others", smallSum, smallSum / totalAmount))
        }

        return finalData.mapIndexed { index, (name, sum, pct) ->
            CategoryUiData(
                name = name,
                totalFormatted = CurrencyUtils.formatEur(sum),
                percentage = pct.toFloat(),
                colorIndex = index // UI will assign actual color based on this index
            )
        }
    }


    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = (this[APPLICATION_KEY] as ReceiptApplication)
                DashboardViewModel(application.container.receiptRepository)
            }
        }
    }
}