package com.painel.nexus

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import kotlin.math.abs

class FloatService : Service() {
    private lateinit var wm: WindowManager
    private lateinit var esp: EspView
    private lateinit var menu: LinearLayout
    private lateinit var menuLp: WindowManager.LayoutParams
    private lateinit var bubble: ImageView
    private lateinit var bubbleLp: WindowManager.LayoutParams

    private val overlayType: Int
        get() = if (Build.VERSION.SDK_INT >= 26) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                else WindowManager.LayoutParams.TYPE_PHONE

    override fun onBind(intent: Intent?): IBinder? = null

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

    private fun params(w: Int, h: Int, touch: Boolean, px: Int, py: Int): WindowManager.LayoutParams {
        var flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
        if (!touch) flags = flags or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
        return WindowManager.LayoutParams(w, h, overlayType, flags, PixelFormat.TRANSLUCENT).apply {
            gravity = Gravity.TOP or Gravity.START
            if (Build.VERSION.SDK_INT >= 28) {
                layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
            x = px
            y = py
        }
    }

    override fun onCreate() {
        super.onCreate()
        wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager

        esp = EspView(this)
        wm.addView(
            esp,
            params(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT, false, 0, 0)
        )

        buildMenu()
        buildBubble()
    }

    private fun buildMenu() {
        menu = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply {
                setColor(Color.BLACK)
                setStroke(dp(2), Theme.RED)
            }
            visibility = View.GONE
        }

        val title = TextView(this).apply {
            text = "NEXUS"
            setTextColor(Color.WHITE)
            textSize = 15f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setBackgroundColor(Theme.RED)
            setPadding(0, dp(8), 0, dp(8))
        }
        menu.addView(title, LinearLayout.LayoutParams(dp(260), LinearLayout.LayoutParams.WRAP_CONTENT))

        val header = TextView(this).apply {
            text = "ESP"
            setTextColor(Theme.RED)
            textSize = 13f
            typeface = Typeface.DEFAULT_BOLD
            setPadding(dp(12), dp(10), dp(12), dp(4))
        }
        menu.addView(header, LinearLayout.LayoutParams(dp(260), LinearLayout.LayoutParams.WRAP_CONTENT))

        check("ESP Linha", { Cfg.espLine }, { Cfg.espLine = it })
        check("ESP Caixa", { Cfg.espBox }, { Cfg.espBox = it })
        check("ESP Distância", { Cfg.espDist }, { Cfg.espDist = it })
        check("ESP Thumber", { Cfg.espThumber }, { Cfg.espThumber = it })
        check("Função exemplo", { Cfg.exampleFn }, { Cfg.exampleFn = it })

        val status = TextView(this).apply {
            text = if (Off.ready()) "offsets ok" else "offsets pendentes (// FILL em Offsets.kt)"
            setTextColor(Color.rgb(160, 160, 160))
            textSize = 11f
            setPadding(dp(12), dp(10), dp(12), dp(4))
        }
        menu.addView(status, LinearLayout.LayoutParams(dp(260), LinearLayout.LayoutParams.WRAP_CONTENT))

        menuLp = params(dp(260), WindowManager.LayoutParams.WRAP_CONTENT, true, dp(60), dp(120))
        wm.addView(menu, menuLp)
        dragOn(title, menuLp)
    }

    private fun check(label: String, get: () -> Boolean, set: (Boolean) -> Unit) {
        val cb = CheckBox(this).apply {
            text = label
            setTextColor(Color.WHITE)
            buttonTintList = ColorStateList.valueOf(Theme.RED)
            isChecked = get()
            setOnCheckedChangeListener { _, v -> set(v) }
        }
        val lp = LinearLayout.LayoutParams(dp(260), LinearLayout.LayoutParams.WRAP_CONTENT)
        lp.setMargins(dp(12), dp(4), dp(12), dp(4))
        menu.addView(cb, lp)
    }

    private fun buildBubble() {
        bubble = ImageView(this).apply {
            setImageResource(R.drawable.logo_painel)
            scaleType = ImageView.ScaleType.FIT_CENTER
        }
        bubbleLp = params(dp(56), dp(56), true, 0, dp(200))
        wm.addView(bubble, bubbleLp)
        dragOn(bubble, bubbleLp) { toggleMenu() }
    }

    private fun toggleMenu() {
        menu.visibility = if (menu.visibility == View.VISIBLE) View.GONE else View.VISIBLE
    }

    private fun dragOn(v: View, lp: WindowManager.LayoutParams, onTap: (() -> Unit)? = null) {
        var sx = 0
        var sy = 0
        var tx = 0f
        var ty = 0f
        var moved = false
        v.setOnTouchListener { _, e ->
            when (e.action) {
                MotionEvent.ACTION_DOWN -> {
                    sx = lp.x
                    sy = lp.y
                    tx = e.rawX
                    ty = e.rawY
                    moved = false
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (e.rawX - tx).toInt()
                    val dy = (e.rawY - ty).toInt()
                    if (abs(dx) > 8 || abs(dy) > 8) moved = true
                    lp.x = sx + dx
                    lp.y = sy + dy
                    wm.updateViewLayout(v, lp)
                }
                MotionEvent.ACTION_UP -> {
                    if (!moved) onTap?.invoke()
                }
            }
            true
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onDestroy() {
        try { wm.removeView(esp) } catch (e: Exception) { }
        try { wm.removeView(menu) } catch (e: Exception) { }
        try { wm.removeView(bubble) } catch (e: Exception) { }
        super.onDestroy()
    }
}
