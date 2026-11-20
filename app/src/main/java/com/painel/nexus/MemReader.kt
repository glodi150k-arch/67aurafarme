package com.painel.nexus

import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder

// le memoria do Free Fire via /proc/<pid>/mem. precisa de root.
class MemReader {
    @Volatile private var raf: RandomAccessFile? = null
    private val b8 = ByteArray(8)
    private val b4 = ByteArray(4)

    var pid = -1
        private set
    var base = 0L
        private set

    fun attach(): Boolean {
        detach()
        val p = findPid()
        if (p < 0) return false
        val f = try {
            RandomAccessFile("/proc/$p/mem", "r")
        } catch (e: Exception) {
            return false
        }
        raf = f
        pid = p
        base = moduleBase(p, Off.LIB_NAME)
        return base != 0L
    }

    fun detach() {
        try { raf?.close() } catch (e: Exception) { }
        raf = null
        pid = -1
        base = 0L
    }

    fun alive(): Boolean = raf != null && File("/proc/$pid").exists()

    private fun read(addr: Long, dst: ByteArray): Boolean {
        val f = raf ?: return false
        return try {
            f.seek(addr)
            f.readFully(dst)
            true
        } catch (e: Exception) {
            false
        }
    }

    fun long(addr: Long): Long {
        if (!read(addr, b8)) return 0L
        return ByteBuffer.wrap(b8).order(ByteOrder.LITTLE_ENDIAN).long
    }

    fun int(addr: Long): Int {
        if (!read(addr, b4)) return 0
        return ByteBuffer.wrap(b4).order(ByteOrder.LITTLE_ENDIAN).int
    }

    fun floats(addr: Long, out: FloatArray): Boolean {
        val tmp = ByteArray(out.size * 4)
        if (!read(addr, tmp)) return false
        val bb = ByteBuffer.wrap(tmp).order(ByteOrder.LITTLE_ENDIAN)
        for (i in out.indices) out[i] = bb.float
        return true
    }

    companion object {
        const val PKG = "com.dts.freefireth"

        fun findPid(): Int {
            val dirs = File("/proc").listFiles() ?: return -1
            for (d in dirs) {
                val n = d.name.toIntOrNull() ?: continue
                val cmd = try {
                    File(d, "cmdline").readBytes().toString(Charsets.UTF_8).trimEnd('\u0000')
                } catch (e: Exception) {
                    continue
                }
                if (cmd == PKG) return n
            }
            return -1
        }

        fun moduleBase(pid: Int, name: String): Long {
            try {
                for (line in File("/proc/$pid/maps").readLines()) {
                    if (line.endsWith(name)) return line.substringBefore('-').toLong(16)
                }
            } catch (e: Exception) { }
            return 0L
        }
    }
}
