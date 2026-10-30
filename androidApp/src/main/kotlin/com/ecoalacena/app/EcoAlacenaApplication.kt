package com.ecoalacena.app

import android.app.Application
import com.ecoalacena.app.data.remote.ai.GeminiConfig
import com.ecoalacena.app.di.initKoin
import org.koin.android.ext.koin.androidContext

class EcoAlacenaApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        initKoin(geminiConfig = GeminiConfig(apiKey = BuildConfig.GEMINI_API_KEY)) {
            androidContext(this@EcoAlacenaApplication)
        }
    }
}
