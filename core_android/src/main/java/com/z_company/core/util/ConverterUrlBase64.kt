package com.z_company.core.util

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Base64.NO_WRAP
import java.io.ByteArrayOutputStream
import android.util.Base64

object ConverterUrlBase64 {
    fun base64toBitmap(base64asString: String): Bitmap {
        val imageBytes = Base64.decode(base64asString, NO_WRAP)
        return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
    }

    fun bitmapToBase64(bitmap: Bitmap): String {
        val byteArrayOutputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream)
        val b = byteArrayOutputStream.toByteArray()
        return Base64.encodeToString(b, NO_WRAP)
    }

    fun uriToBitmap(uri: Uri, contentResolver: ContentResolver): Bitmap {
        return  if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            ImageDecoder.decodeBitmap(
                ImageDecoder.createSource(
                    contentResolver,
                    uri
                )
            )
        } else {
            MediaStore.Images.Media.getBitmap(contentResolver, uri)
        }
    }
}