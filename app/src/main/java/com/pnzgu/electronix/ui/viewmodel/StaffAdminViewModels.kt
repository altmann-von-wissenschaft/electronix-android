package com.pnzgu.electronix.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.pnzgu.electronix.AppContainer
import com.pnzgu.electronix.data.dto.AssignCharacteristicRequest
import com.pnzgu.electronix.data.dto.CategoryDto
import com.pnzgu.electronix.data.dto.CategoryCharacteristicDto
import com.pnzgu.electronix.data.dto.CharacteristicDto
import com.pnzgu.electronix.data.dto.CreateCategoryDto
import com.pnzgu.electronix.data.dto.CreateCharacteristicRequest
import com.pnzgu.electronix.data.dto.CreateProductRequest
import com.pnzgu.electronix.data.dto.OrderDto
import com.pnzgu.electronix.data.dto.PagedCharacteristicsResponse
import com.pnzgu.electronix.data.dto.PagedQuestionsResponse
import com.pnzgu.electronix.data.dto.PagedReviewsResponse
import com.pnzgu.electronix.data.dto.ProductDto
import com.pnzgu.electronix.data.dto.SalesReportDto
import com.pnzgu.electronix.data.dto.UpdateCategoryRequest
import com.pnzgu.electronix.data.dto.UpdateOrderStatusRequest
import com.pnzgu.electronix.data.dto.UpdateProductRequest
import com.pnzgu.electronix.data.dto.UserDto
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.serializer
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.HttpException
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.UUID
import com.pnzgu.electronix.util.millisToUtcDateString

val AdminOrderStatuses = listOf("Pending", "Processing", "ReadyForPickup", "Completed", "Cancelled")

class SupportUnansweredViewModel(
    private val container: AppContainer,
) : ViewModel() {
    private val _data = MutableStateFlow<PagedQuestionsResponse?>(null)
    val data: StateFlow<PagedQuestionsResponse?> = _data.asStateFlow()

    init {
        viewModelScope.launch { load() }
        container.supportListsRefresh
            .onEach { load() }
            .launchIn(viewModelScope)
    }

    private suspend fun load() {
        runCatching { container.api.unansweredQuestions(1, 50) }.onSuccess { _data.value = it }
    }

    companion object {
        fun factory(container: AppContainer): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    SupportUnansweredViewModel(container) as T
            }
    }
}

class ModPendingReviewsViewModel(
    private val container: AppContainer,
) : ViewModel() {
    private val _data = MutableStateFlow<PagedReviewsResponse?>(null)
    val data: StateFlow<PagedReviewsResponse?> = _data.asStateFlow()

    fun reload() {
        viewModelScope.launch {
            runCatching { container.api.reviewsPending(1, 50) }.onSuccess { _data.value = it }
        }
    }

    init {
        reload()
    }

    fun approve(id: String) {
        viewModelScope.launch {
            runCatching { container.api.approveReview(id) }
                .onSuccess {
                    container.requestDrawerBadgesRefresh()
                    reload()
                }
        }
    }

    fun delete(id: String) {
        viewModelScope.launch {
            runCatching { container.api.deleteReview(id) }
                .onSuccess {
                    container.requestDrawerBadgesRefresh()
                    reload()
                }
        }
    }

    companion object {
        fun factory(container: AppContainer): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    ModPendingReviewsViewModel(container) as T
            }
    }
}

class AdminOrdersListViewModel(
    private val container: AppContainer,
) : ViewModel() {
    private val _list = MutableStateFlow<List<OrderDto>?>(null)
    val list: StateFlow<List<OrderDto>?> = _list.asStateFlow()

    init {
        reload()
    }

    fun reload() {
        viewModelScope.launch {
            runCatching { container.api.adminOrders(null) }.onSuccess { _list.value = it }
        }
    }

    companion object {
        fun factory(container: AppContainer): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    AdminOrdersListViewModel(container) as T
            }
    }
}

data class AdminOrderStatusUiState(
    val order: OrderDto? = null,
    val statusPick: String = "",
    val notes: String = "",
    val saveError: String? = null,
)

