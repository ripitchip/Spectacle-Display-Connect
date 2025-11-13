package com.example.spectacle.ui.home

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.spectacle.databinding.FragmentHomeBinding
import com.google.android.material.snackbar.Snackbar

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private var selectedImageUri: Uri? = null

    // Image Picker
    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val imgUri = result.data?.data
            if (imgUri != null) {
                selectedImageUri = imgUri
                binding.selectedImage.setImageURI(imgUri)
            }
        }
    }

    // Permission Request (Android 14+)
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            openGallery()
        } else {
            Snackbar.make(binding.root, "Permission denied.", Snackbar.LENGTH_LONG).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val homeViewModel = ViewModelProvider(this).get(HomeViewModel::class.java)

        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root = binding.root

        // Header text
        val textView: TextView = binding.textHome
        homeViewModel.text.observe(viewLifecycleOwner) { textView.text = it }

        // Image pick buttons
        binding.selectImage.setOnClickListener { checkAndRequestPermission() }
        binding.pickImage.setOnClickListener { checkAndRequestPermission() }

        return root
    }

    private fun checkAndRequestPermission() {
        val permission = Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
        val granted = ContextCompat.checkSelfPermission(
            requireContext(),
            permission
        ) == PackageManager.PERMISSION_GRANTED

        if (granted) {
            openGallery()
        } else {
            permissionLauncher.launch(permission)
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
