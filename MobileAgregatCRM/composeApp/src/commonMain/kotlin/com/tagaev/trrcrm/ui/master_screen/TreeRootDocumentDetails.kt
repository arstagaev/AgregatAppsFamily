package com.tagaev.trrcrm.ui.master_screen

import androidx.compose.runtime.Composable
import com.tagaev.trrcrm.domain.TreeRootResolvedDocument
import com.tagaev.trrcrm.ui.buyer_order.BuyerOrderDetailsSheet
import com.tagaev.trrcrm.ui.cargo.CargoDetailsSheet
import com.tagaev.trrcrm.ui.complectation.ComplectationDetailsSheet
import com.tagaev.trrcrm.ui.complectation.ComplectationTreeStackedUi
import com.tagaev.trrcrm.ui.complaints.ComplaintDetailsSheetWithMessages
import com.tagaev.trrcrm.ui.events.EventDetailsSheet
import com.tagaev.trrcrm.ui.inner_orders.InnerOrderDetailsSheetWithMessages
import com.tagaev.trrcrm.ui.supplier_order.SupplierOrderDetailsSheet
import com.tagaev.trrcrm.ui.work_order.WorkOrderDetailsSheet

@Composable
fun TreeRootDocumentDetailsSheet(
    document: TreeRootResolvedDocument,
    onBack: () -> Unit,
    onOpenBaseDocument: (String) -> Unit,
    /** For [TreeRootResolvedDocument.Complectation] only: restore scroll / sections when deep-linking the stack. */
    complectationStacked: ComplectationTreeStackedUi? = null,
) {
    when (document) {
        is TreeRootResolvedDocument.Event -> EventDetailsSheet(
            event = document.value,
            onBack = onBack,
            onSendMessage = { _, onResult -> onResult("Отправка сообщений в связанном документе недоступна") },
            onOpenBaseDocument = onOpenBaseDocument
        )
        is TreeRootResolvedDocument.WorkOrder -> WorkOrderDetailsSheet(
            order = document.value,
            onBack = onBack,
            onSendMessage = { _, onResult -> onResult("Отправка сообщений в связанном документе недоступна") },
        )
        is TreeRootResolvedDocument.Complectation -> ComplectationDetailsSheet(
            order = document.value,
            onBack = onBack,
            onSendMessage = { _, onResult -> onResult("Отправка сообщений в связанном документе недоступна") },
            stackedDetailsSnapshot = complectationStacked?.detailsSnapshot,
            onStackedDetailsSnapshotChange = complectationStacked?.onDetailsSnapshot,
            detailsScrollState = complectationStacked?.detailsScroll,
            onOpenBaseDocument = onOpenBaseDocument,
        )
        is TreeRootResolvedDocument.Complaint -> ComplaintDetailsSheetWithMessages(
            complaint = document.value,
            onBack = onBack,
            onSendMessage = { _, onResult -> onResult("Отправка сообщений в связанном документе недоступна") },
            onOpenBaseDocument = onOpenBaseDocument,
        )
        is TreeRootResolvedDocument.InnerOrder -> InnerOrderDetailsSheetWithMessages(
            complaint = document.value,
            onBack = onBack,
            onSendMessage = { _, onResult -> onResult("Отправка сообщений в связанном документе недоступна") },
            onOpenBaseDocument = onOpenBaseDocument
        )
        is TreeRootResolvedDocument.BuyerOrder -> BuyerOrderDetailsSheet(
            order = document.value,
            onBack = onBack,
            onSendMessage = { _, onResult -> onResult("Отправка сообщений в связанном документе недоступна") },
            onOpenBaseDocument = onOpenBaseDocument
        )
        is TreeRootResolvedDocument.SupplierOrder -> SupplierOrderDetailsSheet(
            order = document.value,
            onBack = onBack,
            onOpenBaseDocument = onOpenBaseDocument
        )
        is TreeRootResolvedDocument.Cargo -> CargoDetailsSheet(
            cargo = document.value,
            onClose = onBack,
            onOpenBaseDocument = onOpenBaseDocument,
        )
    }
}
