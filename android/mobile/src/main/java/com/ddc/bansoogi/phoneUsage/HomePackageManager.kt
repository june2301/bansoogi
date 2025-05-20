package com.ddc.bansoogi.phoneUsage

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager

object HomePackageManager {
    private var homePackage: String? = null

    private val knownHomePackageNames = setOf(
        "com.sec.android.app.launcher",
        "com.google.android.apps.nexuslauncher",
        "com.miui.home",
        "com.android.launcher3",
        "net.oneplus.launcher",
        "com.lge.launcher2"
    )

    fun initialize(context: Context) {
        homePackage = getDefaultHomePackage(context)
    }

    fun isHomePackage(context: Context, pkg: String): Boolean {
        return pkg == homePackage
                || pkg in knownHomePackageNames
                || pkg in getAllLauncherPackages(context)
    }

    fun getDefaultHomePackage(context: Context): String? {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        val resolveInfo = context.packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
        return resolveInfo?.activityInfo?.packageName
    }

    fun getAllLauncherPackages(context: Context): List<String> {
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
        }

        val pm = context.packageManager
        val resolveInfos = pm.queryIntentActivities(intent, PackageManager.MATCH_ALL)

        return resolveInfos.map { it.activityInfo.packageName }.distinct()
    }
}

