package com.tagaev.trrcrm.ui.complectation

import com.tagaev.trrcrm.ui.i18n.s

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.tagaev.trrcrm.data.MainRepository
import com.tagaev.trrcrm.data.remote.Resource
import com.tagaev.trrcrm.data.remote.userFacingMessage
import com.tagaev.trrcrm.models.ImageDocumentType
import com.tagaev.trrcrm.models.ImageMediatorImageMeta
import com.tagaev.trrcrm.ui.common.ZoomableImagePreview
import com.tagaev.trrcrm.ui.common.rememberBusyActionGate
import com.tagaev.trrcrm.ui.permissions.decodePhotoThumbnail
import compose.icons.FeatherIcons
import compose.icons.feathericons.ArrowLeft
import compose.icons.feathericons.RefreshCw
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import org.koin.compose.koinInject

private sealed interface DocumentPhotoCellState {
    data object Loading : DocumentPhotoCellState
    data class Loaded(val bitmap: ImageBitmap) : DocumentPhotoCellState
    data class Error(val message: String) : DocumentPhotoCellState
}

private data class DocumentPhotosPageUi(
    val page: Int,
    val totalPages: Int,
    val totalCount: Int,
    val hasPrevious: Boolean,
    val hasNext: Boolean,
    val images: List<ImageMediatorImageMeta>,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentPhotosViewerScreen(
    documentNumber: String,
    documentType: ImageDocumentType = ImageDocumentType.Complects,
    onBack: () -> Unit,
) {
    val repository = koinInject<MainRepository>()
    val scope = rememberCoroutineScope()
    val photoActionGate = rememberBusyActionGate()

    var isLoadingPage by remember { mutableStateOf(true) }
    var pageError by remember { mutableStateOf<String?>(null) }
    var pageUi by remember { mutableStateOf<DocumentPhotosPageUi?>(null) }
    var currentPage by remember { mutableStateOf(1) }
    val cellStates = remember { mutableStateMapOf<String, DocumentPhotoCellState>() }
    var previewBitmap by remember { mutableStateOf<ImageBitmap?>(null) }

    val isViewerBusy = photoActionGate.isBusy
        || isLoadingPage
        || cellStates.values.any { it is DocumentPhotoCellState.Loading }

    suspend fun loadImageCell(image: ImageMediatorImageMeta) {
        if (cellStates[image.imageId] is DocumentPhotoCellState.Loaded) return
        cellStates[image.imageId] = DocumentPhotoCellState.Loading
        when (val result = repository.downloadFixatorDocumentImage(
            documentType = documentType,
            documentNumber = documentNumber,
            imageId = image.imageId,
            contentUrl = image.contentUrl,
        )) {
            is Resource.Success -> {
                val bitmap = decodePhotoThumbnail(result.data)
                if (bitmap != null) {
                    cellStates[image.imageId] = DocumentPhotoCellState.Loaded(bitmap)
                } else {
                    cellStates[image.imageId] = DocumentPhotoCellState.Error(s("complectation_ne_udalos_otobrazit_foto"))
                }
            }
            is Resource.Error -> {
                cellStates[image.imageId] = DocumentPhotoCellState.Error(
                    userFacingMessage(
                        result.causes ?: s("complectation_ne_udalos_zagruzit_fotografii"),
                        s("complectation_ne_udalos_zagruzit_fotografii"),
                    ),
                )
            }
            is Resource.Loading -> Unit
        }
    }

    suspend fun loadPageImages(images: List<ImageMediatorImageMeta>) {
        if (images.isEmpty()) return
        coroutineScope {
            val semaphore = Semaphore(3)
            images.map { image ->
                async {
                    semaphore.withPermit {
                        loadImageCell(image)
                    }
                }
            }.awaitAll()
        }
    }

    suspend fun loadPageInternal(page: Int) {
        isLoadingPage = true
        pageError = null
        cellStates.clear()
        try {
            when (val result = repository.listFixatorDocumentImages(documentNumber, page, documentType)) {
                is Resource.Success -> {
                    val data = result.data
                    pageUi = DocumentPhotosPageUi(
                        page = data.page,
                        totalPages = data.totalPages.coerceAtLeast(1),
                        totalCount = data.totalCount,
                        hasPrevious = data.hasPrevious,
                        hasNext = data.hasNext,
                        images = data.images,
                    )
                    currentPage = data.page
                    loadPageImages(data.images)
                }
                is Resource.Error -> {
                    pageUi = null
                    pageError = userFacingMessage(
                        result.causes ?: s("complectation_ne_udalos_zagruzit_fotografii"),
                        s("complectation_ne_udalos_zagruzit_fotografii"),
                    )
                }
                is Resource.Loading -> Unit
            }
        } finally {
            isLoadingPage = false
        }
    }

    fun loadPage(page: Int) {
        photoActionGate.launch(scope) {
            loadPageInternal(page)
        }
    }

    fun refreshPhotos() {
        photoActionGate.launch(scope) {
            repository.clearFixatorDocumentPhotoCache(documentType, documentNumber)
            loadPageInternal(currentPage)
        }
    }

    LaunchedEffect(documentNumber) {
        currentPage = 1
        photoActionGate.run {
            loadPageInternal(1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(s("complectation_fotografii_documentnumber", documentNumber)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(FeatherIcons.ArrowLeft, contentDescription = s("settings_nazad"))
                    }
                },
                actions = {
                    if (isViewerBusy) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .padding(horizontal = 8.dp)
                                .size(18.dp),
                            strokeWidth = 2.dp,
                        )
                    } else {
                        IconButton(onClick = ::refreshPhotos) {
                            Icon(FeatherIcons.RefreshCw, contentDescription = s("menu_obnovit"))
                        }
                    }
                },
            )
        },
        bottomBar = {
            val ui = pageUi
            if (ui != null && !isLoadingPage && pageError == null) {
                Surface(tonalElevation = 3.dp) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = s("complectation_vsego_ui_totalcount_foto_kotoroe_bylo_snyato_s_prilo", ui.totalCount),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            OutlinedButton(
                                onClick = { loadPage(ui.page - 1) },
                                enabled = ui.hasPrevious && !isViewerBusy,
                            ) {
                                Text(s("settings_nazad"))
                            }
                            Text(
                                text = s("complectation_stranitsa_ui_page_iz_ui_totalpages", ui.page, ui.totalPages),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            OutlinedButton(
                                onClick = { loadPage(ui.page + 1) },
                                enabled = ui.hasNext && !isViewerBusy,
                            ) {
                                Text(s("complectation_dalee"))
                            }
                        }
                    }
                }
            }
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            when {
                isLoadingPage -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                pageError != null -> {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(
                            text = pageError.orEmpty(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                        )
                        Button(
                            onClick = { loadPage(currentPage) },
                            enabled = !isViewerBusy,
                        ) {
                            Text(s("login_povtorit"))
                        }
                    }
                }
                pageUi?.images.isNullOrEmpty() -> {
                    Text(
                        text = s("complectation_fotografiy_net"),
                        modifier = Modifier.align(Alignment.Center),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                else -> {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(
                            items = pageUi!!.images,
                            key = { it.imageId },
                        ) { image ->
                            DocumentPhotoGridCell(
                                state = cellStates[image.imageId] ?: DocumentPhotoCellState.Loading,
                                enabled = !isViewerBusy,
                                onPreview = {
                                    val loaded = cellStates[image.imageId] as? DocumentPhotoCellState.Loaded
                                    if (loaded != null) {
                                        previewBitmap = loaded.bitmap
                                    }
                                },
                                onRetry = {
                                    photoActionGate.launch(scope) {
                                        loadImageCell(image)
                                    }
                                },
                            )
                        }
                    }
                }
            }
        }
    }

    previewBitmap?.let { bitmap ->
        ZoomableImagePreview(
            bitmap = bitmap,
            onDismiss = { previewBitmap = null },
        )
    }
}

@Composable
private fun DocumentPhotoGridCell(
    state: DocumentPhotoCellState,
    enabled: Boolean,
    onPreview: () -> Unit,
    onRetry: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(
                enabled = enabled && (state is DocumentPhotoCellState.Loaded || state is DocumentPhotoCellState.Error),
                onClick = {
                    when (state) {
                        is DocumentPhotoCellState.Loaded -> onPreview()
                        is DocumentPhotoCellState.Error -> onRetry()
                        DocumentPhotoCellState.Loading -> Unit
                    }
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        when (state) {
            DocumentPhotoCellState.Loading -> {
                CircularProgressIndicator(modifier = Modifier.size(28.dp), strokeWidth = 2.dp)
            }
            is DocumentPhotoCellState.Loaded -> {
                Image(
                    bitmap = state.bitmap,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            }
            is DocumentPhotoCellState.Error -> {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(8.dp),
                ) {
                    Icon(
                        FeatherIcons.RefreshCw,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = s("login_povtorit"),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
