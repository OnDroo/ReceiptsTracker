package link.ondrov.receipts.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// Singleton DataStore attached to Context
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_settings")

class SettingsRepository(private val context: Context) {

    // --- DATASTORE KEYS ---
    private object Keys {
        val API_KEY = stringPreferencesKey("api_key")
        val CURRENCY = stringPreferencesKey("currency")
        val GEMINI_MODEL = stringPreferencesKey("gemini_model")
    }

    // --- READ DATA (Flows) ---

    val apiKey: Flow<String> = context.dataStore.data
        .map { preferences -> preferences[Keys.API_KEY] ?: "" }

    val currency: Flow<String> = context.dataStore.data
        .map { preferences -> preferences[Keys.CURRENCY] ?: "EUR" }

    val selectedModel: Flow<String> = context.dataStore.data
        .map { preferences -> preferences[Keys.GEMINI_MODEL] ?: "gemini-4-hydrogen" }

    // --- WRITE DATA ---

    suspend fun saveApiKey(key: String) {
        context.dataStore.edit { preferences ->
            preferences[Keys.API_KEY] = key
        }
    }

    suspend fun saveCurrency(currencyCode: String) {
        context.dataStore.edit { preferences ->
            preferences[Keys.CURRENCY] = currencyCode
        }
    }

    suspend fun saveSelectedModel(modelName: String) {
        context.dataStore.edit { preferences ->
            preferences[Keys.GEMINI_MODEL] = modelName
        }
    }
}