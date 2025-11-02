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
import com.tagaev.mobileagregatcrm.ui.details.DetailsComponent
import com.tagaev.mobileagregatcrm.ui.favorites.FavoritesComponent
import com.tagaev.mobileagregatcrm.ui.mainscreen.ListComponent
import org.koin.core.component.inject

interface RootComponent {
    val childStack: Value<ChildStack<Config, Child>>

    fun openList()
    fun openDetails()
    fun openFavorites()
    fun back()

    sealed interface Config {
        data object List : Config
        data object Details : Config
        data object Favorites : Config
    }

    sealed interface Child {
        data class List(val component: ListComponent) : Child
        data class Details(val component: DetailsComponent) : Child
        data class Favorites(val component: FavoritesComponent) : Child
    }
}

class DefaultRootComponent(
    componentContext: ComponentContext
) : RootComponent, ComponentContext by componentContext, KoinComponent {

    private val appSettings: AppSettings by inject()

    private val nav = StackNavigation<RootComponent.Config>()

    override val childStack =
        childStack(
            source = nav,
            serializer = null,
            initialConfiguration = RootComponent.Config.List,
            handleBackButton = true,
            childFactory = ::createChild,
        )

    private fun createChild(cfg: RootComponent.Config, ctx: ComponentContext): RootComponent.Child =
        when (cfg) {

            is RootComponent.Config.List ->
                RootComponent.Child.List(
                    DefaultListComponent(
                        ctx,
                        appSettings = appSettings,
                        onOpenDetails = { number, snapshot -> nav.bringToFront(RootComponent.Config.Details) },
                        onOpenFavorites = { nav.bringToFront(RootComponent.Config.Favorites) }
                    )
                )

            is RootComponent.Config.Details ->
                RootComponent.Child.Details(DefaultDetailsComponent(ctx) { nav.pop() })

            is RootComponent.Config.Favorites ->
                RootComponent.Child.Favorites(DefaultFavoritesComponent(ctx) { nav.pop() })
        }

    override fun openList() = nav.bringToFront(RootComponent.Config.List)
//    override fun openDetails(eventNumber: String, payload: EventItemDto?) = nav.push(RootComponent.Config.Details(eventNumber, payload))
    override fun openDetails() = nav.bringToFront(RootComponent.Config.Details)
    override fun openFavorites() = nav.bringToFront(RootComponent.Config.Favorites)
    override fun back() = nav.pop()
}