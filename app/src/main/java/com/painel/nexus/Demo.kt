package com.painel.nexus

import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

// modo demo: jogadores falsos andando em volta da camera pra testar ESP e contador sem o jogo.
// camera em (0,0,1.7) olhando pro +Y. mundo com Z pra cima, mesma convencao do Snap.
object Demo {
    private const val COUNT = 8
    private val pts = FloatArray(Off.MAX_PLAYERS * 3)
    private val local = FloatArray(3)

    fun snap(nowNs: Long, aspect: Float): Snap {
        val t = nowNs / 1_000_000_000.0
        for (i in 0 until COUNT) {
            val ang = t * 0.35 + i * (2.0 * Math.PI / COUNT)
            val r = 10.0 + 6.0 * sin(t * 0.7 + i)
            pts[i * 3] = (r * cos(ang)).toFloat()
            pts[i * 3 + 1] = (r * sin(ang)).toFloat()
            pts[i * 3 + 2] = 0f
        }
        return Snap(pts, COUNT, local, viewProj(aspect))
    }

    private fun viewProj(aspect: Float): FloatArray {
        val eye = floatArrayOf(0f, 0f, 1.7f)
        val center = floatArrayOf(0f, 1f, 1.7f)
        val up = floatArrayOf(0f, 0f, 1f)
        val view = lookAt(eye, center, up)
        val proj = perspective(Math.toRadians(70.0).toFloat(), aspect, 0.1f, 500f)
        return mul(proj, view)
    }

    private fun lookAt(e: FloatArray, c: FloatArray, up: FloatArray): FloatArray {
        val f = norm(sub(c, e))
        val s = norm(cross(f, up))
        val u = cross(s, f)
        val m = FloatArray(16)
        m[0] = s[0]; m[4] = s[1]; m[8] = s[2]; m[12] = -dot(s, e)
        m[1] = u[0]; m[5] = u[1]; m[9] = u[2]; m[13] = -dot(u, e)
        m[2] = -f[0]; m[6] = -f[1]; m[10] = -f[2]; m[14] = dot(f, e)
        m[15] = 1f
        return m
    }

    private fun perspective(fovY: Float, aspect: Float, near: Float, far: Float): FloatArray {
        val f = (1.0 / kotlin.math.tan(fovY / 2.0)).toFloat()
        val m = FloatArray(16)
        m[0] = f / aspect
        m[5] = f
        m[10] = (far + near) / (near - far)
        m[11] = -1f
        m[14] = 2f * far * near / (near - far)
        return m
    }

    // column-major: r = a * b
    private fun mul(a: FloatArray, b: FloatArray): FloatArray {
        val r = FloatArray(16)
        for (c in 0 until 4) for (row in 0 until 4) {
            var acc = 0f
            for (k in 0 until 4) acc += a[k * 4 + row] * b[c * 4 + k]
            r[c * 4 + row] = acc
        }
        return r
    }

    private fun sub(a: FloatArray, b: FloatArray) = floatArrayOf(a[0] - b[0], a[1] - b[1], a[2] - b[2])
    private fun dot(a: FloatArray, b: FloatArray) = a[0] * b[0] + a[1] * b[1] + a[2] * b[2]
    private fun cross(a: FloatArray, b: FloatArray) =
        floatArrayOf(a[1] * b[2] - a[2] * b[1], a[2] * b[0] - a[0] * b[2], a[0] * b[1] - a[1] * b[0])
    private fun norm(a: FloatArray): FloatArray {
        val l = sqrt(dot(a, a))
        return floatArrayOf(a[0] / l, a[1] / l, a[2] / l)
    }
}
