package com.example.spectacle.utils

import android.graphics.Bitmap
import android.graphics.Color

/**
 * Represents a 64x64 (or configurable) pixel canvas.
 * Can be used to set/get pixel colors and generate a Bitmap.
 */
class PixelCanvas(val width: Int = 64, val height: Int = 64) {

    // 2D array storing pixel colors
    private val pixels = Array(width) { Array(height) { Color.TRANSPARENT } }

    // Set a pixel color
    fun setPixel(x: Int, y: Int, color: Int) {
        if (x in 0 until width && y in 0 until height) {
            pixels[x][y] = color
        }
    }

    // Get a pixel color
    fun getPixel(x: Int, y: Int): Int {
        return if (x in 0 until width && y in 0 until height) {
            pixels[x][y]
        } else Color.TRANSPARENT
    }

    // Convert the pixel grid to a Bitmap
    fun toBitmap(): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        for (x in 0 until width) {
            for (y in 0 until height) {
                bitmap.setPixel(x, y, pixels[x][y])
            }
        }
        return bitmap
    }

    // Clear all pixels
    fun clear() {
        for (x in 0 until width) {
            for (y in 0 until height) {
                pixels[x][y] = Color.TRANSPARENT
            }
        }
    }
}
