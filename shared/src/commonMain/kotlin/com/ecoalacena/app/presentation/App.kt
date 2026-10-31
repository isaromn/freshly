package com.ecoalacena.app.presentation

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ecoalacenakmp.shared.generated.resources.Res
import ecoalacenakmp.shared.generated.resources.logoNevera
import org.jetbrains.compose.resources.painterResource
import com.ecoalacena.app.core.i18n.AppStrings
import com.ecoalacena.app.core.i18n.currentStrings
import com.ecoalacena.app.presentation.i18n.LocalAppStrings
import com.ecoalacena.app.presentation.impact.ImpactScreen
import com.ecoalacena.app.presentation.pantry.PantryScreen
import com.ecoalacena.app.presentation.platform.PlatformBackHandler
import com.ecoalacena.app.presentation.recipes.RecipeScreen
import com.ecoalacena.app.presentation.scan.ScanScreen
import com.ecoalacena.app.presentation.theme.EcoAlacenaTheme

// Icons are vectors, so they look exactly the same on Android, iOS and desktop (emojis do not:
// every system draws them with its own font).
private enum class MainTab(val icon: ImageVector) {
    PANTRY(Icons.Filled.Inventory2),
    SCAN(Icons.Filled.DocumentScanner),
    RECIPES(Icons.Filled.Restaurant),
    IMPACT(Icons.Filled.Eco),
}

/** Short label, for the bottom bar of narrow screens. */
private fun MainTab.shortLabel(strings: AppStrings): String =
    when (this) {
        MainTab.PANTRY -> strings.tabPantry
        MainTab.SCAN -> strings.tabScannerShort
        MainTab.RECIPES -> strings.tabRecipesShort
        MainTab.IMPACT -> strings.tabImpactShort
    }

/** Full name ("AI Scanner", "Smart Recipes", "Eco Impact"), for the side rail of wide windows. */
private fun MainTab.fullLabel(strings: AppStrings): String =
    when (this) {
        MainTab.PANTRY -> strings.tabPantry
        MainTab.SCAN -> strings.tabScanner
        MainTab.RECIPES -> strings.tabRecipes
        MainTab.IMPACT -> strings.tabImpact
    }

private val mainTabSaver = Saver<MainTab, Int>(
    save = { tab -> tab.ordinal },
    restore = { ordinal -> MainTab.entries[ordinal] },
)

/** Windows at least this wide (desktop, tablets) use a side rail instead of the bottom bar. */
private val SIDE_RAIL_MIN_WIDTH = 720.dp

@Composable
fun App() {
    // Spanish when the system language is Spanish, English for any other language.
    val strings = remember { currentStrings() }

    CompositionLocalProvider(LocalAppStrings provides strings) {
        EcoAlacenaTheme {
            // The logo greets the user every time the app starts. `rememberSaveable` keeps it from
            // showing again when Android recreates the screen (for example on rotation).
            var showSplash by rememberSaveable { mutableStateOf(true) }

            Crossfade(targetState = showSplash, animationSpec = tween(durationMillis = 400)) { splash ->
                if (splash) {
                    SplashScreen(onFinished = { showSplash = false })
                } else {
                    MainLayout()
                }
            }
        }
    }
}

/** Navigation (bottom bar or side rail) and the screen of the selected tab. */
@Composable
private fun MainLayout() {
    var selectedTab by rememberSaveable(stateSaver = mainTabSaver) {
        mutableStateOf(MainTab.PANTRY)
    }

    // Back from any other tab returns to the pantry (Android system back).
    PlatformBackHandler(enabled = selectedTab != MainTab.PANTRY) {
        selectedTab = MainTab.PANTRY
    }

    // The layout follows the size of the window: a bottom bar on phones, and a side rail
    // with wider content on computer windows.
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useSideRail = maxWidth >= SIDE_RAIL_MIN_WIDTH

        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            bottomBar = {
                if (!useSideRail) {
                    BottomNavigation(
                        selectedTab = selectedTab,
                        onTabSelected = { selectedTab = it },
                    )
                }
            },
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    // The screens inside have their own Scaffold: don't apply the insets twice.
                    .consumeWindowInsets(innerPadding),
            ) {
                if (useSideRail) {
                    SideNavigation(
                        selectedTab = selectedTab,
                        onTabSelected = { selectedTab = it },
                    )
                }

                MainContent(
                    selectedTab = selectedTab,
                    onTabSelected = { selectedTab = it },
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize(),
                )
            }
        }
    }
}

@Composable
private fun MainContent(
    selectedTab: MainTab,
    onTabSelected: (MainTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Every screen fills the whole content area, so the layout scales with the window.
    Box(modifier = modifier, contentAlignment = Alignment.TopCenter) {
        when (selectedTab) {
            MainTab.PANTRY -> PantryScreen(
                onScanClick = { onTabSelected(MainTab.SCAN) },
                modifier = Modifier.fillMaxSize(),
            )

            MainTab.SCAN -> ScanScreen(
                onBack = { onTabSelected(MainTab.PANTRY) },
                modifier = Modifier.fillMaxSize(),
            )

            MainTab.RECIPES -> RecipeScreen(
                modifier = Modifier.fillMaxSize(),
            )

            MainTab.IMPACT -> ImpactScreen(
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
private fun BottomNavigation(
    selectedTab: MainTab,
    onTabSelected: (MainTab) -> Unit,
) {
    val strings = LocalAppStrings.current

    NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainer) {
        for (tab in MainTab.entries) {
            NavigationBarItem(
                selected = tab == selectedTab,
                onClick = { onTabSelected(tab) },
                icon = { Icon(imageVector = tab.icon, contentDescription = null) },
                label = { Text(text = tab.shortLabel(strings), maxLines = 1) },
            )
        }
    }
}

@Composable
private fun SideNavigation(
    selectedTab: MainTab,
    onTabSelected: (MainTab) -> Unit,
) {
    val strings = LocalAppStrings.current

    NavigationRail(
        header = {
            Image(
                painter = painterResource(Res.drawable.logoNevera),
                contentDescription = "Logo Freshly",
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 8.dp)
                    .size(48.dp),
            )
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        for (tab in MainTab.entries) {
            NavigationRailItem(
                selected = tab == selectedTab,
                onClick = { onTabSelected(tab) },
                icon = { Icon(imageVector = tab.icon, contentDescription = null) },
                label = {
                    Text(
                        text = tab.fullLabel(strings),
                        maxLines = 2,
                        textAlign = TextAlign.Center,
                    )
                },
            )
        }
    }
}
