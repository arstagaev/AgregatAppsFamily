package com.tagaev.trrcrm.ui.product_demo

import com.tagaev.trrcrm.ui.i18n.tr

import com.arkivanov.decompose.ComponentContext
import com.tagaev.trrcrm.data.AppSettings
import com.tagaev.trrcrm.data.AppSettingsKeys
import com.tagaev.trrcrm.data.MainRepository
import com.tagaev.trrcrm.data.remote.CoreApiException
import com.tagaev.trrcrm.data.remote.Resource
import com.tagaev.trrcrm.data.remote.friendlyError
import com.tagaev.trrcrm.data.remote.normalizedErrorCode
import com.tagaev.trrcrm.models.CatalogProductRequestItem
import com.tagaev.trrcrm.models.CoreSessionLogoutRequest
import com.tagaev.trrcrm.push.NotificationsUnreadState
import com.tagaev.trrcrm.push.PushRegistration
import com.tagaev.trrcrm.push.disablePushDeliveryForLoggedOutUser
import com.tagaev.trrcrm.pushPlatformId
import com.tagaev.trrcrm.ui.login.CrmAuthUseCase
import com.tagaev.trrcrm.utils.SessionPermissions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

interface IProductDemoComponent {
    val uiState: StateFlow<ProductDemoUiState>
    fun selectTab(tab: ProductDemoTab)
    fun openProduct(productId: String)
    fun closeProduct()
    fun updateCatalogSearchQuery(value: String)
    fun updateCatalogBrandFilter(value: String?)
    fun updateCatalogModelFilter(value: String?)
    fun updateCatalogCategoryIdFilter(value: String)
    fun clearCatalogFilters()
    fun loadMoreCatalogProducts()
    fun addToBasket(product: DemoProduct)
    fun removeFromBasket(productId: String)
    fun toggleBasketItem(productId: String)
    fun requestOrderForSelectedProducts()
    fun updateProfileLogin(value: String)
    fun updateProfilePassword(value: String)
    fun loginProfileUser(login: String, password: String)
    fun logoutProfileUser()
    fun openSignUp()
    fun closeSignUp()
    fun updateSignUpName(value: String)
    fun updateSignUpPhoneNumber(value: String)
    fun updateSignUpEmail(value: String)
    fun submitSignUp()
    fun openCrm()
    fun resetLocalProfileState()
    fun closeOrderRequest()
    fun updateOrderContactName(value: String)
    fun updateOrderContactAddress(value: String)
    fun updateOrderContactCompanyName(value: String)
    fun updateOrderContactPhoneNumber(value: String)
    fun updateOrderContactEmail(value: String)
    fun submitOrderRequest()
    fun dismissOrderSuccess()
    fun consumeTransientMessage()
}

