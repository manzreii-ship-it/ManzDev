package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import coil.compose.SubcomposeAsyncImage
import com.example.R
import com.example.data.repository.PlayerPreferences
import com.example.ui.auth.signOut
import com.example.ui.components.GenreChipPill
import com.example.ui.theme.ElectricCrimson
import com.example.ui.theme.ElevatedCard
import com.example.ui.theme.ErrorCoral
import com.example.ui.theme.HairlineBorder
import com.example.ui.theme.LuminousCyan
import com.example.ui.theme.ObsidianNight
import com.example.ui.theme.TextSecondary
import com.google.firebase.Firebase
import com.google.firebase.auth.auth

import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.PersonOutline
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.ui.auth.onGoogleSignInClicked
import com.example.ui.theme.CharcoalSurface

@Composable
fun ProfileScreen(
    isGuestMode: Boolean,
    watchlistCount: Int,
    historyCount: Int,
    preferences: PlayerPreferences,
    onSpeedChanged: (Float) -> Unit,
    onAutoPlayChanged: (Boolean) -> Unit,
    onExitGuestMode: () -> Unit,
    onSignedInFromGuest: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }
    val user = if (isGuestMode) null else Firebase.auth.currentUser

    var isSigningIn by remember { mutableStateOf(false) }
    var signInError by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianNight)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Text(
            text = "My Profile",
            style = MaterialTheme.typography.headlineMedium,
            color = Color.White
        )

        // Account Card
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = ElevatedCard,
            border = BorderStroke(1.dp, HairlineBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                val photoUrl = user?.photoUrl?.toString()
                if (!photoUrl.isNullOrBlank()) {
                    SubcomposeAsyncImage(
                        model = photoUrl,
                        contentDescription = "Profile avatar",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(62.dp)
                            .clip(CircleShape)
                    )
                } else {
                    Surface(
                        shape = CircleShape,
                        color = if (isGuestMode) CharcoalSurface else ElectricCrimson,
                        border = if (isGuestMode) BorderStroke(1.5.dp, LuminousCyan.copy(alpha = 0.6f)) else null,
                        modifier = Modifier.size(62.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            if (isGuestMode) {
                                Icon(
                                    imageVector = Icons.Filled.PersonOutline,
                                    contentDescription = "Guest avatar",
                                    tint = LuminousCyan,
                                    modifier = Modifier.size(30.dp)
                                )
                            } else {
                                Text(
                                    text = (user?.displayName?.firstOrNull() ?: 'M').uppercase(),
                                    style = MaterialTheme.typography.headlineSmall,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }

                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = if (isGuestMode) "Guest User" else (user?.displayName ?: "Manznime Fan"),
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White
                    )
                    Text(
                        text = if (isGuestMode) "Local Mode (No Cloud Sync)" else (user?.email ?: ""),
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                    Surface(
                        shape = RoundedCornerShape(100.dp),
                        color = if (isGuestMode) {
                            ElectricCrimson.copy(alpha = 0.18f)
                        } else {
                            LuminousCyan.copy(alpha = 0.14f)
                        },
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Text(
                            text = if (isGuestMode) "GUEST MODE" else "CLOUD SYNC ACTIVE",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isGuestMode) Color(0xFFFF8BA0) else LuminousCyan,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }
        }

        if (isGuestMode) {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = ElevatedCard,
                border = BorderStroke(1.dp, LuminousCyan.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.CloudSync,
                            contentDescription = null,
                            tint = LuminousCyan,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Sync Watchlist & History to Cloud",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White
                        )
                    }
                    Text(
                        text = "Sign in with Google anytime to back up your watchlist and watch history across devices.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                    signInError?.let { err ->
                        Text(
                            text = err,
                            style = MaterialTheme.typography.bodySmall,
                            color = ErrorCoral
                        )
                    }
                    Button(
                        onClick = {
                            signInError = null
                            isSigningIn = true
                            onGoogleSignInClicked(
                                context = context,
                                credentialManager = credentialManager,
                                onAuthSuccess = {
                                    isSigningIn = false
                                    onSignedInFromGuest()
                                },
                                onAuthError = { err ->
                                    isSigningIn = false
                                    signInError = err
                                },
                                scope = scope,
                                onAuthCancelled = {
                                    isSigningIn = false
                                }
                            )
                        },
                        enabled = !isSigningIn,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ElectricCrimson,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("profile_google_sign_in_button")
                    ) {
                        if (isSigningIn) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = stringResource(id = R.string.signing_in),
                                fontWeight = FontWeight.Bold
                            )
                        } else {
                            Text(
                                text = stringResource(id = R.string.sign_in_with_google),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // Stats Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatCard(
                icon = Icons.Filled.Bookmark,
                label = "Saved Episodes",
                value = watchlistCount.toString(),
                accent = ElectricCrimson,
                modifier = Modifier.weight(1f)
            )
            StatCard(
                icon = Icons.Filled.History,
                label = "Series Watched",
                value = historyCount.toString(),
                accent = LuminousCyan,
                modifier = Modifier.weight(1f)
            )
        }

        // Player Preferences Card
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = ElevatedCard,
            border = BorderStroke(1.dp, HairlineBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Speed,
                        contentDescription = null,
                        tint = LuminousCyan,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "In-App Player Preferences",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Default Playback Speed",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(0.75f, 1.0f, 1.25f, 1.5f).forEach { speed ->
                            GenreChipPill(
                                label = "${speed}x",
                                selected = preferences.defaultSpeed == speed,
                                onClick = { onSpeedChanged(speed) }
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Auto-Play Next Episode",
                            style = MaterialTheme.typography.titleSmall,
                            color = Color.White
                        )
                        Text(
                            text = "Automatically advance when an episode finishes",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                    Switch(
                        checked = preferences.autoPlayNextEpisode,
                        onCheckedChange = onAutoPlayChanged,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = ElectricCrimson
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (isGuestMode) {
            OutlinedButton(
                onClick = onExitGuestMode,
                border = BorderStroke(1.dp, ErrorCoral.copy(alpha = 0.6f)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = ErrorCoral),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("exit_guest_mode_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Logout,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(id = R.string.exit_guest_mode),
                    fontWeight = FontWeight.Bold
                )
            }
        } else {
            OutlinedButton(
                onClick = {
                    signOut(
                        credentialManager = credentialManager,
                        scope = scope
                    )
                },
                border = BorderStroke(1.dp, ErrorCoral.copy(alpha = 0.6f)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = ErrorCoral),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("sign_out_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Logout,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(id = R.string.sign_out),
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun StatCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = ElevatedCard,
        border = BorderStroke(1.dp, HairlineBorder),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(24.dp)
            )
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        }
    }
}
