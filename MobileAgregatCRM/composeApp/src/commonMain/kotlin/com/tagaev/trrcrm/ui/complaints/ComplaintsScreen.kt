package com.tagaev.trrcrm.ui.complaints

import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import androidx.compose.ui.unit.sp
import com.tagaev.trrcrm.data.remote.Resource
import com.tagaev.trrcrm.domain.OptionChipsScrollingRow
import com.tagaev.trrcrm.domain.Refiner
import com.tagaev.trrcrm.domain.TreeRootResolvedDocument
import com.tagaev.trrcrm.domain.displayNameRu
import com.tagaev.trrcrm.models.ComplaintDto
import com.tagaev.trrcrm.ui.custom.SearchIconButtonWithIndicator
import com.tagaev.trrcrm.ui.custom.StatusBadge
import com.tagaev.trrcrm.ui.custom.StatusStyle
import com.tagaev.trrcrm.ui.custom.TextC
import com.tagaev.trrcrm.ui.custom.UniversalCardItem
import com.tagaev.trrcrm.ui.master_screen.MasterPanel
import com.tagaev.trrcrm.ui.master_screen.MasterScreen
import com.tagaev.trrcrm.ui.master_screen.RefineSection
import com.tagaev.trrcrm.ui.master_screen.RefineScreen
import com.tagaev.trrcrm.ui.master_screen.TreeRootDocumentDetailsSheet
import com.tagaev.trrcrm.ui.master_screen.models.MessageModel
import com.tagaev.trrcrm.ui.root.LocalAppSnackbar
import com.tagaev.trrcrm.ui.style.DefaultColors
import com.tagaev.trrcrm.ui.work_order.WorkOrderDetailsSheet
import com.tagaev.trrcrm.utils.formatRelativeWorkDate
import compose.icons.FeatherIcons
import compose.icons.feathericons.Filter
import compose.icons.feathericons.RefreshCw
import compose.icons.feathericons.Search
import compose.icons.feathericons.X
import kotlinx.coroutines.launch
import org.jetbrains.compose.ui.tooling.preview.Preview

private val COMPLAINTS_TOPBAR_SEARCH_OPTIONS = listOf(
    Refiner.SearchQueryType.CODE,
    Refiner.SearchQueryType.TOPIC,
    Refiner.SearchQueryType.AUTHOR,
    Refiner.SearchQueryType.COUNTERPARTY,
    Refiner.SearchQueryType.AUTO
)

private fun Refiner.SearchQueryType.complaintsSearchLabel(): String = when (this) {
    Refiner.SearchQueryType.CODE -> "Номер"
    Refiner.SearchQueryType.TOPIC -> "Тема"
    Refiner.SearchQueryType.AUTHOR -> "Автор"
    Refiner.SearchQueryType.COUNTERPARTY -> "Контрагент"
    Refiner.SearchQueryType.AUTO -> "Автомобиль"
    Refiner.SearchQueryType.MANAGER -> "Менеджер"
    Refiner.SearchQueryType.MASTER -> "Мастер"
    Refiner.SearchQueryType.KIT_CHARACTERISTIC -> "Хар. комплекта"
    Refiner.SearchQueryType.LICENSE_PLATE -> "Госномер"
    Refiner.SearchQueryType.VIN_NUMBER -> "VIN"
    Refiner.SearchQueryType.FIX_TYPE -> "Вид ремонта"
    Refiner.SearchQueryType.CLIENT -> "Заказчик"
    Refiner.SearchQueryType.ROUTE -> "Маршрут"
    Refiner.SearchQueryType.CARRIER -> "Перевозчик"
}

