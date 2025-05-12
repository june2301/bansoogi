package com.ddc.bansoogi.landing.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

@Composable
fun CheckboxRow(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    agreementType: AgreementType,
    onLabelClick: (AgreementType) -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
        Text(
            text = agreementType.title,
            modifier = Modifier.clickable {
                onLabelClick(agreementType)
            }
        )
    }
}