class ProductDemoComponent(
    componentContext: ComponentContext,
    private val onOpenCrmRequested: () -> Unit,
) : IProductDemoComponent, ComponentContext by componentContext, KoinComponent {
    companion object {
        private const val CATALOG_SEARCH_DEBOUNCE_MS = 400L
    }

    private val appScope: CoroutineScope by inject()
    private val appSettings: AppSettings by inject()
    private val repository: MainRepository by inject()
    private var catalogCriteriaJob: Job? = null

    private val _uiState = MutableStateFlow(
        ProductDemoUiState(
            products = emptyList(),
            isProductsLoading = true,
            profile = loadCrmProfileSnapshot(),
            orderContactForm = loadInitialOrderContactForm()
        )
    )
    override val uiState: StateFlow<ProductDemoUiState> = _uiState

    // TODO: confirm backend payload details for:
    // GET /catalog/products
    // POST /catalog/signup-request
    // POST /catalog/product-request
    init {
        loadCatalogProducts(reset = true, reason = "initial")
    }

    override fun selectTab(tab: ProductDemoTab) {
        _uiState.update { it.copy(selectedTab = tab) }
        if (tab == ProductDemoTab.Profile) {
            syncProfileFromSettings()
        }
    }

    override fun openProduct(productId: String) {
        _uiState.update { state ->
            state.copy(openedProduct = state.products.firstOrNull { it.id == productId })
        }
    }

    override fun closeProduct() {
        _uiState.update { it.copy(openedProduct = null) }
    }

    override fun updateCatalogSearchQuery(value: String) {
        _uiState.update { state -> withCatalogCriteriaReset(state, searchQuery = value) }
        scheduleCatalogRefresh(reason = "search", debounced = true)
    }

    override fun updateCatalogBrandFilter(value: String?) {
        _uiState.update { state ->
            val normalizedBrand = value?.trim()?.ifBlank { null }
            val nextModels = computeModelOptions(state.products, normalizedBrand)
            val normalizedModel = state.catalogModel?.takeIf { it in nextModels }
            withCatalogCriteriaReset(
                state = state.copy(
                    catalogAvailableModels = nextModels
                ),
                brand = normalizedBrand,
                model = normalizedModel,
            )
        }
        scheduleCatalogRefresh(reason = "brand_filter", debounced = false)
    }

    override fun updateCatalogModelFilter(value: String?) {
        _uiState.update { state ->
            withCatalogCriteriaReset(
                state = state,
                model = value?.trim()?.ifBlank { null }
            )
        }
        scheduleCatalogRefresh(reason = "model_filter", debounced = false)
    }

    override fun updateCatalogCategoryIdFilter(value: String) {
        _uiState.update { state ->
            withCatalogCriteriaReset(state, categoryId = value)
        }
        scheduleCatalogRefresh(reason = "category_filter", debounced = true)
    }

    override fun clearCatalogFilters() {
        _uiState.update { state ->
            withCatalogCriteriaReset(
                state = state,
                searchQuery = "",
                brand = null,
                model = null,
                categoryId = ""
            )
        }
        scheduleCatalogRefresh(reason = "clear_filters", debounced = false)
    }

    override fun loadMoreCatalogProducts() {
        val state = uiState.value
        if (!canLoadMoreCatalog(state)) return
        loadCatalogProducts(reset = false, reason = "load_more")
    }

    private fun scheduleCatalogRefresh(reason: String, debounced: Boolean) {
        catalogCriteriaJob?.cancel()
        if (debounced) {
            catalogCriteriaJob = appScope.launch {
                delay(CATALOG_SEARCH_DEBOUNCE_MS)
                loadCatalogProducts(reset = true, reason = reason)
            }
        } else {
            loadCatalogProducts(reset = true, reason = reason)
        }
    }

    override fun addToBasket(product: DemoProduct) {
        _uiState.update { state ->
            val existing = state.basketItems.firstOrNull { it.product.id == product.id }
            if (existing != null) {
                state.copy(
                    basketItems = state.basketItems.map {
                        if (it.product.id == product.id) it.copy(isSelected = true) else it
                    },
                    transientMessage = tr("product_demo_tovar_uzhe_dobavlen_v_zayavku")
                )
            } else {
                state.copy(
                    basketItems = state.basketItems + BasketItem(product = product, isSelected = true),
                    transientMessage = tr("product_demo_tovar_dobavlen_v_zayavku")
                )
            }
        }
    }

    override fun removeFromBasket(productId: String) {
        _uiState.update { state ->
            state.copy(
                basketItems = state.basketItems.filterNot { it.product.id == productId },
                transientMessage = tr("product_demo_tovar_udalen")
            )
        }
    }

    override fun toggleBasketItem(productId: String) {
        _uiState.update { state ->
            state.copy(
                basketItems = state.basketItems.map {
                    if (it.product.id == productId) it.copy(isSelected = !it.isSelected) else it
                }
            )
        }
    }

    override fun requestOrderForSelectedProducts() {
        _uiState.update { state ->
            val selectedCount = state.basketItems.count { it.isSelected }
            if (selectedCount == 0) {
                state.copy(transientMessage = tr("product_demo_vyberite_tovary_dlya_zayavki"))
            } else {
                val prefilledName = state.orderContactForm.name.ifBlank { state.profile.displayName }
                val prefilledEmail = state.orderContactForm.email.ifBlank { state.profile.email }
                state.copy(
                    isOrderRequestOpen = true,
                    orderContactForm = state.orderContactForm.copy(
                        name = prefilledName,
                        email = prefilledEmail,
                        validationError = null
                    )
                )
            }
        }
    }

    override fun updateProfileLogin(value: String) {
        _uiState.update { state ->
            state.copy(
                profile = state.profile.copy(
                    login = value,
                    loginError = null
                )
            )
        }
    }

    override fun updateProfilePassword(value: String) {
        _uiState.update { state ->
            state.copy(
                profile = state.profile.copy(
                    password = value,
                    loginError = null
                )
            )
        }
    }

    override fun loginProfileUser(login: String, password: String) {
        val normalizedLogin = login.trim()
        if (normalizedLogin.isBlank() || password.isBlank()) {
            _uiState.update { state ->
                state.copy(
                    profile = state.profile.copy(loginError = tr("product_demo_vvedite_login_i_parol"))
                )
            }
            return
        }
        _uiState.update { state ->
            state.copy(
                profile = state.profile.copy(
                    isLoginLoading = true,
                    loginError = null
                )
            )
        }
        appScope.launch {
            when (val auth = CrmAuthUseCase.loginWithCredentials(normalizedLogin, password)) {
                is Resource.Success -> {
                    _uiState.update { state ->
                        state.copy(transientMessage = tr("product_demo_vhod_vypolnen"))
                    }
                    syncProfileFromSettings()
                }
                is Resource.Error -> {
                    val message = auth.causes ?: friendlyError(auth.exception, tr("login_oshibka_avtorizatsii"))
                    _uiState.update { state ->
                        state.copy(
                            profile = state.profile.copy(
                                isLoginLoading = false,
                                loginError = message
                            )
                        )
                    }
                }
                is Resource.Loading -> Unit
            }
        }
    }

    override fun logoutProfileUser() {
        val fullName = appSettings.getStringOrNull(AppSettingsKeys.PERSONAL_DATA).orEmpty().trim()
        val platform = pushPlatformId()
        val coreSessionId = appSettings.getStringOrNull(AppSettingsKeys.CORE_SESSION_ID).orEmpty().trim()

        if (coreSessionId.isNotBlank()) {
            appScope.launch {
                repository.coreSessionLogout(
                    CoreSessionLogoutRequest(
                        sessionId = coreSessionId,
                        deactivateDeviceToken = true
                    )
                )
            }
        }
        if (fullName.isNotBlank()) {
            PushRegistration.logoutCurrentDevice(
                fullName = fullName,
                platform = platform
            )
        }

        SessionPermissions.clear()
        appSettings.setInt(AppSettingsKeys.NOTIFICATIONS_UNREAD_COUNT, 0)
        appSettings.clearForLogoutPreservingInstallIdentity()
        NotificationsUnreadState.setCount(0)
        disablePushDeliveryForLoggedOutUser()
        _uiState.update { state ->
            state.copy(
                profile = loadCrmProfileSnapshot(),
                transientMessage = tr("product_demo_vy_vyshli_iz_crm_profilya")
            )
        }
    }

    override fun openSignUp() {
        _uiState.update { state ->
            val prefilledName = state.signUpForm.name.ifBlank { state.profile.displayName }
            val prefilledEmail = state.signUpForm.email.ifBlank { state.profile.email }
            state.copy(
                isSignUpOpen = true,
                signUpForm = state.signUpForm.copy(
                    name = prefilledName,
                    email = prefilledEmail,
                    validationError = null,
                    fieldErrors = emptyMap()
                )
            )
        }
    }

    override fun closeSignUp() {
        _uiState.update { state ->
            state.copy(
                isSignUpOpen = false,
                signUpForm = state.signUpForm.copy(
                    validationError = null,
                    fieldErrors = emptyMap(),
                    isSubmitting = false
                )
            )
        }
    }

    override fun updateSignUpName(value: String) {
        _uiState.update { state ->
            state.copy(
                signUpForm = state.signUpForm.copy(
                    name = value,
                    validationError = null,
                    fieldErrors = emptyMap()
                )
            )
        }
    }

    override fun updateSignUpPhoneNumber(value: String) {
        _uiState.update { state ->
            state.copy(
                signUpForm = state.signUpForm.copy(
                    phoneNumber = value,
                    validationError = null,
                    fieldErrors = emptyMap()
                )
            )
        }
    }

    override fun updateSignUpEmail(value: String) {
        _uiState.update { state ->
            state.copy(
                signUpForm = state.signUpForm.copy(
                    email = value,
                    validationError = null,
                    fieldErrors = emptyMap()
                )
            )
        }
    }

    override fun submitSignUp() {
        val form = uiState.value.signUpForm
        if (form.name.trim().isBlank() || form.email.trim().isBlank()) {
            _uiState.update { state ->
                state.copy(
                    signUpForm = state.signUpForm.copy(
                        validationError = "Заполните обязательные поля: имя и email",
                        fieldErrors = emptyMap()
                    )
                )
            }
            return
        }

        _uiState.update { state ->
            state.copy(
                signUpForm = state.signUpForm.copy(
                    isSubmitting = true,
                    validationError = null,
                    fieldErrors = emptyMap()
                )
            )
        }

        appScope.launch {
            when (
                val res = repository.catalogSignupRequest(
                    name = form.name,
                    phone = form.phoneNumber,
                    email = form.email
                )
            ) {
                is Resource.Success -> {
                    _uiState.update { state ->
                        state.copy(
                            isSignUpOpen = false,
                            signUpForm = state.signUpForm.copy(
                                validationError = null,
                                fieldErrors = emptyMap(),
                                isSubmitting = false
                            ),
                            transientMessage = "Запрос отправлен. Мы свяжемся с вами после проверки данных."
                        )
                    }
                }

                is Resource.Error -> {
                    val fieldErrors = extractCatalogFieldErrors(res.exception)
                    val message = fieldErrors.values.joinToString(separator = "\n")
                        .ifBlank { res.causes ?: friendlyError(res.exception, tr("product_demo_ne_udalos_otpravit_zayavku")) }
                    _uiState.update { state ->
                        state.copy(
                            signUpForm = state.signUpForm.copy(
                                isSubmitting = false,
                                validationError = message,
                                fieldErrors = fieldErrors
                            )
                        )
                    }
                }

                is Resource.Loading -> Unit
            }
        }
    }

    override fun openCrm() {
        onOpenCrmRequested()
    }

    override fun resetLocalProfileState() {
        val snapshot = loadCrmProfileSnapshot()
        _uiState.update { state ->
            state.copy(
                profile = snapshot.copy(password = ""),
                transientMessage = "Локальные поля профиля сброшены"
            )
        }
    }

    override fun closeOrderRequest() {
        _uiState.update { state ->
            state.copy(
                isOrderRequestOpen = false,
                orderContactForm = state.orderContactForm.copy(
                    validationError = null,
                    fieldErrors = emptyMap(),
                    isSubmitting = false
                )
            )
        }
    }

    override fun updateOrderContactName(value: String) {
        _uiState.update { state ->
            state.copy(
                orderContactForm = state.orderContactForm.copy(
                    name = value,
                    validationError = null,
                    fieldErrors = emptyMap()
                )
            )
        }
    }

    override fun updateOrderContactAddress(value: String) {
        _uiState.update { state ->
            state.copy(
                orderContactForm = state.orderContactForm.copy(
                    address = value,
                    validationError = null,
                    fieldErrors = emptyMap()
                )
            )
        }
    }

    override fun updateOrderContactCompanyName(value: String) {
        _uiState.update { state ->
            state.copy(
                orderContactForm = state.orderContactForm.copy(
                    companyName = value,
                    validationError = null,
                    fieldErrors = emptyMap()
                )
            )
        }
    }

    override fun updateOrderContactPhoneNumber(value: String) {
        _uiState.update { state ->
            state.copy(
                orderContactForm = state.orderContactForm.copy(
                    phoneNumber = value,
                    validationError = null,
                    fieldErrors = emptyMap()
                )
            )
        }
    }

    override fun updateOrderContactEmail(value: String) {
        _uiState.update { state ->
            state.copy(
                orderContactForm = state.orderContactForm.copy(
                    email = value,
                    validationError = null,
                    fieldErrors = emptyMap()
                )
            )
        }
    }

    override fun submitOrderRequest() {
        val stateSnapshot = uiState.value
        val form = stateSnapshot.orderContactForm
        val selectedItems = stateSnapshot.basketItems.filter { it.isSelected }
        if (selectedItems.isEmpty()) {
            _uiState.update { state ->
                state.copy(
                    isOrderRequestOpen = false,
                    transientMessage = "Добавьте товары из каталога для отправки заявки"
                )
            }
            return
        }
        if (form.name.trim().isBlank() || form.email.trim().isBlank()) {
            _uiState.update { state ->
                state.copy(
                    orderContactForm = state.orderContactForm.copy(
                        validationError = "Заполните обязательные поля: имя и email",
                        fieldErrors = emptyMap()
                    )
                )
            }
            return
        }

        _uiState.update { state ->
            state.copy(
                orderContactForm = state.orderContactForm.copy(
                    isSubmitting = true,
                    validationError = null,
                    fieldErrors = emptyMap()
                )
            )
        }

        val items = selectedItems.map {
            CatalogProductRequestItem(
                productId = it.product.id,
                productName = it.product.name,
                qty = 1
            )
        }

        appScope.launch {
            when (
                val res = repository.catalogProductRequest(
                    contactName = form.name,
                    contactEmail = form.email,
                    contactPhone = form.phoneNumber,
                    contactCompany = form.companyName,
                    contactAddress = form.address,
                    items = items
                )
            ) {
                is Resource.Success -> {
                    _uiState.update { state ->
                        state.copy(
                            basketItems = state.basketItems.filterNot { it.isSelected },
                            isOrderRequestOpen = false,
                            orderContactForm = state.orderContactForm.copy(
                                validationError = null,
                                fieldErrors = emptyMap(),
                                isSubmitting = false
                            ),
                            orderSuccessMessage = "Заявка отправлена. Менеджер свяжется с вами по указанным контактам."
                        )
                    }
                }

                is Resource.Error -> {
                    val fieldErrors = extractCatalogFieldErrors(res.exception)
                    val message = fieldErrors.values.joinToString(separator = "\n")
                        .ifBlank { res.causes ?: friendlyError(res.exception, tr("product_demo_ne_udalos_otpravit_zayavku")) }
                    _uiState.update { state ->
                        state.copy(
                            orderContactForm = state.orderContactForm.copy(
                                isSubmitting = false,
                                validationError = message,
                                fieldErrors = fieldErrors
                            )
                        )
                    }
                }

                is Resource.Loading -> Unit
            }
        }
    }

    override fun dismissOrderSuccess() {
        _uiState.update { it.copy(orderSuccessMessage = null) }
    }

    override fun consumeTransientMessage() {
        _uiState.update { it.copy(transientMessage = null) }
    }

    private fun loadCrmProfileSnapshot(): DemoProfileState {
        val isCrmLoggedIn = !appSettings.getStringOrNull(AppSettingsKeys.TOKEN_KEY).isNullOrBlank()
        val savedLogin = appSettings.getStringOrNull(AppSettingsKeys.EMAIL).orEmpty()
        val savedName = appSettings.getStringOrNull(AppSettingsKeys.PERSONAL_DATA).orEmpty()
        return DemoProfileState(
            isCrmLoggedIn = isCrmLoggedIn,
            displayName = savedName,
            email = savedLogin,
            login = savedLogin
        )
    }

    private fun loadInitialOrderContactForm(): OrderContactFormState {
        val name = appSettings.getStringOrNull(AppSettingsKeys.PERSONAL_DATA).orEmpty()
        val email = appSettings.getStringOrNull(AppSettingsKeys.EMAIL).orEmpty()
        return OrderContactFormState(name = name, email = email)
    }

    private fun syncProfileFromSettings() {
        val snapshot = loadCrmProfileSnapshot()
        _uiState.update { state ->
            state.copy(
                profile = state.profile.copy(
                    isCrmLoggedIn = snapshot.isCrmLoggedIn,
                    displayName = snapshot.displayName,
                    email = snapshot.email,
                    login = snapshot.login,
                    isLoginLoading = false,
                    loginError = null
                )
            )
        }
    }

    private fun loadCatalogProducts(reset: Boolean, reason: String) {
        val snapshot = uiState.value
        val query = snapshot.catalogSearchQuery.trim().ifBlank { null }
        val brand = snapshot.catalogBrand?.trim()?.ifBlank { null }
        val model = snapshot.catalogModel?.trim()?.ifBlank { null }
        val categoryId = snapshot.catalogCategoryId.trim().ifBlank { null }
        val page = if (reset) 1 else snapshot.catalogPage + 1
        val limit = snapshot.catalogLimit.coerceIn(1, 100)
        println(
            "CATALOG_UI: request_start reason=$reason query='${query.orEmpty()}' brand='${brand.orEmpty()}' model='${model.orEmpty()}' category='${categoryId.orEmpty()}' page=$page limit=$limit"
        )

        _uiState.update { state ->
            if (reset) {
                state.copy(
                    isProductsLoading = true,
                    isProductsLoadingMore = false,
                    productsError = null,
                    catalogPage = 1,
                    catalogHasNext = false,
                )
            } else {
                state.copy(
                    isProductsLoadingMore = true,
                    productsError = null,
                )
            }
        }

        appScope.launch {
            when (
                val res = repository.catalogProducts(
                    search = query,
                    brand = brand,
                    model = model,
                    categoryId = categoryId,
                    page = page,
                    limit = limit
                )
            ) {
                is Resource.Success -> {
                    val sourceItems = if (res.data.items.isNotEmpty()) res.data.items else res.data.products
                    val remote = sourceItems
                        .filter { it.isActive }
                        .map {
                            DemoProduct(
                                id = it.id,
                                name = it.name,
                                description = it.description,
                                imageName = it.imageUrl,
                                brand = it.brand,
                                model = it.model,
                                categoryId = it.categoryId,
                                parentCategoryId = it.parentCategoryId,
                            )
                        }
                    val hasNext = res.data.pagination?.hasNext ?: false
                    val currentPage = res.data.pagination?.page ?: page
                    val total = res.data.pagination?.total ?: if (reset) remote.size else snapshot.catalogTotal
                    val mergedProducts = if (reset) remote else mergeCatalogProductsById(existing = _uiState.value.products, incoming = remote)
                    val brands = computeBrandOptions(mergedProducts)
                    val models = computeModelOptions(mergedProducts, brand)
                    val normalizedModel = model?.takeIf { it in models }
                    println(
                        "CATALOG_UI: request_success query='${query.orEmpty()}' brand='${brand.orEmpty()}' model='${model.orEmpty()}' category='${categoryId.orEmpty()}' page=$currentPage total=$total has_next=$hasNext"
                    )
                    if (!hasNext) {
                        println("CATALOG_UI: pagination_exhausted page=$currentPage total=$total")
                    }
                    _uiState.update { state ->
                        state.copy(
                            products = mergedProducts,
                            isProductsLoading = false,
                            isProductsLoadingMore = false,
                            productsError = null,
                            catalogBrand = brand,
                            catalogModel = normalizedModel,
                            catalogCategoryId = categoryId.orEmpty(),
                            catalogAvailableBrands = brands,
                            catalogAvailableModels = models,
                            catalogPage = currentPage,
                            catalogHasNext = hasNext,
                            catalogTotal = total,
                        )
                    }
                }

                is Resource.Error -> {
                    val message = res.causes ?: friendlyError(res.exception, tr("product_demo_ne_udalos_zagruzit_katalog"))
                    val errorCode = (res.exception as? CoreApiException)?.normalizedErrorCode() ?: "transport_error"
                    println(
                        "CATALOG_UI: request_fail query='${query.orEmpty()}' brand='${brand.orEmpty()}' model='${model.orEmpty()}' category='${categoryId.orEmpty()}' page=$page error_code=$errorCode reason=$message"
                    )
                    _uiState.update { state ->
                        state.copy(
                            isProductsLoading = false,
                            isProductsLoadingMore = false,
                            productsError = message,
                            transientMessage = message
                        )
                    }
                }

                is Resource.Loading -> Unit
            }
        }
    }

    private fun extractCatalogFieldErrors(exception: Exception?): Map<String, String> {
        val core = exception as? CoreApiException ?: return emptyMap()
        if (core.fields.isEmpty()) return emptyMap()
        val errors = linkedMapOf<String, String>()
        core.fields.forEach { field ->
            val normalizedKey = normalizeFieldKey(field.field)
            val message = field.message ?: field.code ?: return@forEach
            if (normalizedKey != null && normalizedKey !in errors) {
                errors[normalizedKey] = message
            }
        }
        return errors
    }

    private fun normalizeFieldKey(raw: String?): String? {
        val field = raw?.trim()?.lowercase()?.ifBlank { null } ?: return null
        return when {
            "name" in field -> "name"
            "email" in field -> "email"
            "phone" in field || "tel" in field -> "phone"
            "address" in field -> "address"
            "company" in field -> "company"
            "item" in field || "product" in field -> "items"
            else -> field
        }
    }

}

