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
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import androidx.compose.ui.unit.sp
import com.tagaev.trrcrm.models.ComplaintDto
import com.tagaev.trrcrm.ui.custom.StatusBadge
import com.tagaev.trrcrm.ui.custom.StatusStyle
import com.tagaev.trrcrm.ui.custom.TextC
import com.tagaev.trrcrm.ui.custom.UniversalCardItem
import com.tagaev.trrcrm.ui.master_screen.MasterScreen
import com.tagaev.trrcrm.ui.master_screen.RefineScreen
import com.tagaev.trrcrm.ui.master_screen.models.MessageModel
import com.tagaev.trrcrm.ui.style.DefaultColors
import com.tagaev.trrcrm.ui.work_order.WorkOrderDetailsSheet
import com.tagaev.trrcrm.utils.formatRelativeWorkDate
import kotlinx.coroutines.launch
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun InnerOrdersScreen(component: IInnerOrdersComponent) {
    val resource by component.innerOrders.collectAsState()
    val refineState by component.refineState.collectAsState()
    val panel by component.masterScreenPanel.collectAsState()
    val selectedId by component.selectedItemGuid.collectAsState()

    val scope = rememberCoroutineScope()
    var isSendingMessage by remember { mutableStateOf(false) }
    var lastSendError by remember { mutableStateOf<String?>(null) }

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
                        val ok = component.sendMessage(number, date, message)
                        if (ok) {
                            component.addLocalMessage(complaint.guid.toString(), message = MessageModel(author = "я", text = message))
                        }
                        onResult(ok) // this notifies the sheet
                    }
                },
                isSendingMessage = isSendingMessage,
                lastSendError = lastSendError,
                onErrorDismiss = { lastSendError = null }
            )
        },

        // Full-screen filter screen (not dialog)
        filterScreen = { current, onDismiss, onApply ->
            RefineScreen(
                current = current,
                onBack = onDismiss,
                messageForUser = "Корректно работает только сортировка по Дате, остальные фильтры пока в разработке",
                onApply = { newState ->
                    println(">>>>>> ${newState.toString()}")
                    println(">>>>>> ${newState.searchQueryType.wire}")
                    component.setRefineState(newState)
                    onApply(newState)     // MasterDetailFilterScreen получит обновлённый стейт
                }
            )
        },

        panel = panel,
        onPanelChange = {
            component.changePanel(it)

        },

        selectedItemId = selectedId,
        onSelectedItemChange = { id -> component.selectItemFromList(id) },
    )
}
