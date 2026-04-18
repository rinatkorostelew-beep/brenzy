package com.bighead.app

import android.app.ActivityManager
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.view.animation.OvershootInterpolator
import androidx.appcompat.app.AppCompatActivity
import com.bighead.app.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val REQUEST_OVERLAY = 1001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN

        animateEntrance()
        setupButtons()
    }

    override fun onResume() {
        super.onResume()
        updateButtonState()
    }

    private fun animateEntrance() {
        val views = listOf(binding.ivMainLogo, binding.tvMainTitle,
            binding.tvMainSub, binding.btnLaunch, binding.btnLogout)
        views.forEachIndexed { i, v ->
            v.alpha = 0f
            v.translationY = 30f
            v.animate().alpha(1f).translationY(0f)
                .setStartDelay(100L * i).setDuration(400)
                .setInterpolator(OvershootInterpolator(1.1f)).start()
        }
    }

    private fun updateButtonState() {
        binding.btnLaunch.text =
            if (isServiceRunning()) "ОСТАНОВИТЬ ОВЕРЛЕЙ" else "ЗАПУСТИТЬ ОВЕРЛЕЙ"
    }

    private fun setupButtons() {
        binding.btnLaunch.setOnClickListener {
            // Анимация нажатия
            it.animate().scaleX(0.93f).scaleY(0.93f).setDuration(80).withEndAction {
                it.animate().scaleX(1f).scaleY(1f).setDuration(160)
                    .setInterpolator(OvershootInterpolator(2f)).start()
            }.start()

            if (isServiceRunning()) {
                // Остановить
                OverlayService.stop(this)
                updateButtonState()
            } else {
                // Запустить — сначала проверить разрешение
                if (!Settings.canDrawOverlays(this)) {
                    // Открываем настройки разрешения
                    startActivityForResult(
                        Intent(
                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:$packageName")
                        ),
                        REQUEST_OVERLAY
                    )
                } else {
                    startOverlay()
                }
            }
        }

        binding.btnLogout.setOnClickListener {
            OverlayService.stop(this)
            getSharedPreferences("bighead_prefs", MODE_PRIVATE)
                .edit().clear().apply()
            startActivity(Intent(this, KeyActivity::class.java))
            finish()
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_OVERLAY) {
            if (Settings.canDrawOverlays(this)) {
                startOverlay()
            } else {
                // Пользователь отказал в разрешении
                binding.btnLaunch.text = "Разрешение не выдано"
            }
        }
    }

    private fun startOverlay() {
        OverlayService.start(this)
        updateButtonState()
    }

    private fun isServiceRunning(): Boolean {
        return try {
            val am = getSystemService(ACTIVITY_SERVICE) as ActivityManager
            @Suppress("DEPRECATION")
            am.getRunningServices(Int.MAX_VALUE)
                .any { it.service.className == OverlayService::class.java.name }
        } catch (e: Exception) {
            false
        }
    }
}
