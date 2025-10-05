package com.example.spectacle.ui.home

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.spectacle.databinding.FragmentHomeBinding
import com.example.spectacle.ui.widgets.PixelCanvasView

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val homeViewModel = ViewModelProvider(this).get(HomeViewModel::class.java)

        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root: View = binding.root

        // Observe ViewModel text
        val textView: TextView = binding.textHome
        homeViewModel.text.observe(viewLifecycleOwner) {
            textView.text = it
        }

        // Configure PixelCanvasView
        val pixelCanvasView: PixelCanvasView = binding.pixelCanvasView
        pixelCanvasView.editable = true
        pixelCanvasView.selectedColor = Color.RED

        // Example: Draw a diagonal line on load
        for (i in 0 until 64) {
            pixelCanvasView.setPixel(i, i, Color.BLUE)
        }

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
