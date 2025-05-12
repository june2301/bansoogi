package com.ddc.bansoogi.common.notification

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.net.toUri

fun buildWatchDeepLinkPendingIntent(
    context: Context,
    deepLink: String,
    requestCode: Int
): PendingIntent {
    val intent = Intent(Intent.ACTION_VIEW).apply {
        data = deepLink.toUri()
        setClass(context, com.ddc.bansoogi.presentation.MainActivity::class.java)
        addFlags(
            Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        )
    }

    return PendingIntent.getActivity(
        context,
        requestCode,
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
}

