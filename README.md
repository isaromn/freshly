# Freshly 🥬

**Freshly** is a **Kotlin Multiplatform + Compose Multiplatform** app that helps households waste less food.
Scan a grocery receipt or a product photo, keep track of what is in your pantry, see what is about to expire
and cook it before it goes bad.

It runs on **Android**, **iOS** and **Desktop (JVM)** from a single codebase: the domain logic, data layer,
ViewModels and the whole UI live in `shared/commonMain`.

## Features

- **Smart scanner**: take a photo of a receipt or a product and Gemini Vision extracts the food items
  (name, quantity, unit, category, nutrition). Every field can be edited before saving, and the expiry date
  is recalculated when the category changes.
- **Receipt checks**: the receipt date is read and the app warns when the receipt is too old, so expiry
  dates are not based on a purchase from months ago. Spoiled food detected in a photo is flagged with
  composting advice.
- **Pantry**: inventory with expiry dates estimated per food category, filters by category, quick
  "Consumed" / "Discarded" actions and an editable quantity.
- **Expired food banner**: a compact, expandable banner lists expired items and explains how to dispose of
  each kind (compost, organic waste, recycling the container).
- **Hybrid recipes**: Gemini creates recipes that prioritise the ingredients closest to expiring, always
  combined with an **offline recipe engine** with its own recipe templates, so recipes keep coming without
  network, API key or quota. Recipes include servings, calories and macros, and photos from Unsplash.
- **Shopping list**: add products by hand, get suggestions from the food you used up, and share the list.
- **Impact dashboard**: kilograms of food saved, waste avoided, and money lost to wasted food.
- **Offline-first**: all data is stored locally with SQLDelight, with schema migrations.
- **Bilingual**: English and Spanish, following the device language (including the Gemini prompts).
- **Demo Mode**: try the scanner with a sample receipt, no API key needed.

## Kotlin Multiplatform architecture

| Source set | Lines of Kotlin | What lives there |
|---|---|---|
| `commonMain` | ~12,100 | Domain, use cases, repositories, Gemini/Unsplash clients, SQLDelight schema, ViewModels, all Compose screens |
| `androidMain` | ~80 | Platform `actual`s |
| `iosMain` | ~115 | Platform `actual`s + `MainViewController` |
| `jvmMain` | ~95 | Platform `actual`s |

About **97%** of the code is shared. Platform code is limited to a small set of `expect`/`actual` declarations:

| `expect` declaration | Purpose |
|---|---|
| `DatabaseDriverFactory` | SQLDelight driver (Android, Native, JDBC SQLite) |
| `ShareService` | Native share sheet for recipes and shopping lists |
| `rememberCameraLauncher` | Camera access for the scanner |
| `PlatformBackHandler` | System back navigation |
| `systemLanguageCode()` | Device language for the UI and AI prompts |
| `platformModule` | Koin bindings for the platform services |

### Stack

| Area | Technology |
|---|---|
| Language | Kotlin 2.4 |
| UI | Compose Multiplatform + Material 3 |
| Architecture | Clean Architecture (domain / data / presentation) + MVI ViewModels (State, Intent, Effect) |
| Dependency injection | Koin |
| Database | SQLDelight with `.sqm` migrations |
| Networking | Ktor Client + kotlinx.serialization |
| AI | Google Gemini API (structured JSON output with a response schema) |
| Images | Coil 3, Unsplash API |
| Camera / gallery | FileKit |

### Project structure

```
.
├── androidApp/   # Android entry point (Activity, Application)
├── desktopApp/   # Desktop (JVM) entry point
├── iosApp/       # Xcode project and SwiftUI entry point
└── shared/
    └── src/
        ├── commonMain/
        │   ├── kotlin/com/ecoalacena/app/
        │   │   ├── core/           # Dates, i18n
        │   │   ├── data/           # Repositories, local database, Gemini and Unsplash clients, demo data
        │   │   ├── di/             # Koin modules
        │   │   ├── domain/         # Models, use cases, offline recipe engine
        │   │   ├── platform/       # expect declarations for platform services
        │   │   └── presentation/   # Screens, ViewModels, theme
        │   ├── sqldelight/         # Database schema and migrations
        │   └── composeResources/   # Shared images
        ├── androidMain/ iosMain/ jvmMain/   # Platform implementations
        └── commonTest/ jvmTest/             # Tests
```

## Requirements

- JDK 21 (Gradle downloads it through toolchains if it is missing)
- Android Studio or IntelliJ IDEA with the Kotlin Multiplatform plugin
- Android SDK (compileSdk 37)
- macOS with Xcode, for iOS only
- Optional: a [Gemini API key](https://aistudio.google.com/apikey) for AI scanning and AI recipes
- Optional: an [Unsplash access key](https://unsplash.com/developers) for recipe photos

Without any key the app still works: the scanner has Demo Mode, recipes come from the offline engine and
each recipe shows the illustration of its category.

## Setup

1. Clone the repository:

   ```bash
   git clone https://github.com/isaromn/freshly.git
   cd freshly
   ```

2. Add your keys to `local.properties` in the project root (Android Studio creates it with `sdk.dir`):

   ```properties
   sdk.dir=/path/to/Android/sdk
   GEMINI_API_KEY=your_gemini_key
   UNSPLASH_ACCESS_KEY=your_unsplash_access_key
   ```

   > `local.properties` is git-ignored. Never commit a real key.

   Where each platform reads the keys from:

   | Platform | Source of `GEMINI_API_KEY` and `UNSPLASH_ACCESS_KEY` |
   |---|---|
   | Android | `local.properties` or environment variable → `BuildConfig` |
   | Desktop | Environment variable, system property or `local.properties` |
   | iOS | `iosApp/iosApp/Info.plist` (keep them empty in commits) |

## Run

- **Android**: `./gradlew :androidApp:installDebug` with a device or emulator connected, or the *Run* button in the IDE.
- **Desktop**: `./gradlew :desktopApp:run` (hot reload: `./gradlew :desktopApp:hotRun --auto`).
- **iOS**: open `iosApp/iosApp.xcodeproj` in Xcode, pick a simulator and run the `iosApp` scheme.
  The build links the system SQLite library through `iosApp/Configuration/Config.xcconfig`.

## Try it in two minutes

1. Open the **Scanner** tab and tap **Load sample receipt (Demo Mode)**. Review or edit the items and add them to the pantry.
2. On **Pantry**, mark an item as *Consumed* or *Discarded*, or tap its quantity to change it.
3. Open **Recipes** to get dishes that use the items expiring first. Open one to see steps, servings and nutrition.
4. Check **Impact** to see the food saved and the money lost to waste.
5. Add a Gemini key and scan a real receipt or a photo of your fridge.

## Tests

The tests live in `commonTest` and cover the offline recipe engine, expiry estimation, use cases,
repositories and ViewModels.

```bash
./gradlew :shared:jvmTest                 # common tests on the JVM
./gradlew :shared:iosSimulatorArm64Test   # common tests on the iOS simulator
```

## License

Released under the [MIT License](LICENSE).
