package com.pnzgu.electronix.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.pnzgu.electronix.AppContainer
import com.pnzgu.electronix.data.dto.ApiErrorDto
import com.pnzgu.electronix.data.dto.CartDto
import com.pnzgu.electronix.data.dto.CartItemDto
import com.pnzgu.electronix.data.dto.ChangePasswordRequest
import com.pnzgu.electronix.data.dto.UpdateCartItemRequest
import com.pnzgu.electronix.data.dto.OrderDto
import com.pnzgu.electronix.data.dto.PagedQuestionsResponse
import com.pnzgu.electronix.data.dto.QuestionDto
import com.pnzgu.electronix.data.dto.CreateQuestionRequest
import com.pnzgu.electronix.data.dto.CreateReviewRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.serialization.SerializationException
import retrofit2.HttpException
import java.util.regex.Pattern

private object InputRules {
    const val EMAIL_MAX = 254
    const val PASSWORD_MIN = 8
    const val PASSWORD_MAX = 72
    const val NICKNAME_MIN = 4
    const val NICKNAME_MAX = 24
    const val SUPPORT_SUBJECT_MIN = 4
    const val SUPPORT_SUBJECT_MAX = 120
    const val SUPPORT_CONTENT_MIN = 10
    const val SUPPORT_CONTENT_MAX = 2000
    const val SUPPORT_REPLY_MIN = 8
    const val SUPPORT_REPLY_MAX = 2000
    const val REVIEW_TITLE_MIN = 4
    const val REVIEW_TITLE_MAX = 120
    const val REVIEW_CONTENT_MIN = 15
    const val REVIEW_CONTENT_MAX = 2000

    val EMAIL_RE: Pattern = Pattern.compile("^[A-Za-z0-9._%+\\-]+@[A-Za-z0-9.\\-]+\\.[A-Za-z]{2,}$")
    val NICK_RE: Pattern = Pattern.compile("^[A-Za-zА-Яа-яЁё][A-Za-zА-Яа-яЁё0-9_]{3,23}$")
}

class LoginViewModel(
    private val container: AppContainer,
) : ViewModel() {
    private val _email = MutableStateFlow("")
    val email: StateFlow<String> = _email.asStateFlow()
    private val _password = MutableStateFlow("")
    val password: StateFlow<String> = _password.asStateFlow()
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun setEmail(v: String) {
        _email.value = v.take(InputRules.EMAIL_MAX)
    }
    fun setPassword(v: String) {
        _password.value = v.take(InputRules.PASSWORD_MAX)
    }

    fun login(onSuccess: () -> Unit) {
        viewModelScope.launch {
            _error.value = null
            val email = _email.value.trim()
            val pass = _password.value
            if (!InputRules.EMAIL_RE.matcher(email).matches()) {
                _error.value = "Введите корректный email."
                return@launch
            }
            if (pass.length !in InputRules.PASSWORD_MIN..InputRules.PASSWORD_MAX) {
                _error.value = "Пароль должен быть от 8 до 72 символов."
                return@launch
            }
            runCatching { container.sessionRepository.login(email, pass) }
                .onSuccess {
                    runCatching { container.pushSync.syncFcmTokenAndPreferences() }
                    onSuccess()
                }
                .onFailure { e ->
                    _password.value = ""
                    _error.value = when (e) {
                        is HttpException -> when (e.code()) {
                            401, 400 -> "Неверный email или пароль."
                            else -> "Ошибка сервера (${e.code()})."
                        }
                        else -> e.message?.takeIf { it.isNotBlank() } ?: "Не удалось войти."
                    }
                }
        }
    }

    companion object {
        fun factory(container: AppContainer): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    LoginViewModel(container) as T
            }
    }
}

