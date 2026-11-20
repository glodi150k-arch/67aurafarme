package com.painel.nexus

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.net.VpnService
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {
    private lateinit var btnVpn: Button

    private val vpnConsent = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { r ->
        if (r.resultCode == RESULT_OK) startVpn()
        else Toast.makeText(this, "vpn não autorizada", Toast.LENGTH_SHORT).show()
    }

    private val notifPerm = registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            notifPerm.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        btnVpn = findViewById(R.id.btnVpn)
        btnVpn.setOnClickListener {
            if (Cfg.vpnOn) {
                stopVpn()
            } else {
                val intent = VpnService.prepare(this)
                if (intent != null) vpnConsent.launch(intent) else startVpn()
            }
        }
        findViewById<Button>(R.id.btnFloat).setOnClickListener { openFloat() }
    }

    override fun onResume() {
        super.onResume()
        updateVpnButton()
    }

    private fun updateVpnButton() {
        btnVpn.text = if (Cfg.vpnOn) "Desconectar VPN local" else "Conectar VPN local (Free Fire)"
    }

    private fun startVpn() {
        startService(Intent(this, LocalVpnService::class.java).setAction(LocalVpnService.ACTION_START))
        btnVpn.postDelayed({ updateVpnButton() }, 400)
    }

    private fun stopVpn() {
        startService(Intent(this, LocalVpnService::class.java).setAction(LocalVpnService.ACTION_STOP))
        btnVpn.postDelayed({ updateVpnButton() }, 400)
    }

    private fun openFloat() {
        if (!Settings.canDrawOverlays(this)) {
            startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
            Toast.makeText(this, "libera a sobreposição e toca de novo", Toast.LENGTH_LONG).show()
            return
        }
        startService(Intent(this, FloatService::class.java))
    }
}
