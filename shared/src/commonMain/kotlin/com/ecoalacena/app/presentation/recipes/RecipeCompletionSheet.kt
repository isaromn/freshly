package com.ecoalacena.app.presentation.recipes

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Co2
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ecoalacena.app.domain.model.ImpactMetrics
import com.ecoalacena.app.presentation.i18n.LocalAppStrings
import com.ecoalacena.app.presentation.util.formatDecimal
import com.ecoalacena.app.presentation.util.formatGrouped
import com.ecoalacena.app.presentation.util.formatQuantity
import com.ecoalacena.app.presentation.util.groupSeparator
import com.ecoalacena.app.presentation.util.quantityStep
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random
import kotlinx.coroutines.launch

/**
 * "Recipe finished" sheet: first the user confirms which pantry items the meal used (all ticked at
 * first, with an adjustable amount), then the selection slides away and a celebration with
 * confetti counts up the food, money and CO2e this meal saved.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun RecipeCompletionSheet(
    completion: RecipeCompletion,
    onIntent: (RecipeIntent) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = { onIntent(RecipeIntent.DismissCompletion) },
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        CompletionContent(completion = completion, onIntent = onIntent)
    }
}

/** The selection, which slides away, and the celebration, which slides in once it is saved. */
@Composable
private fun CompletionContent(
    completion: RecipeCompletion,
    onIntent: (RecipeIntent) -> Unit,
) {
    val celebrating = completion.phase == CompletionPhase.CELEBRATING

    Box(modifier = Modifier.fillMaxWidth()) {
        AnimatedVisibility(
            visible = !celebrating,
            exit = slideOutVertically(tween(durationMillis = 350)) { height -> height / 2 } +
                fadeOut(tween(durationMillis = 250)),
        ) {
            SelectionContent(completion = completion, onIntent = onIntent)
        }

        AnimatedVisibility(
            visible = celebrating,
            enter = slideInVertically(spring(dampingRatio = Spring.DampingRatioLowBouncy)) { height -> height / 3 } +
                fadeIn(tween(durationMillis = 300, delayMillis = 150)),
        ) {
            CelebrationContent(
                impact = completion.impact,
                onDone = { onIntent(RecipeIntent.DismissCompletion) },
            )
        }
    }
}

@Composable
private fun SelectionContent(
    completion: RecipeCompletion,
    onIntent: (RecipeIntent) -> Unit,
) {
    val strings = LocalAppStrings.current
    val saving = completion.phase == CompletionPhase.SAVING

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
            .padding(bottom = 24.dp)
            .navigationBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        FestiveHeader(text = strings.completionTitle)

        if (completion.ingredients.isEmpty()) {
            Text(
                text = strings.completionNoPantryItems,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            Button(
                onClick = { onIntent(RecipeIntent.DismissCompletion) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(strings.completionDone)
            }
            return@Column
        }

        Text(
            text = strings.completionQuestion,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )

        completion.ingredients.forEach { ingredient ->
            IngredientRow(
                ingredient = ingredient,
                enabled = !saving,
                onToggle = { onIntent(RecipeIntent.ToggleCompletionIngredient(ingredient.item.id)) },
                onQuantityChange = { quantity ->
                    onIntent(RecipeIntent.SetCompletionQuantity(ingredient.item.id, quantity))
                },
            )
        }

        completion.errorMessage?.let { message ->
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
            )
        }

        Button(
            onClick = { onIntent(RecipeIntent.ConfirmCompletion) },
            enabled = !saving && completion.selectedCount > 0,
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (saving) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
            } else {
                Text(strings.discountFromPantry)
            }
        }
        TextButton(
            onClick = { onIntent(RecipeIntent.DismissCompletion) },
            enabled = !saving,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(strings.notNow)
        }
    }
}

