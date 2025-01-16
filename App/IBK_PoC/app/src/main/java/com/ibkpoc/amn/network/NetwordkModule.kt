// network/NetworkModule.kt
package com.ibkpoc.amn.network

import com.ibkpoc.amn.config.Config
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit
import javax.inject.Singleton
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            })
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .addHeader("Content-Type", "application/json")
                    .build()
                chain.proceed(request)
            }
            .connectTimeout(Config.TIMEOUT, TimeUnit.SECONDS)
            .readTimeout(Config.TIMEOUT, TimeUnit.SECONDS)
            .writeTimeout(Config.TIMEOUT, TimeUnit.SECONDS)
            .build()
    }
    @Provides
    @Singleton
    fun provideApiService(okHttpClient: OkHttpClient): ApiService {
        return Retrofit.Builder()
            .baseUrl(Config.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }

    fun createWavUploadClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS) // 연결 대기 시간
            .readTimeout(10, TimeUnit.SECONDS)   // 응답 대기 시간
            .writeTimeout(10, TimeUnit.SECONDS)  // 쓰기 대기 시간
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            })
            .build()
    }
}