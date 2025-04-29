package com.example.eggi.common.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import com.example.eggi.R

@Composable
fun CommonNavigationBar (
    currentRoute: String,
    onNavigate: (String) -> Unit
) {
    val navItems = listOf(
        BottomNavItem("home", "홈", R.drawable.ic_home),
        BottomNavItem("collection", "컬랙션", R.drawable.ic_collection),
        BottomNavItem("calender", "캘린더", R.drawable.ic_calender),
        BottomNavItem("myInfo", "프로필", R.drawable.ic_my_info)
    )

    NavigationBar(
        modifier = Modifier.fillMaxWidth(),
        containerColor = Color.White.copy(alpha = 0.7f)
    ) {
        navItems.forEach { item ->
            val selected = item.route == currentRoute

            NavigationBarItem(
                selected = selected,
                onClick = { onNavigate(item.route) },
                icon = {
                    Icon(
                        painter = painterResource(id = item.iconResId),
                        contentDescription = item.title
                    )
                }
            )
        }
    }
}

data class BottomNavItem(
    val route: String,
    val title: String,
    val iconResId: Int
)
