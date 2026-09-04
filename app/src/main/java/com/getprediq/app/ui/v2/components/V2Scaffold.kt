package com.getprediq.app.ui.v2.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
            NavigationBar(
                containerColor = V2SurfacePrimary,
                tonalElevation = 0.dp
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
                            selectedIconColor = V2White,
                            selectedTextColor = V2DecisionLime,
                            indicatorColor = V2SurfaceElevated,
                            unselectedIconColor = V2White.copy(alpha = 0.64f),
                            unselectedTextColor = V2White.copy(alpha = 0.64f)
                        )
                    )
                }
            }
        },
        content = content
    )
}
