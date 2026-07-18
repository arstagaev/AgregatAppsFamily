package com.tagaev.trrcrm.ui.product_demo

import com.tagaev.trrcrm.ui.i18n.s

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CatalogTab(
    products: List<DemoProduct>,
    addedProductIds: Set<String>,
    isLoading: Boolean,
    isLoadingMore: Boolean,
    errorText: String?,
    searchQuery: String,
    selectedBrand: String?,
    availableBrands: List<String>,
    hasNext: Boolean,
    onSearchQueryChanged: (String) -> Unit,
    onBrandChanged: (String?) -> Unit,
    onClearFilters: () -> Unit,
    onLoadMore: () -> Unit,
    onOpenProduct: (String) -> Unit,
    onAddProduct: (DemoProduct) -> Unit,
) {
    var brandExpanded by remember { mutableStateOf(false) }

    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 220.dp),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = searchQuery,
                onValueChange = onSearchQueryChanged,
                singleLine = true,
                label = { Text(s("product_demo_poisk_po_nazvaniyu")) }
            )
        }

        item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ExposedDropdownMenuBox(
                    expanded = brandExpanded,
                    onExpandedChange = { brandExpanded = !brandExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = selectedBrand.orEmpty(),
                        onValueChange = {},
                        readOnly = true,
                        singleLine = true,
                        label = { Text("АКПП и комплектующие по бренду") },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = brandExpanded,
                        onDismissRequest = { brandExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("АКПП и комплектующие для всех брендов") },
                            onClick = {
                                brandExpanded = false
                                onBrandChanged(null)
                            }
                        )
                        availableBrands.forEach { brand ->
                            DropdownMenuItem(
                                text = { Text(brand) },
                                onClick = {
                                    brandExpanded = false
                                    onBrandChanged(brand)
                                }
                            )
                        }
                    }
                }
                Button(
                    onClick = onClearFilters,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(s("product_demo_sbrosit_filtry"))
                }
            }
        }

        if (products.isEmpty()) {
            item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
                Box(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                    val text = catalogEmptyStateMessage(
                        isLoading = isLoading,
                        errorText = errorText,
                        searchQuery = searchQuery,
                        brand = selectedBrand,
                        model = null,
                        categoryId = ""
                    )
                    Text(
                        modifier = Modifier.padding(16.dp),
                        text = text,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            return@LazyVerticalGrid
        }

        items(products, key = { it.id }) { product ->
            OutlinedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpenProduct(product.id) },
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.outlinedCardColors()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                    ) {
                        val imageUrl = product.imageName?.trim().orEmpty()
                        if (imageUrl.startsWith("http://") || imageUrl.startsWith("https://")) {
                            AsyncImage(
                                model = imageUrl,
                                contentDescription = product.name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Text(
                                modifier = Modifier.padding(8.dp),
                                text = "Изображение скоро появится",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Text(
                        text = product.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
//                    Text(
//                        text = product.description,
//                        style = MaterialTheme.typography.bodySmall,
//                        maxLines = 3,
//                        overflow = TextOverflow.Ellipsis
//                    )
                    Button(
                        onClick = { onAddProduct(product) },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = product.id !in addedProductIds
                    ) {
                        Text(if (product.id in addedProductIds) "В заявке" else s("product_demo_dobavit_k_zayavke"))
                    }
                }
            }
        }

        if (hasNext) {
            item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
                Button(
                    onClick = onLoadMore,
                    enabled = !isLoadingMore,
                    modifier = Modifier.fillMaxWidth().padding(top = 6.dp)
                ) {
                    Text(if (isLoadingMore) "Загружаем..." else s("product_demo_pokazat_esche"))
                }
            }
        }
    }
}
