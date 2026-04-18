package com.bighead.app

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import androidx.core.graphics.withSave
import kotlin.math.min

/**
 * Full-screen transparent overlay that draws a glowing circle in the centre.
 * Call [setRadius] (0–100) to animate the circle size.
 */
class CircleOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : View(context, attrs, defStyle) {

    // ── Radius ────────────────────────────────────────────────────────────────

    /** 0–100 mapped to minRadiusPx .. maxRadiusPx */
    private var radiusProgress: Int = 40
        set(value) {
            field = value.coerceIn(0, 100)
            updateTargetRadius()
        }

    private var currentRadiusPx = 0f
    private var targetRadiusPx  = 0f

    private val minRadiusPx get() = resources.displayMetrics.density * 40f
    private val maxRadiusPx get() = min(width, height) * 0.45f

    fun setRadius(progress: Int) {
        radiusProgress = progress
        // Animate to new target
        val animator = android.animation.ValueAnimator.ofFloat(currentRadiusPx, targetRadiusPx)
        animator.duration = 220
        animator.interpolator = android.view.animation.DecelerateInterpolator(1.5f)
        animator.addUpdateListener {
            currentRadiusPx = it.animatedValue as Float
            invalidate()
        }
        animator.start()
    }

    private fun updateTargetRadius() {
        val frac = radiusProgress / 100f
        targetRadiusPx = minRadiusPx + frac * (maxRadiusPx - minRadiusPx)
        if (currentRadiusPx == 0f) currentRadiusPx = targetRadiusPx
    }

    // ── Paints ────────────────────────────────────────────────────────────────

    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        xfermode = null
    }

    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 3f
        color = 0xFF00E5FF.toInt()
    }

    private val innerStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1.5f
        color = 0x80FFFFFF.toInt()
    }

    // Cross-hair paints
    private val crosshairPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1f
        color = 0x5500E5FF.toInt()
        pathEffect = DashPathEffect(floatArrayOf(8f, 6f), 0f)
    }

    // ── Draw ──────────────────────────────────────────────────────────────────

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        updateTargetRadius()
        currentRadiusPx = targetRadiusPx
        setupGlowShader()
    }

    private fun setupGlowShader() {
        val cx = width / 2f
        val cy = height / 2f
        val r  = currentRadiusPx
        glowPaint.shader = RadialGradient(
            cx, cy,
            r * 1.4f,
            intArrayOf(
                0x4400E5FF.toInt(),
                0x2200E5FF.toInt(),
                0x0000E5FF.toInt()
            ),
            floatArrayOf(0f, 0.6f, 1f),
            Shader.TileMode.CLAMP
        )
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (currentRadiusPx <= 0f) return

        val cx = width / 2f
        val cy = height / 2f
        val r  = currentRadiusPx

        // Rebuild shader when radius changes so it scales correctly
        glowPaint.shader = RadialGradient(
            cx, cy, r * 1.6f,
            intArrayOf(0x3300E5FF.toInt(), 0x1A00E5FF.toInt(), 0x0000E5FF.toInt()),
            floatArrayOf(0f, 0.55f, 1f),
            Shader.TileMode.CLAMP
        )

        // Outer glow fill
        canvas.drawCircle(cx, cy, r * 1.5f, glowPaint)

        // Main circle stroke (cyan)
        canvas.drawCircle(cx, cy, r, strokePaint)

        // Inner ring
        canvas.drawCircle(cx, cy, r * 0.75f, innerStrokePaint)

        // Cross-hair lines
        canvas.withSave {
            canvas.drawLine(cx - r * 1.2f, cy, cx + r * 1.2f, cy, crosshairPaint)
            canvas.drawLine(cx, cy - r * 1.2f, cx, cy + r * 1.2f, crosshairPaint)
        }

        // Dot in the centre
        val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF00E5FF.toInt() }
        canvas.drawCircle(cx, cy, 4f, dotPaint)
    }
}
