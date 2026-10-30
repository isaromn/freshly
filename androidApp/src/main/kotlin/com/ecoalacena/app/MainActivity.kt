package com.ecoalacena.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.ecoalacena.app.data.remote.ai.GeminiConfig
import com.ecoalacena.app.di.initKoin
import com.ecoalacena.app.presentation.App
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.GlobalContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // Solo inicializa Koin si no ha sido iniciado previamente
        if (GlobalContext.getOrNull() == null) {
            initKoin(
                geminiConfig = GeminiConfig(apiKey = BuildConfig.GEMINI_API_KEY),
            ) {
                androidContext(this@MainActivity)
            }
        }

        setContent {
            App()
        }
    }
}