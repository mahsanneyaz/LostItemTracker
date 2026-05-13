package com.srm.lostitemtracker

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

object ImageLabelingHelper {

    // Only keep labels where ML Kit is 70% or more confident
    // This filters out noisy guesses like "Rectangle" or "Wood"
    private const val CONFIDENCE_THRESHOLD = 0.70f

    // Takes a photo URI, runs ML Kit on it, returns all detected labels
    // Example result for a photo of a notebook: ["Notebook", "Book", "Paper", "Spiral"]
    suspend fun getLabels(context: Context, uri: Uri): List<String> {
        return suspendCancellableCoroutine { continuation ->
            try {
                val bitmap = uriToBitmap(context, uri)
                    ?: return@suspendCancellableCoroutine continuation.resume(emptyList())

                val image   = InputImage.fromBitmap(bitmap, 0)
                val options = ImageLabelerOptions.Builder()
                    .setConfidenceThreshold(CONFIDENCE_THRESHOLD)
                    .build()
                val labeler = ImageLabeling.getClient(options)

                labeler.process(image)
                    .addOnSuccessListener { labels ->
                        // labels is the full list — Label 0, Label 1, Label 2, etc.
                        // .map { it.text } extracts the text from every single label
                        val labelTexts = labels.map { it.text }
                        continuation.resume(labelTexts)
                    }
                    .addOnFailureListener {
                        // If labeling fails, return empty list
                        // The item will still be saved, just without image labels
                        // Matching will skip the image score for this item
                        continuation.resume(emptyList())
                    }
            } catch (e: Exception) {
                continuation.resume(emptyList())
            }
        }
    }

    private fun uriToBitmap(context: Context, uri: Uri): Bitmap? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
            BitmapFactory.decodeStream(inputStream)
        } catch (e: Exception) {
            null
        }
    }
}