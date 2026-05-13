package com.srm.lostitemtracker

import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.zxing.BarcodeFormat
import com.journeyapps.barcodescanner.BarcodeEncoder

class QRCodeActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_qr_code)

        val ivBack     = findViewById<ImageView>(R.id.ivQrBack)
        val ivQrCode   = findViewById<ImageView>(R.id.ivQrCode)
        val tvItemName = findViewById<TextView>(R.id.tvQrItemName)
        val tvDetails  = findViewById<TextView>(R.id.tvQrDetails)
        val btnShare   = findViewById<Button>(R.id.btnShareQr)

        ivBack.setOnClickListener { finish() }

        // Read data passed from the bottom sheet in DashboardActivity
        val itemName  = intent.getStringExtra("itemName")  ?: ""
        val category  = intent.getStringExtra("category")  ?: ""
        val location  = intent.getStringExtra("location")  ?: ""
        val date      = intent.getStringExtra("date")      ?: ""
        val type      = intent.getStringExtra("type")      ?: "LOST"
        val reportId  = intent.getStringExtra("reportId")  ?: ""
        val userEmail = intent.getStringExtra("userEmail") ?: ""

        tvItemName.text = itemName
        tvDetails.text  = "Type: $type\nCategory: $category\nLocation: $location\nDate: $date"

        // Build the text encoded into the QR code
        val qrContent = """
            === LOST ITEM TRACKER ===
            SRM University, Sonipat
            
            Type: $type ITEM
            Name: $itemName
            Category: $category
            Location: $location
            Date: $date
            Report ID: $reportId
            Contact: $userEmail
        """.trimIndent()

        try {
            val encoder = BarcodeEncoder()
            val bitmap: Bitmap = encoder.encodeBitmap(
                qrContent, BarcodeFormat.QR_CODE, 600, 600
            )
            ivQrCode.setImageBitmap(bitmap)

            btnShare.setOnClickListener { shareQrCode(bitmap, itemName) }

        } catch (e: Exception) {
            Toast.makeText(this, "Failed to generate QR code.", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun shareQrCode(bitmap: Bitmap, itemName: String) {
        try {
            val cacheDir = java.io.File(cacheDir, "qrcodes")
            cacheDir.mkdirs()
            val file   = java.io.File(cacheDir, "qr_${System.currentTimeMillis()}.png")
            val stream = java.io.FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            stream.flush()
            stream.close()

            val uri = androidx.core.content.FileProvider.getUriForFile(
                this, "${packageName}.fileprovider", file
            )

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "LiT Report — $itemName")
                putExtra(Intent.EXTRA_TEXT,
                    "Scan this QR code to view the item report on Lost Item Tracker.")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(intent, "Share QR Code via"))

        } catch (e: Exception) {
            android.widget.Toast.makeText(
                this, "Could not share QR code.", android.widget.Toast.LENGTH_SHORT
            ).show()
        }
    }
}