class RegisterViewModel(
    private val container: AppContainer,
) : ViewModel() {
    private val _email = MutableStateFlow("")
    val email: StateFlow<String> = _email.asStateFlow()
    private val _password = MutableStateFlow("")
    val password: StateFlow<String> = _password.asStateFlow()
    private val _nickname = MutableStateFlow("")
    val nickname: StateFlow<String> = _nickname.asStateFlow()
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()

    fun setEmail(v: String) {
        _email.value = v.take(InputRules.EMAIL_MAX)
    }
    fun setPassword(v: String) {
        _password.value = v.take(InputRules.PASSWORD_MAX)
    }
    fun setNickname(v: String) {
        _nickname.value = v.take(InputRules.NICKNAME_MAX)
    }

    fun register(onSuccess: () -> Unit) {
        viewModelScope.launch {
            _error.value = null
            val email = _email.value.trim()
            val password = _password.value
            val nickname = _nickname.value.trim()
            if (!InputRules.EMAIL_RE.matcher(email).matches()) {
                _error.value = "Email: корректный формат, например name@example.com."
                return@launch
            }
            val hasUpper = password.any(Char::isUpperCase)
            val hasLower = password.any(Char::isLowerCase)
            val hasDigit = password.any(Char::isDigit)
            if (password.length !in InputRules.PASSWORD_MIN..InputRules.PASSWORD_MAX || !hasUpper || !hasLower || !hasDigit) {
                _error.value = "Пароль: 8-72 символа, минимум одна заглавная, одна строчная буква и цифра."
                return@launch
            }
            if (nickname.isNotBlank() && !InputRules.NICK_RE.matcher(nickname).matches()) {
                _error.value = "Псевдоним: 4-24 символа, начинается с буквы; только кириллица/латиница, цифры и _."
                return@launch
            }
            _busy.value = true
            runCatching {
                container.sessionRepository.register(
                    email,
                    password,
                    nickname.ifBlank { null },
                )
            }.onSuccess {
                _busy.value = false
                onSuccess()
            }.onFailure { e ->
                _busy.value = false
                _error.value = when (e) {
                    is HttpException -> {
                        val apiMsg = e.response()?.errorBody()?.use { it.string() }?.let { raw ->
                            runCatching { container.json.decodeFromString<ApiErrorDto>(raw).message }.getOrNull()
                        }?.takeIf { !it.isNullOrBlank() }
                        when (e.code()) {
                            400 -> apiMsg ?: "Проверьте email и пароль."
                            409 -> apiMsg ?: "Пользователь с таким email уже зарегистрирован."
                            in 500..599 -> apiMsg ?: "Ошибка сервера (${e.code()})."
                            else -> apiMsg ?: "Ошибка сети или сервера (${e.code()})."
                        }
                    }
                    is SerializationException -> "Некорректный ответ сервера. Проверьте адрес API (HTTP/HTTPS и порт)."
                    else -> e.message?.takeIf { it.isNotBlank() }
                        ?: "Не удалось зарегистрироваться. Проверьте подключение к серверу."
                }
            }
        }
    }

    companion object {
        fun factory(container: AppContainer): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    RegisterViewModel(container) as T
            }
    }
}

class ProfileViewModel(
    private val container: AppContainer,
) : ViewModel() {
    private val _changePasswordBusy = MutableStateFlow(false)
    val changePasswordBusy: StateFlow<Boolean> = _changePasswordBusy.asStateFlow()
    private val _changePasswordError = MutableStateFlow<String?>(null)
    val changePasswordError: StateFlow<String?> = _changePasswordError.asStateFlow()
    private val _changePasswordSuccess = MutableStateFlow<String?>(null)
    val changePasswordSuccess: StateFlow<String?> = _changePasswordSuccess.asStateFlow()

    fun logout(onDone: () -> Unit) {
        viewModelScope.launch {
            container.pushSync.unregisterDeviceOnLogout()
            container.sessionRepository.logout()
            container.resetCartBadge()
            onDone()
        }
    }

    fun changePassword(currentPassword: String, newPassword: String, confirmPassword: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _changePasswordError.value = null
            _changePasswordSuccess.value = null
            if (currentPassword.isBlank() || newPassword.isBlank() || confirmPassword.isBlank()) {
                _changePasswordError.value = "Заполните все поля пароля."
                return@launch
            }
            if (newPassword != confirmPassword) {
                _changePasswordError.value = "Новый пароль и подтверждение не совпадают."
                return@launch
            }
            val hasUpper = newPassword.any(Char::isUpperCase)
            val hasLower = newPassword.any(Char::isLowerCase)
            val hasDigit = newPassword.any(Char::isDigit)
            if (newPassword.length !in InputRules.PASSWORD_MIN..InputRules.PASSWORD_MAX || !hasUpper || !hasLower || !hasDigit) {
                _changePasswordError.value = "Новый пароль: 8-72 символа, заглавная, строчная и цифра."
                return@launch
            }
            _changePasswordBusy.value = true
            runCatching {
                container.api.changePassword(
                    ChangePasswordRequest(
                        currentPassword = currentPassword,
                        newPassword = newPassword,
                        confirmNewPassword = confirmPassword,
                    ),
                )
            }.onSuccess {
                _changePasswordBusy.value = false
                _changePasswordSuccess.value = "Пароль успешно изменен."
                onSuccess()
            }.onFailure { e ->
                _changePasswordBusy.value = false
                _changePasswordError.value = when (e) {
                    is HttpException -> {
                        val apiMsg = e.response()?.errorBody()?.use { it.string() }?.let { raw ->
                            runCatching { container.json.decodeFromString<ApiErrorDto>(raw).message }.getOrNull()
                        }
                        apiMsg?.takeIf { !it.isNullOrBlank() } ?: "Не удалось изменить пароль."
                    }
                    else -> e.message?.takeIf { it.isNotBlank() } ?: "Не удалось изменить пароль."
                }
            }
        }
    }

    companion object {
        fun factory(container: AppContainer): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    ProfileViewModel(container) as T
            }
    }
}

