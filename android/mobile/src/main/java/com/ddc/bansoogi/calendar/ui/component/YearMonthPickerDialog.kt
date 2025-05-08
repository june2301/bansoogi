package com.ddc.bansoogi.calendar.ui.component

import android.widget.NumberPicker
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.DialogProperties
import com.ddc.bansoogi.calendar.ui.state.YearMonthState
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun YearMonthPickerDialog(
    currentDate: LocalDate,
    onDateSelected: (year: Int, month: Int) -> Unit,
    onDismiss: () -> Unit
) {
    val yearMonthState = remember {
        YearMonthState(currentDate.year, currentDate.monthValue)
    }

    BasicAlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        DialogContent(
            yearMonthState = yearMonthState,
            onConfirm = { onDateSelected(yearMonthState.year, yearMonthState.month) },
            onDismiss = onDismiss
        )
    }
}

@Composable
fun DialogContent(
    yearMonthState: YearMonthState,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    Column(
        modifier = Modifier
            .width(300.dp)
            .background(
                color = Color.White,
                shape = RoundedCornerShape(24.dp)
            ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        PickerContent(
            yearMonthState = yearMonthState
        )

        Spacer(modifier = Modifier.height(8.dp))

        DialogButtons(
            onDismiss = onDismiss,
            onConfirm = {
                onConfirm()
                onDismiss()
            }
        )
    }
}

@Composable
fun PickerContent(
    yearMonthState: YearMonthState
) {
    Row(
        modifier = Modifier
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        NumberPicker(
            value = yearMonthState.year,
            range = (1920..2150),
            onValueChange = { yearMonthState.year = it }
        )

        NumberPicker(
            value = yearMonthState.month,
            range = (1..12),
            onValueChange = { yearMonthState.month = it }
        )
    }
}

@Composable
fun NumberPicker(
    value: Int,
    range: IntRange,
    onValueChange: (Int) -> Unit
) {
    AndroidView(
        factory = { context ->
            NumberPicker(context).apply {
                minValue = range.first
                maxValue = range.last
                wrapSelectorWheel = false
            }
        },
        update = {
            it.value = value
            it.setOnValueChangedListener { _, _, newVal ->
                onValueChange(newVal)
            }
        }
    )
}

@Composable
fun DialogButtons(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp),
        horizontalArrangement = Arrangement.End
    ) {
        TextButton(
            onClick = {
                onConfirm()
            }
        ) {
            Text("확인")
        }

        TextButton(
            onClick = onDismiss
        ) {
            Text("취소")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun YearMonthPickerDialogPreView() {
    YearMonthPickerDialog(
        currentDate = LocalDate.now(),
        onDismiss = { },
        onDateSelected = { y, m -> },
    )
}