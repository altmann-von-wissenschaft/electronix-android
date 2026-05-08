package com.pnzgu.electronix.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.pnzgu.electronix.AppContainer
import com.pnzgu.electronix.data.dto.CategoryCharacteristicDto
import com.pnzgu.electronix.data.dto.CategoryDto
import com.pnzgu.electronix.data.dto.PagedReviewsResponse
import com.pnzgu.electronix.data.dto.ProductDto
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CategoryTreeUiState(
    val categories: List<CategoryDto>? = null,
    val error: String? = null,
    val titleFromApi: String? = null,
    val isPullRefreshing: Boolean = false,
)

sealed interface CategoryNavEvent {
    data class OpenProducts(val categoryId: String) : CategoryNavEvent
    data class OpenFolder(val categoryId: String) : CategoryNavEvent
}

class CategoryTreeViewModel(
    private val container: AppContainer,
    private val parentKey: String,
) : ViewModel() {

    private val parentId: String? get() = if (parentKey == "ROOT") null else parentKey

    private val _ui = MutableStateFlow(CategoryTreeUiState())
    val ui: StateFlow<CategoryTreeUiState> = _ui.asStateFlow()

    private val refreshEpoch = MutableStateFlow(0)

    /**
     * Канал в режиме CONFLATED: один актуальный переход, без очереди «висящих» кликов после возврата с корзины.
     * shareIn(Eagerly) даёт один подписчик на receiveAsFlow (валидно для канала) и безопасный collect из нескольких LaunchedEffect.
     */
    private val _navEvents = Channel<CategoryNavEvent>(Channel.CONFLATED)
    val navEvents: SharedFlow<CategoryNavEvent> =
        _navEvents.receiveAsFlow().shareIn(
            viewModelScope,
            SharingStarted.Eagerly,
            replay = 0,
        )

    init {
        viewModelScope.launch {
            refreshEpoch.collect { load() }
        }
    }

    fun pullRefresh() {
        viewModelScope.launch {
            _ui.update { it.copy(isPullRefreshing = true) }
            container.catalogRepository.invalidateAll()
            refreshEpoch.update { it + 1 }
        }
    }

    fun retryAfterError() {
        viewModelScope.launch {
            container.catalogRepository.invalidateAll()
            refreshEpoch.update { it + 1 }
        }
    }

    fun onCategoryClick(categoryId: String) {
        viewModelScope.launch {
            runCatching { container.catalogRepository.categories(categoryId) }
                .onSuccess { children ->
                    val event =
                        if (children.isEmpty()) {
                            CategoryNavEvent.OpenProducts(categoryId)
                        } else {
                            CategoryNavEvent.OpenFolder(categoryId)
                        }
                    _navEvents.send(event)
                }
                .onFailure { e -> _ui.update { it.copy(error = e.message) } }
        }
    }

    private suspend fun load() {
        _ui.update {
            it.copy(error = null, categories = null)
        }
        val pid = parentId
        runCatching {
            val title = if (pid != null) {
                container.api.category(pid).name
            } else {
                null
            }
            val list = if (pid == null) {
                container.catalogRepository.categoriesForCatalogRoot()
            } else {
                container.catalogRepository.categories(pid)
            }
            title to list
        }.onSuccess { (title, list) ->
            _ui.update {
                it.copy(
                    categories = list,
                    titleFromApi = title,
                    error = null,
                    isPullRefreshing = false,
                )
            }
        }.onFailure { e ->
            _ui.update {
                it.copy(
                    error = e.message ?: e.toString(),
                    isPullRefreshing = false,
                )
            }
        }
    }

    companion object {
        fun factory(container: AppContainer, parentKey: String): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    CategoryTreeViewModel(container, parentKey) as T
            }
    }
}

data class FilterRange(val min: String = "", val max: String = "")

data class ProductListUiState(
    val categoryName: String = "",
    val products: List<ProductDto>? = null,
    val page: Int = 1,
    val hasMore: Boolean = true,
    val loading: Boolean = false,
    val error: String? = null,
    val isPullRefreshing: Boolean = false,
    val presets: List<CategoryCharacteristicDto>? = null,
    val filterRanges: Map<String, FilterRange> = emptyMap(),
)

