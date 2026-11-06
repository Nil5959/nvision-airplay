package io.github.jqssun.airplay.service

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import io.github.jqssun.airplay.R

/**
 * The AirPlay code, shown over whatever is on the TV (not the whole app): a tvOS-style card that springs in,
 * digits that rise one by one, then "connected" and gone. Needs the overlay permission
 * (`appops set io.github.jqssun.airplay SYSTEM_ALERT_WINDOW allow`); without it [show] returns false.
 */
class PinOverlay(private val ctx: Context) {
    private val wm = ctx.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val main = Handler(Looper.getMainLooper())
    private var root: FrameLayout? = null
    private var digitsRow: LinearLayout? = null
    private var hint: TextView? = null
    private var ok: TextView? = null
    private val timeout = Runnable { hide() }

    val showing get() = root != null

    fun canShow(): Boolean = Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(ctx)

    /** Show [pin] for [room]. Main thread. Returns false when the overlay permission is missing. */
    fun show(pin: String, room: String): Boolean {
        if (!canShow()) return false
        hideNow()
        val w = ctx.resources.displayMetrics.widthPixels.toFloat()
        fun px(f: Float) = (w * f).toInt()
        val inter = ResourcesCompat.getFont(ctx, R.font.inter_extralight)

        val dim = FrameLayout(ctx).apply { setBackgroundColor(Color.argb(110, 0, 0, 0)); alpha = 0f }
        val card = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(px(.026f), px(.026f), px(.026f), px(.022f))
            background = GradientDrawable().apply {
                cornerRadius = w * .022f
                setColor(Color.argb(240, 44, 44, 46))          // tvOS dark material
                setStroke(1, Color.argb(30, 255, 255, 255))
            }
            elevation = w * .02f
            alpha = 0f; scaleX = .92f; scaleY = .92f; translationY = w * .02f
        }
        val head = LinearLayout(ctx).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        head.addView(ImageView(ctx).apply { setImageResource(R.drawable.ic_air_fg_on) }, LinearLayout.LayoutParams(px(.026f), px(.026f)))
        head.addView(TextView(ctx).apply {
            text = ctx.getString(R.string.pin_overlay_title, room)
            setTextColor(Color.WHITE); setTextSize(TypedValue.COMPLEX_UNIT_PX, w * .017f)
            paint.isFakeBoldText = true
            setPadding(px(.008f), 0, px(.008f), 0)
        })
        card.addView(head)

        val row = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER
            layoutDirection = View.LAYOUT_DIRECTION_LTR
            setPadding(0, px(.014f), 0, px(.012f))
        }
        pin.take(8).forEach { c ->
            val d = TextView(ctx).apply {
                text = c.toString(); gravity = Gravity.CENTER
                typeface = inter; setTextColor(Color.WHITE); setTextSize(TypedValue.COMPLEX_UNIT_PX, w * .052f)
                includeFontPadding = false
                background = GradientDrawable().apply { cornerRadius = w * .012f; setColor(Color.argb(26, 255, 255, 255)) }
                alpha = 0f; translationY = w * .016f; scaleX = .9f; scaleY = .9f
            }
            row.addView(d, LinearLayout.LayoutParams(px(.064f), px(.08f)).apply { marginStart = px(.0055f); marginEnd = px(.0055f) })
        }
        card.addView(row)
        val hintView = TextView(ctx).apply {
            text = ctx.getString(R.string.pin_overlay_hint)
            setTextColor(Color.argb(170, 235, 235, 245)); setTextSize(TypedValue.COMPLEX_UNIT_PX, w * .0135f)
        }
        card.addView(hintView)
        val okView = TextView(ctx).apply {
            text = ctx.getString(R.string.pin_overlay_ok)
            setTextColor(Color.rgb(48, 209, 88)); setTextSize(TypedValue.COMPLEX_UNIT_PX, w * .018f)
            paint.isFakeBoldText = true; visibility = View.GONE
            setPadding(0, px(.01f), 0, px(.004f))
        }
        card.addView(okView)

        val frame = FrameLayout(ctx)
        frame.addView(dim, FrameLayout.LayoutParams(-1, -1))
        frame.addView(card, FrameLayout.LayoutParams(px(.38f), -2, Gravity.CENTER))

        @Suppress("DEPRECATION")
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                   else WindowManager.LayoutParams.TYPE_PHONE
        val lp = WindowManager.LayoutParams(-1, -1, type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN, PixelFormat.TRANSLUCENT)
        try {
            wm.addView(frame, lp)
        } catch (e: Exception) {
            Log.w("PinOverlay", "addView failed", e); return false
        }
        root = frame; digitsRow = row; hint = hintView; ok = okView

        dim.animate().alpha(1f).setDuration(450).start()
        card.animate().alpha(1f).scaleX(1f).scaleY(1f).translationY(0f)
            .setInterpolator(OvershootInterpolator(1.2f)).setDuration(700).start()
        for (i in 0 until row.childCount) {
            row.getChildAt(i).animate().alpha(1f).translationY(0f).scaleX(1f).scaleY(1f)
                .setInterpolator(OvershootInterpolator(1.6f)).setStartDelay(280L + 80L * i).setDuration(620).start()
        }
        main.removeCallbacks(timeout)
        main.postDelayed(timeout, 120_000)
        return true
    }

    /** The iPhone was accepted: "connected", then fade away. Main thread. */
    fun success() {
        val row = digitsRow ?: return
        main.removeCallbacks(timeout)
        row.animate().alpha(0f).scaleX(.96f).scaleY(.96f).setDuration(300).setListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(a: Animator) {
                row.visibility = View.GONE; hint?.visibility = View.GONE
                ok?.apply { alpha = 0f; visibility = View.VISIBLE; animate().alpha(1f).setDuration(300).start() }
                main.postDelayed({ hide() }, 1400)
            }
        }).start()
    }

    /** Fade out and remove. Main thread. */
    fun hide() {
        val r = root ?: return
        main.removeCallbacks(timeout)
        r.animate().alpha(0f).setInterpolator(DecelerateInterpolator()).setDuration(400)
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(a: Animator) { if (root === r) hideNow() else removeView(r) }
            }).start()
    }

    private fun hideNow() {
        root?.let { removeView(it) }
        root = null; digitsRow = null; hint = null; ok = null
    }

    private fun removeView(v: View) { try { wm.removeView(v) } catch (_: Exception) {} }
}
