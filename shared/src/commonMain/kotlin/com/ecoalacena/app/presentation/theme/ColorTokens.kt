package com.ecoalacena.app.presentation.theme

import androidx.compose.ui.graphics.Color

// These are the raw palette values. Only Theme.kt reads them, to build the Material 3 color schemes:
// screens and components must use MaterialTheme.colorScheme so that light and dark mode both work.

// Brand palette (organic / eco-friendly)
val SAGE_GREEN = Color(0xFF1B4332)
val WARM_BEIGE = Color(0xFFF8F6F0)
val TERRACOTTA = Color(0xFFD9534F)
val AMBER = Color(0xFFF0A500)
val CHARCOAL = Color(0xFF2B2D42)
val SNOW_WHITE = Color(0xFFFFFFFF)

// Light theme tints derived from the brand palette
val SAGE_CONTAINER = Color(0xFFD8EADF)
val AMBER_CONTAINER = Color(0xFFFCEBC2)
val TERRACOTTA_CONTAINER = Color(0xFFF7DAD9)
val SEA_TEAL = Color(0xFF2F6F6A)
val SEA_TEAL_CONTAINER = Color(0xFFD2EBE6)
val ON_SEA_TEAL_CONTAINER = Color(0xFF0F332F)

// Warm neutral surfaces (light theme)
val BEIGE_LOW = Color(0xFFF2EFE6)
val BEIGE_MID = Color(0xFFEDE9DD)
val BEIGE_HIGH = Color(0xFFE7E3D6)
val BEIGE_HIGHEST = Color(0xFFE1DDCF)

// Dark theme
// --- Main App Background & Card Surfaces ---
val NIGHT_BACKGROUND = Color(0xFF121815) // Deep Charcoal Pine Background
val NIGHT_CONTAINER_LOWEST = Color(0xFF1B2721) // Cards sitting directly on the background
val NIGHT_SURFACE = Color(0xFF22332A) // Intermediate Pine Slate (unmistakably green, not black)
val NIGHT_SURFACE_VARIANT = Color(0xFF2C3E34) // Elevated surface / Text inputs / Navigation bar
val NIGHT_SURFACE_HIGH = Color(0xFF364A3E) // Dialogs & Popups
val NIGHT_CONTAINER_HIGHEST = Color(0xFF40564A) // Highest elevation

// --- Primary Actions & Food Accent (Soft Muted Sage - ZERO MINT / NON-GLARE) ---
val FOOD_MINT_ACCENT = Color(0xFF65A080) // Muted Sage Green (calm, matte, desaturated)
val ON_PRIMARY_DARK = Color(0xFF0C1F15) // High contrast text/icon on Primary
val PRIMARY_CONTAINER_DARK = Color(0xFF1C3025) // Background for Primary badges
val ON_PRIMARY_CONTAINER_DARK = Color(0xFFBCE5D0) // Text/icon on Primary Containers

// --- Secondary Actions & CO2 Accent (Desaturated Eucalyptus Teal) ---
val CO2_TEAL_ACCENT = Color(0xFF5B998E) // Soft Slate Eucalyptus Teal
val ON_SECONDARY_DARK = Color(0xFF0B1F1C) // Text/icon on Secondary
val SECONDARY_CONTAINER_DARK = Color(0xFF1B302C) // Background for Secondary badges
val ON_SECONDARY_CONTAINER_DARK = Color(0xFFB7E2DB) // Text on Secondary Containers

// --- Tertiary Actions & Warning Accent (Warm Muted Honey Amber) ---
val MONEY_GOLD_ACCENT = Color(0xFFD6A265) // Warm Muted Honey Amber
val ON_TERTIARY_DARK = Color(0xFF2E1E04) // Text on Tertiary
val TERTIARY_CONTAINER_DARK = Color(0xFF3D2C12) // Background for Money Cards/Highlights
val ON_TERTIARY_CONTAINER_DARK = Color(0xFFF0D6B2) // Text on Money Containers

// --- Typography & Text (Clean Warm Cream) ---
val LINEN_WHITE = Color(0xFFECEAE0) // Standard Header & Body Text across all screens
val MUTED_SLATE_TEXT = Color(0xFF92A89A) // Subtitles & Placeholders
val INVERSE_ON_SURFACE_DARK = Color(0xFF121815)

// --- Errors & Alerts (Soft Terracotta Clay) ---
val TERRACOTTA_ALERT_ACCENT = Color(0xFFCB6E68) // Soft Muted Terracotta
val ON_ERROR_DARK = Color(0xFF2D0E0D) // Text on Error buttons
val ERROR_CONTAINER_DARK = Color(0xFF3E1816) // Background for Expired Badges
val ON_ERROR_CONTAINER_DARK = Color(0xFFF2C4C1) // Text on Expired Containers

// --- Outlines & Dividers ---
val OUTLINE_DARK = Color(0xFF3A4E42) // Crisp Subtle Card Border
val OUTLINE_VARIANT_DARK = Color(0xFF2A3B31) // Soft section dividers
val SCRIM_DARK = Color(0xFF0A0F0C)
