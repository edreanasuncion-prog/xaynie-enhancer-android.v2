package com.xstudio.app.authentication

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xstudio.app.core.designsystem.XBlack
import com.xstudio.app.core.designsystem.XGlassBorder
import com.xstudio.app.core.designsystem.XStudioLogo
import com.xstudio.app.core.designsystem.XWhite
import com.xstudio.app.core.designsystem.XWhiteMuted
import com.xstudio.app.core.designsystem.liquidGlass
import com.xstudio.app.data.model.UserProfile
import com.xstudio.app.network.SupabaseInitResult
import com.xstudio.app.network.SupabaseModule
import io.github.jan.supabase.auth.OtpType
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

enum class AuthStep { LOGIN, SIGN_UP, VERIFY_OTP, PASSWORD_RECOVERY }

data class AuthUiState(
    val isLoading: Boolean = true,
    val isAuthenticated: Boolean = false,
    val step: AuthStep = AuthStep.LOGIN,
    val pendingEmail: String = "",
    val userProfile: UserProfile? = null,
    val errorMessage: String? = null,
    val infoMessage: String? = null,
    /** Non-null when Supabase could not be initialized. Authentication is never bypassed in this state. */
    val configError: String? = null
)

class AuthViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private var sessionJob: Job? = null

    init {
        initializeAndObserve()
    }

    /** Re-runs initialization (used by the Retry button on the configuration-error card). */
    fun retryInitialization() = initializeAndObserve()

    /**
     * Order matters: Supabase must be initialized (build-time credentials or /api/config)
     * BEFORE the session observer starts and before any auth state is decided.
     * If initialization fails the user is shown a configuration error and stays signed out.
     */
    private fun initializeAndObserve() {
        sessionJob?.cancel()
        _uiState.value = _uiState.value.copy(isLoading = true, isAuthenticated = false, configError = null)
        sessionJob = viewModelScope.launch {
            when (val init = SupabaseModule.initialize()) {
                is SupabaseInitResult.Failed -> {
                    _uiState.value = AuthUiState(
                        isLoading = false,
                        isAuthenticated = false,
                        configError = init.reason
                    )
                    return@launch
                }
                SupabaseInitResult.Ready -> Unit
            }
            SupabaseModule.client.auth.sessionStatus.collect { status ->
                when (status) {
                    is SessionStatus.Authenticated -> {
                        val user = status.session.user
                        val profile = fetchOrBuildProfile(
                            userId = user?.id.orEmpty(),
                            email = user?.email.orEmpty(),
                            createdAt = user?.createdAt?.toString().orEmpty()
                        )
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            isAuthenticated = true,
                            userProfile = profile,
                            errorMessage = null,
                            configError = null
                        )
                    }
                    is SessionStatus.NotAuthenticated -> {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            isAuthenticated = false,
                            userProfile = null
                        )
                    }
                    is SessionStatus.Initializing -> {
                        _uiState.value = _uiState.value.copy(isLoading = true)
                    }
                    else -> {
                        // e.g. a saved session that could not be refreshed yet (offline). Stay signed out
                        // and let the SDK keep retrying; do not leave the UI spinning forever.
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            isAuthenticated = false,
                            infoMessage = "Could not refresh your saved session. Check your connection."
                        )
                    }
                }
            }
        }
    }

    private suspend fun fetchOrBuildProfile(
        userId: String,
        email: String,
        createdAt: String
    ): UserProfile {
        return runCatching {
            SupabaseModule.client.postgrest["profiles"]
                .select {
                    filter { eq("id", userId) }
                }
                .decodeSingle<UserProfile>()
        }.getOrElse {
            UserProfile(
                id = userId,
                username = email.substringBefore("@"),
                email = email,
                role = "free",
                createdAt = createdAt.take(10)
            )
        }
    }

    fun setStep(step: AuthStep) {
        _uiState.value = _uiState.value.copy(step = step, errorMessage = null, infoMessage = null)
    }

    fun signIn(email: String, password: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            runCatching {
                SupabaseModule.client.auth.signInWith(Email) {
                    this.email = email.trim()
                    this.password = password
                }
            }.onFailure { e ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.localizedMessage ?: "Login failed"
                )
            }
        }
    }

    fun signUp(username: String, email: String, password: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            runCatching {
                SupabaseModule.client.auth.signUpWith(Email) {
                    this.email = email.trim()
                    this.password = password
                    data = buildJsonObject {
                        put("username", username.trim())
                    }
                }
            }.onSuccess {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    pendingEmail = email.trim(),
                    step = AuthStep.VERIFY_OTP,
                    infoMessage = "Enter the 6-digit verification code sent to ${email.trim()}"
                )
            }.onFailure { e ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.localizedMessage ?: "Sign up failed"
                )
            }
        }
    }

    fun verifySixDigitOtp(email: String, sixDigitCode: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            runCatching {
                SupabaseModule.client.auth.verifyEmailOtp(
                    type = OtpType.Email.SIGNUP,
                    email = email.trim(),
                    token = sixDigitCode.trim()
                )
            }.onFailure { e ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.localizedMessage ?: "Invalid 6-digit verification code"
                )
            }
        }
    }

    fun sendPasswordRecovery(email: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            runCatching {
                SupabaseModule.client.auth.resetPasswordForEmail(email.trim())
            }.onSuccess {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    infoMessage = "Password recovery instructions sent to ${email.trim()}"
                )
            }.onFailure { e ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.localizedMessage ?: "Failed to send recovery email"
                )
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            runCatching { SupabaseModule.client.auth.signOut() }
            _uiState.value = AuthUiState(isLoading = false, isAuthenticated = false, step = AuthStep.LOGIN)
        }
    }
}

