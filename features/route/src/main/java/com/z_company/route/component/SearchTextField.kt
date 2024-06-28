package com.z_company.route.component

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import com.z_company.core.ui.theme.custom.AppTypography
import com.z_company.route.R

@Composable
fun SearchTextField(
    modifier: Modifier = Modifier,
    query: TextFieldValue,
    onQueryChange: (TextFieldValue) -> Unit,
    onSearchFocusChange: (Boolean) -> Unit,
    onBack: () -> Unit,
    onSearch: () -> Unit,
    openSetting: () -> Unit
) {

    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    val keyboardController = LocalSoftwareKeyboardController.current

    TextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier
            .onFocusChanged {
                onSearchFocusChange(it.isFocused)
            }
            .focusRequester(focusRequester),
        singleLine = true,
        placeholder = {
            Text(
                color = MaterialTheme.colorScheme.secondary,
                text = "Я хочу найти...",
            )
        },
        textStyle = AppTypography.getType().bodyLarge.copy(color = MaterialTheme.colorScheme.primary),
//            colors = transparentColorForTextField(),
        leadingIcon = {
            IconButton(onClick = {
                focusManager.clearFocus()
                keyboardController?.hide()
                onBack()
            }) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = null
                )
            }
        },
        trailingIcon = {
            Row {
                when {
                    query.text.isNotEmpty() -> {
                        IconButton(onClick = onSearch) {
                            Icon(
                                imageVector = Icons.Outlined.Search,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }
                }
                IconButton(onClick = openSetting) {
                    Icon(
                        painter = painterResource(id = R.drawable.baseline_tune_24),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onBackground
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
        )
    )
}