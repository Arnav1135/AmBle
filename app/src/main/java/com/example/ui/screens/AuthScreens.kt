package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.example.R
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.ui.theme.*
import com.example.viewmodel.ChatViewModel

val AVATAR_PRESETS = listOf(
    "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=150",
    "https://images.unsplash.com/photo-1539571696357-5a69c17a67c6?w=150",
    "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=150",
    "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=150",
    "https://images.unsplash.com/photo-1517841905240-472988babdf9?w=150",
    "https://images.unsplash.com/photo-1501196354995-cbb51c65aaea?w=150"
)

@Composable
fun AuthScreens(viewModel: ChatViewModel) {
    var authTab by remember { mutableStateOf("phone") } // "phone", "email", "signup"

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                    colors = listOf(
                        AmBlePale1, // #D2E5F4
                        AmBlePale2  // #F1F6FC
                    )
                )
            )
    ) {
        // Sky Blue soft top gradient
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(320.dp)
                .background(
                    brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                        colors = listOf(
                            AmBleSky.copy(alpha = 0.7f),
                            AmBleSky.copy(alpha = 0.25f),
                            Color.Transparent
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 520.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(32.dp))

            // Logo & Header
            Image(
                painter = painterResource(id = R.drawable.ic_app_logo),
                contentDescription = "AmBle Logo",
                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .border(2.dp, Color.White.copy(alpha = 0.95f), RoundedCornerShape(20.dp))
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "AmBle",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = AmBleInk
            )

            Text(
                text = "Secure, real-time messaging",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = AmBlePrimaryDark,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Tab selectors if not in Sign Up Mode
            if (authTab != "signup") {
                TabRow(
                    selectedTabIndex = if (authTab == "phone") 0 else 1,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .border(1.5.dp, Color.White.copy(alpha = 0.9f), RoundedCornerShape(16.dp)),
                    containerColor = AmBleSky.copy(alpha = 0.45f),
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[if (authTab == "phone") 0 else 1]),
                            color = Color(0xFF1B2A5E)
                        )
                    },
                    divider = {}
                ) {
                    Tab(
                        selected = authTab == "phone",
                        onClick = { authTab = "phone" },
                        text = { Text("Phone Login", fontWeight = FontWeight.Bold, color = AmBleInk) }
                    )
                    Tab(
                        selected = authTab == "email",
                        onClick = { authTab = "email" },
                        text = { Text("Email Login", fontWeight = FontWeight.Bold, color = AmBleInk) }
                    )
                }
            }

            // Glassy Sky Blue & Pale Card container for Auth Forms
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White.copy(alpha = 0.88f)
                ),
                border = BorderStroke(2.dp, AmBleSky),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                when (authTab) {
                    "phone" -> {
                        PhoneForm(
                            viewModel = viewModel,
                            onSwitchToSignUp = { authTab = "signup" }
                        )
                    }
                    "email" -> {
                        SignInForm(
                            viewModel = viewModel,
                            onSwitchToSignUp = { authTab = "signup" }
                        )
                    }
                    "signup" -> {
                        SignUpForm(
                            viewModel = viewModel,
                            onSwitchToSignIn = { authTab = "phone" }
                        )
                    }
                }
            }
        }
    }
}
}

