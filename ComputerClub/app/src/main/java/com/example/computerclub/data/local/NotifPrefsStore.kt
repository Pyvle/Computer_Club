package com.example.computerclub.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

private val Context.notifDataStore by preferencesDataStore(name = "notif_prefs")

class NotifPrefsStore(private val context: Context) {

    private val KEY_ENABLED = booleanPreferencesKey("notifications_enabled")
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Volatile private var cached: Boolean = true

    init {
        scope.launch {
            context.notifDataStore.data.collect { prefs ->
                cached = prefs[KEY_ENABLED] ?: true
            }
        }
    }

    /** Мгновенное чтение из кеша — не блокирует UI. */
    fun peek(): Boolean = cached

    suspend fun setEnabled(enabled: Boolean) {
        cached = enabled
        context.notifDataStore.edit { it[KEY_ENABLED] = enabled }
    }
}