data class CartUiState(
    val cart: CartDto? = null,
    val error: String? = null,
)

class CartViewModel(
    private val container: AppContainer,
) : ViewModel() {
    private val _ui = MutableStateFlow(CartUiState())
    val ui: StateFlow<CartUiState> = _ui.asStateFlow()

    init {
        reload()
    }

    private suspend fun loadCart() {
        runCatching { container.api.cart() }
            .onSuccess { cart ->
                _ui.value = CartUiState(cart = cart)
                container.updateCartBadgeFromSnapshot(cart)
            }
            .onFailure { _ui.value = CartUiState(error = it.message) }
    }

    fun reload() {
        viewModelScope.launch { loadCart() }
    }

    fun removeItem(itemId: String) {
        viewModelScope.launch {
            runCatching { container.api.removeCartItem(itemId) }.onSuccess { reload() }
        }
    }

    fun incrementQuantity(item: CartItemDto) {
        viewModelScope.launch {
            runCatching {
                container.api.updateCartItem(item.id, UpdateCartItemRequest(item.quantity + 1))
            }.onSuccess { loadCart() }
        }
    }

    fun decrementQuantity(item: CartItemDto) {
        if (item.quantity <= 1) {
            removeItem(item.id)
            return
        }
        viewModelScope.launch {
            runCatching {
                container.api.updateCartItem(item.id, UpdateCartItemRequest(item.quantity - 1))
            }.onSuccess { loadCart() }
        }
    }

    fun checkout(onOrderId: (String) -> Unit) {
        viewModelScope.launch {
            runCatching { container.api.createOrder() }
                .onSuccess { order ->
                    loadCart()
                    container.requestDrawerBadgesRefresh()
                    onOrderId(order.id)
                }
                .onFailure { e -> _ui.update { it.copy(error = e.message) } }
        }
    }

    companion object {
        fun factory(container: AppContainer): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    CartViewModel(container) as T
            }
    }
}

data class OrdersUiState(
    val orders: List<OrderDto>? = null,
    val error: String? = null,
)

class OrdersViewModel(
    private val container: AppContainer,
) : ViewModel() {
    private val _ui = MutableStateFlow(OrdersUiState())
    val ui: StateFlow<OrdersUiState> = _ui.asStateFlow()

    init {
        reload()
    }

    fun reload() {
        viewModelScope.launch {
            runCatching { container.api.myOrders() }
                .onSuccess { _ui.value = OrdersUiState(orders = it) }
                .onFailure { _ui.value = OrdersUiState(error = it.message) }
        }
    }

    companion object {
        fun factory(container: AppContainer): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    OrdersViewModel(container) as T
            }
    }
}

data class OrderDetailUiState(
    val order: OrderDto? = null,
    val error: String? = null,
)

