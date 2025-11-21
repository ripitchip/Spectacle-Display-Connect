package com.example.spectacle.ui.home

import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.example.spectacle.databinding.FragmentHomeBinding
import com.example.spectacle.util.PixelationTransformation
import com.google.android.material.snackbar.Snackbar
import com.yalantis.ucrop.UCrop
import java.io.File

class HomeFragment : Fragment() {

    companion object {
        private const val TAG = "HomeFragment"
    }

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val cropImageLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.let { data ->
                UCrop.getOutput(data)?.let { uri ->
                    loadAndSavePixelArt(uri)
                } ?: Log.e(TAG, "UCrop output URI is null")
            }
        } else {
            Log.w(TAG, "UCrop cancelled or failed")
        }
    }

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                startCrop(uri)
            } ?: Snackbar.make(binding.root, "Failed to get image URI", Snackbar.LENGTH_LONG).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)

        binding.selectImageButton.setOnClickListener { openGallery() }
        binding.pickImageFab.setOnClickListener { openGallery() }

        return binding.root
    }

    private fun startCrop(sourceUri: Uri) {
        val destinationUri = Uri.fromFile(
            File(requireContext().cacheDir, "cropped_${System.currentTimeMillis()}.jpg")
        )

        val options = UCrop.Options().apply {
            setCompressionQuality(100)
            setFreeStyleCropEnabled(false)
        }

        val cropIntent = UCrop.of(sourceUri, destinationUri)
            .withAspectRatio(1f, 1f)
            .withMaxResultSize(64, 64)
            .withOptions(options)
            .getIntent(requireContext())

        cropImageLauncher.launch(cropIntent)
    }

    /** Load image → Pixelate → Save to Pictures/Spectacle (MTP visible) */
    private fun loadAndSavePixelArt(uri: Uri) {
        Glide.with(this)
            .asBitmap()
            .load(uri)
            .override(64, 64) // ensure 64x64 bitmap
            .centerCrop()
            .into(object : CustomTarget<Bitmap>() {
                override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                    // Scale to 64x64 if needed
                    val pixelArtBitmap = Bitmap.createScaledBitmap(resource, 64, 64, false)

                    // Apply custom pixelation function (not Glide)
                    val pixelated = pixelateBitmap(pixelArtBitmap, blockSize = 1)

                    savePixelArt(pixelated)
                    binding.selectedImage.setImageBitmap(pixelated)
                    binding.imagePreviewCard.visibility = View.VISIBLE
                }

                override fun onLoadCleared(placeholder: Drawable?) {}
            })
    }

    /** Simple pixelation: blockSize = 1 means each pixel stays as is */
    private fun pixelateBitmap(bitmap: Bitmap, blockSize: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

        for (y in 0 until height step blockSize) {
            for (x in 0 until width step blockSize) {
                val pixel = bitmap.getPixel(x, y)
                for (dy in 0 until blockSize) {
                    for (dx in 0 until blockSize) {
                        if (x + dx < width && y + dy < height) {
                            result.setPixel(x + dx, y + dy, pixel)
                        }
                    }
                }
            }
        }
        return result
    }


    /** Save pixel art to MediaStore → Pictures/Spectacle (visible via MTP) */
    private fun savePixelArt(bitmap: Bitmap) {
        val filename = "pixel_art_${System.currentTimeMillis()}.png"

        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, filename)
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Spectacle")
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }

        val resolver = requireContext().contentResolver
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)

        if (uri != null) {
            resolver.openOutputStream(uri)?.use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }

            values.clear()
            values.put(MediaStore.Images.Media.IS_PENDING, 0)
            resolver.update(uri, values, null, null)

            Log.d(TAG, "Saved pixel art to Pictures/Spectacle → $uri")
            Snackbar.make(binding.root, "Saved to Pictures/Spectacle", Snackbar.LENGTH_LONG).show()
        } else {
            Log.e(TAG, "Failed to save image using MediaStore")
        }
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        pickImageLauncher.launch(intent)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
