package co.booknook.feature.auth

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

private val Cream = Color(0xFFF5F0E8)
private val DarkBrown = Color(0xFF1A1512)
private val WarmBrown = Color(0xFF8B7355)
private val SoftWhite = Color(0xFFFEFCF9)

enum class AuthScreen { LOGIN, SIGNUP, FORGOT_PASSWORD, OTP }

@Composable
fun AuthFlow(
    onAuthenticated: () -> Unit,
    onDismiss: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var screen by remember { mutableStateOf(AuthScreen.LOGIN) }

    LaunchedEffect(state.isAuthenticated) {
        if (state.isAuthenticated) onAuthenticated()
    }

    AnimatedContent(
        targetState = screen,
        transitionSpec = {
            slideInHorizontally { it } togetherWith slideOutHorizontally { -it }
        },
        label = "auth_screen"
    ) { currentScreen ->
        when (currentScreen) {
            AuthScreen.LOGIN -> LoginScreen(
                state = state,
                onLogin = { email, pw -> viewModel.onEvent(AuthEvent.LoginSubmit(email, pw)) },
                onGoSignUp = { screen = AuthScreen.SIGNUP },
                onForgotPassword = { screen = AuthScreen.FORGOT_PASSWORD },
                onDismiss = onDismiss
            )
            AuthScreen.SIGNUP -> SignUpScreen(
                state = state,
                onSignUp = { name, email, pw -> viewModel.onEvent(AuthEvent.SignUpSubmit(name, email, pw)) },
                onGoLogin = { screen = AuthScreen.LOGIN },
                onSuccess = { screen = AuthScreen.OTP }
            )
            AuthScreen.FORGOT_PASSWORD -> ForgotPasswordScreen(
                state = state,
                onSubmit = { email -> viewModel.onEvent(AuthEvent.ForgotPasswordSubmit(email)) },
                onBack = { screen = AuthScreen.LOGIN },
                onSuccess = { screen = AuthScreen.OTP }
            )
            AuthScreen.OTP -> OtpScreen(
                state = state,
                onVerify = { code -> viewModel.onEvent(AuthEvent.OtpSubmit(code)) },
                onBack = { screen = AuthScreen.LOGIN }
            )
        }
    }
}

@Composable
private fun AuthContainer(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SoftWhite)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(64.dp))
        // Book nook logo mark
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("book", color = DarkBrown, fontSize = 14.sp, letterSpacing = 4.sp, fontWeight = FontWeight.Light)
            Text("nook", color = WarmBrown, fontSize = 14.sp, letterSpacing = 4.sp, fontWeight = FontWeight.Light)
        }
        Spacer(Modifier.height(40.dp))
        content()
    }
}

@Composable
private fun BookibaTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    isPassword: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Next
) {
    var passwordVisible by remember { mutableStateOf(false) }

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, color = WarmBrown, fontSize = 13.sp) },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = DarkBrown,
            unfocusedBorderColor = Cream,
            focusedContainerColor = Cream,
            unfocusedContainerColor = Cream,
            cursorColor = DarkBrown,
            focusedTextColor = DarkBrown,
            unfocusedTextColor = DarkBrown
        ),
        singleLine = true,
        visualTransformation = if (isPassword && !passwordVisible) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = imeAction),
        trailingIcon = if (isPassword) {
            {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        imageVector = if (passwordVisible) Icons.Outlined.Visibility else Icons.Outlined.VisibilityOff,
                        contentDescription = null,
                        tint = WarmBrown
                    )
                }
            }
        } else null
    )
}

