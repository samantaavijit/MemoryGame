package com.avijitsamanta.memorygame

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.text.Editable
import android.text.InputFilter
import android.text.TextWatcher
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.avijitsamanta.memorygame.adapter.ImagePickerAdopter
import com.avijitsamanta.memorygame.models.BoardSize
import com.avijitsamanta.memorygame.utils.*
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import java.io.ByteArrayOutputStream


class CreateActivity : AppCompatActivity() {

    private lateinit var rvImagePicker: RecyclerView
    private lateinit var etGameName: EditText
    private lateinit var btnSave: Button
    private lateinit var pbUploading: ProgressBar

    private lateinit var boardSize: BoardSize
    private lateinit var adapter: ImagePickerAdopter
    private var numImagesRequired: Int = -1
    private val chosenImageUris = mutableListOf<Uri>()
    private val storage = Firebase.storage
    private val db = Firebase.firestore

    companion object {
        private const val PICK_PHOTO_CODE = 655
        private const val READ_EXTERNAL_PHOTO_CODE = 248
        private const val READ_PHOTO_PERMISSION = android.Manifest.permission.READ_EXTERNAL_STORAGE
        private const val TAG = "CreateActivity"
        private const val MIN_GAME_NAME_LENGTH = 5
        private const val MAX_GAME_NAME_LENGTH = 10
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create)

        init()

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        boardSize = intent.getSerializableExtra(EXTRA_BOARD_SIZE) as BoardSize
        numImagesRequired = boardSize.getNumPairs()
        supportActionBar?.title = "Choose pics (0 / $numImagesRequired)"

        etGameName.filters =
            arrayOf(InputFilter.LengthFilter(MAX_GAME_NAME_LENGTH))  // set max length
        etGameName.addTextChangedListener(object : TextWatcher {
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun afterTextChanged(s: Editable?) {
                btnSave.isEnabled = shouldEnabledSaveButton()
            }
        })

        adapter = ImagePickerAdopter(
            this,
            chosenImageUris,
            boardSize,
            object : ImagePickerAdopter.ImageClickListener {
                override fun onPlaceholderClicked() {
                    if (isPermissionGranted(this@CreateActivity, READ_PHOTO_PERMISSION)) {
                        launchIntentForPhoto()
                    } else {
                        requestPermission(
                            this@CreateActivity, READ_PHOTO_PERMISSION,
                            READ_EXTERNAL_PHOTO_CODE
                        )
                    }
                }

            })
        rvImagePicker.adapter = adapter
        rvImagePicker.setHasFixedSize(true)
        rvImagePicker.layoutManager = GridLayoutManager(this, boardSize.getWidth())

