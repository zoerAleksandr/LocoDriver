package com.example.route.component.camera

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionState
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import android.provider.Settings
import androidx.compose.foundation.layout.Box
import androidx.core.content.ContextCompat


@ExperimentalPermissionsApi
@Composable
fun Permission(
    permission: String = android.Manifest.permission.CAMERA,
    onDismissPermission: () -> Unit,
    content: @Composable () -> Unit = { }
) {

    val permissionState = rememberPermissionState(permission)

    var launchPermissionDialog by remember { mutableStateOf(true) }
    var showRationale by remember { mutableStateOf(true) }

    if (permissionState.status.isGranted) {
        Box(modifier = Modifier.fillMaxSize()) {
            content()
        }
    } else if (permissionState.status.shouldShowRationale) {

        if (showRationale) {
            OptionalRationalPermissionDialog(
                permission,
                dismissCallback = {
                    showRationale = false
                    onDismissPermission()
                }
            )
        }

    } else {
        if (launchPermissionDialog) {
            OptionalLaunchPermissionDialog(
                permission,
                permissionState,
                dismissCallback = {
                    launchPermissionDialog = false
                }
            )

            SideEffect {
                permissionState.launchPermissionRequest()
            }
        }

    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun OptionalLaunchPermissionDialog(
    permission: String,
    permissionState: PermissionState,
    dismissCallback: () -> Unit
) {
    val context = LocalContext.current
    val permissionLabel = stringResource(
        context.packageManager.getPermissionInfo(permission, 0).labelRes
    )

    AlertDialog(
        onDismissRequest = { dismissCallback() },
        title = { Text(text = "Требуется разрешение!") },
        text = { Text(text = permissionLabel) },
        confirmButton = {
            Button(onClick = {
                permissionState.launchPermissionRequest()
            }) {
                Text(text = "Разрешить")
            }
        },
        dismissButton = {
            Button(onClick = {
                dismissCallback()
            }) {
                Text(text = "Отказать")
            }
        }
    )
}

@Composable
fun OptionalRationalPermissionDialog(
    permission: String,
    dismissCallback: () -> Unit
) {
    val context = LocalContext.current
    val permissionLabel = stringResource(
        context.packageManager.getPermissionInfo(permission, 0).labelRes
    )

    AlertDialog(
        onDismissRequest = { dismissCallback() },
        title = { Text(text = "Требуется разрешение!") },
        text = { Text(text = permissionLabel) },
        confirmButton = {
            Button(onClick = {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    .apply {
                        data = Uri.fromParts("package", context.packageName, null)
                    }
                ContextCompat.startActivity(context, intent, null)
            }) {
                Text(text = "Настройки")
            }
        },
        dismissButton = {
            Button(onClick = {
                dismissCallback()
            }) {
                Text(text = "Отмена")
            }
        }
    )
}