@Composable
fun ComplaintsScreen(component: IComplaintsComponent) {
    val resource by component.complaints.collectAsState()
    val refineState by component.refineState.collectAsState()
    val panel by component.masterScreenPanel.collectAsState()
    val selectedId by component.selectedItemGuid.collectAsState()
    var isSearchMode by rememberSaveable { mutableStateOf(false) }
    var searchQueryDraft by rememberSaveable { mutableStateOf(refineState.searchQuery) }
    var searchTypeDraft by rememberSaveable { mutableStateOf(refineState.searchQueryType) }
    val isTopBarLoading = resource is Resource.Loading ||
            (resource as? Resource.Success<*>)?.additionalLoading == true

    val scope = rememberCoroutineScope()
    val showSnackbar = LocalAppSnackbar.current
    val linkedDocuments = remember { emptyList<TreeRootResolvedDocument>().toMutableStateList() }
    var isResolvingBaseDocument by rememberSaveable { mutableStateOf(false) }

    androidx.compose.runtime.LaunchedEffect(refineState.searchQuery, refineState.searchQueryType, isSearchMode) {
        if (!isSearchMode) {
            searchQueryDraft = refineState.searchQuery
            searchTypeDraft = if (refineState.searchQueryType in COMPLAINTS_TOPBAR_SEARCH_OPTIONS) {
                refineState.searchQueryType
            } else {
                Refiner.SearchQueryType.CODE
            }
        }
    }
    LaunchedEffect(selectedId) {
        linkedDocuments.clear()
        isResolvingBaseDocument = false
    }

    val applySearch: () -> Unit = {
        component.setRefineState(
            refineState.copy(
                searchQuery = searchQueryDraft.trim(),
                searchQueryType = searchTypeDraft
            )
        )
    }
    val clearSearchAndExit: () -> Unit = {
        searchQueryDraft = ""
        isSearchMode = false
        component.setRefineState(refineState.copy(searchQuery = ""))
    }
    val handleDetailsBack: () -> Unit = {
        if (linkedDocuments.isNotEmpty()) {
            linkedDocuments.removeAt(linkedDocuments.lastIndex)
        } else {
            component.selectItemFromList(null)
            component.changePanel(MasterPanel.List)
        }
    }
    val linkedDocTitle = linkedDocuments.lastOrNull()?.kind?.displayNameRu()

    MasterScreen(
        title = "Рекламации",
        resource = resource,
        errorText = "Не удалось загрузить рекламации",
        notFoundText = "Рекламации не найдены",
        refineState = refineState,
        onRefresh = { component.fullRefresh() },
        onLoadMore = { component.loadMore() },
        onFilterChanged = { component.setRefineState(it) },

        itemId = { it.guid.toString() },
        isItemChanged = { old, new -> old.messages.size != new.messages.size },

        listItem = { complaint, isChanged, onClick ->
//            ComplaintCard(
//                complaint = complaint,
//                onClick = onClick
//            )
            ////
            UniversalCardItem(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                onClick = onClick,
                title = complaint.number ?: "",
                allowTitleLongPressCopy = true,
                subtitle = "${complaint.branch} - ${complaint.organization}",

                topRightPrimary = {
                    val status = complaint.state
                    if (!status.isNullOrBlank()) {
                        StatusBadge(
                            state = status,
                            styles = mapOf(
                                "Выполнен"       to StatusStyle(DefaultColors.RainbowGreenBg,   DefaultColors.RainbowGreenFg),
                                "Закрыт"         to StatusStyle(DefaultColors.StatusMutedBg,   DefaultColors.StatusMutedFg),
                                "Запланировано"  to StatusStyle(DefaultColors.RainbowIndigoBg, DefaultColors.RainbowIndigoFg),
                                "Выполняется"    to StatusStyle(DefaultColors.RainbowBlueFg,   DefaultColors.RainbowBlueBg),
                                "Начать работу"  to StatusStyle(DefaultColors.RainbowVioletBg, DefaultColors.RainbowVioletFg)
                            )
                        )
                    }
                },
                topRightSecondary = {
                    val priority = complaint.priority
                    if (!priority.isNullOrBlank()) {
                        StatusBadge(
                            state = priority,
                            styles = mapOf(
                                Pair("Высокая", StatusStyle(background = DefaultColors.RainbowRedBg, foreground =DefaultColors.RainbowRedFg)),
                                Pair("Средняя", StatusStyle(background = DefaultColors.RainbowOrangeBg, foreground =DefaultColors.RainbowOrangeFg)),
                                Pair("Низкая", StatusStyle(background = DefaultColors.RainbowGreenBg, foreground =DefaultColors.RainbowGreenFg)),
                            )
                        )
                    }
                },
                // Middle A (big)
                bigText1 = "${complaint.topic} ",
                bigText2 = "${complaint.car} (${complaint.transmissionType}/${complaint.engineType})",
//                bigText3 = "${complaint.error}",

                // Middle B (two medium texts)
//                midBText1 = "Организация: ООО САМАРА АКПП",
//                midBText2 = "Подразделение: Сургут",
//
//                // Middle C (two medium texts)
//                midCText1 = "Ответственный: Голиков Максим",
//                midCText2 = "Источник: Яндекс",
//                complaint.date?.let {
//                    Text(
//                        text = "созд. ${it}",
//                        style = TextStyle(fontSize = 9.sp),
//                        color = MaterialTheme.colorScheme.onSurfaceVariant
//                    )
//                }
//                        complaint.messages.lastOrNull()?.let {
//                    Text(
//                        text = "изм. ${formatRelativeWorkDate(it.workDate)}",
//                        style = TextStyle(fontSize = 9.sp),
//                        color = MaterialTheme.colorScheme.onSurfaceVariant
//                    )
//                }
//                if (complaint.messages.lastOrNull() == null) {
//                    Text(
//                        text = "Сообщений нет",
//                        style = TextStyle(fontSize = 9.sp),
//                        color = MaterialTheme.colorScheme.onSurfaceVariant
//                    )
//                }
                // Bottom
                bottomLeftText = "созд. ${complaint.startDate}",
                bottomRightText = "изм. ${formatRelativeWorkDate(complaint.date)}"
            )
            ////
        },

        // Full-screen details content (not bottom-sheet)
        detailsContent = { complaint, onClose ->
            val onOpenBaseDocument: (String) -> Unit = { rawBaseDocument ->
                scope.launch {
                    isResolvingBaseDocument = true
                    try {
                        when (val resolved = component.resolveBaseDocument(rawBaseDocument)) {
                            is Resource.Success -> linkedDocuments.add(resolved.data)
                            is Resource.Error -> showSnackbar(resolved.causes ?: "Документ-основание не найден")
                            is Resource.Loading -> Unit
                        }
                    } finally {
                        isResolvingBaseDocument = false
                    }
                }
            }
            val onNestedBack: () -> Unit = {
                if (linkedDocuments.isNotEmpty()) linkedDocuments.removeAt(linkedDocuments.lastIndex)
                else onClose()
            }

            val currentLinked = linkedDocuments.lastOrNull()
            if (currentLinked != null) {
                TreeRootDocumentDetailsSheet(
                    document = currentLinked,
                    onBack = onNestedBack,
                    onOpenBaseDocument = onOpenBaseDocument
                )
            } else {
                ComplaintDetailsSheetWithMessages(
                    complaint = complaint,
                    onBack = onNestedBack,
                    onOpenBaseDocument = onOpenBaseDocument,
                    onSendMessage = { message, onResult ->
                        val number = complaint.number.orEmpty()
                        val date = complaint.date.orEmpty()
                        scope.launch {
                            val err = component.sendMessage(number, date, message)
                            if (err == null) {
                                component.addLocalMessage(complaint.guid.toString(), message = MessageModel(author = "я", text = message))
                            }
                            onResult(err)
                        }
                    }
                )
            }
        },

        // Full-screen filter screen (not dialog)
        filterScreen = { current, onDismiss, onApply ->
            RefineScreen(
                current = current,
                onBack = onDismiss,
                messageForUser = "Корректно работает только сортировка по Дате, остальные фильтры пока в разработке",
                sections = setOf(
                    RefineSection.STATUS,
                    RefineSection.FILTER_VAL,
                    RefineSection.ORDER,
                    RefineSection.DIRECTION
                ),
                onApply = { newState ->
                    val applied = newState.copy(
                        searchQuery = refineState.searchQuery,
                        searchQueryType = refineState.searchQueryType
                    )
                    component.setRefineState(applied)
                    onApply(applied)
                }
            )
        },

        panel = panel,
        onPanelChange = {
            component.changePanel(it)

        },

        selectedItemId = selectedId,
        onSelectedItemChange = { id -> component.selectItemFromList(id) },
        topBarNavigationIcon = if (panel == MasterPanel.List && isSearchMode) {
            {
                IconButton(
                    onClick = clearSearchAndExit,
                    enabled = !isTopBarLoading
                ) {
                    Icon(FeatherIcons.X, contentDescription = "Закрыть поиск")
                }
            }
        } else null,
        topBarTitleContent = when {
            panel == MasterPanel.List && isSearchMode -> {
                {
                    OutlinedTextField(
                        value = searchQueryDraft,
                        onValueChange = { searchQueryDraft = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 6.dp),
                        placeholder = { Text("Поиск рекламации") },
                        singleLine = true,
                        enabled = !isTopBarLoading,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = androidx.compose.foundation.text.KeyboardActions(onSearch = { applySearch() })
                    )
                }
            }
            panel == MasterPanel.Details && linkedDocTitle != null -> {
                { Text("Документ Основание: $linkedDocTitle") }
            }
            else -> null
        },
        topBarActionsContent = { isLoadingTopBar ->
            if (panel == MasterPanel.List) {
                if (isSearchMode) {
                    if (isLoadingTopBar) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .padding(horizontal = 8.dp)
                                .size(18.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        IconButton(onClick = applySearch) {
                            Icon(FeatherIcons.Search, contentDescription = "Искать")
                        }
                    }
                } else {
                    IconButton(onClick = { component.changePanel(MasterPanel.Filter) }) {
                        Icon(FeatherIcons.Filter, contentDescription = "Фильтр")
                    }
                    SearchIconButtonWithIndicator(
                        showIndicator = refineState.searchQuery.isNotBlank(),
                        enabled = !isLoadingTopBar,
                        onClick = {
                            searchQueryDraft = refineState.searchQuery
                            searchTypeDraft = if (refineState.searchQueryType in COMPLAINTS_TOPBAR_SEARCH_OPTIONS) {
                                refineState.searchQueryType
                            } else {
                                Refiner.SearchQueryType.CODE
                            }
                            isSearchMode = true
                        }
                    )
                    if (isLoadingTopBar) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .padding(horizontal = 8.dp)
                                .size(18.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        IconButton(onClick = { component.fullRefresh() }) {
                            Icon(FeatherIcons.RefreshCw, contentDescription = "Обновить")
                        }
                    }
                }
            }
        },
        topBarBottomContent = if (panel == MasterPanel.List && isSearchMode) {
            {
                ComplaintsSearchTypeRow(
                    selected = searchTypeDraft,
                    onSelected = { searchTypeDraft = it }
                )
            }
        } else null,
        onDetailsBack = handleDetailsBack,
    )

    if (isResolvingBaseDocument) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text("Пожалуйста, подождите") },
            text = { Text("ищем документ основание....") },
            confirmButton = {}
        )
    }
}


