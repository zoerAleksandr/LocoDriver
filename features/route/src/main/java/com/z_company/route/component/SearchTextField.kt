package com.z_company.route.component

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import com.z_company.core.ui.theme.Shapes
import com.z_company.core.ui.theme.custom.AppTypography
import com.z_company.core.ui.theme.transparentColorForTextField
import com.z_company.route.R

@Composable
fun SearchTextField(
    modifier: Modifier = Modifier,
    query: TextFieldValue,
    onQueryChange: (TextFieldValue) -> Unit,
    onBack: () -> Unit,
    onSearch: () -> Unit,
    openSetting: () -> Unit
) {

    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val dataTextStyle = MaterialTheme.typography.bodyLarge
    val keyboardController = LocalSoftwareKeyboardController.current
    val primaryColor = MaterialTheme.colorScheme.primary

    val lineCount = query.text.count { it == '\n' } + 1
    val dynamicMaxLines = lineCount.coerceAtMost(5)

    OutlinedTextFieldApp(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier
            .focusRequester(focusRequester),
        placeholder = {
            Text(
                color = primaryColor,
                text = "Я хочу найти...",
                style = dataTextStyle
            )
        },
        textStyle = dataTextStyle,
        leadingIcon = {
            IconButton(onClick = {
                focusManager.clearFocus()
                keyboardController?.hide()
                onBack()
            }) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    tint = primaryColor,
                    contentDescription = null
                )
            }
        },
        trailingIcon = {
            Row {
                when {
                    query.text.isNotEmpty() -> {
                        IconButton(onClick = {
                            onSearch()
                            focusManager.clearFocus()
                            keyboardController?.hide()
                        }) {
                            Icon(
                                imageVector = Icons.Outlined.Search,
                                contentDescription = null,
                                tint = primaryColor
                            )
                        }
                    }
                }
                IconButton(onClick = openSetting) {
                    Icon(
                        painter = painterResource(id = R.drawable.baseline_tune_24),
                        contentDescription = null,
                        tint = primaryColor
                    )
                }
            }
        },
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(
            onSearch = {
                onSearch.invoke()
                focusManager.clearFocus()
            }
        ),
        shape = Shapes.medium,
        maxLines = dynamicMaxLines
    )
}