class AdminOrderStatusViewModel(
    private val container: AppContainer,
    private val orderId: String,
) : ViewModel() {
    private val _ui = MutableStateFlow(AdminOrderStatusUiState())
    val ui: StateFlow<AdminOrderStatusUiState> = _ui.asStateFlow()

    companion object {
        private val statusPipeline = listOf("Pending", "Processing", "ReadyForPickup", "Completed")

        fun allowedNextStatuses(currentStatus: String): List<String> {
            if (currentStatus.equals("Cancelled", ignoreCase = true)) return emptyList()
            val idx = statusPipeline.indexOfFirst { it.equals(currentStatus, ignoreCase = true) }
            val out = mutableListOf<String>()
            if (idx in 0 until statusPipeline.lastIndex) {
                out.add(statusPipeline[idx + 1])
            }
            out.add("Cancelled")
            return out
        }

        fun factory(container: AppContainer, orderId: String): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    AdminOrderStatusViewModel(container, orderId) as T
            }
    }

    init {
        reload()
    }

    fun reload() {
        viewModelScope.launch {
            runCatching { container.api.order(orderId) }.onSuccess { o ->
                val allowed = allowedNextStatuses(o.status)
                _ui.value = AdminOrderStatusUiState(
                    order = o,
                    statusPick = allowed.firstOrNull().orEmpty(),
                    notes = "",
                    saveError = null,
                )
            }
        }
    }

    fun setStatusPick(s: String) {
        _ui.update { it.copy(statusPick = s, saveError = null) }
    }

    fun setNotes(n: String) {
        _ui.update { it.copy(notes = n.take(500), saveError = null) }
    }

    fun save() {
        viewModelScope.launch {
            val s = _ui.value
            if (s.statusPick.isBlank()) {
                _ui.update { it.copy(saveError = "__pick__") }
                return@launch
            }
            if (s.statusPick.equals("Cancelled", ignoreCase = true) && s.notes.trim().length <= 20) {
                _ui.update { it.copy(saveError = "__cancel_note__") }
                return@launch
            }
            runCatching {
                container.api.updateOrderStatus(
                    orderId,
                    UpdateOrderStatusRequest(s.statusPick, s.notes.ifBlank { null }),
                )
            }.onSuccess {
                container.requestDrawerBadgesRefresh()
                reload()
            }.onFailure { e ->
                val msg = e.message?.takeIf { it.isNotBlank() } ?: "Не удалось обновить статус"
                _ui.update { it.copy(saveError = msg) }
            }
        }
    }
}

data class AdminUsersSearchUiState(
    val query: String = "",
    val busy: Boolean = false,
    val error: String? = null,
)

class AdminUsersListViewModel(
    private val container: AppContainer,
) : ViewModel() {
    private val _ui = MutableStateFlow(AdminUsersSearchUiState())
    val ui: StateFlow<AdminUsersSearchUiState> = _ui.asStateFlow()

    fun setQuery(q: String) {
        _ui.update { it.copy(query = q.take(64), error = null) }
    }

    fun find(onFound: (String) -> Unit) {
        val raw = _ui.value.query.trim()
        if (raw.isEmpty()) {
            _ui.update { it.copy(error = "__empty__") }
            return
        }
        if (runCatching { UUID.fromString(raw) }.isFailure) {
            _ui.update { it.copy(error = "__invalid__") }
            return
        }
        viewModelScope.launch {
            _ui.update { it.copy(busy = true, error = null) }
            runCatching { container.api.adminUser(raw) }
                .onSuccess { u ->
                    _ui.update { it.copy(busy = false) }
                    onFound(u.id)
                }
                .onFailure { e ->
                    val msg = when {
                        e is HttpException && e.code() == 404 -> "__not_found__"
                        e is HttpException -> e.message()?.takeIf { it.isNotBlank() }
                            ?: "HTTP ${e.code()}"
                        else -> e.message?.takeIf { it.isNotBlank() } ?: "Error"
                    }
                    _ui.update { it.copy(busy = false, error = msg) }
                }
        }
    }

    companion object {
        fun factory(container: AppContainer): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    AdminUsersListViewModel(container) as T
            }
    }
}

