package com.tagaev.trrcrm.ui.inner_orders

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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
import com.tagaev.trrcrm.models.ComplaintDto
import com.tagaev.trrcrm.ui.custom.StatusBadge
import com.tagaev.trrcrm.ui.custom.StatusStyle
import com.tagaev.trrcrm.ui.custom.TextC
import com.tagaev.trrcrm.ui.custom.UniversalCardItem
import com.tagaev.trrcrm.ui.master_screen.MasterPanel
import com.tagaev.trrcrm.ui.master_screen.MasterScreen
import com.tagaev.trrcrm.ui.master_screen.RefineSection
import com.tagaev.trrcrm.ui.master_screen.RefineScreen
import com.tagaev.trrcrm.ui.master_screen.models.MessageModel
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

private val INNER_ORDERS_TOPBAR_SEARCH_OPTIONS = listOf(
    Refiner.SearchQueryType.CODE,
    Refiner.SearchQueryType.TOPIC,
    Refiner.SearchQueryType.AUTHOR,
    Refiner.SearchQueryType.COUNTERPARTY,
    Refiner.SearchQueryType.AUTO
)

private fun Refiner.SearchQueryType.innerOrdersSearchLabel(): String = when (this) {
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
fun InnerOrdersScreen(component: IInnerOrdersComponent) {
    val resource by component.innerOrders.collectAsState()
    val refineState by component.refineState.collectAsState()
    val panel by component.masterScreenPanel.collectAsState()
    val selectedId by component.selectedItemGuid.collectAsState()
    var isSearchMode by rememberSaveable { mutableStateOf(false) }
    var searchQueryDraft by rememberSaveable { mutableStateOf(refineState.searchQuery) }
    var searchTypeDraft by rememberSaveable { mutableStateOf(refineState.searchQueryType) }
    val isTopBarLoading = resource is Resource.Loading ||
            (resource as? Resource.Success<*>)?.additionalLoading == true

    val scope = rememberCoroutineScope()

    androidx.compose.runtime.LaunchedEffect(refineState.searchQuery, refineState.searchQueryType, isSearchMode) {
        if (!isSearchMode) {
            searchQueryDraft = refineState.searchQuery
            searchTypeDraft = if (refineState.searchQueryType in INNER_ORDERS_TOPBAR_SEARCH_OPTIONS) {
                refineState.searchQueryType
            } else {
                Refiner.SearchQueryType.CODE
            }
        }
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

    MasterScreen(
        title = "Внутренние заказы",
        resource = resource,
        errorText = "Не удалось загрузить внутренние заказы",
        notFoundText = "Внутренние заказы не найдены",
        refineState = refineState,
        onRefresh = { component.fullRefresh() },
        onLoadMore = { component.loadMore() },
        onFilterChanged = { component.setRefineState(it) },

        itemId = { it.guid.toString() },
        isItemChanged = { old, new -> old.messages.size != new.messages.size },

        listItem = { innerOrder, isChanged, onClick ->
            UniversalCardItem(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                onClick = onClick,
                title = innerOrder.number ?: "",
                subtitle = "${innerOrder.branch} - ${innerOrder.organization}",

                topRightPrimary = {
                    val status = innerOrder.state
                    if (!status.isNullOrBlank()) {
                        StatusBadge(
                            state = status,
                            styles = mapOf(
                                // Colors are aligned with 1C screenshot palette
                                "Заявка"                      to StatusStyle(DefaultColors.RainbowSkyBg,        DefaultColors.RainbowSkyFg),
                                "В работе"                    to StatusStyle(DefaultColors.RainbowMintBg,       DefaultColors.RainbowMintFg),
                                "Выполнено"                   to StatusStyle(DefaultColors.RainbowBrightPurpleBg, DefaultColors.RainbowBrightPurpleFg),
                                "В пути на основной склад"    to StatusStyle(DefaultColors.RainbowSoftAmberBg,  DefaultColors.RainbowSoftAmberFg),
                                "Готово к отгрузке"           to StatusStyle(DefaultColors.RainbowStrongOrangeBg, DefaultColors.RainbowStrongOrangeFg),
                                "Заказано"                    to StatusStyle(DefaultColors.RainbowNeonMagentaBg, DefaultColors.RainbowNeonMagentaFg),
                                "Дефектовка"                  to StatusStyle(DefaultColors.RainbowGreyBg,       DefaultColors.RainbowGreyFg),
                                "Дефектовка (выполнено)"      to StatusStyle(DefaultColors.RainbowOliveBg,      DefaultColors.RainbowOliveFg),
                                "Сборка"                      to StatusStyle(DefaultColors.RainbowNeonGreenBg,  DefaultColors.RainbowNeonGreenFg),
                                "Отправлено получателю"       to StatusStyle(DefaultColors.RainbowAquaBg,       DefaultColors.RainbowAquaFg),
                                "Получено"                    to StatusStyle(DefaultColors.RainbowRoyalBlueBg,  DefaultColors.RainbowRoyalBlueFg),
                                "ОТКАЗ"                       to StatusStyle(DefaultColors.RainbowDarkTealBg,   DefaultColors.RainbowDarkTealFg),
                                "Получено на основном складе" to StatusStyle(DefaultColors.RainbowSkyBg,        DefaultColors.RainbowSkyFg),
                                "Передано в производстве"     to StatusStyle(DefaultColors.RainbowSandBg,       DefaultColors.RainbowSandFg),
                            )
                        )
                    }
                },
                topRightSecondary = {
//                    val priority = innerOrder.priority
//                    if (!priority.isNullOrBlank()) {
//                        StatusBadge(
//                            state = priority,
//                            styles = mapOf(
//                                Pair("Высокая", StatusStyle(background = DefaultColors.RainbowRedBg, foreground =DefaultColors.RainbowRedFg)),
//                                Pair("Средняя", StatusStyle(background = DefaultColors.RainbowOrangeBg, foreground =DefaultColors.RainbowOrangeFg)),
//                                Pair("Низкая", StatusStyle(background = DefaultColors.RainbowGreenBg, foreground =DefaultColors.RainbowGreenFg)),
//                            )
//                        )
//                    }
                },
                // Middle A (big)
                bigText1 = "${innerOrder.operationType} ",
                bigText2 = "${innerOrder.carText}",
                bigText3 = "${innerOrder.documentAmount} ${innerOrder.currency}",

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
                bottomLeftText = "созд. ${innerOrder.creationDate}",
                bottomRightText = "изм. ${formatRelativeWorkDate(innerOrder.date)}"
            )
            ////
        },

        // Full-screen details content (not bottom-sheet)
        detailsContent = { complaint, onClose ->
            InnerOrderDetailsSheetWithMessages(
                complaint = complaint,
                onBack = onClose,
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
        topBarTitleContent = if (panel == MasterPanel.List && isSearchMode) {
            {
                OutlinedTextField(
                    value = searchQueryDraft,
                    onValueChange = { searchQueryDraft = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 6.dp),
                    placeholder = { Text("Поиск внутреннего заказа") },
                    singleLine = true,
                    enabled = !isTopBarLoading,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = androidx.compose.foundation.text.KeyboardActions(onSearch = { applySearch() })
                )
            }
        } else null,
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
                    IconButton(
                        onClick = {
                            searchQueryDraft = refineState.searchQuery
                            searchTypeDraft = if (refineState.searchQueryType in INNER_ORDERS_TOPBAR_SEARCH_OPTIONS) {
                                refineState.searchQueryType
                            } else {
                                Refiner.SearchQueryType.CODE
                            }
                            isSearchMode = true
                        }
                    ) {
                        Icon(FeatherIcons.Search, contentDescription = "Поиск")
                    }
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
                InnerOrdersSearchTypeRow(
                    selected = searchTypeDraft,
                    onSelected = { searchTypeDraft = it }
                )
            }
        } else null,
    )
}

@Composable
private fun InnerOrdersSearchTypeRow(
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
                options = INNER_ORDERS_TOPBAR_SEARCH_OPTIONS,
                selected = selected,
                onSelect = onSelected,
                labelFor = { it.innerOrdersSearchLabel() }
            )
        }
    }
}
