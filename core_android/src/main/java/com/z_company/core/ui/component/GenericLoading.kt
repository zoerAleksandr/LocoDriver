package com.z_company.core.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.z_company.core.ui.theme.LocoAppTheme
import com.z_company.core.R

@Composable
fun GenericLoading(
    message: String? = null,
    onCloseClick: () -> Unit = {}
) {
    Column(
        Modifier
            .zIndex(1f)
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background.copy(alpha = 0.9f)),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(contentAlignment = Alignment.Center) {
            CircularProgressIndicator(
                Modifier.size(48.dp)
            )
            IconButton(
                modifier = Modifier
                    .zIndex(1f),
                onClick = onCloseClick) {
                Icon(imageVector = Icons.Outlined.Close, tint = MaterialTheme.colorScheme.primary, contentDescription = null)
            }
        }
        Spacer(modifier = Modifier.size(16.dp))
        Text(text = message ?: stringResource(id = R.string.msg_loading))
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewGenericLoading() {
    LocoAppTheme {
        GenericLoading()
    }
}