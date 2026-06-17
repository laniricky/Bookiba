package co.booknook.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationPreferencesDataSource @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    // We store JSON or | delimited strings. Let's just use JSON string representations 
    // or simple KEY=VALUE;KEY=VALUE formats for simplicity since it's just ID to Status/Price.

    val lastKnownOrderStatuses: Flow<Map<String, String>> = dataStore.data.map { preferences ->
        val raw = preferences[LAST_ORDER_STATUSES] ?: ""
        if (raw.isBlank()) emptyMap()
        else raw.split(",").mapNotNull {
            val parts = it.split(":")
            if (parts.size == 2) parts[0] to parts[1] else null
        }.toMap()
    }

    val lastKnownWishlistPrices: Flow<Map<String, Long>> = dataStore.data.map { preferences ->
        val raw = preferences[LAST_WISHLIST_PRICES] ?: ""
        if (raw.isBlank()) emptyMap()
        else raw.split(",").mapNotNull {
            val parts = it.split(":")
            if (parts.size == 2) parts[0] to (parts[1].toLongOrNull() ?: 0L) else null
        }.toMap()
    }

    suspend fun updateOrderStatuses(statuses: Map<String, String>) {
        dataStore.edit { preferences ->
            preferences[LAST_ORDER_STATUSES] = statuses.entries.joinToString(",") { "${it.key}:${it.value}" }
        }
    }

    suspend fun updateWishlistPrices(prices: Map<String, Long>) {
        dataStore.edit { preferences ->
            preferences[LAST_WISHLIST_PRICES] = prices.entries.joinToString(",") { "${it.key}:${it.value}" }
        }
    }

    companion object {
        private val LAST_ORDER_STATUSES = stringPreferencesKey("last_order_statuses")
        private val LAST_WISHLIST_PRICES = stringPreferencesKey("last_wishlist_prices")
    }
}
