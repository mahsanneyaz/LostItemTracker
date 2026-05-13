package com.srm.lostitemtracker

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfReceiptHelper {

    private const val PAGE_WIDTH  = 595
    private const val PAGE_HEIGHT = 842

    fun generateAndShare(
        context       : Context,
        matchId       : String,
        lostItemName  : String,
        foundItemName : String,
        lostUserName  : String,
        lostEmail     : String,
        foundUserName : String,
        foundEmail    : String,
        score         : Int
    ) {
        try {
            val document = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
            val page     = document.startPage(pageInfo)

            drawReceipt(
                page.canvas, matchId, lostItemName, foundItemName,
                lostUserName, lostEmail, foundUserName, foundEmail, score
            )

            document.finishPage(page)

            // Save to cache — no storage permission needed
            val dir  = File(context.cacheDir, "receipts")
            dir.mkdirs()
            val file = File(dir, "LiT_Receipt_${System.currentTimeMillis()}.pdf")
            val fos  = FileOutputStream(file)
            document.writeTo(fos)
            document.close()
            fos.close()

            val uri = FileProvider.getUriForFile(
                context, "${context.packageName}.fileprovider", file
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "LiT Item Handover Receipt")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(
                Intent.createChooser(shareIntent, "Save or Share Receipt")
            )

        } catch (e: Exception) {
            Toast.makeText(
                context, "Failed to generate receipt: ${e.message}",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun drawReceipt(
        canvas        : Canvas,
        matchId       : String,
        lostItemName  : String,
        foundItemName : String,
        lostUserName  : String,
        lostEmail     : String,
        foundUserName : String,
        foundEmail    : String,
        score         : Int
    ) {
        val margin  = 48f
        val gap     = 22f
        var y       = 60f

        val titlePaint = Paint().apply {
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = 22f
            color    = 0xFF006099.toInt()
        }
        val headingPaint = Paint().apply {
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = 13f
            color    = 0xFF191C1E.toInt()
        }
        val bodyPaint = Paint().apply {
            textSize = 12f
            color    = 0xFF404751.toInt()
        }
        val smallPaint = Paint().apply {
            textSize = 10f
            color    = 0xFF707882.toInt()
        }
        val linePaint = Paint().apply {
            color       = 0xFFBFC7D2.toInt()
            strokeWidth = 1f
        }

        canvas.drawText("Lost Item Tracker", margin, y, titlePaint)
        y += 24f
        canvas.drawText("SRM University, Sonipat", margin, y, smallPaint)
        y += 14f
        canvas.drawLine(margin, y, PAGE_WIDTH - margin, y, linePaint)
        y += 20f

        headingPaint.textSize = 16f
        canvas.drawText("ITEM HANDOVER CONFIRMATION RECEIPT", margin, y, headingPaint)
        headingPaint.textSize = 13f
        y += gap + 6f

        val dateStr = SimpleDateFormat("dd MMMM yyyy, hh:mm a", Locale.getDefault()).format(Date())
        canvas.drawText("Date: $dateStr", margin, y, bodyPaint)
        y += gap
        canvas.drawText("Match ID: $matchId", margin, y, smallPaint)
        y += gap + 10f
        canvas.drawLine(margin, y, PAGE_WIDTH - margin, y, linePaint)
        y += 20f

        canvas.drawText("LOST ITEM", margin, y, headingPaint)
        y += gap
        canvas.drawText("Item Name:   $lostItemName", margin, y, bodyPaint)
        y += gap
        canvas.drawText("Reported by: $lostUserName", margin, y, bodyPaint)
        y += gap
        canvas.drawText("Email:       $lostEmail", margin, y, bodyPaint)
        y += gap + 10f
        canvas.drawLine(margin, y, PAGE_WIDTH - margin, y, linePaint)
        y += 20f

        canvas.drawText("FOUND ITEM", margin, y, headingPaint)
        y += gap
        canvas.drawText("Item Name:   $foundItemName", margin, y, bodyPaint)
        y += gap
        canvas.drawText("Found by:    $foundUserName", margin, y, bodyPaint)
        y += gap
        canvas.drawText("Email:       $foundEmail", margin, y, bodyPaint)
        y += gap + 10f
        canvas.drawLine(margin, y, PAGE_WIDTH - margin, y, linePaint)
        y += 20f

        canvas.drawText("Match Similarity Score:   $score%", margin, y, headingPaint)
        y += gap + 20f
        canvas.drawLine(margin, y, PAGE_WIDTH - margin, y, linePaint)
        y += 20f

        canvas.drawText(
            "This receipt confirms the match was verified by both parties.",
            margin, y, smallPaint
        )
        y += gap
        canvas.drawText(
            "Generated by Lost Item Tracker — BCA Live Project, SRM University, Sonipat",
            margin, y, smallPaint
        )
    }
}