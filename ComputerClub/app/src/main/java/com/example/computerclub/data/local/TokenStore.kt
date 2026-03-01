package com.example.computerclub.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private val Context.dataStore by preferencesDataStore(name = "auth_tokens")

/**
 * Хранит токены в DataStore с in-memory кешем — чтобы избежать блокирующих чтений
 * (runBlocking + DataStore внутри перехватчиков OkHttp вызывает ANR).
 */
class TokenStore(private val context: Context) {

    private val KEY_ACCESS = stringPreferencesKey("access")
    private val KEY_REFRESH = stringPreferencesKey("refresh")

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Volatile private var cachedAccess: String? = null
    @Volatile private var cachedRefresh: String? = null

    init {
        // синхронизируем кеш в фоне
        scope.launch {
            context.dataStore.data.collect { prefs ->
                cachedAccess = prefs[KEY_ACCESS]
                cachedRefresh = prefs[KEY_REFRESH]
            }
        }
    }

    /** Неблокирующее чтение для сетевого слоя (перехватчики/authenticator). */
    fun peekAccess(): String? = cachedAccess
    fun peekRefresh(): String? = cachedRefresh

    /** Блокирующее suspend-чтение из DataStore — только из ViewModel/background. */
    suspend fun getAccess(): String? = context.dataStore.data.first()[KEY_ACCESS]
    suspend fun getRefresh(): String? = context.dataStore.data.first()[KEY_REFRESH]

    suspend fun save(access: String, refresh: String) {
        // обновляем кеш сразу — сеть не ждёт следующего collect
        cachedAccess = access
        cachedRefresh = refresh
        context.dataStore.edit {
            it[KEY_ACCESS] = access
            it[KEY_REFRESH] = refresh
        }
    }

    suspend fun clear() {
        cachedAccess = null
        cachedRefresh = null
        context.dataStore.edit {
            it.remove(KEY_ACCESS)
            it.remove(KEY_REFRESH)
        }
    }
}
