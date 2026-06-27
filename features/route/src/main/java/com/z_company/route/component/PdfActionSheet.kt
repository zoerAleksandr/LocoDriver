package com.z_company.route.component

import android.content.Intent
import android.net.Uri
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfActionSheet(
    uri: Uri,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    AppBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        title = "PDF сформирован",
        actions = listOf(
            BottomSheetAction(text = "Открыть PDF") {
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, "application/pdf")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(Intent.createChooser(intent, "Открыть в…"))
            },
            BottomSheetAction(text = "Поделиться") {
                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/pdf"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(sendIntent, "Поделиться PDF"))
            },

        )
    )
//
//    ModalBottomSheet(
//        onDismissRequest = onDismiss,
//        sheetState = sheetState,
//        containerColor = MaterialTheme.colorScheme.background
//    )
//    {
//        Column(
//            modifier = Modifier
//                .fillMaxWidth()
//                .padding(horizontal = 24.dp)
//                .padding(bottom = 32.dp)
//        ) {
//            Text(
//                text = "PDF сформирован",
//                style = MaterialTheme.typography.titleSmall,
//                color = MaterialTheme.colorScheme.primary,
//                modifier = Modifier.padding(bottom = 16.dp)
//            )
//
//            Button(
//                shape = Shapes.medium,
//                colors = ButtonDefaults.buttonColors(
//                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
//                ),
//                elevation = ButtonDefaults.elevatedButtonElevation(defaultElevation = 1.dp),
//                onClick = {
//                    val intent = Intent(Intent.ACTION_VIEW).apply {
//                        setDataAndType(uri, "application/pdf")
//                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
//                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
//                    }
//                    context.startActivity(Intent.createChooser(intent, "Открыть в…"))
//                    onDismiss()
//                },
//                modifier = Modifier.fillMaxWidth()
//            ) {
//                Text("Открыть в редакторе PDF")
//            }
//
//            Spacer(modifier = Modifier.height(8.dp))
//
//            OutlinedButton(
//                onClick = {
//                    val sendIntent = Intent(Intent.ACTION_SEND).apply {
//                        type = "application/pdf"
//                        putExtra(Intent.EXTRA_STREAM, uri)
//                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
//                    }
//                    context.startActivity(Intent.createChooser(sendIntent, "Поделиться PDF"))
//                    onDismiss()
//                },
//                modifier = Modifier.fillMaxWidth()
//            ) {
//                Text("Поделиться")
//            }
//
//            Spacer(modifier = Modifier.height(8.dp))
//
//            OutlinedButton(
//                onClick = onDismiss,
//                modifier = Modifier.fillMaxWidth()
//            ) {
//                Text("Отмена")
//            }
//        }
//    }
}
