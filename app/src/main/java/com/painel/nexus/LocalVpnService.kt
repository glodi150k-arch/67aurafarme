package com.painel.nexus

import android.content.Intent
import android.net.VpnService
import android.os.ParcelFileDescriptor

// vpn local: tun so pra 10.77.0.0/24, o resto do trafego nao passa por aqui.
// o loop de leitura roda aqui e grava Cfg.snap. a ESP le daqui.
class LocalVpnService : VpnService() {
    private var tun: ParcelFileDescriptor? = null
    private var worker: Thread? = null
    private val reader = MemReader()

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            shutdown()
            stopSelf()
            return START_NOT_STICKY
        }
        if (tun == null) {
            tun = try {
                Builder()
                    .setSession("Painel Nexus VPN local")
                    .addAddress("10.77.0.2", 32)
                    .addRoute("10.77.0.0", 24)
                    .setMtu(1500)
                    .establish()
            } catch (e: Exception) {
                null
            }
            if (tun == null) {
                Cfg.vpnStatus = "vpn não autorizada"
                stopSelf()
                return START_NOT_STICKY
            }
            Cfg.vpnOn = true
            Cfg.vpnStatus = "vpn ligada"
            startLoop()
        }
        return START_STICKY
    }

    private fun startLoop() {
        worker = Thread {
            while (Cfg.vpnOn) {
                if (!reader.alive()) {
                    if (!Cfg.demoOn) Cfg.snap = Snap.EMPTY
                    if (!reader.attach()) {
                        Cfg.vpnStatus = "aguardando o jogo abrir"
                        Thread.sleep(500)
                        continue
                    }
                }
                if (!Off.ready()) {
                    if (!Cfg.demoOn) Cfg.snap = Snap.EMPTY
                    Cfg.vpnStatus = "jogo achado, offsets pendentes"
                    Thread.sleep(200)
                    continue
                }
                Cfg.vpnStatus = "lendo posições"
                if (!Cfg.demoOn) Cfg.snap = poll()
                Thread.sleep(2)
            }
            Cfg.vpnStatus = "vpn desligada"
        }.also {
            it.isDaemon = true
            it.start()
        }
    }

    private fun poll(): Snap {
        val base = reader.base
        val list = reader.long(base + Off.PLAYER_LIST)
        val total = minOf(reader.int(base + Off.PLAYER_COUNT), Off.MAX_PLAYERS)
        val pts = FloatArray(Off.MAX_PLAYERS * 3)
        val tmp = FloatArray(3)
        var n = 0
        for (i in 0 until total) {
            val p = reader.long(list + i * 8L)
            if (p == 0L) continue
            if (!reader.floats(p + Off.PLAYER_POS, tmp)) continue
            pts[n * 3] = tmp[0]
            pts[n * 3 + 1] = tmp[1]
            pts[n * 3 + 2] = tmp[2]
            n++
        }
        val local = FloatArray(3)
        val lp = reader.long(base + Off.LOCAL_PLAYER)
        if (lp != 0L) reader.floats(lp + Off.PLAYER_POS, local)
        val m = FloatArray(16)
        reader.floats(base + Off.VIEW_MATRIX, m)
        return Snap(pts, n, local, m)
    }

    private fun shutdown() {
        Cfg.vpnOn = false
        try { tun?.close() } catch (e: Exception) { }
        tun = null
        reader.detach()
        if (!Cfg.demoOn) Cfg.snap = Snap.EMPTY
        Cfg.vpnStatus = "vpn desligada"
    }

    override fun onDestroy() {
        shutdown()
        super.onDestroy()
    }

    companion object {
        const val ACTION_START = "com.painel.nexus.VPN_START"
        const val ACTION_STOP = "com.painel.nexus.VPN_STOP"
    }
}