/** The title pops in with a soft spring the first time the sheet appears. */
@Composable
private fun FestiveHeader(text: String) {
    val visible = remember { MutableTransitionState(false).apply { targetState = true } }

    AnimatedVisibility(
        visibleState = visible,
        enter = scaleIn(
            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
            initialScale = 0.6f,
        ) + fadeIn(spring(stiffness = Spring.StiffnessLow)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/**
 * One ingredient: the whole row ticks it on and off, and the checkbox gives a small bounce and
 * tilt each time. Unticked rows fade so it is clear they will stay in the pantry.
 */
@Composable
private fun IngredientRow(
    ingredient: CompletionIngredient,
    enabled: Boolean,
    onToggle: () -> Unit,
    onQuantityChange: (Double) -> Unit,
) {
    val strings = LocalAppStrings.current
    val item = ingredient.item
    val unit = strings.unitShort(item.unit)

    val scale = remember { Animatable(1f) }
    val rotation = remember { Animatable(0f) }
    var firstComposition by remember { mutableStateOf(true) }
    LaunchedEffect(ingredient.selected) {
        if (firstComposition) {
            firstComposition = false
            return@LaunchedEffect
        }
        val bounce = spring<Float>(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)
        launch {
            scale.snapTo(0.75f)
            scale.animateTo(1f, bounce)
        }
        launch {
            rotation.snapTo(if (ingredient.selected) -15f else 15f)
            rotation.animateTo(0f, bounce)
        }
    }
    val rowAlpha by animateFloatAsState(if (ingredient.selected) 1f else 0.55f)

    Surface(
        shape = MaterialTheme.shapes.medium,
        color = if (ingredient.selected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerHigh
        },
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { alpha = rowAlpha },
    ) {
        Row(
            modifier = Modifier
                .toggleable(value = ingredient.selected, enabled = enabled, role = Role.Checkbox) { onToggle() }
                .padding(end = 8.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // The row handles the click, so the checkbox only shows the state.
            Checkbox(
                checked = ingredient.selected,
                onCheckedChange = null,
                enabled = enabled,
                modifier = Modifier
                    .padding(12.dp)
                    .graphicsLayer {
                        scaleX = scale.value
                        scaleY = scale.value
                        rotationZ = rotation.value
                    },
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = strings.inPantry("${formatQuantity(item.quantity)} $unit"),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (ingredient.selected) {
                val step = quantityStep(item.unit)
                val minimum = min(step, item.quantity)
                IconButton(
                    onClick = { onQuantityChange((ingredient.quantity - step).coerceAtLeast(minimum)) },
                    enabled = enabled && ingredient.quantity > minimum,
                    modifier = Modifier.size(36.dp),
                ) {
                    Icon(Icons.Filled.Remove, contentDescription = strings.lessQuantity)
                }
                QuantityField(
                    quantity = ingredient.quantity,
                    unit = unit,
                    enabled = enabled,
                    onQuantityChange = onQuantityChange,
                )
                IconButton(
                    onClick = { onQuantityChange(min(ingredient.quantity + step, item.quantity)) },
                    enabled = enabled && ingredient.quantity < item.quantity,
                    modifier = Modifier.size(36.dp),
                ) {
                    Icon(Icons.Filled.Add, contentDescription = strings.moreQuantity)
                }
            }
        }
    }
}

/**
 * The amount of an ingredient. It reads like a small chip; a tap turns it into a text field with the
 * number selected, so typing replaces it. The value is applied with the keyboard's Done key or when
 * the field loses focus; anything that is not a positive number is ignored.
 */
@Composable
private fun QuantityField(
    quantity: Double,
    unit: String,
    enabled: Boolean,
    onQuantityChange: (Double) -> Unit,
) {
    val strings = LocalAppStrings.current
    var editing by remember { mutableStateOf(false) }
    val shape = MaterialTheme.shapes.small
    val chipModifier = Modifier.width(76.dp)

    if (!editing) {
        Surface(
            onClick = { editing = true },
            enabled = enabled,
            shape = shape,
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
            modifier = chipModifier.semantics { contentDescription = strings.editQuantity },
        ) {
            Text(
                text = "${formatQuantity(quantity)} $unit",
                style = MaterialTheme.typography.labelLarge,
                textAlign = TextAlign.Center,
                maxLines = 1,
                modifier = Modifier.padding(vertical = 8.dp),
            )
        }
        return
    }

    val start = formatQuantity(quantity)
    var field by remember { mutableStateOf(TextFieldValue(start, selection = TextRange(0, start.length))) }
    val focusRequester = remember { FocusRequester() }
    var hadFocus by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    fun commit() {
        field.text.trim().replace(',', '.').toDoubleOrNull()
            ?.takeIf { it.isFinite() && it > 0.0 }
            ?.let(onQuantityChange)
        editing = false
    }

    BasicTextField(
        value = field,
        onValueChange = { field = it },
        singleLine = true,
        textStyle = MaterialTheme.typography.labelLarge.copy(
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        ),
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { commit() }),
        modifier = chipModifier
            .focusRequester(focusRequester)
            .onFocusChanged { state ->
                // Leaving the field (tapping elsewhere) keeps what was typed.
                if (state.isFocused) hadFocus = true else if (hadFocus) commit()
            },
        decorationBox = { inner ->
            Surface(
                shape = shape,
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary),
            ) {
                Box(modifier = Modifier.padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                    inner()
                }
            }
        },
    )
}

/**
 * The "done" badge of the celebration: a green circle pops in with a bounce, a check mark draws
 * itself stroke by stroke, a ring pulses outwards and little sparkles twinkle around it.
 */
@Composable
private fun CelebrationBadge() {
    val circleScale = remember { Animatable(0f) }
    val checkProgress = remember { Animatable(0f) }
    val sparklesIn = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        launch { circleScale.animateTo(1f, spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessLow)) }
        launch {
            checkProgress.animateTo(1f, tween(durationMillis = 450, delayMillis = 300, easing = FastOutSlowInEasing))
        }
        launch { sparklesIn.animateTo(1f, tween(durationMillis = 400, delayMillis = 600)) }
    }

    val loop = rememberInfiniteTransition()
    val pulse by loop.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(durationMillis = 1_600, easing = LinearEasing)),
    )
    val twinkle by loop.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(durationMillis = 1_200, easing = LinearEasing)),
    )

    val circleColor = MaterialTheme.colorScheme.primary
    val checkColor = MaterialTheme.colorScheme.onPrimary
    val sparkleColors = listOf(MaterialTheme.colorScheme.tertiary, circleColor, Color(0xFFF2C14E))
    val checkPath = remember { Path() }
    val drawnCheck = remember { Path() }
    val measure = remember { PathMeasure() }

    Canvas(modifier = Modifier.size(128.dp)) {
        val center = this.center
        val radius = 40.dp.toPx()

        // A ring that grows and fades out, over and over.
        drawCircle(
            color = circleColor.copy(alpha = 0.35f * (1f - pulse)),
            radius = radius + (size.minDimension / 2 - radius) * pulse,
            style = Stroke(width = 3.dp.toPx()),
        )

        // Sparkles: small four-pointed stars that twinkle, each one at its own beat.
        SPARKLES.forEachIndexed { index, sparkle ->
            val beat = (twinkle + sparkle.phase) % 1f
            val shine = 0.5f + 0.5f * sin(beat * 2f * PI.toFloat())
            val distance = radius + sparkle.distanceDp.dp.toPx()
            val position = Offset(
                x = center.x + distance * cos(sparkle.angle),
                y = center.y + distance * sin(sparkle.angle),
            )
            drawSparkle(
                center = position,
                size = sparkle.sizeDp.dp.toPx() * sparklesIn.value * (0.6f + 0.4f * shine),
                color = sparkleColors[index % sparkleColors.size].copy(alpha = sparklesIn.value * (0.4f + 0.6f * shine)),
            )
        }

        scale(circleScale.value, pivot = center) {
            drawCircle(color = circleColor, radius = radius)

            // The check mark, drawn up to the current progress.
            checkPath.reset()
            checkPath.moveTo(center.x - radius * 0.42f, center.y + radius * 0.02f)
            checkPath.lineTo(center.x - radius * 0.12f, center.y + radius * 0.32f)
            checkPath.lineTo(center.x + radius * 0.45f, center.y - radius * 0.30f)
            measure.setPath(checkPath, false)
            drawnCheck.reset()
            measure.getSegment(0f, measure.length * checkProgress.value, drawnCheck, true)
            drawPath(
                path = drawnCheck,
                color = checkColor,
                style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
            )
        }
    }
}

