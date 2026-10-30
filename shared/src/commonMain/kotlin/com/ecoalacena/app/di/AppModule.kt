package com.ecoalacena.app.di

import com.ecoalacena.app.data.local.DatabaseDriverFactory
import com.ecoalacena.app.data.remote.ai.GeminiConfig
import com.ecoalacena.app.data.remote.ai.GeminiRecipeService
import com.ecoalacena.app.data.remote.ai.GeminiVisionService
import com.ecoalacena.app.data.remote.ai.geminiJson
import com.ecoalacena.app.data.repository.PantryRepositoryImpl
import com.ecoalacena.app.database.PantryDatabase
import com.ecoalacena.app.domain.repository.PantryRepository
import com.ecoalacena.app.domain.usecase.AnalyzeFoodUseCase
import com.ecoalacena.app.domain.usecase.CalculateImpactUseCase
import com.ecoalacena.app.domain.usecase.EstimateExpirationDateUseCase
import com.ecoalacena.app.domain.usecase.GetHybridRecipeUseCase
import com.ecoalacena.app.presentation.impact.ImpactViewModel
import com.ecoalacena.app.presentation.pantry.PantryViewModel
import com.ecoalacena.app.presentation.recipes.RecipeViewModel
import com.ecoalacena.app.presentation.scan.ScanViewModel
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import org.koin.core.KoinApplication
import org.koin.core.context.startKoin
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.module

/**
 * Shared Koin module: local database, networking, AI service, repositories,
 * use cases and MVI view models.
 */
val appModule = module {
    // Local persistence (single source of truth)
    single { PantryDatabase(get<DatabaseDriverFactory>().createDriver()) }
    single<PantryRepository> { PantryRepositoryImpl(get()) }

    // Networking: the engine is picked per platform (OkHttp on Android/Desktop, Darwin on iOS)
    single {
        HttpClient {
            install(ContentNegotiation) { json(geminiJson) }
            install(HttpTimeout) {
                connectTimeoutMillis = 15_000
                socketTimeoutMillis = 60_000
                requestTimeoutMillis = 60_000
            }
        }
    }

    // Vision AI
    single { GeminiVisionService(httpClient = get(), config = get<GeminiConfig>()) }
    factory { EstimateExpirationDateUseCase() }
    factory { AnalyzeFoodUseCase(visionService = get(), estimateExpiration = get()) }

    // Hybrid recipes: Gemini first, offline templates as a fallback
    single { GeminiRecipeService(httpClient = get(), config = get<GeminiConfig>()) }
    factory { GetHybridRecipeUseCase(repository = get(), recipeService = get()) }

    // Impact dashboard (SDG 12)
    factory { CalculateImpactUseCase() }

    // Presentation (MVI)
    viewModel { PantryViewModel(repository = get(), estimateExpiration = get()) }
    viewModel { ScanViewModel(analyzeFood = get(), repository = get()) }
    viewModel { RecipeViewModel(getRecipes = get()) }
    viewModel { ImpactViewModel(repository = get(), calculateImpact = get()) }
}

/**
 * Starts Koin with [platformModule] and the shared [appModule].
 *
 * @param geminiConfig API key and model for Gemini, provided by the platform
 * (for example from a BuildConfig field on Android).
 * @param appDeclaration extra platform configuration, e.g. `androidContext(...)` on Android.
 */
fun initKoin(
    geminiConfig: GeminiConfig,
    appDeclaration: KoinAppDeclaration = {},
): KoinApplication =
    startKoin {
        appDeclaration()
        modules(
            module { single { geminiConfig } },
            platformModule,
            appModule,
        )
    }
