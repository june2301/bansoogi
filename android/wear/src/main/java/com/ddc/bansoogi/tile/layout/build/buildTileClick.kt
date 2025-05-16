package com.ddc.bansoogi.tile.layout.build

import android.content.Context
import androidx.wear.protolayout.ActionBuilders
import androidx.wear.protolayout.ModifiersBuilders

fun buildClickableModifier(context: Context): ModifiersBuilders.Modifiers {
    val launchAction = buildLaunchAction(context)
    return ModifiersBuilders.Modifiers.Builder()
        .setClickable(
            ModifiersBuilders.Clickable.Builder()
                .setId("open_app_click")
                .setOnClick(launchAction)
                .build()
        )
        .build()
}

private fun buildLaunchAction(context: Context): ActionBuilders.LaunchAction {
    return ActionBuilders.LaunchAction.Builder()
        .setAndroidActivity(
            ActionBuilders.AndroidActivity.Builder()
                .setPackageName(context.packageName)
                .setClassName("com.ddc.bansoogi.presentation.MainActivity") // ← 앱에서 열고 싶은 액티비티 이름
                .build()
        )
        .build()
}