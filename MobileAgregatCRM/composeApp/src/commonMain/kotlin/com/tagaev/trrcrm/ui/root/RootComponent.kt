package com.tagaev.trrcrm.ui.root

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.*
import com.tagaev.trrcrm.ui.details.DefaultDetailsComponent
import com.tagaev.trrcrm.ui.favorites.DefaultFavoritesComponent
import org.koin.core.component.KoinComponent
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.value.Value
import com.tagaev.trrcrm.data.AppSettings
import com.tagaev.trrcrm.data.AppSettingsKeys
import com.tagaev.trrcrm.data.remote.EventsApi
import com.tagaev.trrcrm.domain.Refiner
import com.tagaev.trrcrm.push.DeepLinkBridge
import com.tagaev.trrcrm.push.NotificationContextParser
import com.tagaev.trrcrm.ui.buyer_order.BuyerOrdersComponent
import com.tagaev.trrcrm.ui.cargo.CargoComponent
import com.tagaev.trrcrm.ui.complectation.ComplectationComponent
import com.tagaev.trrcrm.ui.complaints.ComplaintsComponent
import com.tagaev.trrcrm.ui.details.DetailsComponent
import com.tagaev.trrcrm.ui.events.EventsComponent
import com.tagaev.trrcrm.ui.favorites.FavoritesComponent
import com.tagaev.trrcrm.ui.incoming_applications.IncomingApplicationsComponent
import com.tagaev.trrcrm.ui.repair_template_catalog.RepairTemplateCatalogComponent
import com.tagaev.trrcrm.ui.inner_orders.InnerOrdersComponent
import com.tagaev.trrcrm.ui.login.ILoginComponent
import com.tagaev.trrcrm.ui.login.LoginComponent
import com.tagaev.trrcrm.ui.master_screen.MasterPanel
import com.tagaev.trrcrm.ui.menu.IMenuComponent
import com.tagaev.trrcrm.ui.menu.MenuComponent
import com.tagaev.trrcrm.ui.qrscanner.DefaultQRScannerComponent
import com.tagaev.trrcrm.ui.qrscanner.IQRScannerComponent
import com.tagaev.trrcrm.ui.settings.ISettingsComponent
import com.tagaev.trrcrm.ui.settings.SettingsComponent
import com.tagaev.trrcrm.ui.supplier_order.SupplierOrdersComponent
import com.tagaev.trrcrm.ui.work_order.WorkOrdersComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.component.inject

interface IRootComponent {
    val childStack: Value<ChildStack<Config, Child>>

    fun openList()
    fun openEvents(needBackToList: Boolean)
    fun openDetails()
    fun openCargo(needBackToList: Boolean)
    fun openBuyerOrders(needBackToList: Boolean)
    fun openSupplierOrders(needBackToList: Boolean)
    fun openComplaint(needBackToList: Boolean)
    fun openInnerOrder(needBackToList: Boolean)
    fun openIncomingApplications(needBackToList: Boolean)
    fun openRepairTemplateCatalog(needBackToList: Boolean)
    fun openComplectation(needBackToList: Boolean)
    fun openWorkOrders(needBackToList: Boolean)
    fun openQRScanner()
    fun openFavorites()
    fun openMenu()
    fun openSettings()
    fun openLogin()
    fun back()
    /**
     * Handles deep link navigation from push notifications.
     * Always refreshes the target screen's data before navigation.
     * @param screen The target screen name
     * (events, work_orders, cargo, complaints, inner_orders, complectation)
     * @param docId Optional document ID to select after refresh
     */
    fun onDeepLink(screen: String, docId: String?, messageHint: String? = null, title: String? = null)
    val notFoundDialogMessage: Value<String>
    val searchDiagnosticMessage: Value<String>
    fun consumeNotFoundDialog()
    fun consumeSearchDiagnostic()

