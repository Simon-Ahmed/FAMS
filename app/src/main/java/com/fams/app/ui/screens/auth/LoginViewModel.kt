package com.fams.app.ui.screens.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.fams.app.data.repository.AuthRepository
import com.fams.app.domain.model.UserRole
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val loginSuccess: UserRole? = null,
    val showResetDialog: Boolean = false,
    val resetEmail: String = "",
    val resetMessage: String? = null,
    val isResetLoading: Boolean = false
)

class LoginViewModel(
    private val authRepository: AuthRepository = AuthRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun onEmailChange(value: String) {
        _uiState.value = _uiState.value.copy(email = value, errorMessage = null)
    }

    fun onPasswordChange(value: String) {
        _uiState.value = _uiState.value.copy(password = value, errorMessage = null)
    }

    fun onLoginClick() {
        val state = _uiState.value
        if (state.email.isBlank() || state.password.isBlank()) {
            _uiState.value = state.copy(errorMessage = "Please enter your email and password.")
            return
        }
        viewModelScope.launch {
            _uiState.value = state.copy(isLoading = true, errorMessage = null)
            val result = authRepository.signIn(state.email.trim(), state.password)
            result.fold(
                onSuccess = { user ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        loginSuccess = user.role
                    )
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = error.message
                    )
                }
            )
        }
    }

    fun sendPasswordResetEmail() {
        val state = _uiState.value
        if (state.resetEmail.isBlank()) {
            _uiState.value = state.copy(resetMessage = "Enter your email address.")
            return
        }

        viewModelScope.launch {
            _uiState.value = state.copy(isResetLoading = true, resetMessage = null)
            authRepository.sendPasswordResetEmail(state.resetEmail.trim()).fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(
                        isResetLoading = false,
                        resetMessage = "Reset link sent. Check your email.",
                        resetEmail = state.resetEmail.trim()
                    )
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isResetLoading = false,
                        resetMessage = error.message
                    )
                }
            )
        }
    }

    fun onForgotPasswordClick() {
        _uiState.value = _uiState.value.copy(
            showResetDialog = true,
            resetEmail = _uiState.value.email,
            resetMessage = null
        )
    }

    fun onResetEmailChange(value: String) {
        _uiState.value = _uiState.value.copy(resetEmail = value, resetMessage = null)
    }

    fun closeResetDialog() {
        _uiState.value = _uiState.value.copy(showResetDialog = false, resetMessage = null)
    }

    fun clearLoginSuccess() {
        _uiState.value = _uiState.value.copy(loginSuccess = null)
    }

    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return LoginViewModel() as T
            }
        }
    }
}