/** Where each sparkle sits around the badge and how it twinkles. */
private class Sparkle(val angle: Float, val distanceDp: Float, val sizeDp: Float, val phase: Float)

private val SPARKLES = listOf(
    Sparkle(angle = -2.4f, distanceDp = 14f, sizeDp = 9f, phase = 0.0f),
    Sparkle(angle = -1.2f, distanceDp = 18f, sizeDp = 6f, phase = 0.35f),
    Sparkle(angle = -0.3f, distanceDp = 12f, sizeDp = 8f, phase = 0.7f),
    Sparkle(angle = 0.9f, distanceDp = 16f, sizeDp = 7f, phase = 0.2f),
    Sparkle(angle = 2.1f, distanceDp = 13f, sizeDp = 9f, phase = 0.55f),
    Sparkle(angle = 3.0f, distanceDp = 19f, sizeDp = 5f, phase = 0.85f),
)

/** A four-pointed star, like a twinkle. */
private fun DrawScope.drawSparkle(center: Offset, size: Float, color: Color) {
    if (size <= 0f) return
    val inner = size * 0.28f
    val star = Path().apply {
        moveTo(center.x, center.y - size)
        lineTo(center.x + inner, center.y - inner)
        lineTo(center.x + size, center.y)
        lineTo(center.x + inner, center.y + inner)
        lineTo(center.x, center.y + size)
        lineTo(center.x - inner, center.y + inner)
        lineTo(center.x - size, center.y)
        lineTo(center.x - inner, center.y - inner)
        close()
    }
    drawPath(star, color)
}

