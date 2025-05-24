package com.ddc.bansoogi.common.notification

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.net.toUri
import com.ddc.bansoogi.main.ui.MainActivity

fun buildDeepLinkPendingIntent(
    context: Context,
    deepLink: String,
    requestCode: Int = 0,
    watchPackage: String? = null
): PendingIntent {

    val intent = Intent(
        Intent.ACTION_VIEW,
        deepLink.toUri(),
        context,
        MainActivity::class.java
    ).apply {
        watchPackage?.let { setPackage(it) }
    }

    return PendingIntent.getActivity(
        context,
        requestCode,
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
}
