package com.bighead.app

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.animation.OvershootInterpolator
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.AppCompatActivity
import com.bighead.app.databinding.ActivityKeyBinding

class KeyActivity : AppCompatActivity() {

    private lateinit var binding: ActivityKeyBinding

    // ── Замени на свой Telegram ──────────────────────────────────────────────
    private val TELEGRAM_URL = "https://t.me/ezbal"

    // Список валидных ключей — добавляй свои
    private val VALID_KEYS = setOf(
        "BIGHEAD-2024",
        "BIGHEAD-PRO",
        "DIGTER-ACCESS"
        // добавляй любые ключи сюда
    )

    private val PREF_NAME  = "bighead_prefs"
    private val PREF_KEY   = "activated"

    // ────────────────────────────────────────────────────────────────────────

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Если ключ уже введён — сразу в главный экран
        if (isActivated()) {
            goToMain()
            return
        }

        binding = ActivityKeyBinding.inflate(layoutInflater)
        setContentView(binding.root)

        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN

        animateEntrance()
        setupButtons()
    }

    // ── Анимация появления ───────────────────────────────────────────────────

    private fun animateEntrance() {
        val views = listOf(
            binding.ivLogo,
            binding.tvTitle,
            binding.tvSubtitle,
            binding.cardKey,
            binding.btnActivate,
            binding.btnTelegram
        )
        views.forEachIndexed { i, v ->
            v.alpha = 0f
            v.translationY = 40f
            v.animate()
                .alpha(1f)
                .translationY(0f)
                .setStartDelay(120L * i)
                .setDuration(420)
                .setInterpolator(OvershootInterpolator(1.1f))
                .start()
        }
    }

    // ── Кнопки ───────────────────────────────────────────────────────────────

    private fun setupButtons() {

        // Кнопка «Активировать»
        binding.btnActivate.setOnClickListener {
            val input = binding.etKey.text.toString().trim().uppercase()
            hideKeyboard()

            if (input.isEmpty()) {
                shakeField()
                showError("Введи ключ")
                return@setOnClickListener
            }

            if (input in VALID_KEYS) {
                saveActivated()
                showSuccess()
                binding.root.postDelayed({ goToMain() }, 900)
            } else {
                shakeField()
                showError("Неверный ключ")
            }
        }

        // Кнопка Telegram
        binding.btnTelegram.setOnClickListener {
            binding.btnTelegram.animate()
                .scaleX(0.93f).scaleY(0.93f).setDuration(80)
                .withEndAction {
                    binding.btnTelegram.animate().scaleX(1f).scaleY(1f)
                        .setDuration(150).setInterpolator(OvershootInterpolator(2f)).start()
                }.start()
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(TELEGRAM_URL)))
        }
    }

    // ── Анимация ошибки (тряска поля) ────────────────────────────────────────

    private fun shakeField() {
        val shake = android.view.animation.AnimationUtils
            .loadAnimation(this, R.anim.shake)
        binding.cardKey.startAnimation(shake)
    }

    private fun showError(msg: String) {
        binding.tvError.text = msg
        binding.tvError.alpha = 0f
        binding.tvError.visibility = View.VISIBLE
        binding.tvError.animate().alpha(1f).setDuration(220).start()
    }

    private fun showSuccess() {
        binding.tvError.setTextColor(getColor(R.color.accent_cyan))
        binding.tvError.text = "✓ Активировано!"
        binding.tvError.alpha = 0f
        binding.tvError.visibility = View.VISIBLE
        binding.tvError.animate().alpha(1f).setDuration(220).start()
        binding.cardKey.animate().scaleX(1.03f).scaleY(1.03f).setDuration(120)
            .withEndAction {
                binding.cardKey.animate().scaleX(1f).scaleY(1f)
                    .setDuration(200).setInterpolator(OvershootInterpolator(2f)).start()
            }.start()
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun isActivated(): Boolean =
        getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).getBoolean(PREF_KEY, false)

    private fun saveActivated() =
        getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).edit()
            .putBoolean(PREF_KEY, true).apply()

    private fun goToMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    private fun hideKeyboard() {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(binding.etKey.windowToken, 0)
    }
}
