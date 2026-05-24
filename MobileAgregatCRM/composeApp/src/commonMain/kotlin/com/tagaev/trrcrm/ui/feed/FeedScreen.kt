package com.tagaev.trrcrm.ui.feed

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tagaev.trrcrm.models.CoreNotificationFeedItem
import com.tagaev.trrcrm.ui.root.LocalAppSnackbar
import com.tagaev.trrcrm.utils.formatFeedTimestamp
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedScreen(component: IFeedComponent) {
    val snackbar = LocalAppSnackbar.current
    val items by component.items.collectAsState()
    val isLoadingFirstPage by component.isLoadingFirstPage.collectAsState()
    val isRefreshing by component.isRefreshing.collectAsState()
    val isLoadingNextPage by component.isLoadingNextPage.collectAsState()
    val firstPageError by component.firstPageError.collectAsState()
    val nextPageError by component.nextPageError.collectAsState()
    val hasNext by component.hasNext.collectAsState()
    val unreadCount by component.unreadCount.collectAsState()
    val searchQuery by component.searchQuery.collectAsState()
    val selectedFilter by component.statusFilter.collectAsState()
    val transientMessage by component.transientMessage.collectAsState()

    var searchInput by rememberSaveable { mutableStateOf(searchQuery) }
    val listState = rememberLazyListState()
    var pendingScrollToTopAfterPullRefresh by rememberSaveable { mutableStateOf(false) }
    var topItemIdBeforePullRefresh by rememberSaveable { mutableStateOf<String?>(null) }

    LaunchedEffect(searchQuery) {
        if (searchInput != searchQuery) {
            searchInput = searchQuery
        }
    }

    LaunchedEffect(searchInput) {
        delay(300)
        if (searchInput.trim() != searchQuery) {
            component.setSearchQuery(searchInput)
        }
    }

    LaunchedEffect(transientMessage) {
        val text = transientMessage
        if (!text.isNullOrBlank()) {
            snackbar(text)
            component.consumeTransientMessage()
        }
    }

    LaunchedEffect(isRefreshing, items) {
        if (!pendingScrollToTopAfterPullRefresh || isRefreshing) return@LaunchedEffect
        val currentTopItemId = items.firstOrNull()?.id
        val hasNewItemsOnTop = !currentTopItemId.isNullOrBlank() && currentTopItemId != topItemIdBeforePullRefresh
        if (hasNewItemsOnTop) {
            listState.animateScrollToItem(0)
        }
        pendingScrollToTopAfterPullRefresh = false
        topItemIdBeforePullRefresh = null
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Главная", style = MaterialTheme.typography.headlineSmall)
            TextButton(onClick = component::markAllRead, enabled = items.isNotEmpty()) {
                Text("Прочитать все")
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                modifier = Modifier.weight(1f),
                value = searchInput,
                onValueChange = { searchInput = it },
                singleLine = true,
                label = { Text("Поиск уведомлений") }
            )
            AssistChip(
                modifier = Modifier.padding(start = 8.dp),
                onClick = {},
                label = { Text("Непрочитано: $unreadCount") },
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = if (unreadCount > 0) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    },
                    labelColor = if (unreadCount > 0) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FeedStatusFilter.entries.forEach { filter ->
                FilterChip(
                    selected = selectedFilter == filter,
                    onClick = { component.setStatusFilter(filter) },
                    label = { Text(filter.label) }
                )
            }
        }

        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = {
                if (!isLoadingNextPage) {
                    topItemIdBeforePullRefresh = items.firstOrNull()?.id
                    pendingScrollToTopAfterPullRefresh = true
                    component.refresh()
                }
            },
            modifier = Modifier.fillMaxSize()
        ) {
            when {
                isLoadingFirstPage && items.isEmpty() -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 32.dp),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                items.isEmpty() && !firstPageError.isNullOrBlank() -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = firstPageError.orEmpty(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(Modifier.height(12.dp))
                        OutlinedButton(onClick = component::retryFirstPage) {
                            Text("Повторить")
                        }
                    }
                }
                items.isEmpty() && !isLoadingFirstPage -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (searchQuery.isBlank()) "Нет уведомлений" else "Ничего не найдено",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                else -> {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        itemsIndexed(items, key = { _, item -> item.id }) { index, item ->
                            FeedRow(
                                item = item,
                                onOpen = { component.openNotification(item) },
                                onToggleRead = { component.toggleRead(item) }
                            )

                            if (index >= items.lastIndex - 4 && hasNext && !isLoadingNextPage && !isRefreshing && nextPageError == null) {
                                LaunchedEffect(item.id, hasNext, isLoadingNextPage, isRefreshing) {
                                    component.loadMore()
                                }
                            }
                        }

                        when {
                            isLoadingNextPage -> {
                                item("loading_more") {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 12.dp),
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        CircularProgressIndicator()
                                    }
                                }
                            }
                            nextPageError != null -> {
                                item("next_page_error") {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 8.dp, vertical = 12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            modifier = Modifier.weight(1f),
                                            text = nextPageError.orEmpty(),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.error,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        TextButton(onClick = component::retryNextPage) {
                                            Text("Повторить")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FeedRow(
    item: CoreNotificationFeedItem,
    onOpen: () -> Unit,
    onToggleRead: () -> Unit,
) {
    val isRead = item.status.equals("read", ignoreCase = true)
    val scheme = MaterialTheme.colorScheme

    val containerColor = if (isRead) scheme.surface else scheme.surfaceVariant
    val borderColor = if (isRead) scheme.outlineVariant else scheme.outline
    val borderWidth = if (isRead) 1.dp else 1.5.dp
    val titleColor = if (isRead) scheme.onSurfaceVariant else scheme.onSurface
    val titleWeight = if (isRead) FontWeight.Normal else FontWeight.SemiBold

    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.outlinedCardColors(containerColor = containerColor),
        border = BorderStroke(borderWidth, borderColor),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
        ) {
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .fillMaxHeight()
                    .background(if (isRead) scheme.outlineVariant else scheme.primary)
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (!isRead) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(scheme.primary)
                        )
                        Spacer(Modifier.width(6.dp))
                    }
                    Text(
                        modifier = Modifier.weight(1f),
                        text = item.docTitle ?: item.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = titleWeight,
                        color = titleColor,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (!item.messageText.isNullOrBlank()) {
                    Text(
                        text = item.messageText,
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = formatFeedTimestamp(item.createdAt),
                        style = MaterialTheme.typography.labelSmall,
                        color = scheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    TextButton(
                        onClick = onToggleRead,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Text(
                            text = if (isRead) "Непрочитано" else "Прочитано",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }
        }
    }
}