data class AdminUserDetailUiState(
    val user: UserDto? = null,
    val roleCode: String = "CLIENT",
)

class AdminUserDetailViewModel(
    private val container: AppContainer,
    private val userId: String,
) : ViewModel() {
    private val _ui = MutableStateFlow(AdminUserDetailUiState())
    val ui: StateFlow<AdminUserDetailUiState> = _ui.asStateFlow()

    init {
        reload()
    }

    fun reload() {
        viewModelScope.launch {
            runCatching { container.api.adminUser(userId) }.onSuccess { u ->
                _ui.update { it.copy(user = u) }
            }
        }
    }

    fun setRoleCode(code: String) {
        _ui.update { it.copy(roleCode = code.take(32)) }
    }

    fun toggleBlock() {
        viewModelScope.launch {
            val u = _ui.value.user ?: return@launch
            runCatching { container.api.adminBlockUser(u.id, !u.isBlocked) }.onSuccess { reload() }
        }
    }

    fun assignRole() {
        viewModelScope.launch {
            val u = _ui.value.user ?: return@launch
            val code = _ui.value.roleCode
            val body = container.json.encodeToString(String.serializer(), code)
                .toRequestBody("application/json".toMediaType())
            runCatching { container.api.adminAssignRole(u.id, body) }.onSuccess { reload() }
        }
    }

    fun removeRole(role: String) {
        viewModelScope.launch {
            val u = _ui.value.user ?: return@launch
            runCatching { container.api.adminRemoveRole(u.id, role) }.onSuccess { reload() }
        }
    }

    companion object {
        fun factory(container: AppContainer, userId: String): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    AdminUserDetailViewModel(container, userId) as T
            }
    }
}

data class AdminCategoryBrowseUiState(
    val categories: List<CategoryDto>? = null,
    val error: String? = null,
    val title: String? = null,
    /** Target parent when creating a category from this folder (null = API root). */
    val createParentId: String? = null,
)

class AdminCategoryBrowseViewModel(
    private val container: AppContainer,
    private val parentKey: String,
) : ViewModel() {

    private val _ui = MutableStateFlow(AdminCategoryBrowseUiState())
    val ui: StateFlow<AdminCategoryBrowseUiState> = _ui.asStateFlow()

    init {
        viewModelScope.launch { load() }
    }

    fun reload() {
        viewModelScope.launch { load() }
    }

    private suspend fun load() {
        _ui.value = AdminCategoryBrowseUiState(categories = null, error = null)
        runCatching {
            if (parentKey == "ROOT") {
                val top = container.api.categories(null)
                when {
                    top.isEmpty() -> BrowseLoadResult(emptyList(), null, null)
                    top.size == 1 -> BrowseLoadResult(
                        list = container.api.categories(top[0].id),
                        createParentId = top[0].id,
                        title = null,
                    )
                    else -> BrowseLoadResult(list = top, createParentId = null, title = null)
                }
            } else {
                BrowseLoadResult(
                    list = container.api.categories(parentKey),
                    createParentId = parentKey,
                    title = container.api.category(parentKey).name,
                )
            }
        }.onSuccess { r ->
            _ui.value = AdminCategoryBrowseUiState(
                categories = r.list,
                error = null,
                title = r.title,
                createParentId = r.createParentId,
            )
        }.onFailure { e ->
            _ui.value = AdminCategoryBrowseUiState(
                error = e.message ?: e.toString(),
            )
        }
    }

    private data class BrowseLoadResult(
        val list: List<CategoryDto>,
        val createParentId: String?,
        val title: String?,
    )

    companion object {
        fun factory(container: AppContainer, parentKey: String): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    AdminCategoryBrowseViewModel(container, parentKey) as T
            }
    }
}

