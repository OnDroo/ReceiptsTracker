package link.ondrov.receipts.ui.utils

import java.text.NumberFormat
import java.util.Locale

object CurrencyUtils {

    // Exchange rates relative to 1 EUR
    private val rates = mapOf(
        "EUR" to 1.0,
        "USD" to 0.92,  // 1 USD = 0.92 EUR
        "CZK" to 0.04,  // 1 CZK = 0.04 EUR
        "GBP" to 1.17,  // 1 GBP = 1.17 EUR
        "PLN" to 0.23   // 1 PLN = 0.23 EUR
    )

    private fun getRate(currency: String): Double {
        return when (currency.uppercase()) {
            "USD" -> 1.08 // Example: 1 EUR = 1.08 USD
            "CZK" -> 25.3 // Example: 1 EUR = 25.3 CZK
            "GBP" -> 0.85
            else -> 1.0
        }
    }

    fun toEur(amount: Double, currencyCode: String): Double {
        val code = currencyCode.uppercase()
        // Default to 1.0 conversion (assume EUR) if code not found
        val rate = rates[code] ?: 1.0
        return amount * rate
    }

    fun formatEur(amount: Double): String {
        val format = NumberFormat.getCurrencyInstance(Locale.GERMANY)
        format.currency = java.util.Currency.getInstance("EUR")
        return format.format(amount)
    }

    fun format(amount: Double, currencyCode: String): String {
        return try {
            val format = NumberFormat.getCurrencyInstance(Locale.getDefault())
            format.currency = java.util.Currency.getInstance(currencyCode)
            format.format(amount)
        } catch (e: Exception) {
            "$amount $currencyCode"
        }
    }

    fun convert(amount: Double, fromCurrency: String, toCurrency: String): Double {
        if (fromCurrency.equals(toCurrency, ignoreCase = true)) return amount

        val amountInEur = toEur(amount, fromCurrency)

        return if (toCurrency.equals("EUR", ignoreCase = true)) {
            amountInEur
        } else {
            val rate = getRate(toCurrency)
            amountInEur * rate
        }
    }
}