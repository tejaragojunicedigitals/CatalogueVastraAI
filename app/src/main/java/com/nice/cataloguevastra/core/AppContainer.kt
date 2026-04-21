package com.nice.cataloguevastra.core

import android.content.Context
import com.nice.cataloguevastra.api.ApiClient
import com.nice.cataloguevastra.api.ApiServices
import com.nice.cataloguevastra.repositories.AuthRepository
import com.nice.cataloguevastra.repositories.CatalogueRepository
import com.nice.cataloguevastra.repositories.CatalogueRepositoryImpl
import com.nice.cataloguevastra.utils.SessionManager
import retrofit2.Retrofit

class AppContainer(context: Context) {

    private val appContext = context.applicationContext

    val sessionManager: SessionManager by lazy {
        SessionManager(appContext)
    }

    private val retrofit: Retrofit by lazy {
        ApiClient.createRetrofit()
    }

    val apiServices: ApiServices by lazy {
        retrofit.create(ApiServices::class.java)
    }

    val authRepository: AuthRepository by lazy {
        AuthRepository(
            apiServices = apiServices,
            sessionManager = sessionManager,
            context = appContext
        )
    }

    val catalogueRepository: CatalogueRepository by lazy {
        CatalogueRepositoryImpl(appContext, sessionManager, apiServices)
    }
}