/** Confetti and counters that climb up to the impact of this meal. */
@Composable
private fun CelebrationContent(
    impact: ImpactMetrics?,
    onDone: () -> Unit,
) {
    val strings = LocalAppStrings.current
    val metrics = impact ?: return
    val separator = strings.groupSeparator()

    Box(modifier = Modifier.fillMaxWidth()) {
        Confetti(modifier = Modifier.matchParentSize())

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp)
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            CelebrationBadge()
            Text(
                text = strings.completionDoneTitle,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
            )

            if (metrics.savedKg > 0.0) {
                val kg = countUp(metrics.savedKg)
                val money = countUp(metrics.moneySavedCop)
                val co2 = countUp(metrics.co2AvoidedKg)

                ImpactCounter(
                    icon = Icons.Filled.Eco,
                    label = strings.completionFoodSaved,
                    value = "${formatDecimal(kg, 2)} kg",
                )
                ImpactCounter(
                    icon = Icons.Filled.Savings,
                    label = strings.moneyTitle,
                    value = "\$ ${formatGrouped(money.toLong(), separator)} COP",
                )
                ImpactCounter(
                    icon = Icons.Filled.Co2,
                    label = strings.co2Title,
                    value = "${formatDecimal(co2, 2)} ${strings.co2Unit}",
                )
            } else {
                Text(
                    text = strings.completionNothingCounted,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }

            Button(onClick = onDone, modifier = Modifier.fillMaxWidth()) {
                Text(strings.completionDone)
            }
        }
    }
}

@Composable
private fun ImpactCounter(icon: ImageVector, label: String, value: String) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(28.dp))
            Column {
                Text(text = label, style = MaterialTheme.typography.labelLarge)
                Text(text = value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
        }
    }
}

/** Climbs from 0 to [target] once, so the numbers "count up" when the celebration appears. */
@Composable
private fun countUp(target: Double): Double {
    val value = remember { Animatable(0f) }
    LaunchedEffect(target) {
        value.animateTo(target.toFloat(), tween(durationMillis = COUNT_UP_MILLIS, delayMillis = 250))
    }
    return value.value.toDouble()
}

private const val COUNT_UP_MILLIS = 1_200
private const val CONFETTI_MILLIS = 2_400
private const val CONFETTI_PIECES = 70

/** One piece of confetti; positions and speeds are fractions of the area it falls through. */
private class ConfettiPiece(
    val x: Float,
    val drift: Float,
    val delay: Float,
    val speed: Float,
    val spin: Float,
    val sizeDp: Float,
    val colorIndex: Int,
)

/** Simple falling confetti drawn on a canvas, in the theme colors, played once. */
@Composable
private fun Confetti(modifier: Modifier = Modifier) {
    val colors = listOf(
        MaterialTheme.colorScheme.primary,
        MaterialTheme.colorScheme.tertiary,
        MaterialTheme.colorScheme.secondary,
        MaterialTheme.colorScheme.error,
        Color(0xFFF2C14E),
    )
    val pieces = remember {
        List(CONFETTI_PIECES) { index ->
            ConfettiPiece(
                x = Random.nextFloat(),
                drift = (Random.nextFloat() - 0.5f) * 0.3f,
                delay = Random.nextFloat() * 0.35f,
                speed = 0.8f + Random.nextFloat() * 0.5f,
                spin = Random.nextFloat() * 720f - 360f,
                sizeDp = 6f + Random.nextFloat() * 6f,
                colorIndex = index,
            )
        }
    }
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        progress.animateTo(1f, tween(durationMillis = CONFETTI_MILLIS, easing = LinearEasing))
    }

    Canvas(modifier = modifier) {
        val p = progress.value
        pieces.forEach { piece ->
            val t = ((p - piece.delay) / (1f - piece.delay)).coerceIn(0f, 1f)
            if (t <= 0f || t >= 1f) return@forEach

            val side = piece.sizeDp * density
            val center = Offset(
                x = (piece.x + piece.drift * t) * size.width,
                y = -side + t * piece.speed * (size.height + side * 2),
            )
            rotate(degrees = piece.spin * t, pivot = center) {
                drawRect(
                    color = colors[piece.colorIndex % colors.size],
                    topLeft = Offset(center.x - side / 2, center.y - side * 0.3f),
                    size = Size(side, side * 0.6f),
                    alpha = 1f - t * 0.4f,
                )
            }
        }
    }
}
