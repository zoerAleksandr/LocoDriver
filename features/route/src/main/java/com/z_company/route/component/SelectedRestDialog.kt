package com.z_company.route.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.z_company.core.R
import com.z_company.core.ui.theme.Shapes
import com.z_company.core.ui.theme.custom.AppTypography

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectedRestDialog(
    onDismissRequest: () -> Unit,
    onConfirmRequest: (Boolean) -> Unit,
    selectedItem: Int = 0,
){
    val peekList = listOf("Отдых в ПО", "Домашний отдых")
    BasicAlertDialog(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surface,
                shape = Shapes.medium
            ),
        onDismissRequest = onDismissRequest,
    ) {
        var itemIsSelected by remember {
            mutableIntStateOf(selectedItem)
        }

        Column(
            modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.End
        ) {
            peekList.forEachIndexed { index, item ->
                SelectedItem(
                    text = item,
                    isSelected = itemIsSelected == index
                ) {
                    itemIsSelected = index
                }
            }
            Row(
                modifier = Modifier
                    .padding(top = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                TextButton(onClick = onDismissRequest) {
                    Text(text = stringResource(id = R.string.text_btn_dismiss))
                }

                TextButton(
                    onClick = { onConfirmRequest(itemIsSelected == 0) },
                    shape = Shapes.medium,
                    border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.primary)
                ) {
                    Text(
                        text = stringResource(id = R.string.text_btn_confirm),
                        style = AppTypography.getType().bodyMedium
                    )
                }
            }
        }
    }
}
@Composable
private fun SelectedItem(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = isSelected, onClick = onClick),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = text, style = AppTypography.getType().titleMedium)
        if (isSelected) {
            Icon(imageVector = Icons.Outlined.Check, contentDescription = "Выбрать")
        }
    }
}