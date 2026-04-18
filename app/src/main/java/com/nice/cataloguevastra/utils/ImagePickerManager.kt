package com.nice.cataloguevastra.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.result.ActivityResultCaller
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import java.io.File

class ImagePickerManager(
    private val caller: ActivityResultCaller,
    private val context: Context,
    private val onImagePicked: (Uri) -> Unit
) {

    private var imageUri: Uri? = null
    private var pendingOption: Option? = null

    private lateinit var cameraLauncher: ActivityResultLauncher<Uri>
    private lateinit var galleryLauncher: ActivityResultLauncher<String>
    private lateinit var permissionLauncher: ActivityResultLauncher<Array<String>>

    private enum class Option { CAMERA, GALLERY }

    init {
        setupLaunchers()
    }

    private fun setupLaunchers() {

        cameraLauncher = caller.registerForActivityResult(
            ActivityResultContracts.TakePicture()
        ) { success ->
            if (success && imageUri != null) onImagePicked(imageUri!!)
        }

        galleryLauncher = caller.registerForActivityResult(
            ActivityResultContracts.GetContent()
        ) { uri ->
            uri?.let { onImagePicked(it) }
        }

        permissionLauncher = caller.registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { perms ->
            pendingOption?.let { option ->
                val required = getRequiredPermissions(option)
                val allGranted = required.all {
                    perms[it] == true || ContextCompat.checkSelfPermission(
                        context,
                        it
                    ) == PackageManager.PERMISSION_GRANTED
                }

                if (allGranted) {
                    when (option) {
                        Option.CAMERA -> openCamera()
                        Option.GALLERY -> openGallery()
                    }
                } else {
                    Toast.makeText(context, "Permission denied", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    fun openPicker() {
        val options = arrayOf("Camera", "Gallery")

        AlertDialog.Builder(context)
            .setTitle("Select Option")
            .setItems(options) { _, which ->
                pendingOption = if (which == 0) Option.CAMERA else Option.GALLERY
                checkAndRequestPermissions(pendingOption!!)
            }
            .show()
    }

    private fun checkAndRequestPermissions(option: Option) {
        val required = getRequiredPermissions(option)

        if (required.isEmpty()) {
            when (option) {
                Option.CAMERA -> openCamera()
                Option.GALLERY -> openGallery()
            }
            return
        }

        val grantedAll = required.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }

        if (grantedAll) {
            when (option) {
                Option.CAMERA -> openCamera()
                Option.GALLERY -> openGallery()
            }
        } else {
            permissionLauncher.launch(required)
        }
    }

    private fun getRequiredPermissions(option: Option): Array<String> {
        return when (option) {
            Option.CAMERA -> arrayOf(Manifest.permission.CAMERA)
            Option.GALLERY -> emptyArray()
        }
    }

    private fun openCamera() {
        val file = File.createTempFile(
            "IMG_${System.currentTimeMillis()}",
            ".jpg",
            context.cacheDir
        )

        imageUri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            file
        )

        cameraLauncher.launch(imageUri!!)
    }

    private fun openGallery() {
        galleryLauncher.launch("image/*")
    }
}
//
//class ImagePickerManager(
//    private val activity: AppCompatActivity,
//    private val onImagePicked: (Uri) -> Unit
//) {
//
//    private var imageUri: Uri? = null
//    private var pendingOption: Option? = null
//
//    private lateinit var cameraLauncher: ActivityResultLauncher<Uri>
//    private lateinit var galleryLauncher: ActivityResultLauncher<String>
//    private lateinit var permissionLauncher: ActivityResultLauncher<Array<String>>
//
//    private enum class Option { CAMERA, GALLERY }
//
//    init {
//        setupLaunchers()
//    }
//
//    private fun setupLaunchers() {
//        cameraLauncher = activity.registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
//            if (success && imageUri != null) onImagePicked(imageUri!!)
//        }
//
//        galleryLauncher = activity.registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
//            uri?.let { onImagePicked(it) }
//        }
//
//        permissionLauncher = activity.registerForActivityResult(
//            ActivityResultContracts.RequestMultiplePermissions()
//        ) { perms ->
//
//            pendingOption?.let { option ->
//
//                val required = getRequiredPermissions(option)
//
//                val allGranted = required.all { perms[it] == true }
//
//                if (allGranted) {
//                    when (option) {
//                        Option.CAMERA -> openCamera()
//                        Option.GALLERY -> openGallery()
//                    }
//                } else {
//
//                    // 🔥 IMPORTANT FIX
//                    val permanentlyDenied = required.any {
//                        ContextCompat.checkSelfPermission(activity, it) != PackageManager.PERMISSION_GRANTED &&
//                                !ActivityCompat.shouldShowRequestPermissionRationale(activity, it)
//                    }
//
//                    if (permanentlyDenied) {
//                        showSettingsDialog()
//                    } else {
//                        // Normal deny → DO NOTHING
//                        Toast.makeText(activity, "Permission denied", Toast.LENGTH_SHORT).show()
//                    }
//                }
//            }
//        }
//    }
//
//    fun openPicker() {
//        val options = arrayOf("Camera", "Gallery")
//        AlertDialog.Builder(activity)
//            .setTitle("Select Option")
//            .setItems(options) { _, which ->
//                pendingOption = if (which == 0) Option.CAMERA else Option.GALLERY
//                checkAndRequestPermissions(pendingOption!!)
//            }
//            .show()
//    }
//
//    private fun checkAndRequestPermissions(option: Option) {
//        val required = getRequiredPermissions(option)
//        val grantedAll = required.all { ContextCompat.checkSelfPermission(activity, it) == PackageManager.PERMISSION_GRANTED }
//
//        if (grantedAll) {
//            // Permissions already granted, launch directly
//            when (option) {
//                Option.CAMERA -> openCamera()
//                Option.GALLERY -> openGallery()
//            }
//        } else {
//            // Request missing permissions
//            permissionLauncher.launch(required)
//        }
//    }
//
//    private fun getRequiredPermissions(option: Option): Array<String> {
//        return when (option) {
//            Option.CAMERA -> arrayOf(Manifest.permission.CAMERA)
//
//            Option.GALLERY -> arrayOf(
//                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
//                    Manifest.permission.READ_MEDIA_IMAGES
//                else
//                    Manifest.permission.READ_EXTERNAL_STORAGE
//            )
//        }
//    }
//
//    private fun openCamera() {
//        val file = File.createTempFile("IMG_${System.currentTimeMillis()}", ".jpg", activity.cacheDir)
//        imageUri = FileProvider.getUriForFile(activity, "${activity.packageName}.provider", file)
//        cameraLauncher.launch(imageUri!!)
//    }
//
//    private fun openGallery() {
//        galleryLauncher.launch("image/*")
//    }
//
//    private fun showSettingsDialog() {
//        AlertDialog.Builder(activity)
//            .setTitle("Permission Required")
//            .setMessage("Camera & Gallery permissions are required. Please enable from settings.")
//            .setPositiveButton("Open Settings") { _, _ ->
//                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
//                intent.data = Uri.fromParts("package", activity.packageName, null)
//                activity.startActivity(intent)
//            }
//            .setNegativeButton("Cancel", null)
//            .show()
//    }
//}
