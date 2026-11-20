package com.painel.nexus

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.widget.SwitchCompat
import androidx.core.app.NotificationCompat
import kotlin.math.abs

// flutuante: bolha + menu + overlay da ESP. tudo que mexe em janela e protegido,
// o servico roda em foreground pro Android nao matar quando o app sai da tela.
class FloatService : Service() {
    private lateinit var wm: WindowManager
    private lateinit var esp: EspView
    private lateinit var menu: LinearLayout
    private lateinit var menuLp: WindowManager.LayoutParams
    private lateinit var bubble: ImageView
    private lateinit var statusView: TextView

    private val ui = Handler(Looper.getMainLooper())
    private var menuOpen = false
    private var started = false

    private val statusTick = object : Runnable {
        override fun run() {
            refreshStatus()
            if (menuOpen) ui.postDelayed(this, 500)
        }
    }

    private val overlayType: Int
        get() = if (Build.VERSION.SDK_INT >= 26) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        else WindowManager.LayoutParams.TYPE_PHONE

    override fun onBind(intent: Intent?): IBinder? = null

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()
    private fun dpf(v: Float): Float = v * resources.displayMetrics.density

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

    private fun addWindow(v: View, lp: WindowManager.LayoutParams) {
        try { wm.addView(v, lp) } catch (e: Exception) { }
    }

    private fun updateWindow(v: View, lp: WindowManager.LayoutParams) {
        try { wm.updateViewLayout(v, lp) } catch (e: Exception) { }
    }

    private fun removeWindow(v: View) {
        try { wm.removeView(v) } catch (e: Exception) { }
    }

    override fun onCreate() {
        super.onCreate()
        if (!Settings.canDrawOverlays(this)) {
            stopSelf()
            return
        }
        wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        startForegroundNotice()

        esp = EspView(this)
        addWindow(esp, params(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT, false, 0, 0))

        buildMenu()
        buildBubble()
        started = true
    }