@Composable
private fun ComplaintsSearchTypeRow(
    selected: Refiner.SearchQueryType,
    onSelected: (Refiner.SearchQueryType) -> Unit
) {
    Surface(
        tonalElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "Поиск по:",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OptionChipsScrollingRow(
                options = COMPLAINTS_TOPBAR_SEARCH_OPTIONS,
                selected = selected,
                onSelect = onSelected,
                labelFor = { it.complaintsSearchLabel() }
            )
        }
    }
}

@Composable
fun ComplaintListItem(
    complaint: ComplaintDto,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick)
            ,
        shape = MaterialTheme.shapes.medium,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
//        colors = CardDefaults.cardColors(
//            containerColor = MaterialTheme.colorScheme.onSurfaceVariant,
//            contentColor = MaterialTheme.colorScheme.onSurface
//        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            // Top row: number + status
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val number = complaint.number ?: ""
                if (number.isNotBlank()) {
                    Text(
                        text = number,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(Modifier.weight(1f))
                Row {
                    val priority = complaint.priority
                    if (!priority.isNullOrBlank()) {
                        StatusBadge(
                            state = priority,
                            styles = mapOf(
                                Pair("Высокая", StatusStyle(background = DefaultColors.RainbowRedBg, foreground =DefaultColors.RainbowRedFg)),
                                Pair("Средняя", StatusStyle(background = DefaultColors.RainbowOrangeBg, foreground =DefaultColors.RainbowOrangeFg)),
                                Pair("Низкая", StatusStyle(background = DefaultColors.RainbowGreenBg, foreground =DefaultColors.RainbowGreenFg)),
                            )
                        )
                    }

                    val status = complaint.state
                    if (!status.isNullOrBlank()) {
                        ComplaintStatusChip(status = status)
                    }
                }

            }

            // Middle: topic / content
            val title = when {
                !complaint.topic.isNullOrBlank() -> complaint.topic
                !complaint.content.isNullOrBlank() -> complaint.content
                else -> null
            }

            if (!title.isNullOrBlank()) {
                Spacer(Modifier.height(4.dp))
                TextC(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    // slightly accent the title
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            // Car line
            complaint.car?.takeIf { it.isNotBlank() }?.let { car ->
                Spacer(Modifier.height(4.dp))
                TextC(
                    text = "Авто: $car",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Bottom row: date + source / branch / priority
            val date = complaint.date?.takeIf { it.isNotBlank() }
            val source = complaint.infoSource?.takeIf { it.isNotBlank() }
            val branch = complaint.branch?.takeIf { it.isNotBlank() }
            val priority = complaint.priority?.takeIf { it.isNotBlank() }

            if (date != null || source != null || branch != null || priority != null) {
                Spacer(Modifier.height(1.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().height(60.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Right: source / branch / priority (short summary)
                    val rightParts = buildList {
                        branch?.let { add(it) }
                        source?.let { add(it) }
//                        priority?.let { add("Приор.: $it") }
                    }

                    if (rightParts.isNotEmpty()) {
                        Text(
                            modifier = Modifier.fillMaxSize().weight(1f).basicMarquee(),
                            text = rightParts.joinToString(" • "),
                            maxLines = 1,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

//                    Spacer(Modifier.weight(1f))

                    if (date != null) {
                        Text(
                            modifier = Modifier.fillMaxSize().weight(1f),
                            text = date,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ComplaintCard(
    complaint: ComplaintDto,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(100.dp)
            .padding(4.dp)
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.medium,
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Row(Modifier.fillMaxSize().weight(1f)) {
                Row(modifier = Modifier.fillMaxWidth().weight(1f),
                    verticalAlignment = Alignment.CenterVertically,) {
                    TextC(
                        text = complaint.number?.let { "$it" } ?: "Без номера",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.width(2.dp))
                    complaint.branch?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Row(modifier = Modifier.fillMaxWidth().weight(1f),
                    verticalAlignment = Alignment.CenterVertically,) {


                    val priority = complaint.priority
                    if (!priority.isNullOrBlank()) {
                        StatusBadge(
                            state = priority,
                            styles = mapOf(
                                Pair("Высокая", StatusStyle(background = DefaultColors.RainbowRedBg, foreground =DefaultColors.RainbowRedFg)),
                                Pair("Средняя", StatusStyle(background = DefaultColors.RainbowOrangeBg, foreground =DefaultColors.RainbowOrangeFg)),
                                Pair("Низкая", StatusStyle(background = DefaultColors.RainbowGreenBg, foreground =DefaultColors.RainbowGreenFg)),
                            )
                        )
                    }
                    val status = complaint.state
                    if (!status.isNullOrBlank()) {
                        ComplaintStatusChip(status = status)
                    }
                }
            }
            Column(Modifier.fillMaxSize().weight(1f)) {
                // Middle: topic / content
                val title = when {
                    !complaint.topic.isNullOrBlank() -> complaint.topic
                    !complaint.content.isNullOrBlank() -> complaint.content
                    else -> null
                }

                if (!title.isNullOrBlank()) {
                    Spacer(Modifier.height(4.dp))
                    TextC(
                        text = title,
                        style = MaterialTheme.typography.bodyMedium,
                        // slightly accent the title
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Car line
                complaint.car?.takeIf { it.isNotBlank() }?.let { car ->
                    Spacer(Modifier.height(4.dp))
                    TextC(
                        text = "Авто: $car",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Bottom
                Spacer(Modifier.fillMaxWidth().height(5.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    complaint.date?.let {
                        Text(
                            text = "созд. ${it}",
                            style = TextStyle(fontSize = 9.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    complaint.messages.lastOrNull()?.let {
                        Text(
                            text = "изм. ${formatRelativeWorkDate(it.workDate)}",
                            style = TextStyle(fontSize = 9.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (complaint.messages.lastOrNull() == null) {
                        Text(
                            text = "Сообщений нет",
                            style = TextStyle(fontSize = 9.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            Row(Modifier.fillMaxSize().weight(1f)) {

            }

//            Row(
//                modifier = Modifier.fillMaxWidth(),
//                verticalAlignment = Alignment.CenterVertically,
//                horizontalArrangement = Arrangement.SpaceBetween
//            ) {
//                Row(modifier = Modifier.fillMaxWidth().weight(1f),
//                    verticalAlignment = Alignment.CenterVertically,) {
//                    TextC(
//                        text = complaint.number?.let { "$it" } ?: "Без номера",
//                        style = MaterialTheme.typography.titleMedium,
//                        fontWeight = FontWeight.SemiBold
//                    )
//                    Spacer(Modifier.width(2.dp))
//                    complaint.branch?.let {
//                        Text(
//                            text = it,
//                            style = MaterialTheme.typography.bodySmall,
//                            color = MaterialTheme.colorScheme.onSurfaceVariant
//                        )
//                    }
//                }
//
//                Row(verticalAlignment = Alignment.CenterVertically,) {
//                    Column {
//                        val status = complaint.state
//                        if (!status.isNullOrBlank()) {
//                            ComplaintStatusChip(status = status)
//                        }
//
//                        val priority = complaint.priority
//                        if (!priority.isNullOrBlank()) {
//                            StatusBadge(
//                                state = priority,
//                                styles = mapOf(
//                                    Pair("Высокая", StatusStyle(background = DefaultColors.RainbowRedBg, foreground =DefaultColors.RainbowRedFg)),
//                                    Pair("Средняя", StatusStyle(background = DefaultColors.RainbowOrangeBg, foreground =DefaultColors.RainbowOrangeFg)),
//                                    Pair("Низкая", StatusStyle(background = DefaultColors.RainbowGreenBg, foreground =DefaultColors.RainbowGreenFg)),
//                                )
//                            )
//                        }
//                    }
//                }
//
////                Spacer(Modifier.weight(1f))
//
//            }
//
//            // Middle: topic / content
//            val title = when {
//                !complaint.topic.isNullOrBlank() -> complaint.topic
//                !complaint.content.isNullOrBlank() -> complaint.content
//                else -> null
//            }
//
//            if (!title.isNullOrBlank()) {
//                Spacer(Modifier.height(4.dp))
//                TextC(
//                    text = title,
//                    style = MaterialTheme.typography.bodyMedium,
//                    // slightly accent the title
//                    color = MaterialTheme.colorScheme.onSurface
//                )
//            }
//
//            // Car line
//            complaint.car?.takeIf { it.isNotBlank() }?.let { car ->
//                Spacer(Modifier.height(4.dp))
//                TextC(
//                    text = "Авто: $car",
//                    style = MaterialTheme.typography.bodySmall,
//                    color = MaterialTheme.colorScheme.onSurfaceVariant
//                )
//            }
//
//            // Bottom
//            Spacer(Modifier.fillMaxWidth().height(5.dp))
//            Row(
//                modifier = Modifier.fillMaxWidth(),
//                verticalAlignment = Alignment.CenterVertically,
//                horizontalArrangement = Arrangement.SpaceBetween
//            ) {
//                complaint.date?.let {
//                    Text(
//                        text = "созд. ${it}",
//                        style = TextStyle(fontSize = 9.sp),
//                        color = MaterialTheme.colorScheme.onSurfaceVariant
//                    )
//                }
//                complaint.messages.lastOrNull()?.let {
//                    Text(
//                        text = "изм. ${formatRelativeWorkDate(it.workDate)}",
//                        style = TextStyle(fontSize = 9.sp),
//                        color = MaterialTheme.colorScheme.onSurfaceVariant
//                    )
//                }
//                if (complaint.messages.lastOrNull() == null) {
//                    Text(
//                        text = "Сообщений нет",
//                        style = TextStyle(fontSize = 9.sp),
//                        color = MaterialTheme.colorScheme.onSurfaceVariant
//                    )
//                }
//            }
        }
    }
}



@Composable
private fun ComplaintStatusChip(
    status: String,
    modifier: Modifier = Modifier
) {
    val (bg, fg) = when {
        status.contains("Выполн", ignoreCase = true) -> // Выполнен, Выполнено…
            MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer
        status.contains("Новый", ignoreCase = true) ||
                status.contains("Открыт", ignoreCase = true) ->
            MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
        status.contains("Отмен", ignoreCase = true) ||
                status.contains("Закрыт", ignoreCase = true) ->
            MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
        else ->
            MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
    }

    Surface(
        modifier = modifier,
        color = bg,
        contentColor = fg,
        shape = MaterialTheme.shapes.small,
        tonalElevation = 0.dp
    ) {
        Text(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            text = status,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1
        )
    }
}
