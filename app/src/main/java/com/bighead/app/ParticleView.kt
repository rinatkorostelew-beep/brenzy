package com.bighead.app

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * Subtle animated particle background — small glowing dots drifting slowly.
 */
class ParticleView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private data class Particle(
        var x: Float,
        var y: Float,
        val radius: Float,
        val speed: Float,
        val angle: Float,
        val alpha: Float
    )

    private val particles = mutableListOf<Particle>()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var initialized = false
    private var lastTime = System.currentTimeMillis()

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (!initialized && w > 0 && h > 0) {
            initialized = true
            repeat(60) {
                particles += Particle(
                    x      = Random.nextFloat() * w,
                    y      = Random.nextFloat() * h,
                    radius = Random.nextFloat() * 2f + 0.5f,
                    speed  = Random.nextFloat() * 0.4f + 0.1f,
                    angle  = Random.nextFloat() * Math.PI.toFloat() * 2f,
                    alpha  = Random.nextFloat() * 0.5f + 0.1f
                )
            }
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val now = System.currentTimeMillis()
        val dt  = (now - lastTime).coerceAtMost(50L)
        lastTime = now

        for (p in particles) {
            p.x += cos(p.angle) * p.speed * dt / 16f
            p.y += sin(p.angle) * p.speed * dt / 16f
            // Wrap around
            if (p.x < -10f) p.x = width + 10f
            if (p.x > width + 10f) p.x = -10f
            if (p.y < -10f) p.y = height + 10f
            if (p.y > height + 10f) p.y = -10f

            paint.color = ((p.alpha * 255).toInt() shl 24) or 0x7C4DFF
            canvas.drawCircle(p.x, p.y, p.radius, paint)
        }

        postInvalidateOnAnimation()
    }
}
