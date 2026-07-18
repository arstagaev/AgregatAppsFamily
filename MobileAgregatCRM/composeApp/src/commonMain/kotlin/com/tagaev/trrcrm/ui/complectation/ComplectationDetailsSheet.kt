package com.tagaev.trrcrm.ui.complectation

import com.tagaev.trrcrm.ui.i18n.s

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.ScrollState
import com.tagaev.trrcrm.models.ComplectationChecklistItemDto
import com.tagaev.trrcrm.models.ComplectationPlanningRowDto
import com.tagaev.trrcrm.models.WorkOrderDefectDto
import com.tagaev.trrcrm.models.WorkOrderDto
import com.tagaev.trrcrm.models.WorkOrderJobDto
import com.tagaev.trrcrm.models.WorkOrderProductDto
import com.tagaev.trrcrm.domain.normalizeRawDocumentLabel
import com.tagaev.trrcrm.ui.cargo.ExpandableListSection
import com.tagaev.trrcrm.ui.custom.TextC
import com.tagaev.trrcrm.ui.work_order.WorkOrderJobLineRowCompact
import com.tagaev.trrcrm.ui.work_order.WorkOrderLineItemsExpandableDividerOutdent
import com.tagaev.trrcrm.ui.work_order.WorkOrderLineItemsExpandableListPadding
import com.tagaev.trrcrm.ui.work_order.WorkOrderProductLineRowCompact
import com.tagaev.trrcrm.ui.work_order.buildGoodsTitle
import com.tagaev.trrcrm.ui.work_order.dashOr
import com.tagaev.trrcrm.ui.work_order.formatRubleAmount
import com.tagaev.trrcrm.ui.work_order.parseMoneyAmount
import com.tagaev.trrcrm.ui.master_screen.DetailsWithMessagesSheet
import com.tagaev.trrcrm.ui.master_screen.models.MessageModel
import compose.icons.FeatherIcons
import compose.icons.feathericons.CheckCircle
import compose.icons.feathericons.Circle

/** Material green 700 — checklist «выполнено» icon */
private val ChecklistCompleteIconGreen = Color(0xFF388E3C)

private fun complectationProductsForDisplay(wo: WorkOrderDto): List<WorkOrderProductDto> =
    (wo.products2.orEmpty() + wo.products.orEmpty()).distinctBy {
        listOf(
            it.lineNumber,
            it.name,
            it.article,
            it.quantity,
            it.unit,
            it.price,
            it.amount
        ).joinToString("|").lowercase()
    }

private fun complectationJobsForDisplay(wo: WorkOrderDto): List<WorkOrderJobDto> =
    (wo.jobs2.orEmpty() + wo.jobs.orEmpty()).distinctBy {
        listOf(
            it.lineNumber,
            it.workId,
            it.work,
            it.workLine1,
            it.quantity,
            it.price,
            it.amount
        ).joinToString("|").lowercase()
    }

private fun complectationQuantityWithUnit(wo: WorkOrderDto): String {
    val quantity = wo.complectationQuantity?.trim().orEmpty()
    if (quantity.isBlank()) return "—"
    val unit = wo.complectationUnit?.trim().orEmpty()
    return if (unit.isNotBlank()) "$quantity $unit" else s("complectation_qty_sht", quantity)
}

private fun planningWorkTitle(row: ComplectationPlanningRowDto): String {
    val work = row.autoWork?.trim().orEmpty()
    val duration = row.duration?.trim().orEmpty()
    return when {
        work.isNotBlank() && duration.isNotBlank() -> s("complectation_work_duration_h", work, duration)
        work.isNotBlank() -> work
        duration.isNotBlank() -> s("complectation_duration_h", duration)
        else -> "—"
    }
}

private fun isPlanningZeroDate(raw: String?): Boolean =
    raw?.trim()?.startsWith("01.01.0001") == true

private fun planningDateValue(raw: String?): String? {
    val value = raw?.trim().orEmpty()
    if (value.isBlank()) return null
    if (isPlanningZeroDate(value)) return null
    return value
}

private fun planningDateRange(row: ComplectationPlanningRowDto): String? {
    val start = planningDateValue(row.startAt)
    val end = planningDateValue(row.endAt)
    if (start == null && end == null) return null
    return "${start ?: "—"}  -  ${end ?: "—"}"
}

