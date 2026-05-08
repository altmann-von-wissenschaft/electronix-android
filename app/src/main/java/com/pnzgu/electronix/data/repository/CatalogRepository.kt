package com.pnzgu.electronix.data.repository

import com.pnzgu.electronix.data.api.ElectronixApi
import com.pnzgu.electronix.data.dto.CategoryDto
import com.pnzgu.electronix.data.dto.CategoryCharacteristicDto
import com.pnzgu.electronix.data.dto.ProductDto
import com.pnzgu.electronix.data.dto.PagedProductsResponse
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class CatalogRepository(
    private val api: ElectronixApi,
    private val cacheTtlMs: Long = 45_000L,
) {
    private val mutex = Mutex()
    private val categoryCache = mutableMapOf<String, CacheEntry<List<CategoryDto>>>()
    private val productPageCache = mutableMapOf<String, CacheEntry<PagedProductsResponse>>()
    private val productDetailCache = mutableMapOf<String, CacheEntry<ProductDto>>()

    data class CacheEntry<T>(val value: T, val storedAt: Long)

    private fun now() = System.currentTimeMillis()

    suspend fun invalidateAll() {
        mutex.withLock {
            categoryCache.clear()
            productPageCache.clear()
            productDetailCache.clear()
        }
    }

    suspend fun categories(parentId: String?): List<CategoryDto> {
        val key = parentId ?: "root"
        mutex.withLock {
            val hit = categoryCache[key]
            if (hit != null && now() - hit.storedAt < cacheTtlMs) return hit.value
        }
        val fresh = api.categories(parentId)
        mutex.withLock { categoryCache[key] = CacheEntry(fresh, now()) }
        return fresh
    }

    /**
     * Catalog home: when the backend keeps a single synthetic root (only one category with no parent),
     * show that root's children instead of the root row.
     */
    suspend fun categoriesForCatalogRoot(): List<CategoryDto> {
        val top = categories(null)
        return if (top.size == 1) categories(top[0].id) else top
    }

    suspend fun categoryCharacteristics(categoryId: String): List<CategoryCharacteristicDto> =
        api.categoryCharacteristics(categoryId)

    suspend fun products(
        categoryId: String,
        page: Int,
        pageSize: Int,
        filters: Map<String, String>,
    ): PagedProductsResponse {
        val key = "$categoryId|$page|$pageSize|${filters.entries.sortedBy { it.key }.joinToString { "${it.key}=${it.value}" }}"
        mutex.withLock {
            val hit = productPageCache[key]
            if (hit != null && now() - hit.storedAt < cacheTtlMs) return hit.value
        }
        val fresh = api.products(categoryId, page, pageSize, search = null, filters = filters)
        mutex.withLock { productPageCache[key] = CacheEntry(fresh, now()) }
        return fresh
    }

    suspend fun product(id: String, skipCache: Boolean = false): ProductDto {
        if (!skipCache) {
            mutex.withLock {
                val hit = productDetailCache[id]
                if (hit != null && now() - hit.storedAt < cacheTtlMs) return hit.value
            }
        }
        val fresh = api.product(id)
        mutex.withLock { productDetailCache[id] = CacheEntry(fresh, now()) }
        return fresh
    }
}
