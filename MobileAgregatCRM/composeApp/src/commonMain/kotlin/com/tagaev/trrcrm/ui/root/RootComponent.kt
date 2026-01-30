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
import com.tagaev.trrcrm.ui.cargo.CargoComponent
import com.tagaev.trrcrm.ui.complaints.ComplaintsComponent
import com.tagaev.trrcrm.ui.details.DetailsComponent
import com.tagaev.trrcrm.ui.events.EventsComponent
import com.tagaev.trrcrm.ui.favorites.FavoritesComponent
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
import com.tagaev.trrcrm.ui.work_order.WorkOrdersComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.core.component.inject

interface IRootComponent {
    val childStack: Value<ChildStack<Config, Child>>

    fun openList()
    fun openEvents(needBackToList: Boolean)
    fun openDetails()
    fun openCargo(needBackToList: Boolean)
    fun openComplaint(needBackToList: Boolean)
    fun openInnerOrder(needBackToList: Boolean)
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
     * @param screen The target screen name (events, work_orders, cargo, complaints, inner_orders)
     * @param docId Optional document ID to select after refresh
     */
    fun onDeepLink(screen: String, docId: String?)

    sealed interface Config {
        data object Events : Config
        data object Details : Config
        data object WorkOrder : Config
        data object Cargo : Config
        data object Complaint : Config
        data object InnerOrder : Config
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
        data class Favorites(val component: FavoritesComponent) : Child
        data class Cargo(val component: CargoComponent) : Child
        data class Complaint(val component: ComplaintsComponent) : Child
        data class InnerOrder(val component: InnerOrdersComponent) : Child
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

    private val appSettings: AppSettings by inject()
    private val appScope: CoroutineScope by inject()
    private val api: EventsApi by inject()

    private val nav = StackNavigation<IRootComponent.Config>()
    
    // Store pending deep link to process after login
    private var pendingDeepLink: Pair<String, String?>? = null

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

            is IRootComponent.Config.Cargo ->
                IRootComponent.Child.Cargo(CargoComponent(ctx) { nav.pop() })

            is IRootComponent.Config.Complaint ->
                IRootComponent.Child.Complaint(ComplaintsComponent(ctx) { nav.pop() })

            is IRootComponent.Config.InnerOrder ->
                IRootComponent.Child.InnerOrder(InnerOrdersComponent(ctx) { nav.pop() })

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
                IRootComponent.Child.QRScanner(DefaultQRScannerComponent(ctx) { nav.pop() })


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
                        println(">>> Login success callback: pendingDeepLink=$pendingDeepLink")
                        // Check if there's a pending deep link to process
                        val pending = pendingDeepLink
                        pendingDeepLink = null
                        if (pending != null) {
                            val (screen, docId) = pending
                            println(">>> Login success: Processing pending deep link: screen=$screen, docId=$docId")
                            // Process the deep link after login
                            appScope.launch(Dispatchers.Main.immediate) {
                                delay(50) // let Login settle, then navigate on Main
                                onDeepLink(screen, docId)
                            }
                        } else {
                            println(">>> Login success: No pending deep link, navigating to Events")
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

    override fun openSettings() = nav.bringToFront(IRootComponent.Config.Settings)
    override fun openLogin() = nav.bringToFront(IRootComponent.Config.Login)
    override fun back() = nav.pop()

    override fun onDeepLink(screen: String, docId: String?) {
        val normalizedScreen = normalizeScreen(screen)
        val normalizedDocId = extractGuidOrTrim(docId)
        println(">>> onDeepLink called: screen='$screen' -> '$normalizedScreen', docId='$normalizedDocId' (raw='$docId')")

        // Check if user is logged in by checking if token exists
        val isLoggedIn = !appSettings.getString(AppSettingsKeys.TOKEN_KEY, "").isBlank()

        // If we're currently on Login screen, always store the deep link.
        // Login flow may auto-navigate to Events on success; pendingDeepLink prevents that override.
        val currentConfig = childStack.value.active.configuration
        if (currentConfig is IRootComponent.Config.Login) {
            println(">>> onDeepLink: on Login; store pending deep link (isLoggedIn=$isLoggedIn)")
            pendingDeepLink = normalizedScreen to normalizedDocId
            if (!isLoggedIn) {
                return
            }
        }

        val config = mapScreenToConfig(normalizedScreen) ?: run {
            println(">>> onDeepLink: Unknown screen '$screen', ignoring")
            return
        }

        // Always navigate on Main (Decompose navigation is not thread-safe)
        appScope.launch(Dispatchers.Main.immediate) {
            val active = childStack.value.active.configuration
            println(">>> onDeepLink: Mapped to config $config; active=$active")

            // If we're coming from Login, replace stack to avoid Login staying in back stack
            if (active is IRootComponent.Config.Login) {
                nav.replaceAll(config)
            } else {
                nav.bringToFront(config)
            }

            // Wait until target child exists (navigation is async relative to composition)
            val masterComponent = awaitMasterComponent(config)
            if (masterComponent == null) {
                println(">>> onDeepLink: Component not found for config $config")
                return@launch
            }

            println(">>> onDeepLink: Found component; refresh + select docId=$normalizedDocId")

            // Always refresh as requested
            masterComponent.fullRefresh()

            // If a document ID was provided, select and open details.
            // We don't need to wait for refresh completion; the UI will update when data arrives.
            if (!normalizedDocId.isNullOrBlank()) {
                masterComponent.selectItemFromList(normalizedDocId)
                masterComponent.changePanel(MasterPanel.Details)
            } else {
                masterComponent.changePanel(MasterPanel.List)
            }
        }
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
                is IRootComponent.Child.Cargo -> childInstance.component
                is IRootComponent.Child.Complaint -> childInstance.component
                is IRootComponent.Child.InnerOrder -> childInstance.component
                else -> null
            }
            if (master != null) return master
            delay(pollMs)
        }
        return null
    }

    private fun mapScreenToConfig(normalizedScreen: String): IRootComponent.Config? {
        return when (normalizedScreen) {
            "events", "event" -> IRootComponent.Config.Events
            "work_orders", "workorders", "work_order", "workorder" -> IRootComponent.Config.WorkOrder
            "cargo", "cargos" -> IRootComponent.Config.Cargo
            "complaints", "complaint" -> IRootComponent.Config.Complaint
            "inner_orders", "innerorders", "inner_order", "innerorder" -> IRootComponent.Config.InnerOrder
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

    private companion object {
        private val GUID_REGEX = Regex("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}")
    }
}
