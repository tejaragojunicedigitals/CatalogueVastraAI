package com.nice.cataloguevastra

import android.app.Application
import com.nice.cataloguevastra.core.AppContainer

class CatalogueVastraApp : Application() {

    lateinit var appContainer: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        appContainer = AppContainer(this)
    }
}
