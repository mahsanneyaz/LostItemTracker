package com.srm.lostitemtracker

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

object CloudinaryManager {

    private const val CLOUD_NAME    = "drmu5qyqf"
    private const val UPLOAD_PRESET = "lost_item_tracker"
    private const val UPLOAD_URL    =
        "https://api.cloudinary.com/v1_1/$CLOUD_NAME/image/upload"

    private val client = OkHttpClient()

    suspend fun uploadImage(context: Context, imageUri: Uri): String {
        return withContext(Dispatchers.IO) {
            val inputStream = context.contentResolver.openInputStream(imageUri)
                ?: throw Exception("Cannot open image")
            val imageBytes = inputStream.readBytes()
            inputStream.close()

            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("upload_preset", UPLOAD_PRESET)
                .addFormDataPart(
                    "file", "item_photo.jpg",
                    imageBytes.toRequestBody("image/jpeg".toMediaTypeOrNull())
                )
                .build()

            val request = Request.Builder()
                .url(UPLOAD_URL)
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()
                ?: throw Exception("Empty response from Cloudinary")

            if (!response.isSuccessful) {
                throw Exception("Upload failed: $responseBody")
            }

            JSONObject(responseBody).getString("secure_url")
        }
    }
}