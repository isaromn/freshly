package com.ecoalacena.app.presentation.scan

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ecoalacena.app.core.MILLIS_PER_DAY
import com.ecoalacena.app.core.currentEpochMillis
import com.ecoalacena.app.core.i18n.AppStrings
import com.ecoalacena.app.domain.model.PantryItem
import com.ecoalacena.app.presentation.i18n.LocalAppStrings
import com.ecoalacena.app.presentation.platform.PlatformBackHandler
import com.ecoalacena.app.presentation.platform.rememberCameraLauncher
import com.ecoalacena.app.presentation.util.formatQuantity
import com.ecoalacena.app.presentation.util.icon
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.compose.rememberFilePickerLauncher
import io.github.vinceglb.filekit.extension
import io.github.vinceglb.filekit.readBytes
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.decodeToImageBitmap
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ScanScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ScanViewModel = koinViewModel(),
) {
    val strings = LocalAppStrings.current
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val currentOnBack by rememberUpdatedState(onBack)
    val scope = rememberCoroutineScope()

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                ScanEffect.NavigateBack -> currentOnBack()
                is ScanEffect.ShowMessage -> snackbarHostState.showSnackbar(effect.message)
            }
        }
    }

    fun onImagePicked(file: PlatformFile?) {
        // A null file means the user cancelled the picker.
        if (file == null) return
        scope.launch {
            try {
                val image = ScanImage(
                    bytes = file.readBytes(),
                    mimeType = file.extension.toImageMimeType(),
                )
                viewModel.onIntent(ScanIntent.ImageSelected(image))
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (error: Exception) {
                viewModel.onIntent(ScanIntent.ImagePickFailed(strings.readImageError))
            }
        }
    }

    val galleryLauncher = rememberFilePickerLauncher(
        type = FileKitType.Image,
        onError = {
            viewModel.onIntent(ScanIntent.ImagePickFailed(strings.openPickerError))
        },
        onResult = { file -> onImagePicked(file) },
    )
    // `null` on desktop, which has no camera picker: the file explorer is used instead.
    val takePhoto: (() -> Unit)? = rememberCameraLauncher(
        onResult = { file -> onImagePicked(file) },
        onError = {
            viewModel.onIntent(ScanIntent.ImagePickFailed(strings.openCameraError))
        },
    )

    val handleBack = {
        viewModel.onIntent(ScanIntent.Reset)
        currentOnBack()
    }
    PlatformBackHandler(onBack = handleBack)

    ScanContent(
        state = state,
        snackbarHostState = snackbarHostState,
        onIntent = viewModel::onIntent,
        onPickFromGallery = { galleryLauncher.launch() },
        onTakePhoto = takePhoto,
        onBack = handleBack,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ScanContent(
    state: ScanState,
    snackbarHostState: SnackbarHostState,
    onIntent: (ScanIntent) -> Unit,
    onPickFromGallery: () -> Unit,
    onTakePhoto: (() -> Unit)?,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = LocalAppStrings.current

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(strings.tabScanner) },
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            when (state.status) {
                ScanStatus.IDLE -> PickSourceContent(
                    onPickFromGallery = onPickFromGallery,
                    onTakePhoto = onTakePhoto,
                    onLoadDemo = { onIntent(ScanIntent.LoadDemoReceipt) },
                )

                ScanStatus.ANALYZING -> AnalyzingContent(image = state.image)

                ScanStatus.ERROR -> ErrorContent(
                    image = state.image,
                    message = state.errorMessage.orEmpty(),
                    onRetry = { onIntent(ScanIntent.Retry) },
                    onChooseAnother = { onIntent(ScanIntent.Reset) },
                    onLoadDemo = { onIntent(ScanIntent.LoadDemoReceipt) },
                )

                ScanStatus.REVIEW, ScanStatus.SAVING -> ReviewContent(
                    state = state,
                    onIntent = onIntent,
                )
            }
        }
    }
}

