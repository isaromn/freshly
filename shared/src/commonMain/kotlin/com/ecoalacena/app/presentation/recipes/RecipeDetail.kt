package com.ecoalacena.app.presentation.recipes

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ecoalacena.app.domain.model.Recipe
import com.ecoalacena.app.presentation.i18n.LocalAppStrings

/**
 * Full view of one recipe while cooking: header picture, ingredients, and the steps as a
 * checklist with the overall progress on top.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun RecipeDetail(
    recipe: Recipe,
    completedSteps: Set<Int>,
    onToggleStep: (Int) -> Unit,
    onFinish: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = LocalAppStrings.current

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(text = recipe.title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = strings.back)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
    ) { innerPadding ->
        // Phones: one scrolling column. Wide windows: the dish on the left and the steps on the
        // right, so the photo keeps its shape and the checklist stays in view while cooking.
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            if (maxWidth >= TWO_PANE_MIN_WIDTH) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = innerPadding.calculateTopPadding()),
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                ) {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(start = 24.dp, bottom = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        recipeOverview(recipe)
                    }
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(end = 24.dp, bottom = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        recipeSteps(recipe, completedSteps, onToggleStep, onFinish)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = innerPadding.calculateTopPadding(),
                        bottom = innerPadding.calculateBottomPadding() + 24.dp,
                    ),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    recipeOverview(recipe)
                    recipeSteps(recipe, completedSteps, onToggleStep, onFinish)
                }
            }
        }
    }
}

/** Windows at least this wide show the recipe in two columns. */
private val TWO_PANE_MIN_WIDTH = 700.dp

/** Picture, title, time, difficulty and ingredients of the recipe. */
private fun LazyListScope.recipeOverview(recipe: Recipe) {
    item(key = "image") {
        RecipeHeaderImage(
            recipe = recipe,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(RECIPE_IMAGE_ASPECT_RATIO)
                .clip(MaterialTheme.shapes.large),
        )
    }

    item(key = "title") {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = recipe.title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            RecipeMeta(recipe)
        }
    }

    item(key = "ingredients") {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            DetailSectionTitle(LocalAppStrings.current.ingredientsTitle)
            recipe.ingredients.forEach { ingredient ->
                Text(text = "• $ingredient", style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}

/** Overall progress, the checklist of steps and the button to finish the recipe. */
private fun LazyListScope.recipeSteps(
    recipe: Recipe,
    completedSteps: Set<Int>,
    onToggleStep: (Int) -> Unit,
    onFinish: () -> Unit,
) {
    item(key = "progress") {
        StepsProgress(done = completedSteps.size, total = recipe.steps.size)
    }

    itemsIndexed(recipe.steps, key = { index, _ -> "step-$index" }) { index, step ->
        StepItem(
            number = index + 1,
            text = step,
            done = index in completedSteps,
            onToggle = { onToggleStep(index) },
        )
    }

    // Ticking off the last step also finishes the recipe; the button is there for anyone who
    // cooked without ticking every step.
    item(key = "finish") {
        Button(
            onClick = onFinish,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
        ) {
            Icon(Icons.Filled.Restaurant, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(LocalAppStrings.current.finishRecipe)
        }
    }
}

/** Preparation time and difficulty, shared by the list card and the detail view. */
@Composable
internal fun RecipeMeta(recipe: Recipe, modifier: Modifier = Modifier) {
    val strings = LocalAppStrings.current

    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = Icons.Filled.Schedule,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.width(4.dp))
        Text(
            text = strings.minutes(recipe.prepTimeMinutes),
            style = MaterialTheme.typography.labelLarge,
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = strings.difficulty(recipe.difficulty),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun StepsProgress(done: Int, total: Int) {
    val strings = LocalAppStrings.current
    val progress by animateFloatAsState(targetValue = if (total == 0) 0f else done.toFloat() / total)
    val finished = total > 0 && done == total

    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            DetailSectionTitle(strings.stepsTitle)
            Text(
                text = strings.stepsProgress(done, total),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(MaterialTheme.shapes.small),
                trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            )
            AnimatedVisibility(visible = finished) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = strings.allStepsDone,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}

/** One step of the recipe; the whole card toggles it. Done steps are crossed out and faded. */
@Composable
private fun StepItem(
    number: Int,
    text: String,
    done: Boolean,
    onToggle: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = if (done) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceContainerLowest
            },
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .toggleable(value = done, role = Role.Checkbox, onValueChange = { onToggle() })
                .padding(end = 16.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // The whole row is the touch target, so the checkbox itself does not handle clicks.
            Checkbox(checked = done, onCheckedChange = null, modifier = Modifier.padding(12.dp))
            Text(
                text = "$number. $text",
                style = MaterialTheme.typography.bodyLarge,
                textDecoration = if (done) TextDecoration.LineThrough else null,
                color = if (done) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                modifier = Modifier.padding(vertical = 8.dp),
            )
        }
    }
}

@Composable
private fun DetailSectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
    )
}