    sealed interface Config {
        data object Events : Config
        data object Details : Config
        data object WorkOrder : Config
        data object Complectation : Config
        data object Cargo : Config
        data object BuyerOrder : Config
        data object SupplierOrder : Config
        data object Complaint : Config
        data object InnerOrder : Config
        data object IncomingApplications : Config
        data object RepairTemplateCatalog : Config
        data object Favorites : Config
        data object Menu : Config
        data object Settings : Config
        data object QRScanner : Config
        data object Login : Config
    }

    sealed interface Child {
        data class Events(val component: EventsComponent) : Child
        data class Details(val component: DetailsComponent) : Child
        data class WorkOrder(val component: WorkOrdersComponent) : Child
        data class Complectation(val component: ComplectationComponent) : Child
        data class Favorites(val component: FavoritesComponent) : Child
        data class Cargo(val component: CargoComponent) : Child
        data class BuyerOrder(val component: BuyerOrdersComponent) : Child
        data class SupplierOrder(val component: SupplierOrdersComponent) : Child
        data class Complaint(val component: ComplaintsComponent) : Child
        data class InnerOrder(val component: InnerOrdersComponent) : Child
        data class IncomingApplications(val component: IncomingApplicationsComponent) : Child
        data class RepairTemplateCatalog(val component: RepairTemplateCatalogComponent) : Child
        data class Settings(val component: ISettingsComponent) : Child
        data class Menu(val component: IMenuComponent) : Child
        data class QRScanner(val component: IQRScannerComponent) : Child
        data class Login(val component: ILoginComponent) : Child
    }
}

//sealed interface AuthState {
//    data object Checking : AuthState          // short gate
//    data object Unauthenticated : AuthState   // Login flow
//    data object Authenticated : AuthState     // Main flow
//}


