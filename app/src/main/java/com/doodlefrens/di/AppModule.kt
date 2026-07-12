package com.doodlefrens.di

import android.content.Context
import com.doodlefrens.data.remote.api.SetupApi
import com.doodlefrens.data.remote.ws.DrawingApi
import com.doodlefrens.data.remote.ws.KtorDrawingApi
import com.doodlefrens.repository.SetupRepository
import com.doodlefrens.repository.SetupRepositoryImpl
import com.doodlefrens.util.Constants
import com.doodlefrens.util.Constants.USE_LOCALHOST
import com.doodlefrens.util.DispatcherProvider
import com.doodlefrens.util.clientId
import com.doodlefrens.util.dataStore
import com.google.gson.Gson
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.serialization.kotlinx.KotlinxWebsocketSerializationConverter
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Singleton
    @Provides
    fun provideSetupRepository(
        setupApi: SetupApi,
        @ApplicationContext context: Context
    ): SetupRepository = SetupRepositoryImpl(setupApi, context)

    @Singleton
    @Provides
    @Named("clientId")
    fun provideClientId(@ApplicationContext context: Context): String {
        return runBlocking { context.dataStore.clientId() }
    }

    @Singleton
    @Provides
    fun provideOkHttpClient(@Named("clientId") clientId: String): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor { chain ->
                val url = chain.request().url.newBuilder()
                    .addQueryParameter("client_id", clientId)
                    .build()
                val request = chain.request().newBuilder()
                    .url(url)
                    .build()
                chain.proceed(request)
            }
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            })
            .build()
    }

    @Singleton
    @Provides
    fun provideApplicationContext(
        @ApplicationContext context: Context
    ) = context

    @Singleton
    @Provides
    fun provideSetupApi(okHttpClient: OkHttpClient) : SetupApi {
        return Retrofit.Builder()
            .baseUrl(if(USE_LOCALHOST) Constants.HTTP_BASE_URL_LOCALHOST else Constants.HTTP_BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .client(okHttpClient)
            .build()
            .create(SetupApi::class.java)
    }

    @Singleton
    @Provides
    fun provideGsonInstance(): Gson {
        return Gson()
    }

    @Singleton
    @Provides
    fun provideKotlinxJson(): Json {
        return Json {
            ignoreUnknownKeys = true
            isLenient = true
            classDiscriminator = "type"
        }
    }

    @Singleton
    @Provides
    fun provideHttpClient(json: Json, okHttpClient: OkHttpClient): HttpClient {
        return HttpClient(OkHttp) {
            engine {
                preconfigured = okHttpClient
            }
            install(Logging) {
                level = LogLevel.ALL
            }
            install(WebSockets) {
                contentConverter = KotlinxWebsocketSerializationConverter(json)
            }
        }
    }

    @Singleton
    @Provides
    fun provideDrawingApi(
        client: HttpClient,
        json: Json
    ): DrawingApi {
        return KtorDrawingApi(
            client = client,
            json = json,
            baseUrl = if (USE_LOCALHOST) Constants.WS_BASE_URL_LOCALHOST else Constants.WS_BASE_URL
        )
    }

    @Singleton
    @Provides
    fun provideDispatcherProvider(): DispatcherProvider {
        return object : DispatcherProvider {
            override val io: CoroutineDispatcher
                get() = Dispatchers.IO
            override val main: CoroutineDispatcher
                get() = Dispatchers.Main
            override val default: CoroutineDispatcher
                get() = Dispatchers.Default
        }
    }
}