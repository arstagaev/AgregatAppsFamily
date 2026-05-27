package com.tagaev.trrcrm.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CatalogProductItemDto(
    val id: String,
    val name: String,
    val description: String,
    @SerialName("image_url")
    val imageUrl: String? = null,
    @SerialName("is_active")
    val isActive: Boolean = true,
    val brand: String? = null,
    val model: String? = null,
    @SerialName("category_id")
    val categoryId: String? = null,
    @SerialName("parent_category_id")
    val parentCategoryId: String? = null,
)

@Serializable
data class CatalogProductsResponse(
    val status: String? = null,
    val items: List<CatalogProductItemDto> = emptyList(),
    val products: List<CatalogProductItemDto> = emptyList(),
    @SerialName("updated_at")
    val updatedAt: String? = null,
    val pagination: CatalogPaginationDto? = null,
)

@Serializable
data class CatalogPaginationDto(
    val page: Int = 1,
    val limit: Int = 20,
    val total: Int = 0,
    @SerialName("total_pages")
    val totalPages: Int = 1,
    @SerialName("has_next")
    val hasNext: Boolean = false,
    @SerialName("has_prev")
    val hasPrev: Boolean = false,
)

@Serializable
data class CatalogSignupRequest(
    val name: String,
    val phone: String? = null,
    val email: String,
    val source: String,
    @SerialName("crm_session_id")
    val crmSessionId: String? = null,
    @SerialName("crm_login")
    val crmLogin: String? = null,
)

@Serializable
data class CatalogSignupResponse(
    val status: String? = null,
    @SerialName("request_id")
    val requestId: String? = null,
    @SerialName("created_at")
    val createdAt: String? = null,
)

@Serializable
data class CatalogProductRequestContact(
    val name: String,
    val email: String,
    val phone: String? = null,
    val company: String? = null,
    val address: String? = null,
)

@Serializable
data class CatalogProductRequestItem(
    @SerialName("product_id")
    val productId: String,
    @SerialName("product_name")
    val productName: String,
    val qty: Int,
)

@Serializable
data class CatalogProductRequest(
    val contact: CatalogProductRequestContact,
    val items: List<CatalogProductRequestItem>,
    val source: String,
    @SerialName("crm_session_id")
    val crmSessionId: String? = null,
    @SerialName("crm_login")
    val crmLogin: String? = null,
    @SerialName("crm_full_name")
    val crmFullName: String? = null,
)

@Serializable
data class CatalogProductRequestResponse(
    val status: String? = null,
    @SerialName("request_id")
    val requestId: String? = null,
    @SerialName("created_at")
    val createdAt: String? = null,
)
