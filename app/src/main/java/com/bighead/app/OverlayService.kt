package com.bighead.app

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.*
import android.view.animation.AccelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.SeekBar
import android.widget.TextView
import androidx.core.app.NotificationCompat

class OverlayService : Service() {

    private lateinit var wm: WindowManager
    private var rootView: View? = null
    private var circleView: CircleOverlayView? = null

    private var dragOffsetX = 0f
    private var dragOffsetY = 0f
    private var currentMode = Mode.BIGHEAD

    enum class Mode { BIGHEAD, DIGTER }

    companion object {
        const val CHANNEL_ID  = "overlay_channel"
        const val NOTIF_ID    = 1
        const val ACTION_STOP = "com.bighead.app.STOP_OVERLAY"

        fun start(context: Context) {
            val i = Intent(context, OverlayService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                context.startForegroundService(i)
            else
                context.startService(i)
        }

        fun stop(context: Context) {
            context.startService(
                Intent(context, OverlayService::class.java).apply { action = ACTION_STOP }
            )
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        createNotificationChannel()
        startForeground(NOTIF_ID, buildNotification())
        inflateOverlay()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) stopSelf()
        return START_STICKY
    }

    override fun onDestroy() {
        try { rootView?.let { wm.removeView(it) } } catch (_: Exception) {}
        try { circleView?.let { wm.removeView(it) } } catch (_: Exception) {}
        super.onDestroy()
    }

    // ── Inflate ───────────────────────────────────────────────────────────────

    private fun inflateOverlay() {
        val type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY

        // Окружность (фон, без касаний)
        circleView = CircleOverlayView(this).also { cv ->
            cv.visibility = View.INVISIBLE
            cv.alpha = 0f
            wm.addView(cv, WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                type,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                        or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT
            ))
        }

        // Панель
        rootView = LayoutInflater.from(this).inflate(R.layout.overlay_panel, null)
        val panelParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 40; y = 200
        }
        wm.addView(rootView, panelParams)

        // Анимация появления
        rootView!!.apply {
            alpha = 0f; scaleX = 0.8f; scaleY = 0.8f
            animate().alpha(1f).scaleX(1f).scaleY(1f)
                .setDuration(380).setInterpolator(OvershootInterpolator(1.3f)).start()
        }

        setupLogic(panelParams)
    }

    // ── Логика панели ─────────────────────────────────────────────────────────

    private fun setupLogic(params: WindowManager.LayoutParams) {
        val root          = rootView ?: return
        val dragHandle    = root.findViewById<View>(R.id.dragHandle)
        val btnBigHead    = root.findViewById<TextView>(R.id.btnBigHead)
        val btnDigter     = root.findViewById<TextView>(R.id.btnDigter)
        val bigheadCont   = root.findViewById<View>(R.id.bigheadSliderContainer)
        val digterCont    = root.findViewById<View>(R.id.digterSliderContainer)
        val seekDigter    = root.findViewById<SeekBar>(R.id.digterSlider)
        val btnClose      = root.findViewById<View>(R.id.btnOverlayClose)

        // Закрыть
        btnClose.setOnClickListener { stopSelf() }

        // Drag — только за dragHandle
        dragHandle.setOnTouchListener { _, ev ->
            when (ev.action) {
                MotionEvent.ACTION_DOWN -> {
                    dragOffsetX = ev.rawX - params.x
                    dragOffsetY = ev.rawY - params.y
                    root.animate().scaleX(1.04f).scaleY(1.04f).setDuration(100).start()
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    params.x = (ev.rawX - dragOffsetX).toInt().coerceAtLeast(0)
                    params.y = (ev.rawY - dragOffsetY).toInt().coerceAtLeast(0)
                    try { wm.updateViewLayout(root, params) } catch (_: Exception) {}
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    root.animate().scaleX(1f).scaleY(1f).setDuration(180)
                        .setInterpolator(OvershootInterpolator(2f)).start()
                    true
                }
                else -> false
            }
        }

        // Переключатель режимов
        btnBigHead.setOnClickListener {
            if (currentMode == Mode.BIGHEAD) return@setOnClickListener
            currentMode = Mode.BIGHEAD
            applyToggle(btnBigHead, btnDigter)
            revealView(bigheadCont)
            hideView(digterCont)
            hideCircle()
        }

        btnDigter.setOnClickListener {
            if (currentMode == Mode.DIGTER) return@setOnClickListener
            currentMode = Mode.DIGTER
            applyToggle(btnDigter, btnBigHead)
            revealView(digterCont)
            hideView(bigheadCont)
            showCircle()
        }

        // Ползунок размера окружности
        seekDigter.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar, p: Int, u: Boolean) {
                circleView?.setRadius(p)
            }
            override fun onStartTrackingTouch(sb: SeekBar) {}
            override fun onStopTrackingTouch(sb: SeekBar) {}
        })
        circleView?.setRadius(40)
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun applyToggle(on: TextView, off: TextView) {
        on.setBackgroundResource(R.drawable.toggle_selected)
        on.setTextColor(0xFFFFFFFF.toInt())
        off.setBackgroundResource(R.drawable.toggle_unselected)
        off.setTextColor(0xFF5C647E.toInt())
        on.animate().scaleX(1.06f).scaleY(1.06f).setDuration(80).withEndAction {
            on.animate().scaleX(1f).scaleY(1f).setDuration(140)
                .setInterpolator(OvershootInterpolator(2f)).start()
        }.start()
    }

    private fun revealView(v: View) {
        v.visibility = View.VISIBLE
        v.alpha = 0f; v.translationY = -16f
        v.animate().alpha(1f).translationY(0f).setDuration(280)
            .setInterpolator(AccelerateInterpolator(0.8f)).start()
    }

    private fun hideView(v: View) {
        v.animate().alpha(0f).translationY(-12f).setDuration(200)
            .setInterpolator(AccelerateInterpolator())
            .withEndAction { v.visibility = View.GONE }.start()
    }

    private fun showCircle() {
        circleView?.apply {
            visibility = View.VISIBLE
            scaleX = 0.3f; scaleY = 0.3f; alpha = 0f
            animate().alpha(1f).scaleX(1f).scaleY(1f)
                .setDuration(400).setInterpolator(OvershootInterpolator(1.1f)).start()
        }
    }

    private fun hideCircle() {
        circleView?.apply {
            animate().alpha(0f).scaleX(0.3f).scaleY(0.3f).setDuration(250)
                .setInterpolator(AccelerateInterpolator())
                .withEndAction { visibility = View.INVISIBLE }.start()
        }
    }

    // ── Уведомление ───────────────────────────────────────────────────────────

    private fun createNotificationChannel() {
        val ch = NotificationChannel(
            CHANNEL_ID, "BigHead Overlay",
            NotificationManager.IMPORTANCE_LOW
        ).apply { description = "Оверлей активен" }
        getSystemService(NotificationManager::class.java).createNotificationChannel(ch)
    }

    private fun buildNotification(): Notification {
        val stopPi = PendingIntent.getService(
            this, 0,
            Intent(this, OverlayService::class.java).apply { action = ACTION_STOP },
            PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("BigHead активен")
            .setContentText("Панель работает поверх приложений")
            .setSmallIcon(R.drawable.ic_launch)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .addAction(R.drawable.ic_close, "Остановить", stopPi)
            .build()
    }
}