data class CategoryCharAssignmentRow(
    val selectedCharacteristicId: String? = null,
    val isRequired: Boolean = true,
)

data class AdminCategoryEditUiState(
    val name: String = "",
    val displayOrder: String = "0",
    val characteristicCatalog: List<CharacteristicDto> = emptyList(),
    val assignedRows: List<CategoryCharAssignmentRow> = emptyList(),
)

class AdminCategoryEditViewModel(
    private val container: AppContainer,
    private val mode: CategoryEditMode,
) : ViewModel() {
    sealed class CategoryEditMode {
        data class Create(val parentId: String?) : CategoryEditMode()
        data class Edit(val categoryId: String) : CategoryEditMode()
    }

    val isNew: Boolean get() = mode is CategoryEditMode.Create

    private val _ui = MutableStateFlow(AdminCategoryEditUiState())
    val ui: StateFlow<AdminCategoryEditUiState> = _ui.asStateFlow()

    init {
        viewModelScope.launch {
            runCatching { container.api.characteristics(1, 500) }.onSuccess { page ->
                _ui.update { it.copy(characteristicCatalog = page.data) }
            }
            val editId = (mode as? CategoryEditMode.Edit)?.categoryId
            if (editId != null) {
                runCatching { container.api.category(editId) }.onSuccess { c ->
                    _ui.update { s ->
                        s.copy(
                            name = c.name,
                            displayOrder = c.displayOrder.toString(),
                            assignedRows = c.characteristics.map { cc ->
                                CategoryCharAssignmentRow(cc.characteristicId, cc.isRequired)
                            },
                        )
                    }
                }
            }
        }
    }

    fun setName(v: String) {
        _ui.update { it.copy(name = v.take(120)) }
    }

    fun setDisplayOrder(v: String) {
        _ui.update { it.copy(displayOrder = v.take(8)) }
    }

    fun addCharacteristicRow() {
        _ui.update { it.copy(assignedRows = it.assignedRows + CategoryCharAssignmentRow()) }
    }

    fun removeCharacteristicRow(index: Int) {
        _ui.update { s ->
            s.copy(assignedRows = s.assignedRows.filterIndexed { i, _ -> i != index })
        }
    }

    fun setRowCharacteristic(index: Int, characteristicId: String?) {
        _ui.update { s ->
            val rows = s.assignedRows.toMutableList()
            if (index in rows.indices) {
                rows[index] = rows[index].copy(selectedCharacteristicId = characteristicId)
            }
            s.copy(assignedRows = rows)
        }
    }

    fun setRowRequired(index: Int, required: Boolean) {
        _ui.update { s ->
            val rows = s.assignedRows.toMutableList()
            if (index in rows.indices) {
                rows[index] = rows[index].copy(isRequired = required)
            }
            s.copy(assignedRows = rows)
        }
    }

    private fun buildAssignments(s: AdminCategoryEditUiState): List<AssignCharacteristicRequest>? {
        val ids = s.assignedRows.mapNotNull { it.selectedCharacteristicId }
        if (ids.isEmpty()) return emptyList()
        if (ids.toSet().size != ids.size) return null
        return s.assignedRows.mapNotNull { row ->
            row.selectedCharacteristicId?.let { AssignCharacteristicRequest(it, row.isRequired) }
        }
    }

    fun create(onSuccess: () -> Unit) {
        val createMode = mode as? CategoryEditMode.Create ?: return
        viewModelScope.launch {
            val s = _ui.value
            val assignments = buildAssignments(s) ?: return@launch
            runCatching {
                container.api.createCategory(
                    CreateCategoryDto(
                        name = s.name,
                        parentId = createMode.parentId,
                        displayOrder = s.displayOrder.toIntOrNull() ?: 0,
                        characteristics = assignments.takeIf { it.isNotEmpty() },
                    ),
                )
            }.onSuccess {
                container.catalogRepository.invalidateAll()
                onSuccess()
            }
        }
    }

    fun save(onSuccess: () -> Unit) {
        val categoryId = (mode as? CategoryEditMode.Edit)?.categoryId ?: return
        viewModelScope.launch {
            val s = _ui.value
            val assignments = buildAssignments(s) ?: return@launch
            runCatching {
                container.api.updateCategory(
                    categoryId,
                    UpdateCategoryRequest(
                        name = s.name,
                        displayOrder = s.displayOrder.toIntOrNull(),
                        characteristics = assignments,
                    ),
                )
            }.onSuccess {
                container.catalogRepository.invalidateAll()
                onSuccess()
            }
        }
    }

    companion object {
        const val CREATE_PARENT_NONE = "none"

        fun factory(container: AppContainer, mode: CategoryEditMode): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    AdminCategoryEditViewModel(container, mode) as T
            }
    }
}