@Composable
fun ComplectationDetailsSheet(
    order: WorkOrderDto,
    onBack: () -> Unit,
    onSendMessage: (String, (String?) -> Unit) -> Unit,
    initialDraft: String? = null,
    onDraftChanged: (String) -> Unit = {},
    /** Сырой текст [WorkOrderProductDto.characteristic] для поиска комплектаций по характеристике; `null` — клик отключён. */
    onNomenclatureCharacteristicSearch: ((String) -> Unit)? = null,
    /** Переход по «ДокументОснование» в стек связанных; `null` — только текст. */
    onOpenBaseDocument: ((String) -> Unit)? = null,
    /**
     * Когда все три ненулевы — детализация + история + скролл восстанавливаются с учётом стека связанных документов.
     * Иначе (по умолчанию) — внутреннее `remember` как раньше.
     */
    stackedDetailsSnapshot: StackedDocumentDetailsSnapshot? = null,
    onStackedDetailsSnapshotChange: ((StackedDocumentDetailsSnapshot) -> Unit)? = null,
    detailsScrollState: ScrollState? = null,
    documentPhotoCount: Int = 0,
    isDocumentPhotoCountLoading: Boolean = false,
    onOpenDocumentPhotos: (() -> Unit)? = null,
) {
    val useStackedDetails =
        stackedDetailsSnapshot != null && onStackedDetailsSnapshotChange != null && detailsScrollState != null
    DetailsWithMessagesSheet(
        item = order,
        guid = order.guid.toString(),
        messages = order.messages.map {
            MessageModel(
                author = it.author ?: "no author",
                text = it.comment ?: "",
                date = it.workDate ?: "no date"
            )
        },
        onBack = onBack,
        onSendMessage = onSendMessage,
        initialDraft = initialDraft,
        onDraftChanged = onDraftChanged,
        isSendEnabled = { draft, wo ->
            draft.isNotBlank() &&
                !wo.number.isNullOrBlank() &&
                !wo.date.isNullOrBlank()
        },
        historyTitle = s("complectation_istoriya"),
        historyEmptyText = s("complectation_zapisey_net"),
        historyPagerDescription = { showAll, total ->
            if (showAll) s("complectation_pokazany_vse_zapisey", total)
            else s("complectation_pokazany_poslednie_10", total)
        },
        addCommentTitle = s("complectation_dobavit_zapis"),
        composerPlaceholder = s("complectation_tekst_zapisi"),
        sendingDialogTitle = s("complectation_otpravka_zapisi"),
        scrollState = if (useStackedDetails) detailsScrollState else null,
        showAllHistory = if (useStackedDetails) {
            (stackedDetailsSnapshot ?: StackedDocumentDetailsSnapshot()).showAllHistory
        } else {
            null
        },
        onShowAllHistoryChange = if (useStackedDetails) {
            val snap = stackedDetailsSnapshot!!
            val onCh = onStackedDetailsSnapshotChange!!
            { b: Boolean -> onCh(snap.withShowAllHistory(b)) }
        } else {
            null
        },
        headerContent = { wo ->
            wireframeComplectationHeader(
                wo = wo,
                onNomenclatureCharacteristicSearch = onNomenclatureCharacteristicSearch,
                onOpenBaseDocument = onOpenBaseDocument,
                stackedDetailsSnapshot = stackedDetailsSnapshot,
                onStackedDetailsSnapshotChange = onStackedDetailsSnapshotChange,
                documentPhotoCount = documentPhotoCount,
                isDocumentPhotoCountLoading = isDocumentPhotoCountLoading,
                onOpenDocumentPhotos = onOpenDocumentPhotos,
            )
        }
    )
}

@Composable
private fun CompactSectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(top = 4.dp, bottom = 1.dp),
        maxLines = 2,
        overflow = TextOverflow.Ellipsis
    )
}

@Composable
private fun CompactBody(text: String, color: Color = MaterialTheme.colorScheme.onSurface) {
    TextC(
        text = text,
        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp, lineHeight = 14.sp),
        color = color,
        maxLines = 6,
        overflow = TextOverflow.Clip
    )
}

