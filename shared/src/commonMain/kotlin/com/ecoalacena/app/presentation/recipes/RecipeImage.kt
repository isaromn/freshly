package com.ecoalacena.app.presentation.recipes

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.ecoalacena.app.domain.model.Recipe
import com.ecoalacena.app.domain.model.RecipeCategory
import com.ecoalacena.app.presentation.i18n.LocalAppStrings
import ecoalacenakmp.shared.generated.resources.Res
import ecoalacenakmp.shared.generated.resources.recipe_dessert
import ecoalacenakmp.shared.generated.resources.recipe_drink
import ecoalacenakmp.shared.generated.resources.recipe_eggs
import ecoalacenakmp.shared.generated.resources.recipe_meat
import ecoalacenakmp.shared.generated.resources.recipe_other
import ecoalacenakmp.shared.generated.resources.recipe_pasta
import ecoalacenakmp.shared.generated.resources.recipe_rice
import ecoalacenakmp.shared.generated.resources.recipe_salad
import ecoalacenakmp.shared.generated.resources.recipe_sandwich
import ecoalacenakmp.shared.generated.resources.recipe_soup
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

/** Width / height of recipe pictures; the local illustrations are drawn at this shape (320 × 160). */
internal const val RECIPE_IMAGE_ASPECT_RATIO = 2f

/** Local illustration of each kind of dish, used offline and whenever a recipe has no photo. */
fun getOfflineRecipeImage(category: RecipeCategory): DrawableResource =
    when (category) {
        RecipeCategory.SOUP -> Res.drawable.recipe_soup
        RecipeCategory.SALAD -> Res.drawable.recipe_salad
        RecipeCategory.PASTA -> Res.drawable.recipe_pasta
        RecipeCategory.RICE -> Res.drawable.recipe_rice
        RecipeCategory.MEAT -> Res.drawable.recipe_meat
        RecipeCategory.EGGS -> Res.drawable.recipe_eggs
        RecipeCategory.SANDWICH -> Res.drawable.recipe_sandwich
        RecipeCategory.DRINK -> Res.drawable.recipe_drink
        RecipeCategory.DESSERT -> Res.drawable.recipe_dessert
        RecipeCategory.OTHER -> Res.drawable.recipe_other
    }

/**
 * Header picture of a recipe. The photo at [Recipe.imageUrl] is loaded with Coil; the local
 * illustration of the category is shown while it loads, when it fails (for example with no
 * internet) and when there is no URL at all, so the header is never empty or broken.
 */
@Composable
internal fun RecipeHeaderImage(
    recipe: Recipe,
    modifier: Modifier = Modifier,
) {
    val illustration = painterResource(getOfflineRecipeImage(recipe.category))
    // The credit only makes sense over the real photo, not over the illustration.
    var photoShown by remember(recipe.imageUrl) { mutableStateOf(false) }

    Box(modifier = modifier) {
        AsyncImage(
            model = recipe.imageUrl,
            contentDescription = recipe.title,
            placeholder = illustration,
            error = illustration,
            fallback = illustration,
            contentScale = ContentScale.Crop,
            onSuccess = { photoShown = true },
            onError = { photoShown = false },
            modifier = Modifier.fillMaxSize(),
        )

        val credit = recipe.imageCredit
        if (photoShown && credit != null) {
            PhotoCredit(
                name = credit,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(8.dp),
            )
        }
    }
}

/** "Photo by … on Unsplash", required by the Unsplash API guidelines. */
@Composable
private fun PhotoCredit(name: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.small,
        color = Color.Black.copy(alpha = 0.55f),
        contentColor = Color.White,
    ) {
        Text(
            text = LocalAppStrings.current.photoCredit(name),
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        )
    }
}
