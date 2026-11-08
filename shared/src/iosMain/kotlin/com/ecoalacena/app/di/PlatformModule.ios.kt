package com.ecoalacena.app.di

import com.ecoalacena.app.data.local.DatabaseDriverFactory
import com.ecoalacena.app.data.notification.ExpirationNotifier
import com.ecoalacena.app.data.notification.NotificationService
import com.ecoalacena.app.platform.ShareService
import org.koin.core.module.Module
import org.koin.dsl.module

actual val platformModule: Module = module {
    single { DatabaseDriverFactory() }
    single { ShareService() }
    single { NotificationService(planner = get()) }
    single<ExpirationNotifier> { get<NotificationService>() }
}