        btnSave.setOnClickListener {
            saveDataToFirebase()
        }
    }

    /***
     * It is find all
     * variable that we declare
     */
    private fun init() {
        rvImagePicker = findViewById(R.id.rvImagePicker)
        etGameName = findViewById(R.id.etGameName)
        btnSave = findViewById(R.id.btnSave)
        pbUploading = findViewById(R.id.pbUploading)
    }

    /****
     * save images to firebase
     */
    private fun saveDataToFirebase() {
        Log.i(TAG, "saveDataToFirebase")
        btnSave.isEnabled = false
        val customGameName = etGameName.text.toString()
        // Check that we're not over writing someone else's data
        db.collection(COLLECTION_PATH).document(customGameName).get()
            .addOnSuccessListener { document ->
                if (document != null && document.data != null) { // Already exist
                    AlertDialog.Builder(this)
                        .setTitle("name Taken")
                        .setMessage("A game already exists with the name $customGameName. Please choose another")
                        .setPositiveButton("OK", null)
                        .show()
                    btnSave.isEnabled = true
                } else {
                    handleImageUploading(customGameName)
                }
            }.addOnFailureListener { exception ->
                Log.e(TAG, "Encountered error while saving memory game", exception)
                Toast.makeText(
                    this,
                    "Encountered error while saving memory game",
                    Toast.LENGTH_SHORT
                ).show()
                btnSave.isEnabled = true
            }

    }

    /**
     * upload all
     * images to firebase
     */
    private fun handleImageUploading(customGameName: String) {
        pbUploading.visibility = View.VISIBLE
        var didEncounterError = false
        val uploadedImageUrls = mutableListOf<String>()

        for ((index, photoUri) in chosenImageUris.withIndex()) {
            val imageByteArray = getImageByteArray(photoUri)
            val filepath = "images/$customGameName/${System.currentTimeMillis()}-${index}.jpg"
            val photoReference = storage.reference.child(filepath)
            photoReference.putBytes(imageByteArray)
                .continueWithTask { photoUploadTask ->
                    Log.i(TAG, "Upload bytes ${photoUploadTask.result?.bytesTransferred}")
                    photoReference.downloadUrl
                }.addOnCompleteListener { downloadUrlTask ->
                    if (!downloadUrlTask.isSuccessful) {
                        Log.e(TAG, "Exception with Firebase storage", downloadUrlTask.exception)
                        Toast.makeText(this, "Field to upload image", Toast.LENGTH_SHORT).show()
                        didEncounterError = true
                        return@addOnCompleteListener
                    }
                    if (didEncounterError) {
                        pbUploading.visibility = View.GONE
                        return@addOnCompleteListener
                    }
                    val downloadUrl = downloadUrlTask.result.toString()
                    uploadedImageUrls.add(downloadUrl)
                    pbUploading.progress = uploadedImageUrls.size * 100 / chosenImageUris.size
                    Log.i(TAG, "Finished upload $photoUri , num uploaded ${uploadedImageUrls.size}")
                    if (uploadedImageUrls.size == chosenImageUris.size) {
                        handleAllImagesUploadUrls(customGameName, uploadedImageUrls)
                    }
                }
        }
    }

    /**
     * store all image
     * urls in database
     */
    private fun handleAllImagesUploadUrls(gameName: String, imageUrls: MutableList<String>) {
        db.collection(COLLECTION_PATH).document(gameName)
            .set(mapOf("images" to imageUrls))
            .addOnCompleteListener { gameCreationTask ->
                pbUploading.visibility = View.GONE
                if (!gameCreationTask.isSuccessful) {
                    Log.e(TAG, "Exception with game creation", gameCreationTask.exception)
                    Toast.makeText(this, "Failed game creation", Toast.LENGTH_SHORT).show()
                    return@addOnCompleteListener
                }
                Log.i(TAG, "Successfully created game $gameName")
                AlertDialog.Builder(this)
                    .setTitle("Upload complete! Let's play your game '$gameName'")
                    .setPositiveButton("OK") { _, _ ->
                        val resultData = Intent()
                        resultData.putExtra(EXTRA_GAME_NAME, gameName)
                        setResult(Activity.RESULT_OK, resultData)
                        finish()
                    }.show()
            }
    }

    /***
     * it return image uri to
     * byte array
     */
    private fun getImageByteArray(photoUri: Uri): ByteArray {
        val originalBitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val source = ImageDecoder.createSource(contentResolver, photoUri)
            ImageDecoder.decodeBitmap(source)
        } else {
            MediaStore.Images.Media.getBitmap(contentResolver, photoUri)
        }
        Log.i(TAG, "Original width ${originalBitmap.width} and height ${originalBitmap.height}")
        val scaleBitmap = BitmapScalar.scaleToFitHeight(originalBitmap, 250)
        Log.i(TAG, "Scaled width ${scaleBitmap.width} and height ${scaleBitmap.height}")
        val byteOutputStream = ByteArrayOutputStream()
        scaleBitmap.compress(Bitmap.CompressFormat.JPEG, 60, byteOutputStream)
        return byteOutputStream.toByteArray()
    }

    /****
     * launch the intent
     * for choose the images
     */
    private fun launchIntentForPhoto() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        startActivityForResult(Intent.createChooser(intent, "Chose pics"), PICK_PHOTO_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != PICK_PHOTO_CODE || resultCode != Activity.RESULT_OK || data == null) {
            Log.w(
                TAG,
                "Did not get data back from the launched activity, user likely canceled flow ",
            )
            return
        }
        val selectedUri = data.data  // single image selected
        val clipData = data.clipData  // Multiple image selected

        if (clipData != null) {
            Log.i(TAG, "clipData numImages ${clipData.itemCount}: $clipData")
            for (i in 0 until clipData.itemCount) {
                val clipItem = clipData.getItemAt(i)
                if (chosenImageUris.size < numImagesRequired) {
                    chosenImageUris.add(clipItem.uri)
                }
            }
        } else if (selectedUri != null) {
            Log.i(TAG, "data: $selectedUri")
            chosenImageUris.add(selectedUri)
        }
        adapter.notifyDataSetChanged()
        supportActionBar?.title = "Choose pics (${chosenImageUris.size} / $numImagesRequired)"
        btnSave.isEnabled = shouldEnabledSaveButton()
    }

    /***
     * check the all images
     * is field and game game
     * is not empty
     */
    private fun shouldEnabledSaveButton(): Boolean {
        if (chosenImageUris.size != numImagesRequired) return false
        if (etGameName.text.isBlank() || etGameName.text.length < MIN_GAME_NAME_LENGTH) return false
        return true
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == READ_EXTERNAL_PHOTO_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                launchIntentForPhoto()
            } else {
                Toast.makeText(
                    this,
                    "In order to create a custom game, you need to provide access to your photos",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}