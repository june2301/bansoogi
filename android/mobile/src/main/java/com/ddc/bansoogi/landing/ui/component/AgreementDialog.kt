package com.ddc.bansoogi.landing.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun AgreementDialog(
    agreementType: AgreementType,
    onDismiss: () -> Unit,
    onAgree: () -> Unit,
    agreeButtonTitle: String = "동의하기"
) {
    val scrollState = rememberScrollState()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(agreementType.content) },
        text = {
            Box(
                modifier = Modifier
                    .heightIn(max = 350.dp)
            ) {
                Column(modifier = Modifier.verticalScroll(scrollState)) {
                    Text(text = agreementType.contentAnnotated)
                    Spacer(modifier = Modifier.height(48.dp)) // 여백
                }

                if (scrollState.value < scrollState.maxValue) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .align(Alignment.BottomCenter)
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color.Transparent, Color.White)
                                )
                            )
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onAgree) {
                Text(
                    agreeButtonTitle,
                    color = Color(0xFF333333),
                    fontWeight = FontWeight.Bold
                )
            }
        },
        containerColor = Color(0xFFFFFFFF)
    )
}