class OrderDetailViewModel(
    private val container: AppContainer,
    private val orderId: String,
) : ViewModel() {
    private val _ui = MutableStateFlow(OrderDetailUiState())
    val ui: StateFlow<OrderDetailUiState> = _ui.asStateFlow()

    init {
        viewModelScope.launch { load() }
    }

    private suspend fun load() {
        runCatching { container.api.order(orderId) }
            .onSuccess { _ui.value = OrderDetailUiState(order = it) }
            .onFailure { _ui.value = OrderDetailUiState(error = it.message) }
    }

    fun cancelIfPending() {
        viewModelScope.launch {
            val o = _ui.value.order ?: return@launch
            if (!o.status.equals("Pending", true)) return@launch
            runCatching { container.api.cancelOrder(o.id) }
                .onSuccess { ord ->
                    container.requestDrawerBadgesRefresh()
                    _ui.value = OrderDetailUiState(order = ord)
                }
        }
    }

    fun reload() {
        viewModelScope.launch { load() }
    }

    companion object {
        fun factory(container: AppContainer, orderId: String): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    OrderDetailViewModel(container, orderId) as T
            }
    }
}

data class SupportMyUiState(val data: PagedQuestionsResponse? = null)

class SupportMyViewModel(
    private val container: AppContainer,
) : ViewModel() {
    private val _ui = MutableStateFlow(SupportMyUiState())
    val ui: StateFlow<SupportMyUiState> = _ui.asStateFlow()

    init {
        viewModelScope.launch { load() }
        container.supportListsRefresh
            .onEach { load() }
            .launchIn(viewModelScope)
    }

    private suspend fun load() {
        runCatching { container.api.myQuestions(1, 50) }
            .onSuccess { _ui.value = SupportMyUiState(it) }
    }

    companion object {
        fun factory(container: AppContainer): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    SupportMyViewModel(container) as T
            }
    }
}

data class SupportQuestionUiState(
    val question: QuestionDto? = null,
)

class SupportQuestionDetailViewModel(
    private val container: AppContainer,
    private val qId: String,
) : ViewModel() {
    private val _ui = MutableStateFlow(SupportQuestionUiState())
    val ui: StateFlow<SupportQuestionUiState> = _ui.asStateFlow()

    init {
        viewModelScope.launch {
            runCatching { container.api.question(qId) }
                .onSuccess { _ui.value = SupportQuestionUiState(it) }
        }
    }

    companion object {
        fun factory(container: AppContainer, qId: String): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    SupportQuestionDetailViewModel(container, qId) as T
            }
    }
}

class SupportCreateViewModel(
    private val container: AppContainer,
) : ViewModel() {
    private val _subject = MutableStateFlow("")
    val subject: StateFlow<String> = _subject.asStateFlow()
    private val _content = MutableStateFlow("")
    val content: StateFlow<String> = _content.asStateFlow()
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun setSubject(v: String) {
        _subject.value = v.take(InputRules.SUPPORT_SUBJECT_MAX)
    }
    fun setContent(v: String) {
        _content.value = v.take(InputRules.SUPPORT_CONTENT_MAX)
    }

    fun submit(onDone: () -> Unit) {
        viewModelScope.launch {
            _error.value = null
            val s = _subject.value.trim()
            val c = _content.value.trim()
            if (s.length < InputRules.SUPPORT_SUBJECT_MIN) {
                _error.value = "Тема должна быть не короче 4 символов."
                return@launch
            }
            if (c.length < InputRules.SUPPORT_CONTENT_MIN) {
                _error.value = "Текст обращения должен быть не короче 10 символов."
                return@launch
            }
            runCatching {
                container.api.createQuestion(CreateQuestionRequest(s, c))
            }.onSuccess { onDone() }
                .onFailure { e -> _error.value = e.message ?: e.toString() }
        }
    }

    companion object {
        fun factory(container: AppContainer): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    SupportCreateViewModel(container) as T
            }
    }
}

data class SupportAnswerUiState(
    val question: QuestionDto? = null,
    val replyText: String = "",
    val loadError: String? = null,
    val submitError: String? = null,
)

