package com.example.core.ui.component

import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.core.R
import com.example.core.ui.theme.LocoAppTheme

@Composable
fun GenericLoading(
    message: String? = null
) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator()
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