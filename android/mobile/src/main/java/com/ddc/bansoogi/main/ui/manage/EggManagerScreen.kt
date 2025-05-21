package com.ddc.bansoogi.main.ui.manage

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.ddc.bansoogi.myInfo.controller.MyInfoController

@Composable
fun EggManagerScreen(
    onDismiss: () -> Unit,
    onRenewRecord: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(enabled = true) {}

    val context = LocalContext.current
    val myInfoController = remember { MyInfoController() }
    val myInfo by myInfoController.myInfoFlow().collectAsState(initial = null)

    val wrappedDismiss: () -> Unit = {
        myInfoController.markNotFirstUser(context)
        onRenewRecord()
        onDismiss()
    }

    EggManagerModal(
        myInfo    = myInfo,
        onDismiss = wrappedDismiss,
        modifier  = modifier
    )
}