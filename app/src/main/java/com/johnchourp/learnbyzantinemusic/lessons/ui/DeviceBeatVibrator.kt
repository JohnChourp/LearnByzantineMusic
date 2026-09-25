package com.johnchourp.learnbyzantinemusic.lessons.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.annotation.RequiresApi

/**
 * The metronome's [BeatVibrator] on a real device (ClickUp `869f5x2d7`).
 *
 * Goes through the `Vibrator` itself — `VibratorManager.defaultVibrator` on 31+ — and plays what
 * [BeatVibration.choose] picks for this device. `VIBRATE` is a normal permission, granted at
 * install without a prompt. Should it ever be missing, [View.performHapticFeedback] on
 * [fallbackView] takes over: weaker, and subject to the touch-feedback setting, but it needs no
 * permission.
 *
 * Every call is guarded: a vibrator that throws must not take the lesson page down with it.
 */
class DeviceBeatVibrator(context: Context, private val fallbackView: View) : BeatVibrator {

    private val vibrator: Vibrator? = runCatching {
        if (Build.VERSION.SDK_INT >= 31) {
            context.getSystemService(VibratorManager::class.java)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }.getOrNull()

    override val available: Boolean = runCatching { vibrator?.hasVibrator() == true }.getOrDefault(false)

    private val amplitudeControl: Boolean = Build.VERSION.SDK_INT >= 26 && hasAmplitudeControl(vibrator)

    private var permitted: Boolean =
        context.checkSelfPermission(Manifest.permission.VIBRATE) == PackageManager.PERMISSION_GRANTED

    override fun pulse(strength: BeatStrength) {
        val vibrator = vibrator ?: return
        if (!available) return
        if (!permitted) {
            hapticFallback(strength)
            return
        }
        try {
            when (val plan = BeatVibration.choose(strength, Build.VERSION.SDK_INT, amplitudeControl)) {
                is BeatVibration.Legacy -> legacy(vibrator, plan.millis)
                else -> if (Build.VERSION.SDK_INT >= 26) effect(vibrator, plan)
            }
        } catch (_: SecurityException) {
            permitted = false
            hapticFallback(strength)
        } catch (_: RuntimeException) {
            // A vibrator service that fails is a missed pulse, never a crashed lesson.
        }
    }

    @RequiresApi(26)
    private fun hasAmplitudeControl(vibrator: Vibrator?): Boolean =
        runCatching { vibrator?.hasAmplitudeControl() == true }.getOrDefault(false)

    @Suppress("DEPRECATION")
    private fun legacy(vibrator: Vibrator, millis: Long) = vibrator.vibrate(millis)

    @RequiresApi(26)
    private fun effect(vibrator: Vibrator, plan: BeatVibration) {
        val effect = when (plan) {
            is BeatVibration.Amplitude -> VibrationEffect.createOneShot(plan.millis, plan.amplitude)
            is BeatVibration.OneShot -> VibrationEffect.createOneShot(plan.millis, VibrationEffect.DEFAULT_AMPLITUDE)
            is BeatVibration.Predefined -> if (Build.VERSION.SDK_INT >= 29) predefined(plan.effect) else return
            is BeatVibration.Legacy -> return
        }
        vibrator.vibrate(effect)
    }

    @RequiresApi(29)
    private fun predefined(effect: BeatVibration.Effect): VibrationEffect = VibrationEffect.createPredefined(
        when (effect) {
            BeatVibration.Effect.HEAVY_CLICK -> VibrationEffect.EFFECT_HEAVY_CLICK
            BeatVibration.Effect.TICK -> VibrationEffect.EFFECT_TICK
        },
    )

    private fun hapticFallback(strength: BeatStrength) {
        runCatching {
            fallbackView.performHapticFeedback(
                if (strength == BeatStrength.STRONG) HapticFeedbackConstants.LONG_PRESS else HapticFeedbackConstants.KEYBOARD_TAP,
            )
        }
    }
}