class ProductListViewModel(
    private val container: AppContainer,
    private val categoryId: String,
) : ViewModel() {

    private val _ui = MutableStateFlow(ProductListUiState())
    val ui: StateFlow<ProductListUiState> = _ui.asStateFlow()

    init {
        viewModelScope.launch {
            loadCategoryMeta()
            reloadFirstPageInternal()
        }
    }

    private suspend fun loadCategoryMeta() {
        runCatching { container.api.category(categoryId) }.onSuccess { c ->
            _ui.update { it.copy(categoryName = c.name) }
        }
        runCatching { container.catalogRepository.categoryCharacteristics(categoryId) }.onSuccess { presets ->
            _ui.update { state ->
                val ranges = presets.associate { it.characteristicId to FilterRange() }
                state.copy(
                    presets = presets,
                    filterRanges = ranges,
                )
            }
        }
    }

    private fun buildFilterMap(ranges: Map<String, FilterRange>): Map<String, String> = buildMap {
        ranges.forEach { (id, r) ->
            r.min.toDoubleOrNull()?.let { put("filter.$id.min", it.toString()) }
            r.max.toDoubleOrNull()?.let { put("filter.$id.max", it.toString()) }
        }
    }

    private suspend fun reloadFirstPageInternal() {
        _ui.update { it.copy(loading = true, error = null, page = 1) }
        val ranges = _ui.value.filterRanges
        runCatching {
            container.catalogRepository.products(categoryId, 1, 20, buildFilterMap(ranges))
        }.onSuccess { resp ->
            _ui.update {
                it.copy(
                    products = resp.data,
                    hasMore = resp.data.size >= 20,
                    loading = false,
                    isPullRefreshing = false,
                )
            }
        }.onFailure { e ->
            _ui.update {
                it.copy(
                    error = e.message,
                    loading = false,
                    isPullRefreshing = false,
                )
            }
        }
    }

    fun pullRefresh() {
        viewModelScope.launch {
            _ui.update { it.copy(isPullRefreshing = true) }
            container.catalogRepository.invalidateAll()
            reloadFirstPageInternal()
        }
    }

    fun bumpRefresh() {
        viewModelScope.launch { reloadFirstPageInternal() }
    }

    fun retryList() {
        bumpRefresh()
    }

    fun updateFilterMin(characteristicId: String, value: String) {
        _ui.update { s ->
            val cur = s.filterRanges[characteristicId] ?: FilterRange()
            s.copy(filterRanges = s.filterRanges + (characteristicId to cur.copy(min = value)))
        }
    }

    fun updateFilterMax(characteristicId: String, value: String) {
        _ui.update { s ->
            val cur = s.filterRanges[characteristicId] ?: FilterRange()
            s.copy(filterRanges = s.filterRanges + (characteristicId to cur.copy(max = value)))
        }
    }

    fun clearFilters() {
        _ui.update { s ->
            val presets = s.presets ?: return@update s
            val cleared = presets.associate { it.characteristicId to FilterRange() }
            s.copy(filterRanges = cleared)
        }
    }

    fun loadNextPage() {
        viewModelScope.launch {
            val s = _ui.value
            if (!s.hasMore || s.loading) return@launch
            _ui.update { it.copy(loading = true) }
            val next = s.page + 1
            runCatching {
                container.catalogRepository.products(categoryId, next, 20, buildFilterMap(s.filterRanges))
            }.onSuccess { resp ->
                _ui.update {
                    it.copy(
                        page = next,
                        hasMore = resp.data.size >= 20,
                        products = (it.products ?: emptyList()) + resp.data,
                        loading = false,
                    )
                }
            }.onFailure { e ->
                _ui.update { it.copy(error = e.message, loading = false) }
            }
        }
    }

    companion object {
        fun factory(container: AppContainer, categoryId: String): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    ProductListViewModel(container, categoryId) as T
            }
    }
}

data class ProductDetailUiState(
    val product: ProductDto? = null,
    val reviews: PagedReviewsResponse? = null,
    val error: String? = null,
    val cartMessage: String? = null,
    val quantityText: String = "1",
    val isPullRefreshing: Boolean = false,
)

class ProductDetailViewModel(
    private val container: AppContainer,
    private val productId: String,
) : ViewModel() {

    private val _ui = MutableStateFlow(ProductDetailUiState())
    val ui: StateFlow<ProductDetailUiState> = _ui.asStateFlow()

    init {
        viewModelScope.launch { loadInternal() }
    }

    private suspend fun loadInternal() {
        _ui.update { it.copy(error = null) }
        runCatching {
            val p = container.catalogRepository.product(productId, skipCache = true)
            val r = container.api.reviews(productId, 1, 20)
            p to r
        }.onSuccess { (p, r) ->
            _ui.update {
                it.copy(
                    product = p,
                    reviews = r,
                    isPullRefreshing = false,
                )
            }
        }.onFailure { e ->
            _ui.update {
                it.copy(
                    error = e.message,
                    isPullRefreshing = false,
                )
            }
        }
    }

    fun pullRefresh() {
        viewModelScope.launch {
            _ui.update { it.copy(isPullRefreshing = true) }
            container.catalogRepository.invalidateAll()
            loadInternal()
        }
    }

    fun retryAfterError() {
        viewModelScope.launch { loadInternal() }
    }

    fun setQuantity(text: String) {
        _ui.update { it.copy(quantityText = text) }
    }

    fun addToCart(onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            val p = _ui.value.product ?: return@launch
            val q = _ui.value.quantityText.toIntOrNull() ?: 1
            _ui.update { it.copy(cartMessage = null) }
            runCatching {
                container.api.addCartItem(com.pnzgu.electronix.data.dto.AddToCartRequest(p.id, q))
            }.onSuccess { cart ->
                container.updateCartBadgeFromSnapshot(cart)
                onSuccess()
            }.onFailure { e ->
                _ui.update { it.copy(cartMessage = e.message) }
            }
        }
    }

    companion object {
        fun factory(container: AppContainer, productId: String): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    ProductDetailViewModel(container, productId) as T
            }
    }
}
