# EcoAlacena 🥬

Aplicación **Kotlin Multiplatform + Compose Multiplatform** para reducir el desperdicio de alimentos en casa. Lleva el control de tu alacena, avisa de lo que está por caducar y propone recetas con lo que ya tienes.

Funciona en **Android**, **iOS** y **Desktop (JVM)** compartiendo la lógica y la interfaz.

## Funcionalidades

- **Alacena**: inventario de productos con fecha de caducidad estimada automáticamente según la categoría del alimento.
- **Escaneo con IA**: fotografía un producto o un recibo del supermercado y Gemini Vision identifica los alimentos para añadirlos a la alacena.
- **Recetas híbridas**: Gemini genera una receta con los ingredientes más próximos a caducar, siempre acompañada de recetas de un motor local *offline* (sin red, sin clave o sin cuota, la app sigue sugiriendo recetas).
- **Impacto**: métricas de comida aprovechada y desperdicio evitado.
- **Notificaciones** de productos próximos a caducar.
- **Offline-first**: los datos se guardan localmente con SQLDelight.
- **Bilingüe**: español e inglés según el idioma del dispositivo (incluidos los prompts a Gemini).

## Stack

| Área | Tecnología |
|---|---|
| Lenguaje | Kotlin 2.4 |
| UI | Compose Multiplatform + Material 3 |
| Arquitectura | Clean Architecture (domain / data / presentation) + MVI-style ViewModels |
| DI | Koin |
| Base de datos | SQLDelight |
| Red | Ktor Client + kotlinx.serialization |
| IA | Google Gemini API |
| Cámara / galería | FileKit |

## Estructura

```
.
├── androidApp/   # Punto de entrada Android (Activity, Application)
├── desktopApp/   # Punto de entrada Desktop (JVM)
├── iosApp/       # Proyecto Xcode y punto de entrada SwiftUI
└── shared/       # Código compartido
    └── src/
        ├── commonMain/   # Lógica y UI comunes
        │   ├── kotlin/com/ecoalacena/app/
        │   │   ├── core/           # Utilidades, fechas, i18n
        │   │   ├── data/           # Repositorios, base de datos local, Gemini (remote/ai)
        │   │   ├── di/             # Módulos de Koin
        │   │   ├── domain/         # Modelos, casos de uso, motor de recetas local
        │   │   └── presentation/   # Pantallas, ViewModels, tema, navegación
        │   ├── sqldelight/         # Esquema de la base de datos (PantryDatabase.sq)
        │   └── composeResources/   # Recursos compartidos
        ├── androidMain/ iosMain/ jvmMain/   # Implementaciones específicas por plataforma
        └── commonTest/ jvmTest/ iosTest/ androidHostTest/
```

## Requisitos

- JDK 21 (Gradle lo descarga automáticamente vía toolchains si no está instalado)
- Android Studio (o IntelliJ IDEA) con el plugin de Kotlin Multiplatform
- Android SDK (compileSdk 37)
- Xcode (solo para iOS, en macOS)
- Una clave de [Gemini API](https://aistudio.google.com/apikey) (opcional: sin ella la app funciona con recetas locales, pero sin escaneo con IA)

## Configuración

1. Clona el repositorio:

   ```bash
   git clone https://github.com/isaromn/freshly.git
   cd freshly
   ```

2. Crea `local.properties` en la raíz (Android Studio lo genera con `sdk.dir`) y añade tu clave:

   ```properties
   sdk.dir=/ruta/a/Android/sdk
   GEMINI_API_KEY=tu_clave_aqui
   ```

   > ⚠️ `local.properties` está en `.gitignore`. **Nunca subas una clave real al repositorio.**

   Cómo lee la clave cada plataforma:

   | Plataforma | Origen de `GEMINI_API_KEY` |
   |---|---|
   | Android | `local.properties` o variable de entorno → `BuildConfig.GEMINI_API_KEY` |
   | Desktop | Variable de entorno, propiedad del sistema o `local.properties` |
   | iOS | Clave `GEMINI_API_KEY` en `iosApp/iosApp/Info.plist` (déjala vacía en los commits) |

## Ejecutar

- **Android**: `./gradlew :androidApp:assembleDebug` o el botón *Run* del IDE.
- **Desktop**:
  - Normal: `./gradlew :desktopApp:run`
  - Hot reload: `./gradlew :desktopApp:hotRun --auto`
- **iOS**: abre [`iosApp/`](./iosApp) en Xcode y ejecuta el esquema `iosApp`.

## Tests

```bash
./gradlew :shared:jvmTest                  # Desktop / lógica común
./gradlew :shared:testAndroidHostTest      # Android (host)
./gradlew :shared:iosSimulatorArm64Test    # iOS (simulador)
```

---

Proyecto creado para el Concurso de Kotlin Multiplatform. Más información sobre [Kotlin Multiplatform](https://www.jetbrains.com/help/kotlin-multiplatform-dev/get-started.html).
