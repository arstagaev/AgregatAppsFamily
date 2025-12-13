package com.tagaev.trrcrm.ui.master_screen

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.tagaev.trrcrm.data.remote.Resource
import com.tagaev.trrcrm.ui.custom.ScreenWithDismissableKeyboard
import com.tagaev.trrcrm.ui.custom.snowflakeBackground
import compose.icons.FeatherIcons
import compose.icons.feathericons.Filter
import compose.icons.feathericons.RefreshCw
import kotlin.collections.orEmpty
import kotlin.random.Random



enum class MasterPanel {
    List,
    Filter,
    Details
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T, F> MasterScreen(
    title: String,
    resource: Resource<List<T>>,
    emptyText: String,
    errorText: String,
    notFoundText: String,
    refineState: F,
    panel: MasterPanel,
    onPanelChange: (MasterPanel) -> Unit,
    onRefresh: () -> Unit,
    onLoadMore: () -> Unit,
    onFilterChanged: (F) -> Unit,
    itemId: (T) -> String,
    isItemChanged: (old: T, new: T) -> Boolean,
    listItem: @Composable (item: T, isChanged: Boolean, onClick: () -> Unit) -> Unit,
    detailsContent: @Composable (item: T, onClose: () -> Unit) -> Unit,
    filterScreen: @Composable (
        current: F,
        onDismiss: () -> Unit,
        onApply: (F) -> Unit
    ) -> Unit,
    modifier: Modifier = Modifier,
    selectedItemId: String?,
    onSelectedItemChange: (String?) -> Unit,
) {
    val listState: LazyListState = rememberLazyListState()
    val isLoadingTopBar = resource is Resource.Loading ||
            (resource as? Resource.Success<*>)?.additionalLoading == true

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Row(Modifier.clickable {
                        onPanelChange(MasterPanel.List)
                    }) {
                        Text(text = title, modifier = Modifier.padding(horizontal = 10.dp))
                    }
                },
                actions = {
                    if (panel == MasterPanel.List) {
//                    if (true) {
                        IconButton(onClick = { onPanelChange(MasterPanel.Filter) }) {
                            Icon(FeatherIcons.Filter, contentDescription = "Фильтр")
                        }
                        if (isLoadingTopBar) {
                            CircularProgressIndicator(
                                modifier = Modifier
                                    .padding(horizontal = 8.dp)
                                    .size(18.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            IconButton(onClick = onRefresh) {
                                Icon(FeatherIcons.RefreshCw, contentDescription = "Обновить")
                            }
                        }
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            when (panel) {
                MasterPanel.List -> {
                    when (val state = resource) {
                        is Resource.Loading -> {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator()
                            }
                        }

                        is Resource.Error -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = errorText,
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Spacer(Modifier.height(8.dp))
//                                state.causes?.let {
//                                    Text(
//                                        text = it,
//                                        style = MaterialTheme.typography.bodySmall,
//                                        color = MaterialTheme.colorScheme.error
//                                    )
//                                }
                                Spacer(Modifier.height(16.dp))
                                Button(onClick = onRefresh) {
                                    Text("Повторить")
                                }
                            }
                        }

                        is Resource.Success -> {
                            val allItems = state.data.orEmpty()

                            if (allItems.isEmpty() && state.additionalLoading == false) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(notFoundText)
                                }
                            } else if (allItems.isEmpty() && state.additionalLoading) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator()
                                }
                            } else {
                                LiveListWrapper(
                                    items = allItems,
                                    maxItems = 1000,
                                    idSelector = itemId,
                                    contentChanged = isItemChanged,
                                    modifier = Modifier.fillMaxSize(),
                                    listState = listState,
                                    onRefresh = { _, _ -> onRefresh() },
                                    onLoadMore = { _, _ ->
                                        onLoadMore()
                                    }
                                ) { item, isChanged, _ ->
                                    listItem(item, isChanged) {
                                        onSelectedItemChange(itemId(item))
                                        onPanelChange(MasterPanel.Details)
                                    }
                                }
                            }
                        }
                    }
                }

                MasterPanel.Filter -> {
                    // Full-screen filter screen instead of dialog
                    filterScreen(
                        refineState,
                        { onPanelChange(MasterPanel.List) },
                        { newFilter ->
                            onFilterChanged(newFilter)
                            onPanelChange(MasterPanel.List)
                        }
                    )
                }

                MasterPanel.Details -> {
                    // Full-screen details instead of bottom sheet
                    val current = (resource as? Resource.Success<List<T>>)
                        ?.data
                        .orEmpty()
                        .firstOrNull { itemId(it) == selectedItemId }

                    if (current != null) {
                        ScreenWithDismissableKeyboard {
                            detailsContent(current) {
                                onSelectedItemChange(null)
                                onPanelChange(MasterPanel.List)
                            }
                        }
                    } else {
                        // Fallback if details panel opened without item
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Элемент не выбран")
                        }
                    }
                }
            }
        }
    }
}
