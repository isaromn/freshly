package com.ecoalacena.app.di

import org.koin.core.module.Module

/**
 * Koin module with the platform-specific bindings (e.g. the database driver factory).
 */
expect val platformModule: Module
