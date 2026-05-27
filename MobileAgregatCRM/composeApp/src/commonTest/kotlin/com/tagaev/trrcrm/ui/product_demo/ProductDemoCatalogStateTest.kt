package com.tagaev.trrcrm.ui.product_demo

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ProductDemoCatalogStateTest {

    @Test
    fun `page reset on search and filter change`() {
        val initial = ProductDemoUiState(
            catalogSearchQuery = "old",
            catalogBrand = "VW",
            catalogModel = "09G",
            catalogCategoryId = "cat_old",
            catalogPage = 4,
            catalogHasNext = true,
            productsError = "x"
        )

        val updated = withCatalogCriteriaReset(
            state = initial,
            searchQuery = "new",
            brand = "AUDI",
            model = null,
            categoryId = "cat_new"
        )

        assertEquals("new", updated.catalogSearchQuery)
        assertEquals("AUDI", updated.catalogBrand)
        assertEquals(null, updated.catalogModel)
        assertEquals("cat_new", updated.catalogCategoryId)
        assertEquals(1, updated.catalogPage)
        assertFalse(updated.catalogHasNext)
        assertEquals(null, updated.productsError)
    }

    @Test
    fun `load more allowed only when has_next and not loading`() {
        assertTrue(
            canLoadMoreCatalog(
                ProductDemoUiState(
                    isProductsLoading = false,
                    isProductsLoadingMore = false,
                    catalogHasNext = true
                )
            )
        )
        assertFalse(
            canLoadMoreCatalog(
                ProductDemoUiState(
                    isProductsLoading = true,
                    isProductsLoadingMore = false,
                    catalogHasNext = true
                )
            )
        )
        assertFalse(
            canLoadMoreCatalog(
                ProductDemoUiState(
                    isProductsLoading = false,
                    isProductsLoadingMore = true,
                    catalogHasNext = true
                )
            )
        )
        assertFalse(
            canLoadMoreCatalog(
                ProductDemoUiState(
                    isProductsLoading = false,
                    isProductsLoadingMore = false,
                    catalogHasNext = false
                )
            )
        )
    }

    @Test
    fun `dedup by id across pages preserves first occurrence order`() {
        val existing = listOf(
            DemoProduct(id = "1", name = "A", description = "D1"),
            DemoProduct(id = "2", name = "B", description = "D2")
        )
        val incoming = listOf(
            DemoProduct(id = "2", name = "B2", description = "D2new"),
            DemoProduct(id = "3", name = "C", description = "D3")
        )
        val merged = mergeCatalogProductsById(existing, incoming)
        assertEquals(listOf("1", "2", "3"), merged.map { it.id })
        assertEquals("D2", merged.first { it.id == "2" }.description)
    }

    @Test
    fun `empty result states are specific`() {
        assertEquals(
            "Каталог пока пуст",
            catalogEmptyStateMessage(
                isLoading = false,
                errorText = null,
                searchQuery = "",
                brand = null,
                model = null,
                categoryId = ""
            )
        )
        assertEquals(
            "По текущим фильтрам ничего не найдено",
            catalogEmptyStateMessage(
                isLoading = false,
                errorText = null,
                searchQuery = "abc",
                brand = null,
                model = null,
                categoryId = ""
            )
        )
    }
}

