package com.painel.nexus

import android.graphics.Color

object Cfg {
    @Volatile var espLine = false
    @Volatile var espBox = false
    @Volatile var espDist = false
    @Volatile var espThumber = false
    @Volatile var exampleFn = false
    @Volatile var vpnOn = false
    @Volatile var frontCount = 0
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
}
