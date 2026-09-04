package com.getprediq.app.ui.v2

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.material3.Icon
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
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
            Spacer(Modifier.height(LocalV2Spacing.current.l))
        }

        if (account != null) {
            item {
                IdentityArea(account)
                Spacer(Modifier.height(LocalV2Spacing.current.xl))
            }

            item {
                PrediqElevatedSurface(
                    contentPadding = 0.dp
                ) {
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
                    message = "Loading your PredIQ account..."
                )
            }
        } else {
            item {
                PrediqEmptyState(
                    title = "Not signed in",
                    message = "Sign in to access your profile, saved tickets, and premium intelligence."
                )
                Spacer(Modifier.height(LocalV2Spacing.current.m))
                PrediqPrimaryButton(
                    onClick = {
                        authVm.startTuku { url ->
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                        }
                    },
                    enabled = !authState.authBusy,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Outlined.Login, contentDescription = null)
                    Spacer(Modifier.width(LocalV2Spacing.current.xs))
                    Text(if (authState.authBusy) "Opening sign in..." else "Sign in or create account")
                }
                authState.authError?.takeIf { it.isNotBlank() }?.let { error ->
                    Spacer(Modifier.height(LocalV2Spacing.current.m))
                    Text(
                        text = error,
                        style = V2Typography.bodyMedium,
                        color = V2Negative
                    )
                }
            }
        }
    }
}

@Composable
fun IdentityArea(account: com.getprediq.app.data.v2.V2AccountResponse) {
    PrediqElevatedSurface {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(V2SurfacePrimary, CircleShape)
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
                    color = V2White
                )
                Text(
                    text = account.profile.email,
                    style = V2Typography.bodyMedium
                )
            }
        }

        Spacer(Modifier.height(LocalV2Spacing.current.l))

        PrediqSurface(
            color = V2SurfacePrimary,
            shape = V2Shapes.small,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                Modifier.padding(12.dp),
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
                    style = V2Typography.labelMedium
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