data class AdminProductsListUiState(
    val searchQuery: String = "",
    val items: List<ProductDto> = emptyList(),
    val page: Int = 1,
    val hasMore: Boolean = false,
    val loading: Boolean = true,
    val loadingMore: Boolean = false,
    val error: String? = null,
)

class AdminProductsListViewModel(
    private val container: AppContainer,
) : ViewModel() {
    private val _ui = MutableStateFlow(AdminProductsListUiState())
    val ui: StateFlow<AdminProductsListUiState> = _ui.asStateFlow()

    private val searchInput = MutableStateFlow("")
    private var searchJob: Job? = null

    init {
        loadFirstPage("")
        viewModelScope.launch {
            searchInput.drop(1).debounce(300).distinctUntilChanged().collect { term ->
                loadFirstPage(term)
            }
        }
    }

    fun onSearchQueryChange(query: String) {
        val limited = query.take(120)
        _ui.update { it.copy(searchQuery = limited) }
        searchInput.value = limited
    }

    fun refresh() {
        loadFirstPage(_ui.value.searchQuery)
    }

    fun retryAfterError() {
        loadFirstPage(_ui.value.searchQuery)
    }

    fun loadMore() {
        viewModelScope.launch {
            val s = _ui.value
            if (s.loading || s.loadingMore || !s.hasMore) return@launch
            _ui.update { it.copy(loadingMore = true) }
            val term = s.searchQuery
            runCatching {
                container.api.products(
                    categoryId = null,
                    page = s.page + 1,
                    pageSize = PAGE_SIZE,
                    search = term.ifBlank { null },
                    filters = emptyMap(),
                )
            }.onSuccess { resp ->
                _ui.update {
                    it.copy(
                        items = it.items + resp.data,
                        page = it.page + 1,
                        hasMore = resp.data.size >= PAGE_SIZE,
                        loadingMore = false,
                        error = null,
                    )
                }
            }.onFailure { e ->
                _ui.update {
                    it.copy(
                        loadingMore = false,
                        error = e.message?.takeIf { m -> m.isNotBlank() } ?: e.toString(),
                    )
                }
            }
        }
    }

    private fun loadFirstPage(term: String) {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            _ui.update { it.copy(loading = true, error = null) }
            runCatching {
                container.api.products(
                    categoryId = null,
                    page = 1,
                    pageSize = PAGE_SIZE,
                    search = term.ifBlank { null },
                    filters = emptyMap(),
                )
            }.onSuccess { resp ->
                _ui.update {
                    it.copy(
                        items = resp.data,
                        page = 1,
                        hasMore = resp.data.size >= PAGE_SIZE,
                        loading = false,
                        error = null,
                    )
                }
            }.onFailure { e ->
                _ui.update {
                    it.copy(
                        loading = false,
                        error = e.message?.takeIf { m -> m.isNotBlank() } ?: e.toString(),
                    )
                }
            }
        }
    }

    companion object {
        private const val PAGE_SIZE = 24

        fun factory(container: AppContainer): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    AdminProductsListViewModel(container) as T
            }
    }
}

