package com.example.spectacle.ui.widgets

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.example.spectacle.utils.PixelCanvas

/**
 * A custom View that displays a 64x64 PixelCanvas.
 * Starts empty and supports touch editing.
 */
class PixelCanvasView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : View(context, attrs, defStyle) {

    private val pixelCanvas = PixelCanvas(64, 64) // all pixels start as TRANSPARENT
    private val paint = Paint()
    private var pixelSize = 20f

    /** Whether user can edit pixels via touch */
    var editable = true

    /** Currently selected color for painting */
    var selectedColor: Int = Color.RED

    init {
        paint.style = Paint.Style.FILL
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        pixelSize = width / 64f
        for (x in 0 until 64) {
            for (y in 0 until 64) {
                paint.color = pixelCanvas.getPixel(x, y)
                canvas.drawRect(
                    x * pixelSize,
                    y * pixelSize,
                    (x + 1) * pixelSize,
                    (y + 1) * pixelSize,
                    paint
                )
            }
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!editable) return false
        if (event.action == MotionEvent.ACTION_DOWN || event.action == MotionEvent.ACTION_MOVE) {
            val px = (event.x / pixelSize).toInt()
            val py = (event.y / pixelSize).toInt()
            pixelCanvas.setPixel(px, py, selectedColor)
            invalidate()
            return true
        }
        return super.onTouchEvent(event)
    }

    /** Set a specific pixel programmatically */
    fun setPixel(x: Int, y: Int, color: Int) {
        pixelCanvas.setPixel(x, y, color)
        invalidate()
    }

    /** Get the current canvas as a Bitmap */
    fun getBitmap() = pixelCanvas.toBitmap()

    /** Clear all pixels */
    fun clear() {
        pixelCanvas.clear()
        invalidate()
    }
}
