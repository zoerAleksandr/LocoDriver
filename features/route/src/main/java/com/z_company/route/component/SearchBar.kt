package com.z_company.route.component

import android.annotation.SuppressLint
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import com.z_company.core.ui.theme.Shapes

@SuppressLint("SuspiciousIndentation")
@ExperimentalAnimationApi
@Composable
fun SearchBar(
    modifier: Modifier = Modifier,
    query: TextFieldValue,
    onQueryChange: (TextFieldValue) -> Unit,
    onSearchFocusChange: (Boolean) -> Unit,
    onBack: () -> Unit,
    onSearch: () -> Unit,
    openSetting: () -> Unit
) {
        SearchTextField(
            modifier = modifier
                .background(
                    color = MaterialTheme.colorScheme.surface,
                    shape = Shapes.medium
                ).fillMaxWidth(),
            query = query,
            onQueryChange = onQueryChange,
            onSearchFocusChange = onSearchFocusChange,
            onBack = {
                onBack()
            },
            onSearch = onSearch,
            openSetting = openSetting
        )
}