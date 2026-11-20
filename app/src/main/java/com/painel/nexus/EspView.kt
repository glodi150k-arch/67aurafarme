package com.painel.nexus

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PointF
import android.graphics.Typeface
import android.view.View
import kotlin.math.sqrt

// ESP desenhada por cima do jogo (overlay fullscreen, nao toca em nada).
// projecao estilo Unity: matriz coluna-major view*proj, origem bottom-left, Y invertido pra tela.
// altura do player: 1.8 unidades (corpo em pe). cada elemento sai com contorno preto pra ler em qualquer fundo.
class EspView(c: Context) : View(c) {

    private val fillLine = stroke(Color.WHITE, 2.5f)
    private val edgeLine = stroke(Color.BLACK, 4.5f)
    private val fillBox = stroke(Color.WHITE, 2f)
    private val edgeBox = stroke(Color.BLACK, 4f)

    private val txt = text(Color.WHITE, 26f, false)
    private val txtEdge = text(Color.BLACK, 26f, false).apply {
        style = Paint.Style.STROKE
        strokeWidth = 5f
    }

    private val big = text(Color.WHITE, 110f, true)
    private val bigEdge = text(Color.BLACK, 110f, true).apply {
        style = Paint.Style.STROKE
        strokeWidth = 12f
    }

    override fun onSizeChanged(w: Int, h: Int, ow: Int, oh: Int) {
        super.onSizeChanged(w, h, ow, oh)
        if (h > 0) Cfg.aspect = w.toFloat() / h
    }

    override fun onDraw(cv: Canvas) {
        super.onDraw(cv)
        val w = width.toFloat()
        val h = height.toFloat()
        if (Cfg.demoOn) Cfg.snap = Demo.snap(System.nanoTime(), Cfg.aspect)
        val s = Cfg.snap
        var front = 0

        if (s.count > 0 && s.matrix.any { it != 0f }) {
            val hasLocal = s.local.any { it != 0f }
            val origin = PointF(w / 2f, h)

            for (i in 0 until s.count) {
                val x = s.players[i * 3]
                val y = s.players[i * 3 + 1]
                val z = s.players[i * 3 + 2]

                val head = project(s.matrix, x, y, z + HEAD_H, w, h) ?: continue
                val feet = project(s.matrix, x, y, z, w, h) ?: continue
                val bh = feet.y - head.y
                if (bh < 4f) continue
                val bw = bh * 0.5f
                val cx = feet.x
                val l = cx - bw / 2f
                val r = cx + bw / 2f
                val t = head.y
                val b = feet.y

                if (r < 0f || l > w || b < 0f || t > h) continue

                if (Cfg.espBox) {
                    cv.drawRect(l, t, r, b, edgeBox)
                    cv.drawRect(l, t, r, b, fillBox)
                }
                if (Cfg.espLine) {
                    cv.drawLine(origin.x, origin.y, cx, b, edgeLine)
                    cv.drawLine(origin.x, origin.y, cx, b, fillLine)
                }
                if (Cfg.espDist && hasLocal) {
                    val dx = x - s.local[0]
                    val dy = y - s.local[1]
                    val dz = z - s.local[2]
                    val label = "${sqrt(dx * dx + dy * dy + dz * dz).toInt()}m"
                    cv.drawText(label, cx, b + 30f, txtEdge)
                    cv.drawText(label, cx, b + 30f, txt)
                }
                if (cx in w * 0.3f..w * 0.7f && b in 0f..h) front++
            }
        }

        Cfg.frontCount = front
        if (Cfg.espThumber) {
            big.color = if (front == 0) Theme.RED else Theme.GREEN
            cv.drawText(front.toString(), w / 2f, 160f, bigEdge)
            cv.drawText(front.toString(), w / 2f, 160f, big)
        }

        postInvalidateOnAnimation()
    }

    private fun project(m: FloatArray, x: Float, y: Float, z: Float, w: Float, h: Float): PointF? {
        val cw = m[3] * x + m[7] * y + m[11] * z + m[15]
        if (cw < 0.01f) return null
        val cx = m[0] * x + m[4] * y + m[8] * z + m[12]
        val cy = m[1] * x + m[5] * y + m[9] * z + m[13]
        return PointF(w / 2f * (1f + cx / cw), h / 2f * (1f - cy / cw))
    }

    private fun stroke(color: Int, width: Float): Paint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            strokeWidth = width
            style = Paint.Style.STROKE
        }

    private fun text(color: Int, size: Float, bold: Boolean): Paint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            textSize = size
            textAlign = Paint.Align.CENTER
            typeface = if (bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
        }

    companion object {
        const val HEAD_H = 1.8f
    }
}
