package com.tagaev.trrcrm.ui.master_screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlin.collections.iterator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T, Id> LiveListWrapper(
    items: List<T>,
    maxItems: Int,
    idSelector: (T) -> Id,
    contentChanged: (old: T, new: T) -> Boolean,
    modifier: Modifier = Modifier,
    listState: LazyListState? = null,
    isLoading: Boolean = false,
    errorMessage: String? = null,
    onRefresh: (currentSize: Int, maxSize: Int) -> Unit,
    onLoadMore: (currentSize: Int, maxSize: Int) -> Unit,
    onRetry: (() -> Unit)? = null,
    card: @Composable (item: T, isChanged: Boolean, isMoved: Boolean) -> Unit
) {
    var lastSnapshot by remember { mutableStateOf<Map<Id, T>>(emptyMap()) }
    var lastOrder by remember { mutableStateOf<Map<Id, Int>>(emptyMap()) }
    var changedIds by remember { mutableStateOf<Set<Id>>(emptySet()) }
    var movedIds by remember { mutableStateOf<Set<Id>>(emptySet()) }

    LaunchedEffect(items) {
        val newMap = items.associateBy(idSelector)
        val newOrder = items.mapIndexed { index, item -> idSelector(item) to index }.toMap()

        val changed = mutableSetOf<Id>()
        val moved = mutableSetOf<Id>()

        for ((id, newItem) in newMap) {
            val oldItem = lastSnapshot[id]
            if (oldItem == null || contentChanged(oldItem, newItem)) {
                changed += id
            }

            val oldIndex = lastOrder[id]
            val newIndex = newOrder[id]
            if (oldIndex != null && newIndex != null && oldIndex != newIndex) {
                moved += id
            }
        }

        lastSnapshot = newMap
        lastOrder = newOrder
        changedIds = changed
        movedIds = moved
    }

    val lazyState = listState ?: rememberLazyListState()
    var isPaginating by remember { mutableStateOf(false) }
    var refreshing by remember { mutableStateOf(false) }

    // When the items list changes (after refresh/load), stop the refresh spinner and reset paginating flag
    LaunchedEffect(items.size) {
        if (refreshing) refreshing = false
        isPaginating = false
    }

    // Infinite scroll: only trigger when we have at least one full page (30 items)
    // and the user has scrolled over half of the current list.
    LaunchedEffect(lazyState, items.size, isLoading) {
        val pageSize = 30

        snapshotFlow { lazyState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
            .collect { lastVisibleIndex ->
                val total = lazyState.layoutInfo.totalItemsCount
                if (lastVisibleIndex != null && total > 0) {
                    val reachedSecondHalf = lastVisibleIndex >= total / 2
                    val canLoadMorePage =
                        items.size >= pageSize &&
                                items.size < maxItems &&
                                items.size % pageSize == 0

                    if (reachedSecondHalf && canLoadMorePage && !isPaginating && !isLoading) {
                        isPaginating = true
                        onLoadMore(items.size, maxItems)
                    }
                }
            }
    }

    PullToRefreshBox(
        isRefreshing = refreshing,
        onRefresh = {
            if (!refreshing && items.size < maxItems) {
                refreshing = true
                onRefresh(items.size, maxItems)
            }
        },
        modifier = modifier.fillMaxSize()
    ) {
        LazyColumn(
            state = lazyState,
            modifier = Modifier.fillMaxSize()
        ) {
            itemsIndexed(
                items,
                key = { _, item -> idSelector(item) as Any }
            ) { _, item ->
                val id = idSelector(item)
                val isChanged = changedIds.contains(id)
                val isMoved = movedIds.contains(id)
                card(item, isChanged, isMoved)
            }

            if (isLoading || errorMessage != null) {
                item(key = "footer") {
                    LiveListFooter(
                        isLoading = isLoading,
                        errorMessage = errorMessage,
                        onRetry = onRetry
                    )
                }
            }
        }
    }
}

@Composable
private fun LiveListFooter(
    isLoading: Boolean,
    errorMessage: String?,
    onRetry: (() -> Unit)?
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        when {
            isLoading -> {
                CircularProgressIndicator()
            }

            errorMessage != null -> {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = errorMessage,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    if (onRetry != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        TextButton(onClick = onRetry) {
                            Text("Повторить")
                        }
                    }
                }
            }
        }
    }
}