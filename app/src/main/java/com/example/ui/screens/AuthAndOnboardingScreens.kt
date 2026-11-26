package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Login
import androidx.compose.material.icons.automirrored.rounded.RotateRight
import androidx.compose.material.icons.rounded.AddCircleOutline
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.ui.text.input.KeyboardType
import com.example.util.WhatsAppHelper
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import com.example.R
import com.example.data.firebase.GoogleAuthHelper
import com.example.ui.glass.CoolingGlassAvatar
import com.example.ui.glass.CoolingGlassBackground
import com.example.ui.glass.CoolingGlassButton
import com.example.ui.glass.CoolingGlassCard
import com.example.ui.glass.CoolingGlassOutlinedButton
import com.example.ui.glass.CoolingGlassStatusBanner
import com.example.ui.glass.CoolingGlassTabRow
import com.example.ui.glass.CoolingGlassTextField
import com.example.ui.glass.FounderBrandingCard
import com.example.ui.theme.LocalCoolingGlass
import com.example.util.ImageCodec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun CoolingGlassAuthScreen(
    onAuthSuccess: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }
    val glass = LocalCoolingGlass.current

    var loading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        GoogleAuthHelper.attemptAutoSignIn(
            context = context,
            credentialManager = credentialManager,
            onAuthSuccess = onAuthSuccess,
            onUnauthenticated = {},
            scope = scope
        )
    }

    CoolingGlassBackground(showHeroVeil = true) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .border(2.dp, glass.glassBorder, RoundedCornerShape(28.dp))
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_tre4_icon_clean),
                    contentDescription = "TRE 4.0 Emblem",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(Modifier.height(18.dp))

            Text(
                text = "TRE 4.0",
                style = MaterialTheme.typography.displayLarge,
                color = glass.textPrimary,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Aspirant Test & Ranking Platform",
                style = MaterialTheme.typography.titleMedium,
                color = glass.accentPrimary,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = com.example.ui.theme.appText(
                    en = "Aspirant Examination, Live Ranking & Current Affairs Platform",
                    defaultText = "अभ्यर्थी परीक्षा, लाइव रैंकिंग एवं करंट अफेयर्स प्लेटफॉर्म"
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = glass.textSecondary,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(22.dp))

            CoolingGlassCard(
                modifier = Modifier.widthIn(max = 520.dp).fillMaxWidth(),
                strongFrost = true,
                contentPadding = PaddingValues(22.dp)
            ) {
                Text(
                    text = "Welcome, Aspirant",
                    style = MaterialTheme.typography.headlineSmall,
                    color = glass.textPrimary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Sign in securely with your Google account to access centralized Common GS 40-Q tests, 30-Q Language Qualifying papers, live group leaderboards, PDF exports, and interactive photo polls.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = glass.textSecondary
                )

                errorMessage?.takeIf { it.isNotBlank() }?.let { errMsg ->
                    Spacer(Modifier.height(14.dp))
                    CoolingGlassStatusBanner(
                        message = errMsg,
                        isError = true,
                        onDismiss = { errorMessage = null }
                    )
                }

                Spacer(Modifier.height(18.dp))

                CoolingGlassButton(
                    text = "Sign in with Google",
                    icon = Icons.AutoMirrored.Rounded.Login,
                    loading = loading,
                    enabled = !loading,
                    testTagName = "google_sign_in_button",
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        loading = true
                        errorMessage = null
                        GoogleAuthHelper.onGoogleSignInClicked(
                            context = context,
                            credentialManager = credentialManager,
                            onAuthSuccess = {
                                loading = false
                                onAuthSuccess()
                            },
                            onAuthError = { msg ->
                                loading = false
                                errorMessage = msg
                            },
                            scope = scope,
                            onAuthCancelled = {
                                loading = false
                            }
                        )
                    }
                )

                Spacer(Modifier.height(12.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Shield,
                        contentDescription = null,
                        tint = glass.accentCyan,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "Your personal email is never exposed publicly to other aspirants.",
                        style = MaterialTheme.typography.labelSmall,
                        color = glass.textMuted
                    )
                }
            }

            Spacer(Modifier.height(18.dp))

            FounderBrandingCard(
                modifier = Modifier.widthIn(max = 520.dp),
                compact = false
            )
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
fun AspirantProfileSetupScreen(
    suggestedName: String,
    busy: Boolean,
    errorText: String?,
    onRegister: (displayName: String, photoBase64: String?, whatsappNumber: String?) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val glass = LocalCoolingGlass.current
    var displayName by remember { mutableStateOf(suggestedName) }
    var photoBase64 by remember { mutableStateOf<String?>(null) }
    var whatsappInput by remember { mutableStateOf("") }
    val whatsappValid = WhatsAppHelper.isValidOptionalInput(whatsappInput)

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                photoBase64 = withContext(Dispatchers.IO) {
                    ImageCodec.encodeUriToBase64(context, uri, maxDimension = 420, quality = 68)
                }
            }
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            scope.launch {
                photoBase64 = withContext(Dispatchers.IO) {
                    ImageCodec.encodeBitmapToBase64(bitmap, maxDimension = 420, quality = 68)
                }
            }
        }
    }

    CoolingGlassBackground(showHeroVeil = true) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
                .windowInsetsPadding(WindowInsets.navigationBars.union(WindowInsets.ime))
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            CoolingGlassCard(
                modifier = Modifier.widthIn(max = 520.dp).fillMaxWidth(),
                strongFrost = true,
                contentPadding = PaddingValues(22.dp)
            ) {
                Text(
                    text = "Aspirant Registration",
                    style = MaterialTheme.typography.headlineMedium,
                    color = glass.textPrimary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = com.example.ui.theme.appText(
                        en = "Aspirant Profile Setup — Choose your display name & profile photo shown across group tests and rankings.",
                        defaultText = "अभ्यर्थी प्रोफ़ाइल सेटअप — Choose your display name & profile photo shown across group tests and rankings."
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = glass.textSecondary
                )

                Spacer(Modifier.height(20.dp))

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CoolingGlassAvatar(
                        name = displayName.ifBlank { "A" },
                        photoBase64 = photoBase64,
                        size = 92.dp
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CoolingGlassOutlinedButton(
                            text = "Gallery",
                            icon = Icons.Rounded.PhotoLibrary,
                            onClick = {
                                galleryLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            }
                        )
                        CoolingGlassOutlinedButton(
                            text = "Camera",
                            icon = Icons.Rounded.CameraAlt,
                            onClick = { cameraLauncher.launch(null) }
                        )
                        if (photoBase64 != null) {
                            CoolingGlassOutlinedButton(
                                text = "Rotate",
                                icon = Icons.AutoMirrored.Rounded.RotateRight,
                                onClick = {
                                    val currentImg = photoBase64 ?: return@CoolingGlassOutlinedButton
                                    scope.launch {
                                        photoBase64 = withContext(Dispatchers.IO) {
                                            ImageCodec.rotateBase64(currentImg)
                                        }
                                    }
                                }
                            )
                        }
                    }
                }

                Spacer(Modifier.height(18.dp))

                CoolingGlassTextField(
                    value = displayName,
                    onValueChange = { displayName = it },
                    label = com.example.ui.theme.appText("User Display Name", "User Display Name / अभ्यर्थी नाम"),
                    placeholder = "e.g., Rahul Kumar",
                    leadingIcon = Icons.Rounded.Person,
                    testTagName = "display_name_input"
                )

                Spacer(Modifier.height(12.dp))
                CoolingGlassTextField(
                    value = whatsappInput,
                    onValueChange = { whatsappInput = it },
                    label = "WhatsApp Number (Optional • Private to You & Admin)",
                    placeholder = "+91 98765 43210",
                    leadingIcon = Icons.Rounded.Phone,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    testTagName = "register_whatsapp_input"
                )
                if (!whatsappValid) {
                    Spacer(Modifier.height(6.dp))
                    CoolingGlassStatusBanner(
                        message = "Enter a valid 10-digit Indian number or E.164 number (or leave blank).",
                        isError = true
                    )
                }

                if (!errorText.isNullOrBlank()) {
                    Spacer(Modifier.height(12.dp))
                    CoolingGlassStatusBanner(message = errorText, isError = true)
                }

                Spacer(Modifier.height(18.dp))

                CoolingGlassButton(
                    text = "Continue to Group Setup",
                    loading = busy,
                    enabled = displayName.trim().length >= 2 && whatsappValid && !busy,
                    testTagName = "register_profile_button",
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        onRegister(
                            displayName.trim(),
                            photoBase64,
                            whatsappInput.trim().takeIf { it.isNotBlank() }
                        )
                    }
                )
            }
            Spacer(Modifier.height(16.dp))
            FounderBrandingCard(modifier = Modifier.widthIn(max = 520.dp), compact = true)
        }
    }
}

