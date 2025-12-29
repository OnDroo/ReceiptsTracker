package link.ondrov.receipts

import android.app.Application
import android.content.Context
import link.ondrov.receipts.data.AppDatabase
import link.ondrov.receipts.data.repository.GeminiRepository
import link.ondrov.receipts.data.repository.ReceiptRepository
import link.ondrov.receipts.data.repository.RoomReceiptRepository
import link.ondrov.receipts.data.repository.SettingsRepository

class ReceiptApplication : Application() {

    // This "container" holds your shared dependencies (Database, Repository).
    // It is created when the app starts.
    lateinit var container: AppContainer

    override fun onCreate() {
        super.onCreate()
        container = AppDataContainer(this)
    }
}

// --- Manual Dependency Injection (DI) Container ---

/**
 * Interface acts as a contract for what dependencies are available.
 */
interface AppContainer {
    val receiptRepository: ReceiptRepository
    val settingsRepository: SettingsRepository
    val geminiRepository: GeminiRepository
}

/**
 * Implementation that actually creates the database and repository.
 */
class AppDataContainer(private val context: Context) : AppContainer {
    // We use 'lazy' so the database is only created when first needed,
    // not immediately when the app launches (better startup performance).
    override val receiptRepository: ReceiptRepository by lazy {
        RoomReceiptRepository(AppDatabase.getDatabase(context).receiptDao())
        //MockReceiptRepository()
    }

    override val settingsRepository: SettingsRepository by lazy {
        SettingsRepository(context)
    }

    override val geminiRepository: GeminiRepository by lazy {
        GeminiRepository(
            settingsRepository = settingsRepository,
            receiptRepository = receiptRepository
        )
    }
}