@Composable
private fun CompactMuted(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, lineHeight = 13.sp),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 4,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun wireframeComplectationHeader(
    wo: WorkOrderDto,
    onNomenclatureCharacteristicSearch: ((String) -> Unit)?,
    onOpenBaseDocument: ((String) -> Unit)? = null,
    stackedDetailsSnapshot: StackedDocumentDetailsSnapshot? = null,
    onStackedDetailsSnapshotChange: ((StackedDocumentDetailsSnapshot) -> Unit)? = null,
    documentPhotoCount: Int = 0,
    isDocumentPhotoCountLoading: Boolean = false,
    onOpenDocumentPhotos: (() -> Unit)? = null,
) {
    val useStacked =
        stackedDetailsSnapshot != null && onStackedDetailsSnapshotChange != null
    if (!wo.organization.isNullOrBlank() || !wo.branch.isNullOrBlank()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            if (!wo.organization.isNullOrBlank()) {
                Column(modifier = Modifier.weight(1f)) {
                    CompactSectionTitle(s("work_order_organizatsiya"))
                    CompactBody(wo.organization.orEmpty())
                }
            } else {
                Spacer(Modifier.weight(1f))
            }
            if (!wo.branch.isNullOrBlank()) {
                Column(modifier = Modifier.weight(1f)) {
                    CompactSectionTitle(s("work_order_podrazdelenie"))
                    CompactBody(wo.branch.orEmpty())
                }
            } else {
                Spacer(Modifier.weight(1f))
            }
        }
    }

    if (!wo.link.isNullOrBlank()) {
        CompactSectionTitle(s("complectation_ssylka"))
        CompactBody(wo.link.orEmpty())
    }

    wo.baseDocument?.takeIf { it.isNotBlank() }?.let { baseDocument ->
        CompactSectionTitle(s("complectation_dokument_osnovanie"))
        if (onOpenBaseDocument != null) {
            val cleaned = normalizeRawDocumentLabel(baseDocument)
            TextC(
                text = baseDocument,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp, lineHeight = 14.sp),
                color = MaterialTheme.colorScheme.primary,
                maxLines = 6,
                overflow = TextOverflow.Clip,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpenBaseDocument(cleaned) },
                allowLinkTap = false,
                allowLongPressCopy = false,
            )
        } else {
            CompactBody(baseDocument)
        }
        Spacer(Modifier.height(4.dp))
    }

    val docSubtitle = buildString {
        if (!wo.number.isNullOrBlank()) append("№ ${wo.number}")
        if (!wo.date.isNullOrBlank()) {
            if (isNotEmpty()) append(" · ")
            append(wo.date)
        }
    }
    if (docSubtitle.isNotBlank()) {
        CompactMuted(docSubtitle)
    }

    CompactSectionTitle(s("complectation_komplekt_2"))
    CompactBody(dashOr(wo.complectationKit))

    CompactSectionTitle(s("complectation_harakteristika_komplekta"))
    CompactBody(wo.complectationCharacteristic?.takeIf { it.isNotBlank() } ?: "—")

    CompactSectionTitle(s("complectation_kolichestvo_komplektov"))
    CompactBody(complectationQuantityWithUnit(wo))