@Composable
private fun PickSourceContent(
    onPickFromGallery: () -> Unit,
    onTakePhoto: (() -> Unit)?,
    onLoadDemo: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = LocalAppStrings.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(96.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.DocumentScanner,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }

        Spacer(Modifier.height(24.dp))

        Text(
            text = strings.scanPickTitle,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = strings.scanPickDescription,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(32.dp))

        if (onTakePhoto != null) {
            Button(
                onClick = onTakePhoto,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Filled.CameraAlt, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(strings.takePhoto)
            }
            Spacer(Modifier.height(12.dp))
            OutlinedButton(
                onClick = onPickFromGallery,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Filled.PhotoLibrary, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(strings.pickFromGallery)
            }
        } else {
            Button(
                onClick = onPickFromGallery,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Filled.PhotoLibrary, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(strings.pickImage)
            }
        }

        Spacer(Modifier.height(24.dp))

        TextButton(onClick = onLoadDemo) {
            Icon(Icons.Filled.Receipt, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(strings.demoLoadButton)
        }
        Text(
            text = strings.demoHint,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun AnalyzingContent(
    image: ScanImage?,
    modifier: Modifier = Modifier,
) {
    val strings = LocalAppStrings.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        if (image != null) {
            ImagePreview(
                image = image,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 320.dp),
            )
            Spacer(Modifier.height(24.dp))
        }
        CircularProgressIndicator()
        Spacer(Modifier.height(16.dp))
        Text(
            text = strings.analyzing,
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text = strings.analyzingHint,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ErrorContent(
    image: ScanImage?,
    message: String,
    onRetry: () -> Unit,
    onChooseAnother: () -> Unit,
    onLoadDemo: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = LocalAppStrings.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        if (image != null) {
            ImagePreview(
                image = image,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 240.dp),
            )
            Spacer(Modifier.height(24.dp))
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer,
            ),
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Filled.ErrorOutline, contentDescription = null)
                Spacer(Modifier.width(12.dp))
                Text(text = message, style = MaterialTheme.typography.bodyMedium)
            }
        }

        Spacer(Modifier.height(24.dp))

        Button(onClick = onRetry, modifier = Modifier.fillMaxWidth()) {
            Text(strings.retry)
        }
        Spacer(Modifier.height(12.dp))
        OutlinedButton(onClick = onChooseAnother, modifier = Modifier.fillMaxWidth()) {
            Text(strings.chooseAnother)
        }
        Spacer(Modifier.height(4.dp))
        TextButton(onClick = onLoadDemo) {
            Text(strings.demoUseButton)
        }
    }
}

@Composable
private fun ReviewContent(
    state: ScanState,
    onIntent: (ScanIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = LocalAppStrings.current
    val now = remember { currentEpochMillis() }
    val isSaving = state.status == ScanStatus.SAVING
    val count = state.drafts.size

    Column(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(key = "header") {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = strings.foundProducts(count),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = strings.reviewHint,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            items(state.drafts, key = { it.id }) { draft ->
                DraftItemCard(
                    draft = draft,
                    now = now,
                    enabled = !isSaving,
                    onRemove = { onIntent(ScanIntent.RemoveDraft(draft.id)) },
                    onAdjustDays = { delta -> onIntent(ScanIntent.AdjustExpiration(draft.id, delta)) },
                )
            }
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            tonalElevation = 2.dp,
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(
                    onClick = { onIntent(ScanIntent.Confirm) },
                    enabled = count > 0 && !isSaving,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(strings.saving)
                    } else {
                        Text(strings.addToPantry(count))
                    }
                }
                OutlinedButton(
                    onClick = { onIntent(ScanIntent.Reset) },
                    enabled = !isSaving,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(strings.discardAndChoose)
                }
            }
        }
    }
}

@Composable
private fun DraftItemCard(
    draft: PantryItem,
    now: Long,
    enabled: Boolean,
    onRemove: () -> Unit,
    onAdjustDays: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = LocalAppStrings.current

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = draft.category.icon,
                    contentDescription = strings.categoryLabel(draft.category),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = draft.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    if (draft.isOrganic) {
                        Spacer(Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Filled.Eco,
                            contentDescription = strings.organic,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
                Text(
                    text = "${formatQuantity(draft.quantity)} ${strings.unitShort(draft.unit)} · " +
                        strings.categoryLabel(draft.category),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { onAdjustDays(-1) }, enabled = enabled) {
                        Icon(Icons.Filled.Remove, contentDescription = strings.oneDayLess)
                    }
                    Text(
                        text = expirationLabel(strings, draft.expirationDate, now),
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    IconButton(onClick = { onAdjustDays(1) }, enabled = enabled) {
                        Icon(Icons.Filled.Add, contentDescription = strings.oneDayMore)
                    }
                }
            }

            IconButton(onClick = onRemove, enabled = enabled) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = strings.removeProduct,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun ImagePreview(
    image: ScanImage,
    modifier: Modifier = Modifier,
) {
    val strings = LocalAppStrings.current

    // Decode off the main thread: a camera photo can be several megapixels.
    val bitmap by produceState<ImageBitmap?>(initialValue = null, image) {
        value = withContext(Dispatchers.Default) {
            runCatching { image.bytes.decodeToImageBitmap() }.getOrNull()
        }
    }

    val shape = MaterialTheme.shapes.large
    val currentBitmap = bitmap
    if (currentBitmap != null) {
        Image(
            bitmap = currentBitmap,
            contentDescription = strings.imagePreview,
            contentScale = ContentScale.Fit,
            modifier = modifier.clip(shape),
        )
    } else {
        Box(
            modifier = modifier
                .height(160.dp)
                .clip(shape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.PhotoLibrary,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun expirationLabel(strings: AppStrings, expirationDate: Long?, now: Long): String {
    if (expirationDate == null) return strings.noExpirationLabel

    // Round to the nearest day: the suggestion was computed a moment before `now`.
    val days = ((expirationDate - now + MILLIS_PER_DAY / 2) / MILLIS_PER_DAY).toInt()
    return if (days <= 0) strings.expiresToday else strings.expiresIn(days)
}

private fun String.toImageMimeType(): String =
    when (lowercase()) {
        "png" -> "image/png"
        "webp" -> "image/webp"
        "heic" -> "image/heic"
        "heif" -> "image/heif"
        else -> "image/jpeg"
    }
