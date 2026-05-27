package com.tagaev.trrcrm.ui.product_demo

data class DemoProduct(
    val id: String,
    val name: String,
    val description: String,
    val imageName: String? = null,
    val brand: String? = null,
    val model: String? = null,
    val categoryId: String? = null,
    val parentCategoryId: String? = null,
)

data class BasketItem(
    val product: DemoProduct,
    val isSelected: Boolean = true,
)

data class DemoProfileState(
    val isCrmLoggedIn: Boolean = false,
    val displayName: String = "",
    val email: String = "",
    val login: String = "",
    val password: String = "",
    val loginError: String? = null,
    val isLoginLoading: Boolean = false,
)

data class OrderContactFormState(
    val name: String = "",
    val address: String = "",
    val companyName: String = "",
    val phoneNumber: String = "",
    val email: String = "",
    val validationError: String? = null,
    val fieldErrors: Map<String, String> = emptyMap(),
    val isSubmitting: Boolean = false,
)

data class SignUpFormState(
    val name: String = "",
    val phoneNumber: String = "",
    val email: String = "",
    val validationError: String? = null,
    val fieldErrors: Map<String, String> = emptyMap(),
    val isSubmitting: Boolean = false,
)

enum class ProductDemoTab {
    Catalog,
    SelectedProducts,
    Profile,
}

data class ProductDemoUiState(
    val selectedTab: ProductDemoTab = ProductDemoTab.Catalog,
    val products: List<DemoProduct> = emptyList(),
    val isProductsLoading: Boolean = false,
    val isProductsLoadingMore: Boolean = false,
    val productsError: String? = null,
    val catalogSearchQuery: String = "",
    val catalogBrand: String? = null,
    val catalogModel: String? = null,
    val catalogCategoryId: String = "",
    val catalogAvailableBrands: List<String> = emptyList(),
    val catalogAvailableModels: List<String> = emptyList(),
    val catalogPage: Int = 1,
    val catalogLimit: Int = 20,
    val catalogHasNext: Boolean = false,
    val catalogTotal: Int = 0,
    val basketItems: List<BasketItem> = emptyList(),
    val openedProduct: DemoProduct? = null,
    val profile: DemoProfileState = DemoProfileState(),
    val isOrderRequestOpen: Boolean = false,
    val orderContactForm: OrderContactFormState = OrderContactFormState(),
    val isSignUpOpen: Boolean = false,
    val signUpForm: SignUpFormState = SignUpFormState(),
    val orderSuccessMessage: String? = null,
    val transientMessage: String? = null,
)
