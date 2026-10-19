package com.progresodevida

import android.content.Context
import android.graphics.*
import android.os.SystemClock
import android.view.View
import kotlin.math.*

/** Procedural fire: no video, bitmap or emoji. Animation follows completed tasks. */
class FlameView(context: Context, private val progress: Float) : View(context) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val path = Path()
    private val started = SystemClock.uptimeMillis()

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawColor(Color.BLACK)
        val t = (SystemClock.uptimeMillis() - started) / 1000f
        val p = progress.coerceIn(0f, 1f)
        val intensity = ((exp(p * 2.2f) - 1f) / (exp(2.2f) - 1f))
        val h = height * (.12f + .77f * intensity)
        val base = height * .93f
        val center = width * .5f
        val spread = width * (.025f + .12f * intensity)
        paint.shader = RadialGradient(center, base, max(1f, spread * 2.5f), intArrayOf(Color.argb(120, 255, 100, 0), Color.TRANSPARENT), null, Shader.TileMode.CLAMP)
        canvas.drawOval(center - spread * 3, base - h * .6f, center + spread * 3, base + h * .2f, paint)
        // Overlapping translucent tongues create constantly changing luminous folds.
        for (i in 0 until 22) {
            val phase = i * 2.39996f
            val x = center + sin(phase) * spread * .65f
            val tongue = h * (.45f + .55f * ((sin(phase * 3f) + 1f) / 2f))
            val sway = sin(t * (2f + i % 3) + phase) * spread * .65f
            val tip = base - tongue * (.9f + .1f * sin(t * 4f + phase))
            val radius = spread * (.24f + .3f * ((cos(phase) + 1f) / 2f))
            path.reset()
            path.moveTo(x - radius, base)
            path.cubicTo(x - radius * 1.6f, base - tongue * .3f, x + sway - radius, tip + tongue * .4f, x + sway, tip)
            path.cubicTo(x + sway * .5f, tip + tongue * .4f, x + radius * 1.8f, base - tongue * .25f, x + radius, base)
            path.close()
            paint.shader = LinearGradient(x, base, x, tip,
                intArrayOf(Color.argb(185, 255, 244, 150), Color.argb(130, 255, 162, 15), Color.argb(75, 255, 52, 0), Color.TRANSPARENT),
                floatArrayOf(0f, .3f, .8f, 1f), Shader.TileMode.CLAMP)
            canvas.drawPath(path, paint)
        }
        paint.shader = null
        if (p > 0f) for (i in 0 until (6 + intensity * 24).toInt()) {
            val phase = i * 1.618f
            val age = (t * (.18f + (i % 4) * .035f) + phase) % 1f
            paint.color = Color.argb(((1f - age) * 210).toInt(), 255, 181, 55)
            val x = center + sin(phase * 9f + age * 3f) * spread * (1f + age)
            canvas.drawCircle(x, base - h * .25f - age * height * .85f, resources.displayMetrics.density * (1f - age) * 1.3f, paint)
        }
        if (isShown && windowVisibility == VISIBLE) postInvalidateDelayed(33L)
    }

    override fun onWindowVisibilityChanged(visibility: Int) {
        super.onWindowVisibilityChanged(visibility)
        if (visibility == VISIBLE) invalidate()
    }
}