//    CompactSectionTitle("Ед. изм. комплекта:")
//    CompactBody(dashOr(wo.complectationUnit))

    CompactSectionTitle(s("complectation_tsena_komplekta"))
    CompactBody(formatRubleAmount(wo.complectationPrice))

    CompactSectionTitle(s("complectation_summa_dokumenta_2"))
    CompactBody(formatRubleAmount(wo.documentAmount))

    val repairType = wo.repairType?.takeIf { it.isNotBlank() }
        ?: wo.complectationRepairType?.takeIf { it.isNotBlank() }
    if (repairType != null || !wo.status.isNullOrBlank()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                CompactSectionTitle(s("work_order_vid_remonta"))
                if (repairType != null) {
                    Text(
                        text = repairType,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                } else {
                    CompactMuted("—")
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                CompactSectionTitle(s("work_order_sostoyanie"))
                Spacer(Modifier.height(2.dp))
                if (!wo.status.isNullOrBlank()) {
                    ComplectationStatusBadge(wo.status.orEmpty())
                } else {
                    CompactMuted("—")
                }
            }
        }
    }

    if (!wo.author.isNullOrBlank() || !wo.master.isNullOrBlank()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            if (!wo.author.isNullOrBlank()) {
                Column(modifier = Modifier.weight(1f)) {
                    CompactSectionTitle(s("complectation_avtor"))
                    CompactBody(wo.author.orEmpty())
                }
            } else {
                Spacer(Modifier.weight(1f))
            }
            if (!wo.master.isNullOrBlank()) {
                Column(modifier = Modifier.weight(1f)) {
                    CompactSectionTitle(s("complectation_master"))
                    CompactBody(wo.master.orEmpty())
                }
            } else {
                Spacer(Modifier.weight(1f))
            }
        }
    }

    CompactSectionTitle(s("complectation_kommentariy_2"))
    CompactBody(wo.comment?.takeIf { it.isNotBlank() } ?: "—")

    if (onOpenDocumentPhotos != null) {
        ComplectationOpenPhotosButton(
            photoCount = documentPhotoCount,
            isLoading = isDocumentPhotoCountLoading,
            onClick = onOpenDocumentPhotos,
        )
        Spacer(Modifier.height(6.dp))
    }

    val products = complectationProductsForDisplay(wo)
    val productsTotal = products.sumOf { parseMoneyAmount(it.amount) ?: 0.0 }
    ExpandableListSection(
        title = buildGoodsTitle(
            baseTitle = s("work_order_tovary"),
            positionsCount = products.size,
            totalAmount = productsTotal
        ),
        items = products,
        initiallyExpanded = false,
        expanded = if (useStacked) stackedDetailsSnapshot!!.isSectionExpanded(ComplectationDetailsSection.PRODUCTS) else null,
        onExpandedChange = if (useStacked) {
            { b -> onStackedDetailsSnapshotChange!!(stackedDetailsSnapshot!!.withSectionExpanded(ComplectationDetailsSection.PRODUCTS, b)) }
        } else {
            null
        },
        listContentPadding = WorkOrderLineItemsExpandableListPadding,
        itemSpacing = 0.dp,
        showItemDividers = true,
        dividerHorizontalOutdent = WorkOrderLineItemsExpandableDividerOutdent
    ) { product ->
        WorkOrderProductLineRowCompact(
            product = product,
            onNomenclatureCharacteristicSearch = onNomenclatureCharacteristicSearch,
        )
    }
    Spacer(Modifier.height(6.dp))

    val jobs = complectationJobsForDisplay(wo)
    val executors = wo.executors
    ExpandableListSection(
        title = s("work_order_raboty_poz_jobs_size", jobs.size),
        items = jobs,
        initiallyExpanded = false,
        expanded = if (useStacked) stackedDetailsSnapshot!!.isSectionExpanded(ComplectationDetailsSection.JOBS) else null,
        onExpandedChange = if (useStacked) {
            { b -> onStackedDetailsSnapshotChange!!(stackedDetailsSnapshot!!.withSectionExpanded(ComplectationDetailsSection.JOBS, b)) }
        } else {
            null
        },
        listContentPadding = WorkOrderLineItemsExpandableListPadding,
        itemSpacing = 0.dp,
        showItemDividers = true,
        dividerHorizontalOutdent = WorkOrderLineItemsExpandableDividerOutdent
    ) { job ->
        WorkOrderJobLineRowCompact(job, executors = executors)
    }
    Spacer(Modifier.height(6.dp))

    wo.defectSummary?.takeIf { it.isNotBlank() }?.let { summary ->
        CompactSectionTitle(s("complectation_defektovka_tekst"))
        CompactBody(summary)
        Spacer(Modifier.height(4.dp))
    }

    val defectsNew = wo.defectsNew.filter { !it.author.isNullOrBlank() }
    ExpandableListSection(
        title = s("complectation_defektovka_poz_defectsnew_size", defectsNew.size),
        items = defectsNew,
        initiallyExpanded = false,
        expanded = if (useStacked) stackedDetailsSnapshot!!.isSectionExpanded(ComplectationDetailsSection.DEFECTS) else null,
        onExpandedChange = if (useStacked) {
            { b -> onStackedDetailsSnapshotChange!!(stackedDetailsSnapshot!!.withSectionExpanded(ComplectationDetailsSection.DEFECTS, b)) }
        } else {
            null
        },
        listContentPadding = WorkOrderLineItemsExpandableListPadding,
        itemSpacing = 0.dp,
        showItemDividers = true,
        dividerHorizontalOutdent = WorkOrderLineItemsExpandableDividerOutdent
    ) { defect ->
        ComplectationDefectRowCompact(defect)
    }
    Spacer(Modifier.height(6.dp))

    val planning = wo.planning
    ExpandableListSection(
        title = s("complectation_planirovanie_poz_planning_size", planning.size),
        items = planning,
        initiallyExpanded = false,
        expanded = if (useStacked) stackedDetailsSnapshot!!.isSectionExpanded(ComplectationDetailsSection.PLANNING) else null,
        onExpandedChange = if (useStacked) {
            { b -> onStackedDetailsSnapshotChange!!(stackedDetailsSnapshot!!.withSectionExpanded(ComplectationDetailsSection.PLANNING, b)) }
        } else {
            null
        },
        listContentPadding = WorkOrderLineItemsExpandableListPadding,
        itemSpacing = 0.dp,
        showItemDividers = true,
        dividerHorizontalOutdent = WorkOrderLineItemsExpandableDividerOutdent
    ) { row ->
        ComplectationPlanningRowCompact(row)
    }
    Spacer(Modifier.height(6.dp))

    val checklist = wo.checklist
    ExpandableListSection(
        title = s("complectation_chek_list_poz_checklist_size", checklist.size),
        items = checklist,
        initiallyExpanded = false,
        expanded = if (useStacked) stackedDetailsSnapshot!!.isSectionExpanded(ComplectationDetailsSection.CHECKLIST) else null,
        onExpandedChange = if (useStacked) {
            { b -> onStackedDetailsSnapshotChange!!(stackedDetailsSnapshot!!.withSectionExpanded(ComplectationDetailsSection.CHECKLIST, b)) }
        } else {
            null
        },
        listContentPadding = WorkOrderLineItemsExpandableListPadding,
        itemSpacing = 0.dp,
        showItemDividers = true,
        dividerHorizontalOutdent = WorkOrderLineItemsExpandableDividerOutdent
    ) { row ->
        ComplectationChecklistRowCompact(row)
    }
}

