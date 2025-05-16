package com.ddc.bansoogi.common.ui.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.ddc.bansoogi.R
import com.ddc.bansoogi.common.navigation.NavRoutes

@Composable
fun BansoogiNavigationBar(
    currentRoute: String,
    onNavigate: (String) -> Unit
) {
    val items = listOf(
        NavRoutes.HOME to R.drawable.bar_main,
        NavRoutes.COLLECTION to R.drawable.bar_collection,
        NavRoutes.CALENDAR to R.drawable.bar_calander,
        NavRoutes.MYINFO to R.drawable.bar_info
    )

    Surface(
        color = Color.Transparent,
        tonalElevation = 4.dp,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomEnd = 24.dp, bottomStart = 24.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp)
                .background(Color.Transparent),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEach { (route, iconRes) ->
                val isSelected = route == currentRoute

                val scale by animateFloatAsState(
                    targetValue = if (isSelected) 3f else 1.5f,
                    animationSpec = tween(durationMillis = 200),
                    label = "scale"
                )

                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
//                        .
                        .background(if (isSelected) Color(0xFFFDE68A) else Color.Transparent)
                        .clickable { onNavigate(route) }
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = iconRes),
                        contentDescription = route,
                        modifier = Modifier
                            .size(28.dp)
                            .graphicsLayer(
                                scaleX = scale,
                                scaleY = scale
                            )
                    )
                }
            }
        }
    }
}
