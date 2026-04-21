package com.nice.cataloguevastra.api

import com.nice.cataloguevastra.BuildConfig
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {

    private const val TIMEOUT_SECONDS = 30L

    fun createRetrofit(): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_URL)
            .client(createOkHttpClient())
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    private fun createOkHttpClient(): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }

        return OkHttpClient.Builder()
            .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .addInterceptor(createHeaderInterceptor())
            .addInterceptor(loggingInterceptor)
            .build()
    }

    private fun createHeaderInterceptor(): Interceptor {
        return Interceptor { chain ->
            val originalRequest = chain.request()
            val originalContentType = originalRequest.body?.contentType()?.toString()
            val existingAuthorization = originalRequest.header("Authorization")
            val hasApiTokenHeader = !originalRequest.header("X-Api-Token").isNullOrBlank()

            val updatedRequest = originalRequest.newBuilder()
                .header("Accept", "application/json")
                .apply {
                    if (!hasApiTokenHeader) {
                        header("Authorization", existingAuthorization ?: BuildConfig.CLIENT_SECRET)
                    }
                    if (!originalContentType.isNullOrBlank()) {
                        header("Content-Type", originalContentType)
                    }
                }
                .build()

            chain.proceed(updatedRequest)
        }
    }
}