@Composable
private fun ComplectationPlanningRowCompact(row: ComplectationPlanningRowDto) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 1.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
        Text(
            text = planningWorkTitle(row),
            style = MaterialTheme.typography.bodySmall.copy(
                fontSize = 12.sp,
                lineHeight = 14.sp,
                fontWeight = FontWeight.SemiBold
            ),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth()
        )

        TextC(
            text = dashOr(row.workplace),
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, lineHeight = 13.sp),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )

        planningDateRange(row)?.let { dateRange ->
            Text(
                text = dateRange,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 11.sp,
                    lineHeight = 13.sp
                ),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )
        }

        TextC(
            text = dashOr(row.executor),
            style = MaterialTheme.typography.bodySmall.copy(
                fontSize = 12.sp,
                lineHeight = 14.sp,
                fontWeight = FontWeight.SemiBold
            ),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        }
    }
}

@Composable
private fun ComplectationDefectRowCompact(defect: WorkOrderDefectDto) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
    ) {
        defect.name?.takeIf { it.isNotBlank() }?.let {
            TextC(
                text = it,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp, lineHeight = 14.sp),
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
        }
        defect.author?.takeIf { it.isNotBlank() }?.let { author ->
            CompactMuted(s("complectation_avtor_it", author))
        }
        defect.decision?.takeIf { it.isNotBlank() }?.let { CompactMuted(it) }
        defect.action?.takeIf { it.isNotBlank() }?.let { CompactMuted(it) }
    }
}

private fun checklistItemComplete(state: String?): Boolean {
    val s = state?.trim()?.lowercase().orEmpty()
    if (s.isEmpty()) return false
    val yes = setOf("да", "yes", "истина", "1", s("main_vypolneno"), "готово", "ок", "ok", "+", "true")
    val no = setOf("нет", "no", "ложь", "0", "-", "false")
    if (yes.contains(s)) return true
    if (no.contains(s)) return false
    return false
}

@Composable
private fun ComplectationChecklistRowCompact(item: ComplectationChecklistItemDto) {
    val done = checklistItemComplete(item.state)
    val tint = if (done) ChecklistCompleteIconGreen else MaterialTheme.colorScheme.onSurfaceVariant
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = if (done) FeatherIcons.CheckCircle else FeatherIcons.Circle,
            contentDescription = if (done) s("complectation_vypolneno") else s("complectation_ne_vypolneno"),
            modifier = Modifier
                .padding(top = 1.dp, end = 6.dp)
                .size(16.dp),
            tint = tint
        )
        Column(Modifier.weight(1f)) {
            item.name?.takeIf { it.isNotBlank() }?.let {
                TextC(
                    text = it,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp, lineHeight = 14.sp),
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis
                )
            }
            item.state?.takeIf { it.isNotBlank() }?.let { st ->
                CompactMuted(st)
            }
        }
    }
}