class SupportAnswerViewModel(
    private val container: AppContainer,
    private val questionId: String,
) : ViewModel() {
    private val _ui = MutableStateFlow(SupportAnswerUiState())
    val ui: StateFlow<SupportAnswerUiState> = _ui.asStateFlow()

    init {
        viewModelScope.launch { loadQuestion() }
    }

    private suspend fun loadQuestion() {
        _ui.update { it.copy(loadError = null) }
        runCatching { container.api.question(questionId) }
            .onSuccess { q -> _ui.update { it.copy(question = q, loadError = null) } }
            .onFailure { e -> _ui.update { it.copy(loadError = e.message ?: e.toString()) } }
    }

    fun reloadQuestion() {
        viewModelScope.launch { loadQuestion() }
    }

    fun setReplyText(v: String) {
        _ui.update { it.copy(replyText = v.take(InputRules.SUPPORT_REPLY_MAX)) }
    }

    fun submit(onSuccess: () -> Unit) {
        viewModelScope.launch {
            _ui.update { it.copy(submitError = null) }
            val text = _ui.value.replyText.trim()
            if (text.length < InputRules.SUPPORT_REPLY_MIN) {
                _ui.update { it.copy(submitError = "Ответ должен быть не короче 8 символов.") }
                return@launch
            }
            runCatching {
                container.api.answerQuestion(
                    questionId,
                    com.pnzgu.electronix.data.dto.CreateAnswerRequest(text),
                )
            }.onSuccess {
                container.notifySupportListsChanged()
                container.requestDrawerBadgesRefresh()
                onSuccess()
            }
                .onFailure { e -> _ui.update { it.copy(submitError = e.message ?: e.toString()) } }
        }
    }

    companion object {
        fun factory(container: AppContainer, questionId: String): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    SupportAnswerViewModel(container, questionId) as T
            }
    }
}

class ReviewCreateViewModel(
    private val container: AppContainer,
    private val productId: String,
) : ViewModel() {
    private val _rating = MutableStateFlow(0)
    val rating: StateFlow<Int> = _rating.asStateFlow()
    private val _title = MutableStateFlow("")
    val title: StateFlow<String> = _title.asStateFlow()
    private val _content = MutableStateFlow("")
    val content: StateFlow<String> = _content.asStateFlow()
    private val _submitError = MutableStateFlow<String?>(null)
    val submitError: StateFlow<String?> = _submitError.asStateFlow()

    fun setRating(v: Int) {
        _rating.value = v
    }
    fun setTitle(v: String) {
        _title.value = v.take(InputRules.REVIEW_TITLE_MAX)
    }
    fun setContent(v: String) {
        _content.value = v.take(InputRules.REVIEW_CONTENT_MAX)
    }

    fun submit(onSuccess: () -> Unit) {
        viewModelScope.launch {
            _submitError.value = null
            val r = _rating.value
            if (r !in 1..5) {
                _submitError.value = "Выберите оценку от 1 до 5."
                return@launch
            }
            if (_title.value.trim().length < InputRules.REVIEW_TITLE_MIN) {
                _submitError.value = "Заголовок должен быть не короче 4 символов."
                return@launch
            }
            if (_content.value.trim().length < InputRules.REVIEW_CONTENT_MIN) {
                _submitError.value = "Текст отзыва должен быть не короче 15 символов."
                return@launch
            }
            runCatching {
                container.api.createReview(
                    CreateReviewRequest(
                        productId = productId,
                        rating = r,
                        title = _title.value,
                        content = _content.value,
                    ),
                )
            }.onSuccess { onSuccess() }
                .onFailure { e -> _submitError.value = e.message ?: e.toString() }
        }
    }

    companion object {
        fun factory(container: AppContainer, productId: String): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    ReviewCreateViewModel(container, productId) as T
            }
    }
}

class SessionViewModel(
    private val container: AppContainer,
) : ViewModel() {
    fun hydrate() {
        viewModelScope.launch { container.sessionRepository.hydrate() }
    }

    fun refreshMe() {
        viewModelScope.launch { container.sessionRepository.refreshMe() }
    }

    companion object {
        fun factory(container: AppContainer): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    SessionViewModel(container) as T
            }
    }
}
