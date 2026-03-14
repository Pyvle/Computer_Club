package com.example.computerclub.data.network

import android.content.Context
import com.example.computerclub.data.local.TokenStore
import com.example.computerclub.data.network.dto.RefreshDto
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.Authenticator
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.create
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

class NetworkClient(
    context: Context,
    baseUrl: String,
    debug: Boolean
) {
    private val json = Json { ignoreUnknownKeys = true }
    private val tokenStore = TokenStore(context)

    private val refreshing = AtomicBoolean(false)

    private fun baseOkHttpBuilder(): OkHttpClient.Builder {
        return OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .callTimeout(30, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
    }

    // Retrofit без auth (для refresh)
    private val authApiNoAuth: AuthApi by lazy {
        Retrofit.Builder()
            .baseUrl(baseUrl)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .client(baseOkHttpBuilder().build())
            .build()
            .create()
    }

    private val client: OkHttpClient by lazy {
        val builder = baseOkHttpBuilder()

        // Bearer access токен (без runBlocking/чтения DataStore!)
        builder.addInterceptor { chain ->
            val access = tokenStore.peekAccess()
            val req = if (!access.isNullOrBlank()) {
                chain.request().newBuilder()
                    .header("Authorization", "Bearer $access")
                    .build()
            } else chain.request()

            chain.proceed(req)
        }

        // Refresh на 401 (OkHttp вызывает не на main thread)
        builder.authenticator(object : Authenticator {
            override fun authenticate(route: Route?, response: Response): Request? {
                if (responseCount(response) >= 2) return null

                val refresh = tokenStore.peekRefresh()
                if (refresh.isNullOrBlank()) return null

                if (!refreshing.compareAndSet(false, true)) return null

                return try {
                    val newPair = runBlocking(Dispatchers.IO) {
                        authApiNoAuth.refresh(RefreshDto(refresh))
                    }
                    runBlocking(Dispatchers.IO) {
                        tokenStore.save(newPair.accessToken, newPair.refreshToken)
                    }

                    response.request.newBuilder()
                        .header("Authorization", "Bearer ${newPair.accessToken}")
                        .build()
                } catch (_: Exception) {
                    runBlocking(Dispatchers.IO) { tokenStore.clear() }
                    null
                } finally {
                    refreshing.set(false)
                }
            }
        })

        if (debug) {
            builder.addInterceptor(HttpLoggingInterceptor().apply {
                // для отладки: показываем тело + заголовки (в т.ч. токены и debugCode OTP)
                // в релизе не включается (debug=false)
                level = HttpLoggingInterceptor.Level.BODY
            })
        }

        builder.build()
    }

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(baseUrl)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .client(client)
            .build()
    }

    val authApi: AuthApi by lazy { retrofit.create() }
    val clubsApi: ClubsApi by lazy { retrofit.create() }
    val productApi: ProductApi by lazy { retrofit.create() }
    val cartApi: CartApi by lazy { retrofit.create() }
    val checkoutApi: CheckoutApi by lazy { retrofit.create() }
    val seatApi: SeatApi by lazy { retrofit.create() }
    val floorplanApi: FloorplanApi by lazy { retrofit.create() }
    val favoritesApi: FavoritesApi by lazy { retrofit.create() }

    private fun responseCount(response: Response): Int {
        var res: Response? = response
        var count = 1
        while (res?.priorResponse != null) {
            count++
            res = res.priorResponse
        }
        return count
    }
}