class DefaultRootComponent(
    componentContext: ComponentContext,
) : IRootComponent, ComponentContext by componentContext, KoinComponent {

    init {
        DeepLinkBridge.setRoot(this)
    }

    private val appSettings: AppSettings by inject()
    private val appScope: CoroutineScope by inject()
    private val api: EventsApi by inject()

    private val nav = StackNavigation<IRootComponent.Config>()
    
    // Store pending deep link to process after login
    private var pendingDeepLink: List<String?>? = null
    private val _notFoundDialogMessage = com.arkivanov.decompose.value.MutableValue("")
    override val notFoundDialogMessage: Value<String> = _notFoundDialogMessage
    private val _searchDiagnosticMessage = com.arkivanov.decompose.value.MutableValue("")
    override val searchDiagnosticMessage: Value<String> = _searchDiagnosticMessage
    private var deepLinkRequestCounter: Long = 0L
    private var activeDeepLinkRequestId: Long = 0L
    private data class DeepLinkResolutionContext(
        val requestId: Long,
        val screen: String,
        val docTypeLabel: String,
        val identifier: String?,
        val messageHint: String?,
        val searchQueryType: Refiner.SearchQueryType?,
    )

//    private val _auth = MutableStateFlow<AuthState>(AuthState.Checking)
//    val auth: StateFlow<AuthState> = _auth

//    init {
//        // One-time token check
//        appScope.launch {
//            val token = appSettings.getString("API_TOKEN", "")
//            _auth.value = if (token.isBlank()) AuthState.Unauthenticated
//            else if (api.validateToken(token)) AuthState.Authenticated
//            else AuthState.Unauthenticated
//        }
//    }

    override val childStack = childStack(
            source = nav,
            serializer = null,
            initialConfiguration = IRootComponent.Config.Login,
            handleBackButton = true,
            childFactory = ::createChild,
        )

    private fun createChild(cfg: IRootComponent.Config, ctx: ComponentContext): IRootComponent.Child =
        when (cfg) {
            is IRootComponent.Config.Events ->
                IRootComponent.Child.Events(EventsComponent(ctx) { nav.pop() })

            is IRootComponent.Config.Details ->
                IRootComponent.Child.Details(DefaultDetailsComponent(ctx) { nav.pop() })

            is IRootComponent.Config.WorkOrder ->
                IRootComponent.Child.WorkOrder(WorkOrdersComponent(ctx) { nav.pop() })

            is IRootComponent.Config.Complectation ->
                IRootComponent.Child.Complectation(ComplectationComponent(ctx) { nav.pop() })

            is IRootComponent.Config.Cargo ->
                IRootComponent.Child.Cargo(CargoComponent(ctx) { nav.pop() })

            is IRootComponent.Config.BuyerOrder ->
                IRootComponent.Child.BuyerOrder(BuyerOrdersComponent(ctx) { nav.pop() })

            is IRootComponent.Config.SupplierOrder ->
                IRootComponent.Child.SupplierOrder(SupplierOrdersComponent(ctx) { nav.pop() })

            is IRootComponent.Config.Complaint ->
                IRootComponent.Child.Complaint(ComplaintsComponent(ctx) { nav.pop() })

            is IRootComponent.Config.InnerOrder ->
                IRootComponent.Child.InnerOrder(InnerOrdersComponent(ctx) { nav.pop() })

            is IRootComponent.Config.IncomingApplications ->
                IRootComponent.Child.IncomingApplications(IncomingApplicationsComponent(ctx) { nav.pop() })

            is IRootComponent.Config.RepairTemplateCatalog ->
                IRootComponent.Child.RepairTemplateCatalog(RepairTemplateCatalogComponent(ctx) { nav.pop() })

            is IRootComponent.Config.Menu ->
                IRootComponent.Child.Menu(MenuComponent(ctx,
                    onCargo = {
//                        openCargo()
                    },
                    onSettings = {
                        openSettings()
                    },
                    onBack = {
                        nav.pop()
                    }
                ))


            is IRootComponent.Config.QRScanner ->
                IRootComponent.Child.QRScanner(
                    DefaultQRScannerComponent(
                        componentContext = ctx,
                        onBack = { nav.pop() },
                        openComplectationByNumber = { number -> openComplectationByNumberFromQr(number) }
                    )
                )


            is IRootComponent.Config.Favorites ->
                IRootComponent.Child.Favorites(DefaultFavoritesComponent(ctx) { nav.pop() })

            is IRootComponent.Config.Settings ->
                IRootComponent.Child.Settings(SettingsComponent(componentContext = ctx,
                    onLogoutAction = {
                        appSettings.setString(AppSettingsKeys.EMAIL, "")
                        appSettings.setString(AppSettingsKeys.TOKEN_KEY, "")
                        openLogin()
                        nav.replaceAll(IRootComponent.Config.Login)
                    },
                    onBack = {
                        nav.pop()
                    }
                ))

            is IRootComponent.Config.Login ->
                IRootComponent.Child.Login(LoginComponent(
                    componentContext = ctx,
                    onLoginSuccess = { 
                        println("PUSH_SERVICE DEEPLINK: Login success callback pendingDeepLink=$pendingDeepLink")
                        // Check if there's a pending deep link to process
                        val pending = pendingDeepLink
                        pendingDeepLink = null
                        if (pending != null) {
                            val screen = pending.getOrNull(0).orEmpty()
                            val docId = pending.getOrNull(1)
                            val messageHint = pending.getOrNull(2)
                            val title = pending.getOrNull(3)
                            println("PUSH_SERVICE DEEPLINK: Login success processing pending deep link screen=$screen, docId=$docId")
                            // Process the deep link after login
                            appScope.launch(Dispatchers.Main.immediate) {
                                delay(50) // let Login settle, then navigate on Main
                                onDeepLink(screen, docId, messageHint, title)
                            }
                        } else {
                            println("PUSH_SERVICE DEEPLINK: Login success no pending deep link, navigating to Events")
                            // Avoid keeping Login in the back stack
                            nav.replaceAll(IRootComponent.Config.Events)
                        }
                    },
                ) { nav.pop() })
        }
    @Deprecated("MIGRATE NEED")
    override fun openList() {
        nav.bringToFront(IRootComponent.Config.Events)
    }
    override fun openEvents(needBackToList: Boolean) {
//        println(">>>>> openEvents $needBackToList")
        if (needBackToList) {
            val listChild = childStack.value.items
                .firstOrNull { it.configuration is IRootComponent.Config.Events }
                ?.instance as? IRootComponent.Child.Events
            listChild?.component?._masterScreenPanel?.value = MasterPanel.List
        } else {
            nav.bringToFront(IRootComponent.Config.Events)
        }
    }
    override fun openDetails() = nav.bringToFront(IRootComponent.Config.Details)

    override fun openWorkOrders(needBackToList: Boolean){
        if (needBackToList) {
            val listChild = childStack.value.items
                .firstOrNull { it.configuration is IRootComponent.Config.WorkOrder }
                ?.instance as? IRootComponent.Child.WorkOrder
            listChild?.component?._masterScreenPanel?.value = MasterPanel.List
        } else {
            nav.bringToFront(IRootComponent.Config.WorkOrder)
        }
    }

    override fun openComplectation(needBackToList: Boolean) {
        if (needBackToList) {
            val listChild = childStack.value.items
                .firstOrNull { it.configuration is IRootComponent.Config.Complectation }
                ?.instance as? IRootComponent.Child.Complectation
            listChild?.component?._masterScreenPanel?.value = MasterPanel.List
        } else {
            nav.bringToFront(IRootComponent.Config.Complectation)
        }
    }

    override fun openQRScanner() = nav.bringToFront(IRootComponent.Config.QRScanner)

    override fun openMenu() {
        println("childStack.items ${childStack.items.joinToString()}")


        val reversed = childStack.items.reversed()

        reversed.forEach {
            println("childStack.items ${it.toString()}")

            if (it.configuration is IRootComponent.Config.Settings || it.configuration is IRootComponent.Config.Menu) {

                if (childStack.active.configuration is IRootComponent.Config.Cargo || childStack.active.configuration is IRootComponent.Config.Settings ) {
                    nav.bringToFront(IRootComponent.Config.Menu)
                    return
                }

                when(it.configuration) {
                    is IRootComponent.Config.Settings -> {
                        nav.bringToFront(IRootComponent.Config.Settings)
                    }
                    else -> {
                        nav.bringToFront(IRootComponent.Config.Settings)
                    }
                }
                return
            }
        }
        nav.bringToFront(IRootComponent.Config.Menu)
    }
    override fun openFavorites() = nav.bringToFront(IRootComponent.Config.Favorites)
    override fun openCargo(needBackToList: Boolean)  {
        if (needBackToList) {
            val listChild = childStack.value.items
                .firstOrNull { it.configuration is IRootComponent.Config.Cargo }
                ?.instance as? IRootComponent.Child.Cargo
            listChild?.component?._masterScreenPanel?.value = MasterPanel.List
        } else {
            nav.bringToFront(IRootComponent.Config.Cargo)
        }
    }
    override fun openBuyerOrders(needBackToList: Boolean)  {
        if (needBackToList) {
            val listChild = childStack.value.items
                .firstOrNull { it.configuration is IRootComponent.Config.BuyerOrder }
                ?.instance as? IRootComponent.Child.BuyerOrder
            listChild?.component?._masterScreenPanel?.value = MasterPanel.List
        } else {
            nav.bringToFront(IRootComponent.Config.BuyerOrder)
        }
    }

    override fun openSupplierOrders(needBackToList: Boolean) {
        if (needBackToList) {
            val listChild = childStack.value.items
                .firstOrNull { it.configuration is IRootComponent.Config.SupplierOrder }
                ?.instance as? IRootComponent.Child.SupplierOrder
            listChild?.component?._masterScreenPanel?.value = MasterPanel.List
        } else {
            nav.bringToFront(IRootComponent.Config.SupplierOrder)
        }
    }
    override fun openComplaint(needBackToList: Boolean)  {
        if (needBackToList) {
            val listChild = childStack.value.items
                .firstOrNull { it.configuration is IRootComponent.Config.Complaint }
                ?.instance as? IRootComponent.Child.Complaint
            listChild?.component?._masterScreenPanel?.value = MasterPanel.List
        } else {
            nav.bringToFront(IRootComponent.Config.Complaint)
        }
    }

    override fun openInnerOrder(needBackToList: Boolean)  {
        if (needBackToList) {
            val listChild = childStack.value.items
                .firstOrNull { it.configuration is IRootComponent.Config.InnerOrder }
                ?.instance as? IRootComponent.Child.InnerOrder
            listChild?.component?._masterScreenPanel?.value = MasterPanel.List
        } else {
            nav.bringToFront(IRootComponent.Config.InnerOrder)
        }
    }

    override fun openIncomingApplications(needBackToList: Boolean) {
        if (needBackToList) {
            val listChild = childStack.value.items
                .firstOrNull { it.configuration is IRootComponent.Config.IncomingApplications }
                ?.instance as? IRootComponent.Child.IncomingApplications
            listChild?.component?._masterScreenPanel?.value = MasterPanel.List
        } else {
            nav.bringToFront(IRootComponent.Config.IncomingApplications)
        }
    }

    override fun openRepairTemplateCatalog(needBackToList: Boolean) {
        if (needBackToList) {
            val listChild = childStack.value.items
                .firstOrNull { it.configuration is IRootComponent.Config.RepairTemplateCatalog }
                ?.instance as? IRootComponent.Child.RepairTemplateCatalog
            listChild?.component?._masterScreenPanel?.value = MasterPanel.List
        } else {
            nav.bringToFront(IRootComponent.Config.RepairTemplateCatalog)
        }
    }

    override fun openSettings() = nav.bringToFront(IRootComponent.Config.Settings)
    override fun openLogin() = nav.bringToFront(IRootComponent.Config.Login)
    override fun back() = nav.pop()

    override fun onDeepLink(screen: String, docId: String?, messageHint: String?, title: String?) {
        val requestId = ++deepLinkRequestCounter
        activeDeepLinkRequestId = requestId
        _notFoundDialogMessage.value = ""
        _searchDiagnosticMessage.value = ""
        val context = NotificationContextParser.parse(
            title = title,
            screen = screen,
            docId = docId,
            messageText = messageHint
        )
        val normalizedScreen = normalizeScreen(context.screen ?: screen)
        val normalizedDocId = extractGuidOrTrim(context.primaryKey ?: NotificationContextParser.normalizeKey(docId))
        val resolutionContext = DeepLinkResolutionContext(
            requestId = requestId,
            screen = normalizedScreen,
            docTypeLabel = context.docTypeLabel ?: "Документ",
            identifier = normalizedDocId,
            messageHint = context.messageHint,
            searchQueryType = resolveSearchTypeForScreen(normalizedScreen),
        )
        println("PUSH_SERVICE DEEPLINK: onDeepLink called screen='$screen' -> '$normalizedScreen', docId='$normalizedDocId' (raw='$docId')")

        // Check if user is logged in by checking if token exists
        val isLoggedIn = !appSettings.getString(AppSettingsKeys.TOKEN_KEY, "").isBlank()

        // If we're currently on Login screen, always store the deep link.
        // Login flow may auto-navigate to Events on success; pendingDeepLink prevents that override.
        val currentConfig = childStack.value.active.configuration
        if (currentConfig is IRootComponent.Config.Login) {
            println("PUSH_SERVICE DEEPLINK: onDeepLink on Login; store pending deep link (isLoggedIn=$isLoggedIn)")
            pendingDeepLink = listOf(normalizedScreen, normalizedDocId, resolutionContext.messageHint, title)
            if (!isLoggedIn) {
                return
            }
        }

        val config = mapScreenToConfig(normalizedScreen) ?: run {
            println("PUSH_SERVICE DEEPLINK: onDeepLink unknown screen '$screen', ignoring")
            return
        }

        // Always navigate on Main (Decompose navigation is not thread-safe)
        appScope.launch(Dispatchers.Main.immediate) {
            if (requestId != activeDeepLinkRequestId) return@launch
            val active = childStack.value.active.configuration
            println("PUSH_SERVICE DEEPLINK: onDeepLink mapped to config $config; active=$active")

            // If we're coming from Login, replace stack to avoid Login staying in back stack
            if (active is IRootComponent.Config.Login) {
                nav.replaceAll(config)
            } else {
                nav.bringToFront(config)
            }

            // Wait until target child exists (navigation is async relative to composition)
            val masterComponent = awaitMasterComponent(config)
            if (masterComponent == null) {
                println("PUSH_SERVICE DEEPLINK: onDeepLink component not found for config $config")
                return@launch
            }
            if (requestId != activeDeepLinkRequestId) return@launch

            println("PUSH_SERVICE DEEPLINK: onDeepLink found component; refresh + select docId=$normalizedDocId")

            // Always refresh as requested
            masterComponent.fullRefresh()

            processDeepLinkResolution(masterComponent, resolutionContext)
        }
    }

    override fun consumeNotFoundDialog() {
        _notFoundDialogMessage.value = ""
    }

    override fun consumeSearchDiagnostic() {
        _searchDiagnosticMessage.value = ""
    }

    private suspend fun awaitMasterComponent(
        config: IRootComponent.Config,
        timeoutMs: Long = 1500,
        pollMs: Long = 50,
    ): com.tagaev.trrcrm.ui.master_screen.IListMaster? {
        val attempts = ((timeoutMs / pollMs).toInt()).coerceAtLeast(1)
        repeat(attempts) {
            val targetChild = childStack.value.items.firstOrNull { it.configuration == config }
            val childInstance = targetChild?.instance
            val master = when (childInstance) {
                is IRootComponent.Child.Events -> childInstance.component
                is IRootComponent.Child.WorkOrder -> childInstance.component
                is IRootComponent.Child.Complectation -> childInstance.component
                is IRootComponent.Child.Cargo -> childInstance.component
                is IRootComponent.Child.BuyerOrder -> childInstance.component
                is IRootComponent.Child.SupplierOrder -> childInstance.component
                is IRootComponent.Child.Complaint -> childInstance.component
                is IRootComponent.Child.InnerOrder -> childInstance.component
                is IRootComponent.Child.IncomingApplications -> childInstance.component
                is IRootComponent.Child.RepairTemplateCatalog -> childInstance.component
                else -> null
            }
            if (master != null) return master
            delay(pollMs)
        }
        return null
    }

    private suspend fun awaitComplectationComponent(
        timeoutMs: Long = 1500,
        pollMs: Long = 50,
    ): ComplectationComponent? {
        val attempts = ((timeoutMs / pollMs).toInt()).coerceAtLeast(1)
        repeat(attempts) {
            val child = childStack.value.items
                .firstOrNull { it.configuration is IRootComponent.Config.Complectation }
                ?.instance as? IRootComponent.Child.Complectation
            if (child != null) return child.component
            delay(pollMs)
        }
        return null
    }

    private suspend fun openComplectationByNumberFromQr(number: String): Boolean {
        val targetNumber = number.trim()
        if (targetNumber.isBlank()) return false

        withContext(Dispatchers.Main.immediate) {
            nav.bringToFront(IRootComponent.Config.Complectation)
        }

        val component = awaitComplectationComponent() ?: return false
        return component.openDetailsByNumber(targetNumber)
    }

    private fun mapScreenToConfig(normalizedScreen: String): IRootComponent.Config? {
        return when (normalizedScreen) {
            "events", "event" -> IRootComponent.Config.Events
            "work_orders", "workorders", "work_order", "workorder" -> IRootComponent.Config.WorkOrder
            "complectation", "complectations", "complectation_orders" -> IRootComponent.Config.Complectation
            "cargo", "cargos" -> IRootComponent.Config.Cargo
            "buyer_orders", "buyerorders", "buyer_order", "buyerorder", "заказпокупателя" -> IRootComponent.Config.BuyerOrder
            "supplier_orders", "supplierorders", "supplier_order", "supplierorder", "заказпоставщику" -> IRootComponent.Config.SupplierOrder
            "complaints", "complaint" -> IRootComponent.Config.Complaint
            "inner_orders", "innerorders", "inner_order", "innerorder" -> IRootComponent.Config.InnerOrder
            "incoming_applications",
            "incomingapplications",
            "incoming_application",
            "incomingapplication",
            "incoming_orders",
            "входящиезаявки",
            "входящие_заказы",
            -> IRootComponent.Config.IncomingApplications
            "calculation",
            "repair_templates",
            "repair_template_catalog",
            "шаблоныремонта",
            "шаблоны_ремонта",
            "калькуляция",
            -> IRootComponent.Config.RepairTemplateCatalog
            else -> null
        }
    }

    private fun normalizeScreen(raw: String): String {
        return raw
            .trim()
            .lowercase()
            .replace('-', '_')
            .replace(' ', '_')
    }

    private fun extractGuidOrTrim(value: String?): String? {
        if (value.isNullOrBlank()) return null
        val trimmed = value.trim()
        val match = GUID_REGEX.find(trimmed)
        return match?.value ?: trimmed
    }

    private fun isGuid(value: String): Boolean = GUID_REGEX.matches(value)

    private suspend fun processDeepLinkResolution(
        masterComponent: com.tagaev.trrcrm.ui.master_screen.IListMaster,
        context: DeepLinkResolutionContext
    ) {
        val identifier = context.identifier
        if (identifier.isNullOrBlank()) {
            if (context.requestId != activeDeepLinkRequestId) return
            masterComponent.changePanel(MasterPanel.List)
            return
        }

        _searchDiagnosticMessage.value = "Ищем: ${context.docTypeLabel} $identifier"

        if (isGuid(identifier)) {
            if (context.requestId != activeDeepLinkRequestId) return
            masterComponent.selectItemFromList(identifier)
            masterComponent.changePanel(MasterPanel.Details)
            return
        }

        val resolvedTargetGuid = masterComponent.resolveNotificationTarget(
            identifier = identifier,
            messageHint = context.messageHint
        )
        if (!resolvedTargetGuid.isNullOrBlank()) {
            if (context.requestId != activeDeepLinkRequestId) return
            masterComponent.selectItemFromList(resolvedTargetGuid)
            masterComponent.changePanel(MasterPanel.Details)
            return
        }

        if (context.requestId != activeDeepLinkRequestId) return
        applyUnresolvedFallback(
            masterComponent = masterComponent,
            identifier = identifier,
            preferredSearchType = context.searchQueryType
        )
        delay(250)
        if (context.requestId != activeDeepLinkRequestId) return
        if (masterComponent.masterScreenPanel.value == MasterPanel.Details) return
        _notFoundDialogMessage.value = "Документ ${context.docTypeLabel}. $identifier не найден"
    }

    private fun resolveSearchTypeForScreen(normalizedScreen: String): Refiner.SearchQueryType? {
        return when (normalizedScreen) {
            "events", "event", "work_orders", "workorders", "work_order", "workorder" -> Refiner.SearchQueryType.CODE
            "complectation", "complectations", "complectation_orders" -> Refiner.SearchQueryType.KIT_CHARACTERISTIC
            else -> null
        }
    }

    private fun applyUnresolvedFallback(
        masterComponent: com.tagaev.trrcrm.ui.master_screen.IListMaster,
        identifier: String,
        preferredSearchType: Refiner.SearchQueryType?
    ) {
        val currentRefine = masterComponent.refineState.value
        val nextRefine = currentRefine.copy(
            searchQuery = identifier,
            searchQueryType = preferredSearchType ?: currentRefine.searchQueryType
        )
        masterComponent.setRefineState(nextRefine)
        masterComponent.changePanel(MasterPanel.List)
    }

    private companion object {
        private val GUID_REGEX = Regex("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}")
    }
}
