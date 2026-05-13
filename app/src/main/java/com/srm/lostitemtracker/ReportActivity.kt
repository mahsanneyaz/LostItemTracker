package com.srm.lostitemtracker

import android.app.DatePickerDialog
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.textfield.TextInputEditText
import java.util.Calendar
import android.widget.EditText

class ReportActivity : AppCompatActivity() {

    private lateinit var viewModel: ReportViewModel
    private var selectedImageUri: Uri? = null
    private var cameraImageUri: Uri? = null
    private var isLost = true

    private val pickImage = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedImageUri = uri
            findViewById<ImageView>(R.id.ivPreview).setImageURI(uri)
        }
    }

    private val takePhoto = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success: Boolean ->
        if (success && cameraImageUri != null) {
            selectedImageUri = cameraImageUri
            findViewById<ImageView>(R.id.ivPreview).setImageURI(cameraImageUri)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_report)

        findViewById<TextView>(R.id.tvReportBack).setOnClickListener { finish() }

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.READ_MEDIA_IMAGES)
                != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                requestPermissions(
                    arrayOf(android.Manifest.permission.READ_MEDIA_IMAGES), 100
                )
            }
        }

        isLost = intent.getBooleanExtra("IS_LOST", true)

        viewModel = ViewModelProvider(this)[ReportViewModel::class.java]

        val tvTitle      = findViewById<TextView>(R.id.tvReportTitle)
        val etItemName   = findViewById<EditText>(R.id.etItemName)
        val spinnerCat   = findViewById<AutoCompleteTextView>(R.id.spinnerCategory)
        val etDesc       = findViewById<EditText>(R.id.etDescription)
        val etLocation   = findViewById<EditText>(R.id.etLocation)
        val etDate       = findViewById<EditText>(R.id.etDate)
        val btnPickPhoto = findViewById<Button>(R.id.btnPickPhoto)
        val btnSubmit    = findViewById<Button>(R.id.btnSubmit)
        val progress     = findViewById<ProgressBar>(R.id.progressBar)

        tvTitle.text = if (isLost) "Report Lost Item" else "Report Found Item"

        val categories = listOf(
            "Electronics", "Documents", "Accessories",
            "Clothing", "Keys", "Wallet / Purse",
            "Books / Notes", "Other"
        )
        val catAdapter = ArrayAdapter(
            this, android.R.layout.simple_dropdown_item_1line, categories
        )
        spinnerCat.setAdapter(catAdapter)
        spinnerCat.setOnClickListener { spinnerCat.showDropDown() }

        etDate.setOnClickListener {
            val cal = Calendar.getInstance()
            DatePickerDialog(
                this,
                { _, year, month, day ->
                    etDate.setText(
                        "$year-${(month + 1).toString().padStart(2, '0')}-${day.toString().padStart(2, '0')}"
                    )
                },
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        btnPickPhoto.setOnClickListener {
            val options = arrayOf("Take a Photo", "Pick from Gallery")
            android.app.AlertDialog.Builder(this)
                .setTitle("Add Photo")
                .setItems(options) { _, which ->
                    when (which) {
                        0 -> {
                            if (checkSelfPermission(android.Manifest.permission.CAMERA)
                                != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                                requestPermissions(
                                    arrayOf(android.Manifest.permission.CAMERA), 101
                                )
                            } else {
                                launchCamera()
                            }
                        }
                        1 -> pickImage.launch("image/*")
                    }
                }
                .show()
        }

        btnSubmit.setOnClickListener {
            val itemName = etItemName.text.toString().trim()
            val category = spinnerCat.text.toString().trim()
            val desc     = etDesc.text.toString().trim()
            val location = etLocation.text.toString().trim()
            val date     = etDate.text.toString().trim()

            if (itemName.isEmpty() || category.isEmpty() ||
                desc.isEmpty() || location.isEmpty() || date.isEmpty()) {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            progress.visibility = View.VISIBLE
            btnSubmit.isEnabled = false
            btnSubmit.text      = "Submitting..."

            if (isLost) {
                viewModel.submitLost(
                    this, itemName, category, desc, location, date, selectedImageUri
                )
            } else {
                viewModel.submitFound(
                    this, itemName, category, desc, location, date, selectedImageUri
                )
            }
        }

        viewModel.submitResult.observe(this) { result ->
            progress.visibility = View.GONE
            btnSubmit.isEnabled = true
            btnSubmit.text      = "Submit Report"
            result.onSuccess {
                Toast.makeText(
                    this,
                    if (isLost) "Lost item reported!" else "Found item reported!",
                    Toast.LENGTH_LONG
                ).show()
                finish()
            }
            result.onFailure { error ->
                Toast.makeText(this, "Failed: ${error.message}", Toast.LENGTH_LONG).show()
            }
        }
    }   // ← onCreate ends here

    // ↓ these two functions are OUTSIDE onCreate, still inside the class

    private fun launchCamera() {
        val photoFile = java.io.File(cacheDir, "camera_${System.currentTimeMillis()}.jpg")
        cameraImageUri = androidx.core.content.FileProvider.getUriForFile(
            this, "com.srm.lostitemtracker.fileprovider", photoFile
        )
        takePhoto.launch(cameraImageUri!!)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 101 &&
            grantResults.isNotEmpty() &&
            grantResults[0] == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            launchCamera()
        }
    }

}   // ← class ends here