@Composable
private fun PrimaryButton(text: String, isLoading: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = !isLoading,
        modifier = Modifier.fillMaxWidth().height(54.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(containerColor = DarkBrown)
    ) {
        if (isLoading) {
            CircularProgressIndicator(color = Cream, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
        } else {
            Text(text, color = Cream, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

// ── Login Screen ─────────────────────────────────────────────────────────────
@Composable
fun LoginScreen(
    state: AuthUiState,
    onLogin: (String, String) -> Unit,
    onGoSignUp: () -> Unit,
    onForgotPassword: () -> Unit,
    onDismiss: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    AuthContainer {
        Text("Welcome back.", color = DarkBrown, fontSize = 30.sp, fontWeight = FontWeight.Bold)
        Text("Sign in to your shelves.", color = WarmBrown, fontSize = 15.sp, fontStyle = FontStyle.Italic, modifier = Modifier.padding(top = 6.dp, bottom = 32.dp))

        BookibaTextField(value = email, onValueChange = { email = it }, label = "Email address", keyboardType = KeyboardType.Email)
        Spacer(Modifier.height(12.dp))
        BookibaTextField(value = password, onValueChange = { password = it }, label = "Password", isPassword = true, imeAction = ImeAction.Done)

        TextButton(onClick = onForgotPassword, modifier = Modifier.align(Alignment.End)) {
            Text("Forgot password?", color = WarmBrown, fontSize = 13.sp)
        }

        Spacer(Modifier.height(8.dp))
        PrimaryButton("Sign In", isLoading = state.isLoading) { onLogin(email, password) }
        Spacer(Modifier.height(16.dp))

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
            Text("Don't have an account? ", color = WarmBrown, fontSize = 14.sp)
            Text("Sign up", color = DarkBrown, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clickable(onClick = onGoSignUp))
        }

        Spacer(Modifier.height(24.dp))
        TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
            Text("Continue browsing without account", color = WarmBrown.copy(alpha = 0.6f), fontSize = 13.sp, textAlign = TextAlign.Center)
        }

        state.error?.let {
            Spacer(Modifier.height(16.dp))
            Text(it, color = MaterialTheme.colorScheme.error, fontSize = 13.sp, textAlign = TextAlign.Center)
        }
    }
}

// ── Sign Up Screen ───────────────────────────────────────────────────────────
@Composable
fun SignUpScreen(
    state: AuthUiState,
    onSignUp: (String, String, String) -> Unit,
    onGoLogin: () -> Unit,
    onSuccess: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    LaunchedEffect(state.successMessage) {
        if (state.successMessage != null) onSuccess()
    }

    AuthContainer {
        Text("Join the shelves.", color = DarkBrown, fontSize = 30.sp, fontWeight = FontWeight.Bold)
        Text("Create your book nook account.", color = WarmBrown, fontSize = 15.sp, fontStyle = FontStyle.Italic, modifier = Modifier.padding(top = 6.dp, bottom = 32.dp))

        BookibaTextField(value = name, onValueChange = { name = it }, label = "Full name")
        Spacer(Modifier.height(12.dp))
        BookibaTextField(value = email, onValueChange = { email = it }, label = "Email address", keyboardType = KeyboardType.Email)
        Spacer(Modifier.height(12.dp))
        BookibaTextField(value = password, onValueChange = { password = it }, label = "Password", isPassword = true, imeAction = ImeAction.Done)
        Spacer(Modifier.height(24.dp))

        PrimaryButton("Create Account", isLoading = state.isLoading) { onSignUp(name, email, password) }
        Spacer(Modifier.height(16.dp))

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
            Text("Already have an account? ", color = WarmBrown, fontSize = 14.sp)
            Text("Sign in", color = DarkBrown, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clickable(onClick = onGoLogin))
        }

        state.error?.let {
            Spacer(Modifier.height(16.dp))
            Text(it, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
        }
    }
}

// ── Forgot Password ──────────────────────────────────────────────────────────
@Composable
fun ForgotPasswordScreen(
    state: AuthUiState,
    onSubmit: (String) -> Unit,
    onBack: () -> Unit,
    onSuccess: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    LaunchedEffect(state.successMessage) { if (state.successMessage != null) onSuccess() }

    AuthContainer {
        Text("Reset password.", color = DarkBrown, fontSize = 30.sp, fontWeight = FontWeight.Bold)
        Text("We'll send a code to your email.", color = WarmBrown, fontSize = 15.sp, fontStyle = FontStyle.Italic, modifier = Modifier.padding(top = 6.dp, bottom = 32.dp))

        BookibaTextField(value = email, onValueChange = { email = it }, label = "Email address", keyboardType = KeyboardType.Email, imeAction = ImeAction.Done)
        Spacer(Modifier.height(24.dp))
        PrimaryButton("Send Code", isLoading = state.isLoading) { onSubmit(email) }
        Spacer(Modifier.height(16.dp))
        TextButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
            Text("Back to login", color = WarmBrown, fontSize = 14.sp)
        }
    }
}

// ── OTP Screen ───────────────────────────────────────────────────────────────
@Composable
fun OtpScreen(
    state: AuthUiState,
    onVerify: (String) -> Unit,
    onBack: () -> Unit
) {
    var code by remember { mutableStateOf("") }

    AuthContainer {
        Text("Enter code.", color = DarkBrown, fontSize = 30.sp, fontWeight = FontWeight.Bold)
        Text("Check your email for the 6-digit code.", color = WarmBrown, fontSize = 15.sp, fontStyle = FontStyle.Italic, modifier = Modifier.padding(top = 6.dp, bottom = 32.dp))

        OutlinedTextField(
            value = code,
            onValueChange = { if (it.length <= 6) code = it },
            label = { Text("6-digit code", color = WarmBrown) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = DarkBrown,
                unfocusedBorderColor = Cream,
                focusedContainerColor = Cream,
                unfocusedContainerColor = Cream,
                cursorColor = DarkBrown,
                focusedTextColor = DarkBrown,
                unfocusedTextColor = DarkBrown
            ),
            singleLine = true
        )

        Spacer(Modifier.height(24.dp))
        PrimaryButton("Verify", isLoading = state.isLoading) { onVerify(code) }
        Spacer(Modifier.height(16.dp))
        TextButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
            Text("Back", color = WarmBrown, fontSize = 14.sp)
        }
    }
}
