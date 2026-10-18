package com.progresodevida

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView

object UiKit {
    val bg = Color.parseColor("#F7F9FC")
    val surface = Color.WHITE
    val blue = Color.parseColor("#246BFD")
    val blueSoft = Color.parseColor("#EAF2FF")
    val ink = Color.parseColor("#111827")
    val muted = Color.parseColor("#6B7280")
    val line = Color.parseColor("#E6EAF0")
    val green = Color.parseColor("#20B15A")
    val greenSoft = Color.parseColor("#E9F8EF")
    val red = Color.parseColor("#FF4D6D")
    val redSoft = Color.parseColor("#FFF0F3")
    val orange = Color.parseColor("#FF9F1C")
    val orangeSoft = Color.parseColor("#FFF7E8")
    val purple = Color.parseColor("#8B5CF6")
    val yellow = Color.parseColor("#FFB020")

    fun Context.dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    fun rounded(context: Context, color: Int, radius: Int = 20, strokeColor: Int? = null, stroke: Int = 1): GradientDrawable =
        GradientDrawable().apply {
            setColor(color)
            cornerRadius = context.dp(radius).toFloat()
            if (strokeColor != null) setStroke(context.dp(stroke), strokeColor)
        }

    fun card(context: Context, padding: Int = 16): LinearLayout = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(context.dp(padding), context.dp(padding), context.dp(padding), context.dp(padding))
        background = rounded(context, surface, 20, line)
        elevation = context.dp(2).toFloat()
    }

    fun label(context: Context, value: String, size: Float = 14f, color: Int = ink, bold: Boolean = false): TextView =
        TextView(context).apply {
            text = value
            textSize = size
            setTextColor(color)
            if (bold) setTypeface(typeface, Typeface.BOLD)
        }

    fun title(context: Context, value: String, color: Int = ink, size: Float = 30f): TextView =
        label(context, value, size, color, true)

    fun section(context: Context, value: String): TextView = label(context, value, 18f, ink, true)

    fun pill(context: Context, text: String, fill: Int = blueSoft, textColor: Int = blue): TextView =
        label(context, text, 12f, textColor, true).apply {
            gravity = Gravity.CENTER
            setPadding(context.dp(12), context.dp(8), context.dp(12), context.dp(8))
            background = rounded(context, fill, 12)
        }

    fun spacer(context: Context, h: Int): View = View(context).apply {
        layoutParams = ViewGroup.LayoutParams(1, context.dp(h))
    }

    fun full(top: Int = 0, bottom: Int = 0): LinearLayout.LayoutParams =
        LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            topMargin = top
            bottomMargin = bottom
        }
}