@Composable
fun AuthScreen(viewModel: AuthViewModel) {
    val state by viewModel.uiState.collectAsState()

    val configError = state.configError
    if (configError != null) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(XBlack)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            XStudioLogo(iconSize = 40.dp)
            Spacer(modifier = Modifier.height(28.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .liquidGlass(RoundedCornerShape(24.dp))
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Configuration error",
                    color = XWhite,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(text = configError, color = XWhite, fontSize = 13.sp)
                Text(
                    text = "X Studio cannot sign you in until the backend is configured. " +
                        "This build has no offline or guest mode.",
                    color = XWhiteMuted,
                    fontSize = 12.sp
                )
                Button(
                    onClick = { viewModel.retryInitialization() },
                    enabled = !state.isLoading,
                    colors = ButtonDefaults.buttonColors(containerColor = XWhite, contentColor = XBlack),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    Text("RETRY", fontWeight = FontWeight.Bold)
                }
            }
        }
        return
    }

    var username by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var otpCode by rememberSaveable { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(XBlack)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        XStudioLogo(iconSize = 40.dp)
        Spacer(modifier = Modifier.height(28.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .liquidGlass(RoundedCornerShape(24.dp))
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = when (state.step) {
                    AuthStep.LOGIN -> "Sign In"
                    AuthStep.SIGN_UP -> "Create Account"
                    AuthStep.VERIFY_OTP -> "Verify Email (6-Digit Code)"
                    AuthStep.PASSWORD_RECOVERY -> "Password Recovery"
                },
                color = XWhite,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )

            state.infoMessage?.let {
                Text(text = it, color = XWhiteMuted, fontSize = 12.sp)
            }
            state.errorMessage?.let {
                Text(text = it, color = XWhite, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }

            if (state.step == AuthStep.SIGN_UP) {
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Username") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = XWhite,
                        unfocusedBorderColor = XGlassBorder,
                        focusedTextColor = XWhite,
                        unfocusedTextColor = XWhite
                    )
                )
            }

            if (state.step != AuthStep.VERIFY_OTP) {
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = XWhite,
                        unfocusedBorderColor = XGlassBorder,
                        focusedTextColor = XWhite,
                        unfocusedTextColor = XWhite
                    )
                )
            }

            if (state.step == AuthStep.LOGIN || state.step == AuthStep.SIGN_UP) {
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = XWhite,
                        unfocusedBorderColor = XGlassBorder,
                        focusedTextColor = XWhite,
                        unfocusedTextColor = XWhite
                    )
                )
            }

            if (state.step == AuthStep.VERIFY_OTP) {
                OutlinedTextField(
                    value = otpCode,
                    onValueChange = { if (it.length <= 6 && it.all(Char::isDigit)) otpCode = it },
                    label = { Text("6-Digit Verification Code") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = XWhite,
                        unfocusedBorderColor = XGlassBorder,
                        focusedTextColor = XWhite,
                        unfocusedTextColor = XWhite
                    )
                )
            }

            Button(
                onClick = {
                    when (state.step) {
                        AuthStep.LOGIN -> viewModel.signIn(email, password)
                        AuthStep.SIGN_UP -> viewModel.signUp(username, email, password)
                        AuthStep.VERIFY_OTP -> viewModel.verifySixDigitOtp(
                            state.pendingEmail.ifBlank { email },
                            otpCode
                        )
                        AuthStep.PASSWORD_RECOVERY -> viewModel.sendPasswordRecovery(email)
                    }
                },
                enabled = !state.isLoading,
                colors = ButtonDefaults.buttonColors(containerColor = XWhite, contentColor = XBlack),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(color = XBlack, strokeWidth = 2.dp)
                } else {
                    Text(
                        text = when (state.step) {
                            AuthStep.LOGIN -> "LOGIN"
                            AuthStep.SIGN_UP -> "SIGN UP"
                            AuthStep.VERIFY_OTP -> "VERIFY CODE"
                            AuthStep.PASSWORD_RECOVERY -> "SEND RESET LINK"
                        },
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (state.step == AuthStep.LOGIN) {
                    TextButton(onClick = { viewModel.setStep(AuthStep.SIGN_UP) }) {
                        Text("Create Account", color = XWhite)
                    }
                    TextButton(onClick = { viewModel.setStep(AuthStep.PASSWORD_RECOVERY) }) {
                        Text("Forgot Password?", color = XWhiteMuted)
                    }
                } else {
                    TextButton(onClick = { viewModel.setStep(AuthStep.LOGIN) }) {
                        Text("Back to Login", color = XWhite)
                    }
                }
            }
        }
    }
}
