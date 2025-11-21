package com.example.spectacle.util

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation
import java.security.MessageDigest

class PixelationTransformation(private val pixelSize: Int = 4) : BitmapTransformation() {

    override fun updateDiskCacheKey(messageDigest: MessageDigest) {
        messageDigest.update("pixelation_transformation_$pixelSize".toByteArray())
    }

    override fun transform(
        pool: BitmapPool,
        toTransform: Bitmap,
        outWidth: Int,
        outHeight: Int
    ): Bitmap {
        val width = toTransform.width
        val height = toTransform.height

        val result = pool.get(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { isFilterBitmap = false }

        // Scale down
        val small = Bitmap.createScaledBitmap(
            toTransform,
            width / pixelSize,
            height / pixelSize,
            false
        )

        // Scale up (pixelated)
        val scaledUp = Bitmap.createScaledBitmap(small, width, height, false)
        canvas.drawBitmap(scaledUp, 0f, 0f, paint)

        return result
    }
}
