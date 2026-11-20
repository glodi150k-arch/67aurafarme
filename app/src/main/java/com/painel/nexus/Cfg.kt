package com.painel.nexus

import android.graphics.Color

// ponte entre a leitura (VPN local / memoria do jogo) e a ESP.
// o servico grava Cfg.snap, a ESP so le. o modo demo gera o mesmo formato de dado.
object Cfg {
    @Volatile var espLine = false
    @Volatile var espBox = false
    @Volatile var espDist = false
    @Volatile var espThumber = false
    @Volatile var exampleFn = false
    @Volatile var demoOn = false
    @Volatile var vpnOn = false
    @Volatile var vpnStatus = "vpn desligada"
    @Volatile var frontCount = 0
    @Volatile var aspect = 0.46f
    @Volatile var snap: Snap = Snap.EMPTY
}

class Snap(val players: FloatArray, val count: Int, val local: FloatArray, val matrix: FloatArray) {
    companion object {
        val EMPTY = Snap(FloatArray(0), 0, FloatArray(3), FloatArray(16))
    }
}

object Theme {
    val RED: Int = Color.rgb(255, 26, 26)
    val GREEN: Int = Color.rgb(57, 255, 20)
    val PANEL: Int = Color.rgb(10, 10, 10)
    val ROW: Int = Color.rgb(22, 22, 22)
}
