package com.getprediq.app.ui.v2.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.getprediq.app.ui.v2.theme.*

data class V2NavigationItem(
    val route: String,
    val label: String,
    val icon: ImageVector
)

@Composable
fun PrediqV2Scaffold(
    items: List<V2NavigationItem>,
    currentRoute: String?,
    onNavigate: (String) -> Unit,
    content: @Composable (PaddingValues) -> Unit
) {
    Scaffold(
        containerColor = V2Background,
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = V2NavShape,
                    color = V2SurfacePrimary,
                    tonalElevation = 2.dp,
                    shadowElevation = 12.dp,
                ) {
                    NavigationBar(
                        containerColor = V2Transparent,
                        tonalElevation = 0.dp,
                        modifier = Modifier.height(72.dp),
                    ) {
                        items.forEach { item ->
                            val selected = currentRoute == item.route
                            NavigationBarItem(
                                selected = selected,
                                onClick = { onNavigate(item.route) },
                                icon = {
                                    Icon(
                                        imageVector = item.icon,
                                        contentDescription = item.label,
                                        modifier = Modifier.padding(bottom = 2.dp)
                                    )
                                },
                                label = {
                                    Text(
                                        text = item.label,
                                        style = V2Typography.labelMedium,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = V2DecisionLime,
                                    selectedTextColor = V2TextPrimary,
                                    indicatorColor = V2DecisionSoft,
                                    unselectedIconColor = V2TextMuted,
                                    unselectedTextColor = V2TextMuted,
                                )
                            )
                        }
                    }
                }
            }
        },
        content = content
    )
}
