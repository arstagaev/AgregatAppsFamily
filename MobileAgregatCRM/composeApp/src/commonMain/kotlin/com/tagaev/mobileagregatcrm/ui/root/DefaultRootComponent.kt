package com.tagaev.mobileagregatcrm.ui.root

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.*
import com.tagaev.mobileagregatcrm.ui.details.DefaultDetailsComponent
import com.tagaev.mobileagregatcrm.ui.favorites.DefaultFavoritesComponent
import com.tagaev.mobileagregatcrm.ui.mainscreen.DefaultListComponent
import org.koin.core.component.KoinComponent
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.value.Value
import com.tagaev.mobileagregatcrm.data.AppSettings
import com.tagaev.mobileagregatcrm.data.AppSettingsKeys
import com.tagaev.mobileagregatcrm.data.remote.EventsApi
import com.tagaev.mobileagregatcrm.ui.details.DetailsComponent
import com.tagaev.mobileagregatcrm.ui.favorites.FavoritesComponent
import com.tagaev.mobileagregatcrm.ui.login.ILoginComponent
import com.tagaev.mobileagregatcrm.ui.login.LoginComponent
import com.tagaev.mobileagregatcrm.ui.mainscreen.ListComponent
import com.tagaev.mobileagregatcrm.ui.settings.ISettingsComponent
import com.tagaev.mobileagregatcrm.ui.settings.SettingsComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.koin.core.component.inject

interface IRootComponent {
    val childStack: Value<ChildStack<Config, Child>>

    fun openList()
    fun openDetails()
    fun openFavorites()
    fun openSettings()
    fun openLogin()
    fun back()

    sealed interface Config {
        data object List : Config
        data object Details : Config
        data object Favorites : Config
        data object Settings : Config
        data object Login : Config
    }

    sealed interface Child {
        data class List(val component: ListComponent) : Child
        data class Details(val component: DetailsComponent) : Child
        data class Favorites(val component: FavoritesComponent) : Child
        data class Settings(val component: ISettingsComponent) : Child
        data class Login(val component: ILoginComponent) : Child
    }
}

sealed interface AuthState {
    data object Checking : AuthState          // short gate
    data object Unauthenticated : AuthState   // Login flow
    data object Authenticated : AuthState     // Main flow
}


class DefaultRootComponent(
    componentContext: ComponentContext
) : IRootComponent, ComponentContext by componentContext, KoinComponent {

    private val appSettings: AppSettings by inject()
    private val appScope: CoroutineScope by inject()
    private val api: EventsApi by inject()

    private val nav = StackNavigation<IRootComponent.Config>()

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

            is IRootComponent.Config.List ->
                IRootComponent.Child.List(
                    DefaultListComponent(
                        ctx,
                        appSettings = appSettings,
                        onOpenDetails = { number, snapshot -> nav.bringToFront(IRootComponent.Config.Details) },
                        onOpenFavorites = { nav.bringToFront(IRootComponent.Config.Favorites) }
                    )
                )

            is IRootComponent.Config.Details ->
                IRootComponent.Child.Details(DefaultDetailsComponent(ctx) { nav.pop() })

            is IRootComponent.Config.Favorites ->
                IRootComponent.Child.Favorites(DefaultFavoritesComponent(ctx) { nav.pop() })

            is IRootComponent.Config.Settings ->
                IRootComponent.Child.Settings(SettingsComponent(componentContext = ctx,
                    onLogoutAction = {
                        appSettings.setString(AppSettingsKeys.EMAIL, "NULL_EMAIL")
                        appSettings.setString(AppSettingsKeys.TOKEN_KEY, "NULL_TOKEN")
                        openLogin()
                    },
                    onBack = {
                        nav.pop()
                    }
                ))

            is IRootComponent.Config.Login ->
                IRootComponent.Child.Login(LoginComponent(
                    componentContext = ctx,
                    onLoginSuccess = { openList() },
                ) { nav.pop() })
        }

    override fun openList() = nav.bringToFront(IRootComponent.Config.List)
    override fun openDetails() = nav.bringToFront(IRootComponent.Config.Details)
    override fun openFavorites() = nav.bringToFront(IRootComponent.Config.Favorites)
    override fun openSettings() = nav.bringToFront(IRootComponent.Config.Settings)
    override fun openLogin() = nav.bringToFront(IRootComponent.Config.Login)
    override fun back() = nav.pop()
}