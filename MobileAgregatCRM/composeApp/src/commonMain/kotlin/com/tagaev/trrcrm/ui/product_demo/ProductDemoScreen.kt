package com.tagaev.trrcrm.ui.product_demo

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.union
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalDensity
import com.tagaev.trrcrm.ui.root.LocalAppSnackbar
import compose.icons.FeatherIcons
import compose.icons.feathericons.CheckCircle
import compose.icons.feathericons.Home
import compose.icons.feathericons.User

@Composable
fun ProductDemoScreen(component: IProductDemoComponent) {
    val state by component.uiState.collectAsState()
    val snackbar = LocalAppSnackbar.current
    val density = LocalDensity.current
    val isImeVisible = WindowInsets.ime.getBottom(density) > 0
    val openedProduct = state.openedProduct
    val isOrderRequestOpen = state.isOrderRequestOpen

    LaunchedEffect(state.transientMessage) {
        val text = state.transientMessage
        if (!text.isNullOrBlank()) {
            snackbar(text)
            component.consumeTransientMessage()
        }
    }

    Scaffold(
        bottomBar = {
            if (!isImeVisible && openedProduct == null && !isOrderRequestOpen) NavigationBar(
                windowInsets = NavigationBarDefaults.windowInsets
                    .union(WindowInsets.navigationBars)
            ) {
                NavigationBarItem(
                    selected = state.selectedTab == ProductDemoTab.Catalog,
                    onClick = { component.selectTab(ProductDemoTab.Catalog) },
                    icon = { Icon(FeatherIcons.Home,contentDescription = null) },
                    label = { Text("Каталог") }
                )
                NavigationBarItem(
                    selected = state.selectedTab == ProductDemoTab.SelectedProducts,
                    onClick = { component.selectTab(ProductDemoTab.SelectedProducts) },
                    icon = { Icon(FeatherIcons.CheckCircle,contentDescription = null) },
                    label = { Text("Заявка") }
                )
                NavigationBarItem(
                    selected = state.selectedTab == ProductDemoTab.Profile,
                    onClick = { component.selectTab(ProductDemoTab.Profile) },
                    icon = { Icon(FeatherIcons.User,contentDescription = null) },
                    label = { Text("Мой профиль") }
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = androidx.compose.ui.Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (state.selectedTab) {
                ProductDemoTab.Catalog -> CatalogTab(
                    products = state.products,
                    addedProductIds = state.basketItems.map { it.product.id }.toSet(),
                    isLoading = state.isProductsLoading,
                    isLoadingMore = state.isProductsLoadingMore,
                    errorText = state.productsError,
                    searchQuery = state.catalogSearchQuery,
                    selectedBrand = state.catalogBrand,
                    availableBrands = state.catalogAvailableBrands,
                    hasNext = state.catalogHasNext,
                    onSearchQueryChanged = component::updateCatalogSearchQuery,
                    onBrandChanged = component::updateCatalogBrandFilter,
                    onClearFilters = component::clearCatalogFilters,
                    onLoadMore = component::loadMoreCatalogProducts,
                    onOpenProduct = component::openProduct,
                    onAddProduct = { component.addToBasket(it) }
                )

                ProductDemoTab.SelectedProducts -> BasketTab(
                    basketItems = state.basketItems,
                    onToggle = component::toggleBasketItem,
                    onRemove = component::removeFromBasket,
                    onOrderRequest = component::requestOrderForSelectedProducts
                )

                ProductDemoTab.Profile -> ProfileTab(
                    profile = state.profile,
                    onLoginChanged = component::updateProfileLogin,
                    onPasswordChanged = component::updateProfilePassword,
                    onLogin = component::loginProfileUser,
                    onLogout = component::logoutProfileUser,
                    onOpenSignUp = component::openSignUp,
                    onOpenCrm = component::openCrm,
                    onResetLocalProfile = component::resetLocalProfileState
                )
            }
        }
    }

    if (openedProduct != null) {
        ProductDetailsScreen(
            product = openedProduct,
            onBack = component::closeProduct,
            inRequest = state.basketItems.any { it.product.id == openedProduct.id },
            onAdd = {
                component.addToBasket(openedProduct)
            }
        )
    }

    if (isOrderRequestOpen) {
        OrderRequestScreen(
            form = state.orderContactForm,
            onBack = component::closeOrderRequest,
            onNameChanged = component::updateOrderContactName,
            onAddressChanged = component::updateOrderContactAddress,
            onCompanyChanged = component::updateOrderContactCompanyName,
            onPhoneChanged = component::updateOrderContactPhoneNumber,
            onEmailChanged = component::updateOrderContactEmail,
            onSubmit = component::submitOrderRequest
        )
    }
    if (state.isSignUpOpen) {
        CatalogSignUpDialog(
            form = state.signUpForm,
            onDismiss = component::closeSignUp,
            onNameChanged = component::updateSignUpName,
            onPhoneChanged = component::updateSignUpPhoneNumber,
            onEmailChanged = component::updateSignUpEmail,
            onSubmit = component::submitSignUp
        )
    }

    val successMessage = state.orderSuccessMessage
    if (!successMessage.isNullOrBlank()) {
        AlertDialog(
            onDismissRequest = component::dismissOrderSuccess,
            title = { Text("Заявка отправлена") },
            text = { Text(successMessage) },
            confirmButton = {
                TextButton(onClick = component::dismissOrderSuccess) {
                    Text("Понятно")
                }
            }
        )
    }
}
