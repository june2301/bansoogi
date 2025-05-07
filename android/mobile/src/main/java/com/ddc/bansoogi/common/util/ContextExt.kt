package com.ddc.bansoogi.common.util

import android.content.Context
import android.content.Intent
import android.provider.Settings

fun Context.openAppNotificationSettings() {
    Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
        putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
    }.also { startActivity(it) }
}
