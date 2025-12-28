package link.ondrov.receipts.util

import android.content.Context
import java.util.Date

object DateUtils {
    fun formatDateTime(context: Context, timestamp: Long): String {
        val date = Date(timestamp)
        // This automatically picks "12/28/2025" vs "28.12.2025" based on region
        val dateFormat = android.text.format.DateFormat.getDateFormat(context)
        // This automatically picks "10:30 PM" vs "22:30" based on user setting
        val timeFormat = android.text.format.DateFormat.getTimeFormat(context)

        return "${dateFormat.format(date)}, ${timeFormat.format(date)}"
    }

    // Optional: Just the month/year for headers
    fun formatMonthYear(timestamp: Long): String {
        // We can keep this one fairly standard or use a localized pattern
        val date = Date(timestamp)
        return java.text.SimpleDateFormat("MMMM yyyy", java.util.Locale.getDefault()).format(date)
    }
}