@Composable
fun SignInForm(viewModel: ChatViewModel, onSwitchToSignUp: () -> Unit) {
    var email by remember { mutableStateOf(viewModel.getSavedEmail()) }
    var password by remember { mutableStateOf("") }
    var rememberLogin by remember { mutableStateOf(viewModel.isRememberLoginEnabled()) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Welcome Back",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = AmBleInk,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Start
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it; errorMessage = null },
            label = { Text("Email Address", fontWeight = FontWeight.Medium) },
            leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = Color(0xFF1B2A5E)) },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("email_input"),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = AmBlePale2,
                unfocusedContainerColor = Color.White,
                focusedBorderColor = AmBlePrimary,
                unfocusedBorderColor = AmBleSky,
                focusedLabelColor = AmBleInk,
                unfocusedLabelColor = AmBleInkSoft,
                focusedTextColor = AmBleInk,
                unfocusedTextColor = AmBleInk
            )
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it; errorMessage = null },
            label = { Text("Password", fontWeight = FontWeight.Medium) },
            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = Color(0xFF1B2A5E)) },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("password_input"),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = AmBlePale2,
                unfocusedContainerColor = Color.White,
                focusedBorderColor = AmBlePrimary,
                unfocusedBorderColor = AmBleSky,
                focusedLabelColor = AmBleInk,
                unfocusedLabelColor = AmBleInkSoft,
                focusedTextColor = AmBleInk,
                unfocusedTextColor = AmBleInk
            )
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { rememberLogin = !rememberLogin }
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = rememberLogin,
                onCheckedChange = { rememberLogin = it },
                colors = CheckboxDefaults.colors(checkedColor = Color(0xFF1B2A5E))
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Save login info (Stay signed in)",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = AmBleInk
            )
        }

        if (errorMessage != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = errorMessage!!,
                color = MaterialTheme.colorScheme.error,
                fontSize = 14.sp,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Start
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                if (email.isEmpty() || password.isEmpty()) {
                    errorMessage = "Please enter both email and password."
                } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                    errorMessage = "Please enter a valid email address."
                } else {
                    viewModel.signIn(email, rememberLogin = rememberLogin, password = password)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .testTag("login_button"),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B2A5E)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Sign In", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Google Sign In Button
        OutlinedButton(
            onClick = {
                viewModel.signIn("google.user@chatwave.io", rememberLogin = rememberLogin)
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .testTag("google_sign_in_button"),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.White),
            border = BorderStroke(1.5.dp, AmBleSky)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AlternateEmail,
                    contentDescription = "Google Icon",
                    tint = Color(0xFF1B2A5E),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text("Continue with Google", color = AmBleInk, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        TextButton(onClick = onSwitchToSignUp) {
            Text(
                text = "Don't have an account? Sign Up",
                color = AmBleInk,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun SignUpForm(viewModel: ChatViewModel, onSwitchToSignIn: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("") }
    var selectedAvatar by remember { mutableStateOf(AVATAR_PRESETS.first()) }
    var rememberLogin by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Create Account",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = AmBleInk,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Start
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Avatar Picker
        Text(
            text = "Choose Profile Picture",
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = AmBleInkSoft,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Start
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AVATAR_PRESETS.forEach { url ->
                val isSelected = selectedAvatar == url
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .border(
                            width = if (isSelected) 3.dp else 1.dp,
                            color = if (isSelected) Color(0xFF1B2A5E) else Color.Transparent,
                            shape = CircleShape
                        )
                        .clickable { selectedAvatar = url }
                ) {
                    AsyncImage(
                        model = url,
                        contentDescription = "Avatar Preset",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = name,
            onValueChange = { name = it; errorMessage = null },
            label = { Text("Display Name", fontWeight = FontWeight.Medium) },
            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = Color(0xFF1B2A5E)) },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("signup_name_input"),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = AmBlePale2,
                unfocusedContainerColor = Color.White,
                focusedBorderColor = AmBlePrimary,
                unfocusedBorderColor = AmBleSky,
                focusedLabelColor = AmBleInk,
                unfocusedLabelColor = AmBleInkSoft,
                focusedTextColor = AmBleInk,
                unfocusedTextColor = AmBleInk
            )
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it; errorMessage = null },
            label = { Text("Email Address", fontWeight = FontWeight.Medium) },
            leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = Color(0xFF1B2A5E)) },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("signup_email_input"),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = AmBlePale2,
                unfocusedContainerColor = Color.White,
                focusedBorderColor = AmBlePrimary,
                unfocusedBorderColor = AmBleSky,
                focusedLabelColor = AmBleInk,
                unfocusedLabelColor = AmBleInkSoft,
                focusedTextColor = AmBleInk,
                unfocusedTextColor = AmBleInk
            )
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it; errorMessage = null },
            label = { Text("Password", fontWeight = FontWeight.Medium) },
            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = Color(0xFF1B2A5E)) },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("signup_password_input"),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = AmBlePale2,
                unfocusedContainerColor = Color.White,
                focusedBorderColor = AmBlePrimary,
                unfocusedBorderColor = AmBleSky,
                focusedLabelColor = AmBleInk,
                unfocusedLabelColor = AmBleInkSoft,
                focusedTextColor = AmBleInk,
                unfocusedTextColor = AmBleInk
            )
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = status,
            onValueChange = { status = it },
            label = { Text("Bio / Status message (Optional)", fontWeight = FontWeight.Medium) },
            leadingIcon = { Icon(Icons.Default.Info, contentDescription = null, tint = Color(0xFF1B2A5E)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = AmBlePale2,
                unfocusedContainerColor = Color.White,
                focusedBorderColor = AmBlePrimary,
                unfocusedBorderColor = AmBleSky,
                focusedLabelColor = AmBleInk,
                unfocusedLabelColor = AmBleInkSoft,
                focusedTextColor = AmBleInk,
                unfocusedTextColor = AmBleInk
            )
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { rememberLogin = !rememberLogin }
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = rememberLogin,
                onCheckedChange = { rememberLogin = it },
                colors = CheckboxDefaults.colors(checkedColor = Color(0xFF1B2A5E))
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Save login info (Stay signed in)",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = AmBleInk
            )
        }

        if (errorMessage != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = errorMessage!!,
                color = MaterialTheme.colorScheme.error,
                fontSize = 14.sp,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Start
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
                    errorMessage = "Please fill in all mandatory fields."
                } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                    errorMessage = "Please enter a valid email address."
                } else {
                    viewModel.signUp(name, email, status, selectedAvatar, rememberLogin = rememberLogin, password = password)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .testTag("signup_button"),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B2A5E)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Sign Up", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }

        Spacer(modifier = Modifier.height(16.dp))

        TextButton(onClick = onSwitchToSignIn) {
            Text(
                text = "Already have an account? Sign In",
                color = AmBleInk,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun PhoneForm(viewModel: ChatViewModel, onSwitchToSignUp: () -> Unit) {
    var phoneNumber by remember { mutableStateOf(viewModel.getSavedPhone()) }
    var rememberLogin by remember { mutableStateOf(viewModel.isRememberLoginEnabled()) }
    var otpCode by remember { mutableStateOf("") }
    var otpSent by remember { mutableStateOf(false) }
    var generatedOtp by remember { mutableStateOf("") }
    var showOtpToast by remember { mutableStateOf(false) }
    var countdownSeconds by remember { mutableStateOf(45) }
    var isNewUserMode by remember { mutableStateOf(false) }

    // New profile setup
    var displayName by remember { mutableStateOf("") }
    var statusMessage by remember { mutableStateOf("") }
    var selectedAvatar by remember { mutableStateOf(AVATAR_PRESETS.first()) }

    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(otpSent, countdownSeconds) {
        if (otpSent && countdownSeconds > 0) {
            delay(1000)
            countdownSeconds--
        }
    }

    if (showOtpToast) {
        AlertDialog(
            onDismissRequest = { showOtpToast = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Sms, contentDescription = null, tint = Color(0xFF3B7DD8))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("SMS Notification", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1B2A5E))
                }
            },
            text = {
                Column {
                    Text("From: AmBle Verification", fontSize = 11.sp, color = Color.Gray)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Your verification code is $generatedOtp. Enter this code to verify your phone number.",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.Black
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { showOtpToast = false },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B7DD8))
                ) {
                    Text("Okay")
                }
            },
            shape = RoundedCornerShape(16.dp),
            containerColor = Color.White
        )
    }

    Column(
        modifier = Modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (isNewUserMode) {
            // PROFILE SETUP FOR NEW USER
            Text(
                text = "Complete Profile",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Start,
                color = Color(0xFF1B2A5E)
            )

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "This is a new phone number. Please enter your name and choose an avatar to finish setting up your account.",
                fontSize = 13.sp,
                color = Color.Gray,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Avatar scroll
            Text(
                text = "Choose Profile Picture",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Start
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AVATAR_PRESETS.forEach { url ->
                    val isSelected = selectedAvatar == url
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .border(
                                width = if (isSelected) 3.dp else 1.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                shape = CircleShape
                            )
                            .clickable { selectedAvatar = url }
                    ) {
                        AsyncImage(
                            model = url,
                            contentDescription = "Avatar Preset",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = displayName,
                onValueChange = { displayName = it; errorMessage = null },
                label = { Text("Display Name", fontWeight = FontWeight.Medium) },
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = Color(0xFF1B2A5E)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("phone_signup_name"),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = AmBlePale2,
                    unfocusedContainerColor = Color.White,
                    focusedBorderColor = AmBlePrimary,
                    unfocusedBorderColor = AmBleSky,
                    focusedLabelColor = AmBleInk,
                    unfocusedLabelColor = AmBleInkSoft,
                    focusedTextColor = AmBleInk,
                    unfocusedTextColor = AmBleInk
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = statusMessage,
                onValueChange = { statusMessage = it },
                label = { Text("Bio / Status (Optional)", fontWeight = FontWeight.Medium) },
                leadingIcon = { Icon(Icons.Default.Info, contentDescription = null, tint = Color(0xFF1B2A5E)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = AmBlePale2,
                    unfocusedContainerColor = Color.White,
                    focusedBorderColor = AmBlePrimary,
                    unfocusedBorderColor = AmBleSky,
                    focusedLabelColor = AmBleInk,
                    unfocusedLabelColor = AmBleInkSoft,
                    focusedTextColor = AmBleInk,
                    unfocusedTextColor = AmBleInk
                )
            )

            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = errorMessage!!,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 14.sp,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Start
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = {
                    if (displayName.trim().isEmpty()) {
                        errorMessage = "Please enter a display name."
                    } else {
                        viewModel.signUpWithPhone(
                            name = displayName.trim(),
                            phoneNumber = phoneNumber.trim(),
                            status = statusMessage.trim(),
                            avatarPreset = selectedAvatar,
                            rememberLogin = rememberLogin
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("complete_phone_signup_button"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B2A5E))
            ) {
                Text("Complete Profile & Enter", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }

        } else if (!otpSent) {
            // PHONE NUMBER INPUT SCREEN
            Text(
                text = "Sign In with Phone",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Start,
                color = AmBleInk
            )

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Enter your phone number to receive a real-time verification code.",
                fontSize = 13.sp,
                color = AmBleInkSoft,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(20.dp))

            OutlinedTextField(
                value = phoneNumber,
                onValueChange = { phoneNumber = it; errorMessage = null },
                label = { Text("Phone Number", fontWeight = FontWeight.Medium) },
                placeholder = { Text("+1 (555) 0101", color = AmBleInkFaint) },
                leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = Color(0xFF1B2A5E)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("phone_input_field"),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = AmBlePale2,
                    unfocusedContainerColor = Color.White,
                    focusedBorderColor = AmBlePrimary,
                    unfocusedBorderColor = AmBleSky,
                    focusedLabelColor = AmBleInk,
                    unfocusedLabelColor = AmBleInkSoft,
                    focusedTextColor = AmBleInk,
                    unfocusedTextColor = AmBleInk
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { rememberLogin = !rememberLogin }
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = rememberLogin,
                    onCheckedChange = { rememberLogin = it },
                    colors = CheckboxDefaults.colors(checkedColor = Color(0xFF1B2A5E))
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Save login info (Stay signed in)",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = AmBleInk
                )
            }

            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = errorMessage!!,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 14.sp,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Start
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = {
                    val trimmedPhone = phoneNumber.trim()
                    if (trimmedPhone.isEmpty()) {
                        errorMessage = "Please enter your phone number."
                    } else if (trimmedPhone.length < 7) {
                        errorMessage = "Please enter a valid phone number."
                    } else {
                        // Generate real-time OTP code
                        generatedOtp = (100000..999999).random().toString()
                        otpSent = true
                        showOtpToast = true
                        countdownSeconds = 45
                        errorMessage = null
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("request_otp_button"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B2A5E))
            ) {
                Text("Send Verification Code", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }

            Spacer(modifier = Modifier.height(16.dp))

            TextButton(onClick = onSwitchToSignUp) {
                Text(
                    text = "Don't have an account? Sign Up with email",
                    color = AmBleInk,
                    fontWeight = FontWeight.Bold
                )
            }

        } else {
            // OTP VERIFICATION CODE SCREEN
            Text(
                text = "Verify Phone",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Start,
                color = AmBleInk
            )

            Spacer(modifier = Modifier.height(8.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = AmBleSky.copy(alpha = 0.25f)),
                border = BorderStroke(1.dp, AmBleSky.copy(alpha = 0.7f)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.VpnKey, contentDescription = null, tint = Color(0xFF1B2A5E), modifier = Modifier.size(16.dp))
                        Text(
                            text = "DEMO OTP CODE: $generatedOtp",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color(0xFF1B2A5E)
                        )
                    }
                    Text(
                        text = "For testing without carrier SMS costs, the verification code is generated directly above. Note: Direct SMS delivery to cellular SIM messages requires an active Firebase Auth / Twilio SMS Gateway backend.",
                        fontSize = 11.sp,
                        color = AmBleInkSoft,
                        lineHeight = 15.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            OutlinedTextField(
                value = otpCode,
                onValueChange = { otpCode = it; errorMessage = null },
                label = { Text("6-Digit Code", fontWeight = FontWeight.Medium) },
                placeholder = { Text("000000", color = AmBleInkFaint) },
                leadingIcon = { Icon(Icons.Default.LockClock, contentDescription = null, tint = Color(0xFF1B2A5E)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("otp_input_field"),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = AmBlePale2,
                    unfocusedContainerColor = Color.White,
                    focusedBorderColor = AmBlePrimary,
                    unfocusedBorderColor = AmBleSky,
                    focusedLabelColor = AmBleInk,
                    unfocusedLabelColor = AmBleInkSoft,
                    focusedTextColor = AmBleInk,
                    unfocusedTextColor = AmBleInk
                )
            )

            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = errorMessage!!,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 14.sp,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Start
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = {
                    if (otpCode.trim() == generatedOtp) {
                        // Check if phone user exists in DB
                        scope.launch {
                            val existingUser = viewModel.getUserByPhone(phoneNumber.trim())
                            if (existingUser != null) {
                                viewModel.signInWithPhone(phoneNumber.trim(), rememberLogin = rememberLogin)
                            } else {
                                isNewUserMode = true
                            }
                        }
                    } else {
                        errorMessage = "Invalid verification code. Please enter the code sent in the pop-up."
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("verify_otp_button"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B2A5E))
            ) {
                Text("Verify & Continue", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = {
                        // Resend
                        generatedOtp = (100000..999999).random().toString()
                        showOtpToast = true
                        countdownSeconds = 45
                        errorMessage = null
                    },
                    enabled = countdownSeconds == 0
                ) {
                    Text(
                        text = if (countdownSeconds > 0) "Resend code in ${countdownSeconds}s" else "Resend Code",
                        color = if (countdownSeconds > 0) AmBleInkFaint else AmBleInk,
                        fontWeight = FontWeight.Bold
                    )
                }

                TextButton(onClick = { otpSent = false; otpCode = "" }) {
                    Text("Change Number", color = AmBleInkSoft, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

