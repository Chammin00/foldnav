package com.foldnav.app.map

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Shader
import com.foldnav.app.model.NavCursor

/**
 * Generates crisp north-aligned (0 deg) 160x160 ARGB_8888 navigation cursor bitmaps
 * rotated live on GPU by MapLibre's SymbolLayer bearing expression.
 */
object CursorBitmapFactory {
    fun buildBitmap(cursor: NavCursor): Bitmap {
        val size = 160
        val cx = size * 0.5f
        val cy = size * 0.5f
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)

        val primaryColor = Color.parseColor(cursor.primaryHex)
        val glowColor = Color.parseColor(cursor.glowHex)

        // Ambient glow halo under cursor
        val haloPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(
                cx, cy, 74f,
                intArrayOf(
                    Color.argb(150, Color.red(glowColor), Color.green(glowColor), Color.blue(glowColor)),
                    Color.argb(45, Color.red(glowColor), Color.green(glowColor), Color.blue(glowColor)),
                    Color.TRANSPARENT
                ),
                floatArrayOf(0f, 0.55f, 1f),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawCircle(cx, cy, 74f, haloPaint)

        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 3.5f
            strokeJoin = Paint.Join.ROUND
        }

        when (cursor) {
            NavCursor.HYPERCAR_GT -> {
                // Top-down futuristic supercar pointing North
                val body = Path().apply {
                    moveTo(cx, 22f)
                    cubicTo(cx + 26f, 26f, cx + 28f, 64f, cx + 26f, 128f)
                    lineTo(cx + 18f, 138f)
                    lineTo(cx - 18f, 138f)
                    lineTo(cx - 26f, 128f)
                    cubicTo(cx - 28f, 64f, cx - 26f, 26f, cx, 22f)
                    close()
                }
                fillPaint.color = Color.parseColor("#181D26")
                canvas.drawPath(body, fillPaint)
                strokePaint.color = primaryColor
                canvas.drawPath(body, strokePaint)

                // Cockpit canopy
                val glass = Path().apply {
                    moveTo(cx, 48f)
                    lineTo(cx + 15f, 70f)
                    lineTo(cx + 12f, 102f)
                    lineTo(cx - 12f, 102f)
                    lineTo(cx - 15f, 70f)
                    close()
                }
                fillPaint.color = glowColor
                canvas.drawPath(glass, fillPaint)

                // Headlight beams + rear diffuser strip
                fillPaint.color = Color.WHITE
                canvas.drawCircle(cx - 13f, 34f, 4.2f, fillPaint)
                canvas.drawCircle(cx + 13f, 34f, 4.2f, fillPaint)
                fillPaint.color = Color.parseColor("#FF2E63")
                canvas.drawRect(cx - 17f, 132f, cx + 17f, 137f, fillPaint)
            }

            NavCursor.SONIC_SPINDASH -> {
                // Electric cobalt orb with gold supersonic crown & twin speed fins
                val crest = Path().apply {
                    moveTo(cx, 16f)
                    lineTo(cx + 34f, 84f)
                    lineTo(cx + 14f, 118f)
                    lineTo(cx, 102f)
                    lineTo(cx - 14f, 118f)
                    lineTo(cx - 34f, 84f)
                    close()
                }
                fillPaint.color = primaryColor
                canvas.drawPath(crest, fillPaint)
                strokePaint.color = glowColor
                canvas.drawPath(crest, strokePaint)

                fillPaint.color = Color.parseColor("#0A1936")
                canvas.drawCircle(cx, cy + 4f, 22f, fillPaint)
                strokePaint.color = glowColor
                canvas.drawCircle(cx, cy + 4f, 22f, strokePaint)

                // Forward direction chevron
                val chevron = Path().apply {
                    moveTo(cx, 42f)
                    lineTo(cx + 14f, 72f)
                    lineTo(cx, 64f)
                    lineTo(cx - 14f, 72f)
                    close()
                }
                fillPaint.color = glowColor
                canvas.drawPath(chevron, fillPaint)
            }

            NavCursor.SHADOW_RIDER -> {
                // Aggressive swept-back obsidian blade + crimson Chaos core
                val wing = Path().apply {
                    moveTo(cx, 18f)
                    lineTo(cx + 42f, 128f)
                    lineTo(cx + 14f, 110f)
                    lineTo(cx, 136f)
                    lineTo(cx - 14f, 110f)
                    lineTo(cx - 42f, 128f)
                    close()
                }
                fillPaint.color = Color.parseColor("#11090E")
                canvas.drawPath(wing, fillPaint)
                strokePaint.color = primaryColor
                canvas.drawPath(wing, strokePaint)

                val core = Path().apply {
                    moveTo(cx, 38f)
                    lineTo(cx + 14f, 92f)
                    lineTo(cx, 82f)
                    lineTo(cx - 14f, 92f)
                    close()
                }
                fillPaint.color = primaryColor
                canvas.drawPath(core, fillPaint)
                fillPaint.color = glowColor
                canvas.drawCircle(cx, 74f, 5.5f, fillPaint)
            }

            NavCursor.STEALTH_JET -> {
                // F-22 stealth delta silhouette with dual cyan thrusters
                val jet = Path().apply {
                    moveTo(cx, 16f)
                    lineTo(cx + 14f, 54f)
                    lineTo(cx + 48f, 98f)
                    lineTo(cx + 42f, 114f)
                    lineTo(cx + 16f, 108f)
                    lineTo(cx + 22f, 132f)
                    lineTo(cx + 6f, 124f)
                    lineTo(cx, 134f)
                    lineTo(cx - 6f, 124f)
                    lineTo(cx - 22f, 132f)
                    lineTo(cx - 16f, 108f)
                    lineTo(cx - 42f, 114f)
                    lineTo(cx - 48f, 98f)
                    lineTo(cx - 14f, 54f)
                    close()
                }
                fillPaint.color = Color.parseColor("#1E293B")
                canvas.drawPath(jet, fillPaint)
                strokePaint.color = glowColor
                canvas.drawPath(jet, strokePaint)

                fillPaint.color = glowColor
                canvas.drawCircle(cx - 8f, 134f, 5f, fillPaint)
                canvas.drawCircle(cx + 8f, 134f, 5f, fillPaint)
            }

            NavCursor.TITANIUM_ARROW -> {
                val arrow = Path().apply {
                    moveTo(cx, 20f)
                    lineTo(cx + 38f, 130f)
                    lineTo(cx, 104f)
                    lineTo(cx - 38f, 130f)
                    close()
                }
                fillPaint.color = primaryColor
                canvas.drawPath(arrow, fillPaint)
                strokePaint.color = glowColor
                strokePaint.strokeWidth = 5f
                canvas.drawPath(arrow, strokePaint)
            }
        }

        return bmp
    }
}