internal fun withCatalogCriteriaReset(
    state: ProductDemoUiState,
    searchQuery: String = state.catalogSearchQuery,
    brand: String? = state.catalogBrand,
    model: String? = state.catalogModel,
    categoryId: String = state.catalogCategoryId,
): ProductDemoUiState = state.copy(
    catalogSearchQuery = searchQuery,
    catalogBrand = brand,
    catalogModel = model,
    catalogCategoryId = categoryId,
    catalogPage = 1,
    catalogHasNext = false,
    productsError = null
)

internal fun canLoadMoreCatalog(state: ProductDemoUiState): Boolean {
    return !state.isProductsLoading && !state.isProductsLoadingMore && state.catalogHasNext
}

internal fun mergeCatalogProductsById(existing: List<DemoProduct>, incoming: List<DemoProduct>): List<DemoProduct> {
    if (incoming.isEmpty()) return existing
    val seen = existing.mapTo(mutableSetOf()) { it.id }
    val merged = existing.toMutableList()
    incoming.forEach { item ->
        if (seen.add(item.id)) merged += item
    }
    return merged
}

internal fun computeBrandOptions(products: List<DemoProduct>): List<String> {
    return products.mapNotNull { it.brand?.trim() }
        .filter { it.isNotEmpty() }
        .distinct()
        .sorted()
}

internal fun computeModelOptions(products: List<DemoProduct>, selectedBrand: String?): List<String> {
    val normalizedBrand = selectedBrand?.trim()?.ifBlank { null }
    return products.asSequence()
        .filter { normalizedBrand == null || it.brand == normalizedBrand }
        .mapNotNull { it.model?.trim() }
        .filter { it.isNotEmpty() }
        .distinct()
        .sorted()
        .toList()
}

internal fun catalogEmptyStateMessage(
    isLoading: Boolean,
    errorText: String?,
    searchQuery: String,
    brand: String?,
    model: String?,
    categoryId: String,
): String {
    if (isLoading) return "Загружаем каталог..."
    if (!errorText.isNullOrBlank()) return errorText
    val hasActiveCriteria = searchQuery.trim().isNotBlank() ||
        !brand.isNullOrBlank() ||
        !model.isNullOrBlank() ||
        categoryId.trim().isNotBlank()
    return if (hasActiveCriteria) {
        "По текущим фильтрам ничего не найдено"
    } else {
        "Каталог пока пуст"
    }
}
