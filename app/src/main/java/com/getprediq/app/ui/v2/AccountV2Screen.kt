package com.getprediq.app.ui.v2

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.getprediq.app.PrediqContractViewModel
import com.getprediq.app.PrediqViewModel
import com.getprediq.app.ui.v2.components.*
import com.getprediq.app.ui.v2.theme.*

@Composable
fun AccountV2Screen(
    contractVm: PrediqContractViewModel,
    authVm: PrediqViewModel
) {
    val context = LocalContext.current
    val state = contractVm.state
    val authState by authVm.state
    val account = state.account

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(V2Background),
        contentPadding = PaddingValues(
            horizontal = LocalV2Spacing.current.pageHorizontal,
            vertical = LocalV2Spacing.current.m
        )
    ) {
        item {
            Text(text = "Account", style = V2Typography.headlineMedium)
            Text(
                text = "One Tuku account. A session that stays signed in.",
                style = V2Typography.bodyMedium,
                color = V2TextSecondary,
            )
            Spacer(Modifier.height(LocalV2Spacing.current.l))
        }

        if (account != null) {
            item {
                IdentityArea(account)
                Spacer(Modifier.height(LocalV2Spacing.current.xl))
            }

            item {
                PrediqElevatedSurface(contentPadding = 0.dp) {
                    AccountRow(Icons.Outlined.Person, "Profile settings") {}
                    HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = V2Divider)
                    AccountRow(Icons.Outlined.Notifications, "Notifications") {}
                    HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = V2Divider)
                    AccountRow(Icons.Outlined.HealthAndSafety, "Responsible Use") {}
                    HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = V2Divider)
                    AccountRow(Icons.Outlined.Logout, "Sign Out", danger = true) {
                        authVm.logout()
                        contractVm.clearForLogout()
                    }
                }
            }
        } else if (authState.account != null) {
            item {
                PrediqLoadingState(
                    modifier = Modifier.heightIn(min = 280.dp),
                    message = "Restoring your PredIQ session..."
                )
            }
        } else {
            item {
                QuickSignInCard(
                    busy = authState.authBusy,
                    error = authState.authError,
                    onSignIn = { email, password ->
                        authVm.login(email, password) {
                            contractVm.bootstrap(force = true)
                        }
                    },
                    onTukuSignIn = {
                        authVm.startTuku { url ->
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun QuickSignInCard(
    busy: Boolean,
    error: String?,
    onSignIn: (String, String) -> Unit,
    onTukuSignIn: () -> Unit,
) {
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var passwordVisible by rememberSaveable { mutableStateOf(false) }
    val canSubmit = email.isNotBlank() && password.isNotBlank() && !busy

    fun submit() {
        if (canSubmit) onSignIn(email.trim(), password)
    }

    PrediqElevatedSurface(contentPadding = 20.dp) {
        Text(
            text = "Welcome back",
            style = V2Typography.titleLarge,
            color = V2TextPrimary,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "Sign in here without leaving the app. Your existing Tuku credentials work across PredIQ.",
            style = V2Typography.bodyMedium,
            color = V2TextSecondary,
        )
        Spacer(Modifier.height(LocalV2Spacing.current.l))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Email") },
            leadingIcon = { Icon(Icons.Outlined.Email, contentDescription = null) },
            singleLine = true,
            enabled = !busy,
            shape = V2Shapes.medium,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next,
            ),
        )
        Spacer(Modifier.height(LocalV2Spacing.current.s))
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Password") },
            leadingIcon = { Icon(Icons.Outlined.Lock, contentDescription = null) },
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        if (passwordVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                        contentDescription = if (passwordVisible) "Hide password" else "Show password",
                    )
                }
            },
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            singleLine = true,
            enabled = !busy,
            shape = V2Shapes.medium,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done,
            ),
            keyboardActions = KeyboardActions(onDone = { submit() }),
        )

        error?.takeIf { it.isNotBlank() }?.let {
            Spacer(Modifier.height(LocalV2Spacing.current.s))
            Text(
                text = it,
                style = V2Typography.bodyMedium,
                color = V2Negative,
            )
        }

        Spacer(Modifier.height(LocalV2Spacing.current.m))
        PrediqPrimaryButton(
            onClick = { submit() },
            enabled = canSubmit,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(Icons.Outlined.Login, contentDescription = null)
            Spacer(Modifier.width(LocalV2Spacing.current.xs))
            Text(if (busy) "Signing in..." else "Sign in")
        }

        Spacer(Modifier.height(LocalV2Spacing.current.s))
        OutlinedButton(
            onClick = onTukuSignIn,
            enabled = !busy,
            shape = V2ButtonShape,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp),
        ) {
            Text("Continue with Tuku Auth")
        }

        Spacer(Modifier.height(LocalV2Spacing.current.s))
        Text(
            text = "PredIQ keeps the rotating Tuku session so you should not have to sign in again every time the app opens.",
            style = V2Typography.labelSmall,
            color = V2TextMuted,
        )
    }
}

@Composable
fun IdentityArea(account: com.getprediq.app.data.v2.V2AccountResponse) {
    PrediqElevatedSurface {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(V2DecisionSoft, CircleShape)
                    .border(1.dp, V2Divider, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = initials(account.profile.name ?: account.profile.email),
                    style = V2Typography.titleLarge,
                    color = V2DecisionLime
                )
            }
            Spacer(Modifier.width(LocalV2Spacing.current.m))
            Column {
                Text(
                    text = account.profile.name ?: "PredIQ Member",
                    style = V2Typography.titleMedium,
                    color = V2TextPrimary
                )
                Text(
                    text = account.profile.email,
                    style = V2Typography.bodyMedium,
                    color = V2TextSecondary,
                )
            }
        }

        Spacer(Modifier.height(LocalV2Spacing.current.l))

        PrediqSurface(
            color = V2DecisionSoft,
            shape = V2Shapes.medium,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = account.membership.planName ?: "Free Member",
                    style = V2Typography.labelLarge,
                    color = V2DecisionLime,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (account.membership.fullAccess) "Full Access" else "Limited Preview",
                    style = V2Typography.labelMedium,
                    color = V2TextSecondary,
                )
            }
        }
    }
}

@Composable
fun AccountRow(
    icon: ImageVector,
    label: String,
    danger: Boolean = false,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (danger) V2Negative else V2TextSecondary,
            modifier = Modifier.size(22.dp)
        )
        Spacer(Modifier.width(LocalV2Spacing.current.m))
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            style = V2Typography.bodyLarge,
            color = if (danger) V2Negative else V2TextPrimary
        )
        Icon(
            imageVector = Icons.Outlined.ChevronRight,
            contentDescription = null,
            tint = V2TextMuted,
            modifier = Modifier.size(20.dp)
        )
    }
}

private fun initials(name: String): String = name.split(" ")
    .filter { it.isNotEmpty() }
    .take(2)
    .joinToString("") { it.take(1).uppercase() }
    .ifBlank { "P" }
