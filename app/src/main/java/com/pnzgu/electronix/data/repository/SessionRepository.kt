package com.pnzgu.electronix.data.repository

import com.pnzgu.electronix.data.api.AuthTokenHolder
import com.pnzgu.electronix.data.api.ElectronixApi
import com.pnzgu.electronix.data.dto.LoginRequest
import com.pnzgu.electronix.data.dto.RegisterRequest
import com.pnzgu.electronix.data.dto.UserDto
import com.pnzgu.electronix.data.local.AppPreferences
import com.pnzgu.electronix.domain.isManager
import com.pnzgu.electronix.domain.isModerator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first

class SessionRepository(
    private val api: ElectronixApi,
    private val preferences: AppPreferences,
    private val tokenHolder: AuthTokenHolder,
) {
    private val _user = MutableStateFlow<UserDto?>(null)
    val user: StateFlow<UserDto?> = _user.asStateFlow()

    suspend fun hydrate() {
        val token = preferences.accessTokenFlow.first()
        tokenHolder.token = token
        if (token.isNullOrBlank()) {
            _user.value = null
            return
        }
        // Refresh on startup to extend active session and rotate JWT while current token is still valid.
        runCatching { api.refresh() }
            .onSuccess { refreshed ->
                tokenHolder.token = refreshed.token
                preferences.setAccessToken(refreshed.token)
            }
            .onFailure {
                tokenHolder.token = null
                preferences.setAccessToken(null)
                _user.value = null
                return
            }
        runCatching { api.me() }
            .onSuccess {
                _user.value = it
                preferences.ensureNotificationBaselines()
                preferences.syncNotificationPrefsForRoles(it.isModerator(), it.isManager())
            }
            .onFailure {
                tokenHolder.token = null
                preferences.setAccessToken(null)
                _user.value = null
            }
    }

    suspend fun login(email: String, password: String) {
        val r = api.login(LoginRequest(email, password))
        tokenHolder.token = r.token
        preferences.setAccessToken(r.token)
        val loggedIn = UserDto(
            id = r.userId,
            email = r.email,
            nickname = r.nickname,
            isBlocked = false,
            roles = r.roles,
        )
        _user.value = loggedIn
        preferences.ensureNotificationBaselines()
        preferences.syncNotificationPrefsForRoles(loggedIn.isModerator(), loggedIn.isManager())
    }

    suspend fun register(email: String, password: String, nickname: String?) {
        api.register(RegisterRequest(email, password, nickname))
    }

    suspend fun logout() {
        tokenHolder.token = null
        preferences.setAccessToken(null)
        _user.value = null
    }

    suspend fun refreshMe() {
        if (tokenHolder.token.isNullOrBlank()) return
        runCatching { api.refresh() }
            .onSuccess {
                tokenHolder.token = it.token
                preferences.setAccessToken(it.token)
            }
            .onFailure {
                tokenHolder.token = null
                preferences.setAccessToken(null)
                _user.value = null
                return
            }
        runCatching { api.me() }
            .onSuccess {
                _user.value = it
                preferences.syncNotificationPrefsForRoles(it.isModerator(), it.isManager())
            }
            .onFailure {
                tokenHolder.token = null
                preferences.setAccessToken(null)
                _user.value = null
            }
    }
}
