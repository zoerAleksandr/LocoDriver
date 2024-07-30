package com.z_company.route.ui

import android.Manifest
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.util.Log
import android.widget.Toast
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import coil.compose.rememberImagePainter
import com.google.accompanist.permissions.*
import com.z_company.route.R
import java.io.File

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun CameraXDemo() {
    //request for access
    val permissionsState = rememberMultiplePermissionsState(permissions = listOf(Manifest.permission.CAMERA))
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(key1 = lifecycleOwner, effect = {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_START) {
                permissionsState.launchMultiplePermissionRequest()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    })

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        permissionsState.permissions.forEach { permissionState ->
            when (permissionState.permission) {
                Manifest.permission.CAMERA -> {
                    when (permissionState.status) {
                        is PermissionStatus.Granted -> {
                            Log.i("permissions", "CAMERA Granted")
                            MyCameraX()// User consent permission
                        }
                        is PermissionStatus.Denied -> {
                            Log.i("permissions", "CAMERA Denied")
                            NoPermissionView(permissionsState)// User disagreement permissions
                        }
                    }
                }
            }
        }
    }
}

// The content displayed when the user does not grant the authority
@OptIn(ExperimentalPermissionsApi::class)
@Composable
private fun NoPermissionView(permissionsState: MultiplePermissionsState) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(modifier = Modifier.weight(1f)) {

        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = {
                permissionsState.launchMultiplePermissionRequest()
            }) {
                Text(text = "Insufficient permissions, not available, click on the permission")
            }

        }
    }
}

@Composable
private fun MyCameraX() {
    val lifecycleObserver = LocalLifecycleOwner.current// Create a life cycle owner
    val context = LocalContext.current

    val cameraProviderFuture = remember {
        ProcessCameraProvider.getInstance(context)
    }
    // Create a camera preview view
    val previewView = remember {
        PreviewView(context).apply {
            id = R.id.preview_view// Specify the ID created in XML
        }
    }
    // ImageCapture is a case of image capture. Submit the takePiction function to shoot the picture to the memory or save it to the file, and provide the original image data
    val imageCapture = remember {
        ImageCapture.Builder().build()
    }

    // Display pictures on the interface, save URI status
    val imageUri = remember {
        mutableStateOf<Uri?>(null)
    }

    val fileUtils: FileUtils by lazy { FileUtilsImpl() }
    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { previewView },// Add the camera preview view
            modifier = Modifier.fillMaxSize()
        ) {
            cameraProviderFuture.addListener(
                {// Add a monitor
                    val cameraProvider = cameraProviderFuture.get() // Create CameraProvider
                    // Build a camera preview object
                    val preview = androidx.camera.core.Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }
                    // Set the camera and use the camera on the back default
                    val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                    try {
                        // From the life cycle of the application, the camera binding of all applications will be lifted, which will turn off all currently opened cameras and run it when starting
                        cameraProvider.unbindAll()
                        // Binding life cycle
                        cameraProvider.bindToLifecycle(
                            lifecycleObserver,
                            cameraSelector,
                            preview, imageCapture
                        )
                    } catch (e: Exception) {
                        Log.e("Camera", e.toString())
                    }

                },
                ContextCompat.getMainExecutor(context)// Add to the main actuator
            )
        }


        // Store the image into the file
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
                .align(Alignment.BottomCenter),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            // Show the picture of taking pictures
            imageUri.value?.let {
                Image(
                    painter = rememberImagePainter(data = it),
                    contentDescription = "",
                    modifier = Modifier.size(60.dp)
                )
            }
            Spacer(modifier = Modifier.width(24.dp))

            IconButton(onClick = {
                fileUtils.createDirectoryIfNotExist(context)
                val file = fileUtils.createFile(context)

                // Options for storing new capture images OutputOptions
                val outputOption = ImageCapture.OutputFileOptions.Builder(file).build()
                imageCapture.takePicture(outputOption,
                    ContextCompat.getMainExecutor(context),// Call the thread actuator
                    object : ImageCapture.OnImageSavedCallback { // For newly captured image call recovery
                        override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                            val saveUri = Uri.fromFile(file)// Save image address
                            Toast.makeText(context, saveUri.path, Toast.LENGTH_SHORT).show()
                            imageUri.value = saveUri// Set the image display path
                        }

                        override fun onError(exception: ImageCaptureException) {
                            Log.e("Camera", "$exception")
                        }

                    }
                )
            }) {
                Text(text = "Photograph")
            }
        }

    }
}


interface FileUtils {
    // Create a file directory
    fun createDirectoryIfNotExist(context: Context)

    //Create a file
    fun createFile(context: Context): File
}

class FileUtilsImpl : FileUtils {
    companion object {
        private const val IMAGE_PREFIX = "Image_"
        private const val JPG_SUFFIX = ".jpg"
        private const val FOLDER_NAME = "Photo"
    }

    override fun createDirectoryIfNotExist(context: Context) {
        val folder = File(
            "${context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)?.absoluteFile}"
                    + File.separator
                    + FOLDER_NAME
        )
        if (!folder.exists()) {
            folder.mkdir()
        }
    }

    override fun createFile(context: Context) = File(
        "${context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)?.absoluteFile}"
                + File.separator + FOLDER_NAME + File.separator + IMAGE_PREFIX + System.currentTimeMillis() + JPG_SUFFIX
    )

}
