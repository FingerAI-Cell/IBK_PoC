//// network/ApiConfig.kt
//package com.ibkpoc.amn.network
//
//import com.ibkpoc.amn.config.Config
//import okhttp3.OkHttpClient
//import okhttp3.logging.HttpLoggingInterceptor
//import retrofit2.Retrofit
//import retrofit2.converter.gson.GsonConverterFactory
//import java.util.concurrent.TimeUnit
//
//object ApiConfig {
//    private fun createOkHttpClient() = OkHttpClient.Builder()
//        .addInterceptor(HttpLoggingInterceptor().apply {
//            level = HttpLoggingInterceptor.Level.BODY
//        })
//        .addInterceptor { chain ->
//            val request = chain.request().newBuilder()
//                .addHeader("Content-Type", "application/json")
//                .build()
//            chain.proceed(request)
//        }
//        .connectTimeout(Config.TIMEOUT, TimeUnit.SECONDS)
//        .readTimeout(Config.TIMEOUT, TimeUnit.SECONDS)
//        .writeTimeout(Config.TIMEOUT, TimeUnit.SECONDS)
//        .build()
//
//    private fun createRetrofit() = Retrofit.Builder()
//        .baseUrl(Config.BASE_URL)
//        .client(createOkHttpClient())
//        .addConverterFactory(GsonConverterFactory.create())
//        .build()
//
//    fun createApiService(): ApiService = createRetrofit().create(ApiService::class.java)
//}