data class AdminProductEditUiState(
    val name: String = "",
    val description: String = "",
    val price: String = "",
    val stock: String = "",
    val categoryId: String = "",
    /** Resolved name for read-only display when editing. */
    val categoryDisplayName: String = "",
    val categoryCharacteristics: List<CategoryCharacteristicDto> = emptyList(),
    val characteristicValues: Map<String, String> = emptyMap(),
    val loaded: ProductDto? = null,
    val uploadBusy: Boolean = false,
    val uploadError: String? = null,
    val formError: String? = null,
)

class AdminProductEditViewModel(
    private val container: AppContainer,
    private val productKey: String,
) : ViewModel() {
    val isNew: Boolean get() = productKey == "new"

    private val _ui = MutableStateFlow(AdminProductEditUiState())
    val ui: StateFlow<AdminProductEditUiState> = _ui.asStateFlow()

    init {
        if (!isNew) reloadProduct()
    }

    fun setName(v: String) {
        _ui.update { it.copy(name = v.take(120)) }
    }
    fun setDescription(v: String) {
        _ui.update { it.copy(description = v.take(2000)) }
    }
    fun setPrice(v: String) {
        _ui.update { it.copy(price = v.take(20)) }
    }
    fun setStock(v: String) {
        _ui.update { it.copy(stock = v.take(10)) }
    }
    fun setCategoryId(v: String) {
        val id = v.take(64)
        _ui.update { it.copy(categoryId = id, formError = null) }
        if (id.isNotBlank()) {
            loadCategoryCharacteristics(id)
        }
    }

    fun setCharacteristicValue(characteristicId: String, value: String) {
        _ui.update { s ->
            s.copy(
                characteristicValues = s.characteristicValues + (characteristicId to value.take(40)),
                formError = null,
            )
        }
    }

    private fun loadCategoryCharacteristics(categoryId: String) {
        viewModelScope.launch {
            runCatching { container.api.category(categoryId) }.onSuccess { c ->
                val existing = _ui.value.characteristicValues
                val mapped = c.characteristics.associate { ch ->
                    ch.characteristicId to (existing[ch.characteristicId] ?: "")
                }
                _ui.update {
                    it.copy(
                        categoryCharacteristics = c.characteristics,
                        categoryDisplayName = c.name,
                        characteristicValues = mapped,
                    )
                }
            }
        }
    }

    private fun buildCharacteristicValuesOrError(state: AdminProductEditUiState): Map<String, String>? {
        val result = mutableMapOf<String, String>()
        for (ch in state.categoryCharacteristics) {
            val raw = state.characteristicValues[ch.characteristicId]?.trim().orEmpty()
            if (raw.isBlank()) {
                if (ch.isRequired) {
                    _ui.update { it.copy(formError = "Заполните обязательную характеристику: ${ch.characteristicName}") }
                    return null
                }
                continue
            }
            val parsed = raw.toDoubleOrNull()
            if (parsed == null) {
                _ui.update { it.copy(formError = "Некорректное числовое значение: ${ch.characteristicName}") }
                return null
            }
            result[ch.characteristicId] = parsed.toString()
        }
        return result
    }

    fun create(onCreated: (String) -> Unit) {
        viewModelScope.launch {
            val s = _ui.value
            val characteristicValues = buildCharacteristicValuesOrError(s) ?: return@launch
            runCatching {
                container.api.createProduct(
                    CreateProductRequest(
                        name = s.name,
                        description = s.description.ifBlank { null },
                        price = s.price.toDoubleOrNull() ?: 0.0,
                        stock = s.stock.toIntOrNull() ?: 0,
                        categoryId = s.categoryId,
                        characteristicValues = characteristicValues,
                    ),
                )
            }.onSuccess { created ->
                container.catalogRepository.invalidateAll()
                onCreated(created.id)
            }
        }
    }

    fun reloadProduct() {
        if (isNew) return
        viewModelScope.launch {
            runCatching { container.api.product(productKey) }.onSuccess { p ->
                val cat = runCatching { container.api.category(p.categoryId) }.getOrNull()
                val catLabel = cat?.name ?: "—"
                val initialValues = mutableMapOf<String, String>()
                p.characteristics.forEach { cv ->
                    initialValues[cv.characteristicId] = cv.value.toString()
                }
                cat?.characteristics?.forEach { ch ->
                    if (!initialValues.containsKey(ch.characteristicId)) {
                        initialValues[ch.characteristicId] = ""
                    }
                }
                _ui.update {
                    it.copy(
                        name = p.name,
                        description = p.description ?: "",
                        price = p.price.toString(),
                        stock = p.stock.toString(),
                        categoryId = p.categoryId,
                        categoryDisplayName = catLabel,
                        categoryCharacteristics = cat?.characteristics ?: emptyList(),
                        characteristicValues = initialValues,
                        loaded = p,
                        formError = null,
                    )
                }
            }
        }
    }

    fun uploadPhotos(partials: List<Pair<ByteArray, String>>) {
        if (isNew || partials.isEmpty()) return
        viewModelScope.launch {
            _ui.update { it.copy(uploadBusy = true, uploadError = null) }
            var lastErr: String? = null
            withContext(Dispatchers.IO) {
                for ((bytes, fname) in partials) {
                    val media = "image/jpeg".toMediaTypeOrNull()
                    val body = bytes.toRequestBody(media)
                    val part = MultipartBody.Part.createFormData("file", fname, body)
                    runCatching { container.api.uploadProductImage(productKey, part) }
                        .onFailure { e ->
                            lastErr = e.message?.takeIf { it.isNotBlank() } ?: "Upload failed"
                        }
                }
            }
            container.catalogRepository.invalidateAll()
            runCatching { container.api.product(productKey) }.onSuccess { p ->
                val catLabel = runCatching { container.api.category(p.categoryId).name }.getOrElse { "—" }
                _ui.update {
                    it.copy(
                        name = p.name,
                        description = p.description ?: "",
                        price = p.price.toString(),
                        stock = p.stock.toString(),
                        categoryId = p.categoryId,
                        categoryDisplayName = catLabel,
                        loaded = p,
                        uploadBusy = false,
                        uploadError = lastErr,
                    )
                }
            }.onFailure { e ->
                _ui.update { s -> s.copy(uploadBusy = false, uploadError = lastErr ?: e.message) }
            }
        }
    }

    fun save(onSuccess: () -> Unit) {
        viewModelScope.launch {
            val s = _ui.value
            val base = s.loaded
            val characteristicValues = buildCharacteristicValuesOrError(s) ?: return@launch
            runCatching {
                container.api.updateProduct(
                    productKey,
                    UpdateProductRequest(
                        name = s.name.takeIf { it != base?.name },
                        description = s.description.takeIf { it != (base?.description ?: "") },
                        price = s.price.toDoubleOrNull()?.takeIf { it != base?.price },
                        stock = s.stock.toIntOrNull()?.takeIf { it != base?.stock },
                        categoryId = s.categoryId.takeIf { it != base?.categoryId },
                        characteristicValues = characteristicValues,
                    ),
                )
            }.onSuccess {
                container.catalogRepository.invalidateAll()
                onSuccess()
            }
        }
    }

    companion object {
        fun factory(container: AppContainer, productKey: String): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    AdminProductEditViewModel(container, productKey) as T
            }
    }
}