@Composable
fun GroupOnboardingScreen(
    aspirantName: String,
    busy: Boolean,
    errorText: String?,
    onJoinGroup: (String) -> Unit,
    onCreateGroup: (name: String, description: String) -> Unit
) {
    val glass = LocalCoolingGlass.current
    var selectedTab by remember { mutableStateOf(0) }
    var inviteCode by remember { mutableStateOf("") }
    var groupName by remember { mutableStateOf("BPSC TRE 4.0 Aspirants Batch") }
    var groupDesc by remember { mutableStateOf("Centralized Common GS, Language Qualifying Tests & Live Group Ranking") }

    CoolingGlassBackground(showHeroVeil = true) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
                .windowInsetsPadding(WindowInsets.navigationBars.union(WindowInsets.ime))
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            CoolingGlassCard(
                modifier = Modifier.widthIn(max = 520.dp).fillMaxWidth(),
                strongFrost = true,
                contentPadding = PaddingValues(22.dp)
            ) {
                Text(
                    text = "Hello, $aspirantName",
                    style = MaterialTheme.typography.headlineMedium,
                    color = glass.textPrimary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Join an existing TRE 4.0 examination group via invite code or create a new centralized group.",
                    style = MaterialTheme.typography.bodySmall,
                    color = glass.textSecondary
                )

                Spacer(Modifier.height(16.dp))

                CoolingGlassTabRow(
                    tabs = listOf("Join by Code", "Create Group"),
                    selectedIndex = selectedTab,
                    onSelect = { selectedTab = it }
                )

                Spacer(Modifier.height(16.dp))

                if (selectedTab == 0) {
                    CoolingGlassTextField(
                        value = inviteCode,
                        onValueChange = { inviteCode = it.uppercase() },
                        label = "Group Invitation Code",
                        placeholder = "e.g., TRE4X9K2M",
                        leadingIcon = Icons.Rounded.Key,
                        testTagName = "invite_code_input"
                    )
                    Spacer(Modifier.height(16.dp))
                    CoolingGlassButton(
                        text = "Join Examination Group",
                        icon = Icons.Rounded.Groups,
                        loading = busy,
                        enabled = inviteCode.trim().length >= 4 && !busy,
                        testTagName = "join_group_button",
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { onJoinGroup(inviteCode.trim()) }
                    )
                } else {
                    CoolingGlassTextField(
                        value = groupName,
                        onValueChange = { groupName = it },
                        label = "Group Name",
                        placeholder = "BPSC TRE 4.0 Aspirants",
                        leadingIcon = Icons.Rounded.Groups,
                        testTagName = "group_name_input"
                    )
                    Spacer(Modifier.height(12.dp))
                    CoolingGlassTextField(
                        value = groupDesc,
                        onValueChange = { groupDesc = it },
                        label = "Description",
                        singleLine = false,
                        minLines = 2,
                        maxLines = 4
                    )
                    Spacer(Modifier.height(16.dp))
                    CoolingGlassButton(
                        text = "Create Centralized Group",
                        icon = Icons.Rounded.AddCircleOutline,
                        loading = busy,
                        enabled = groupName.trim().length >= 2 && !busy,
                        testTagName = "create_group_button",
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { onCreateGroup(groupName.trim(), groupDesc.trim()) }
                    )
                }

                if (!errorText.isNullOrBlank()) {
                    Spacer(Modifier.height(14.dp))
                    CoolingGlassStatusBanner(message = errorText, isError = true)
                }
            }

            Spacer(Modifier.height(16.dp))
            FounderBrandingCard(modifier = Modifier.widthIn(max = 520.dp), compact = true)
        }
    }
}