    private fun startForegroundNotice() {
        if (Build.VERSION.SDK_INT >= 26) {
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL, "Painel Nexus", NotificationManager.IMPORTANCE_LOW)
            )
        }
        val n: Notification = NotificationCompat.Builder(this, CHANNEL)
            .setSmallIcon(R.drawable.logo_painel)
            .setContentTitle("Painel Nexus")
            .setContentText("flutuante ativo")
            .setOngoing(true)
            .build()
        try { startForeground(NOTIF_ID, n) } catch (e: Exception) { }
    }

    private fun buildMenu() {
        menu = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = panel(Theme.PANEL, dp(18), Theme.RED, dp(1))
            setPadding(0, 0, 0, dp(12))
            visibility = View.GONE
        }
        val head = header()
        menu.addView(head, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))

        section("ESP")
        row("ESP Linha", { Cfg.espLine }, { Cfg.espLine = it })
        row("ESP Caixa", { Cfg.espBox }, { Cfg.espBox = it })
        row("ESP Distância", { Cfg.espDist }, { Cfg.espDist = it })
        row("ESP Thumber", { Cfg.espThumber }, { Cfg.espThumber = it })

        section("EXTRA")
        row("Função exemplo", { Cfg.exampleFn }, { Cfg.exampleFn = it })

        section("TESTE")
        row("Modo demo (sem o jogo)", { Cfg.demoOn }, { Cfg.demoOn = it })

        statusView = TextView(this).apply {
            setTextColor(Color.rgb(150, 150, 150))
            textSize = 11f
            setPadding(dp(16), dp(12), dp(16), dp(2))
        }
        menu.addView(statusView, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))

        menuLp = params(dp(272), ViewGroup.LayoutParams.WRAP_CONTENT, true, dp(24), dp(110))
        addWindow(menu, menuLp)
        dragOn(head, menuLp)
    }

    private fun header(): View {
        val h = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            val r = dpf(18f)
            background = GradientDrawable().apply {
                setColor(Theme.RED)
                cornerRadii = floatArrayOf(r, r, r, r, 0f, 0f, 0f, 0f)
            }
            setPadding(dp(12), dp(10), dp(4), dp(10))
        }
        h.addView(
            ImageView(this).apply { setImageResource(R.drawable.logo_painel) },
            LinearLayout.LayoutParams(dp(26), dp(26))
        )
        h.addView(
            TextView(this).apply {
                text = "PAINEL NEXUS"
                setTextColor(Color.WHITE)
                textSize = 14f
                typeface = Typeface.DEFAULT_BOLD
                letterSpacing = 0.12f
            },
            LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply { marginStart = dp(8) }
        )
        h.addView(
            TextView(this).apply {
                text = "×"
                setTextColor(Color.WHITE)
                textSize = 22f
                setPadding(dp(10), 0, dp(10), 0)
                setOnClickListener { setMenu(false) }
            }
        )
        return h
    }

    private fun section(title: String) {
        menu.addView(
            TextView(this).apply {
                text = title
                setTextColor(Theme.RED)
                textSize = 11f
                typeface = Typeface.DEFAULT_BOLD
                letterSpacing = 0.15f
                setPadding(dp(16), dp(12), dp(16), dp(6))
            },
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        )
    }

    private fun row(title: String, getV: () -> Boolean, setV: (Boolean) -> Unit) {
        val r = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = panel(Theme.ROW, dp(12), Color.TRANSPARENT, 0)
            setPadding(dp(14), dp(6), dp(8), dp(6))
        }
        val label = TextView(this).apply {
            text = title
            setTextColor(Color.WHITE)
            textSize = 15f
        }
        val sw = SwitchCompat(this).apply {
            isChecked = getV()
            thumbTintList = ColorStateList(
                arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf()),
                intArrayOf(Theme.RED, Color.rgb(120, 120, 120))
            )
            trackTintList = ColorStateList(
                arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf()),
                intArrayOf(Color.argb(120, 255, 26, 26), Color.rgb(50, 50, 50))
            )
            setOnCheckedChangeListener { _, v -> setV(v) }
        }
        r.setOnClickListener { sw.toggle() }
        r.addView(label, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        r.addView(sw)
        menu.addView(
            r,
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                .apply { setMargins(dp(12), dp(4), dp(12), dp(4)) }
        )
    }

    private fun buildBubble() {
        bubble = ImageView(this).apply {
            setImageResource(R.drawable.logo_painel)
            scaleType = ImageView.ScaleType.FIT_CENTER
            background = panel(Color.BLACK, dp(30), Theme.RED, dp(2))
            setPadding(dp(5), dp(5), dp(5), dp(5))
        }
        val bubbleLp = params(dp(60), dp(60), true, 0, dp(220))
        addWindow(bubble, bubbleLp)
        dragOn(bubble, bubbleLp) { setMenu(!menuOpen) }
    }

    private fun setMenu(open: Boolean) {
        menuOpen = open
        menu.visibility = if (open) View.VISIBLE else View.GONE
        if (open) {
            refreshStatus()
            ui.removeCallbacks(statusTick)
            ui.post(statusTick)
        } else {
            ui.removeCallbacks(statusTick)
        }
        updateWindow(menu, menuLp)
    }

    private fun refreshStatus() {
        val base = if (Cfg.demoOn) "modo demo ativo" else Cfg.vpnStatus
        statusView.text = "$base · na frente: ${Cfg.frontCount}"
    }

    private fun panel(fill: Int, radius: Int, stroke: Int, strokeW: Int): GradientDrawable =
        GradientDrawable().apply {
            setColor(fill)
            cornerRadius = radius.toFloat()
            if (strokeW > 0) setStroke(strokeW, stroke)
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
                    updateWindow(v, lp)
                }
                MotionEvent.ACTION_UP -> {
                    if (!moved) onTap?.invoke()
                }
            }
            true
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!started) {
            stopSelf()
            return START_NOT_STICKY
        }
        return START_STICKY
    }

    override fun onDestroy() {
        ui.removeCallbacks(statusTick)
        menuOpen = false
        if (started) {
            removeWindow(esp)
            removeWindow(menu)
            removeWindow(bubble)
        }
        started = false
        try { stopForeground(STOP_FOREGROUND_REMOVE) } catch (e: Exception) { }
        super.onDestroy()
    }

    companion object {
        const val CHANNEL = "painel"
        const val NOTIF_ID = 1
    }
}