class AdminCharacteristicsListViewModel(
    private val container: AppContainer,
) : ViewModel() {
    private val _page = MutableStateFlow<PagedCharacteristicsResponse?>(null)
    val page: StateFlow<PagedCharacteristicsResponse?> = _page.asStateFlow()

    init {
        viewModelScope.launch {
            runCatching { container.api.characteristics(1, 100) }.onSuccess { _page.value = it }
        }
    }

    companion object {
        fun factory(container: AppContainer): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    AdminCharacteristicsListViewModel(container) as T
            }
    }
}

data class AdminCharacteristicEditUiState(
    val name: String = "",
    val unit: String = "",
    val loaded: CharacteristicDto? = null,
)

class AdminCharacteristicEditViewModel(
    private val container: AppContainer,
    private val charId: String,
) : ViewModel() {
    val isNew: Boolean get() = charId == "new"

    private val _ui = MutableStateFlow(AdminCharacteristicEditUiState())
    val ui: StateFlow<AdminCharacteristicEditUiState> = _ui.asStateFlow()

    init {
        if (!isNew) {
            viewModelScope.launch {
                runCatching { container.api.characteristic(charId) }.onSuccess { c ->
                    _ui.value = AdminCharacteristicEditUiState(name = c.name, unit = c.unit, loaded = c)
                }
            }
        }
    }

    fun setName(v: String) {
        _ui.update { it.copy(name = v.take(120)) }
    }
    fun setUnit(v: String) {
        _ui.update { it.copy(unit = v.take(20)) }
    }

    fun create(onSuccess: () -> Unit) {
        viewModelScope.launch {
            val s = _ui.value
            val ok = runCatching { container.api.createCharacteristic(CreateCharacteristicRequest(s.name, s.unit)) }
            if (ok.isSuccess) {
                container.catalogRepository.invalidateAll()
                onSuccess()
            }
        }
    }

    fun save(onSuccess: () -> Unit) {
        viewModelScope.launch {
            val s = _ui.value
            val base = s.loaded
            val ok = runCatching {
                container.api.updateCharacteristic(
                    charId,
                    CreateCharacteristicRequest(
                        name = s.name.ifBlank { base?.name ?: "" },
                        unit = s.unit.ifBlank { base?.unit ?: "" },
                    ),
                )
            }
            if (ok.isSuccess) {
                container.catalogRepository.invalidateAll()
                onSuccess()
            }
        }
    }

    companion object {
        fun factory(container: AppContainer, charId: String): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    AdminCharacteristicEditViewModel(container, charId) as T
            }
    }
}

