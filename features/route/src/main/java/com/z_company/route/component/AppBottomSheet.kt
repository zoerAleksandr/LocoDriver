package com.z_company.route.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.SheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/**
 * Action model for button inside sheet body (not the cancel button).
 */
data class BottomSheetAction(
    val id: String = "",                       // optional id for testing / keying
    val text: String,
    val enabled: Boolean = true,
    val onClick: () -> Unit
)

/**
 * Reusable application BottomSheet.
 *
 * - title: simple title text (used when headerContent is null)
 * - headerContent: optional custom header composable (preferred when you need complex header)
 * - actions: list of actions rendered in the main block (as full-width TextButtons)
 * - onDismissRequest: called when sheet requests dismiss
 * - sheetState: ModalBottomSheetState from caller
 * - cancelText / onCancel: text + handler for bottom cancel button (rendered separately)
 *
 * Usage examples are shown after function.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
@Composable
fun AppBottomSheet(
    onDismissRequest: () -> Unit,
    sheetState: SheetState,
    title: String? = null,
    headerContent: (@Composable () -> Unit)? = null,
    actions: List<BottomSheetAction> = emptyList(),
    cancelText: String = "Отмена",
    onCancel: () -> Unit = onDismissRequest,
    // optional composable slot for extra content between header and actions
    contentAfterHeader: (@Composable ColumnScope.() -> Unit)? = null,
    // appearance customization hooks (colors / shapes) if needed
    sheetBackgroundColor: Color = MaterialTheme.colorScheme.background,
    sheetCornerRadius: Int = 16,
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = Color.Transparent,
        tonalElevation = 8.dp,
        dragHandle = {}
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, start = 8.dp, end = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = sheetBackgroundColor,
                        shape = RoundedCornerShape(sheetCornerRadius.dp)
                    ),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(modifier = Modifier.height(16.dp))

                // Header: either provided composable or simple title text
                if (headerContent != null) {
                    headerContent()
                } else {
                    title?.let {
                        Text(
                            text = it,
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp),
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Optional extra content (slot)
                contentAfterHeader?.invoke(this)

                // Menu / Actions block
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    actions.forEach { action ->
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            thickness = 0.5.dp
                        )
                        TextButton(
                            onClick = {
                                // close sheet first (caller may also dismiss inside handler)
                                onDismissRequest()
                                // then perform action
                                action.onClick()
                            },
                            enabled = action.enabled,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                        ) {
                            Text(
                                text = action.text,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.tertiary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Cancel block: separate visual container at bottom
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = sheetBackgroundColor,
                        shape = RoundedCornerShape(sheetCornerRadius.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                // The cancel button should NOT auto-dismiss the sheet here; we call onCancel so caller can decide.
                Text(
                    text = cancelText,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier
                        .clickable {
                            onCancel()
                        }
                        .padding(vertical = 14.dp),
                    // center the text
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}