data class SalesReportUiState(
    val report: SalesReportDto? = null,
    val loading: Boolean = false,
    val loadError: String? = null,
    val periodError: String? = null,
    /** UTC epoch millis at start of selected calendar day */
    val selectionStartMillis: Long,
    val selectionEndMillis: Long,
)

private fun defaultSalesRangeMillis(): Pair<Long, Long> {
    val zone = ZoneOffset.UTC
    val today = LocalDate.now(zone)
    val start = today.minusDays(30)
    val startMillis = start.atStartOfDay(zone).toInstant().toEpochMilli()
    val endMillis = today.atStartOfDay(zone).toInstant().toEpochMilli()
    return startMillis to endMillis
}

class SalesReportViewModel(
    private val container: AppContainer,
) : ViewModel() {
    private val _ui = MutableStateFlow(
        run {
            val (s, e) = defaultSalesRangeMillis()
            SalesReportUiState(selectionStartMillis = s, selectionEndMillis = e)
        },
    )
    val ui: StateFlow<SalesReportUiState> = _ui.asStateFlow()

    init {
        reload()
    }

    fun setSelectionStartMillis(v: Long) {
        _ui.update { it.copy(selectionStartMillis = v, periodError = null) }
    }

    fun setSelectionEndMillis(v: Long) {
        _ui.update { it.copy(selectionEndMillis = v, periodError = null) }
    }

    fun reload() {
        viewModelScope.launch {
            val st = _ui.value
            if (st.selectionEndMillis < st.selectionStartMillis) {
                _ui.update { it.copy(periodError = "__period_invalid__") }
                return@launch
            }
            _ui.update { it.copy(loading = true, loadError = null, periodError = null) }
            val startStr = millisToUtcDateString(st.selectionStartMillis)
            val endStr = millisToUtcDateString(st.selectionEndMillis)
            runCatching {
                container.api.salesReport(startDate = startStr, endDate = endStr)
            }.onSuccess { r ->
                _ui.update { it.copy(report = r, loading = false) }
            }.onFailure { e ->
                val msg = e.message?.takeIf { it.isNotBlank() } ?: "Error"
                _ui.update { it.copy(loading = false, loadError = msg) }
            }
        }
    }

    companion object {
        fun factory(container: AppContainer): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    SalesReportViewModel(container) as T
            }
    }
}
