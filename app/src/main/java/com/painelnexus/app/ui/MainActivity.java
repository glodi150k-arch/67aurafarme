package com.painelnexus.app.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.painelnexus.app.R;
import com.painelnexus.app.module.ModuleBridge;
import com.painelnexus.app.service.FloatingService;
import com.painelnexus.app.util.FeatureState;

public class MainActivity extends AppCompatActivity {

    private static final int REQ_OVERLAY = 1001;

    private boolean mPanelRunning = false;
    private ModuleBridge mBridge;
    private Handler mPollHandler;
    private Runnable mStatusPoller;

    // Views
    private Button btnToggle, btnPermission;
    private TextView tvStatus, statusDot;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mBridge = new ModuleBridge();
        mPollHandler = new Handler(Looper.getMainLooper());

        findViews();
        setupMainButton();
        setupPermissionButton();
        setupEspToggles();
        setupAimToggles();
        setupFovItem();
        setupMiscToggles();
        startStatusPolling();
    }

    private void findViews() {
        btnToggle     = findViewById(R.id.btnToggle);
        btnPermission = findViewById(R.id.btnPermission);
        tvStatus      = findViewById(R.id.tvStatus);
        statusDot     = findViewById(R.id.statusDot);
    }

    private void setupMainButton() {
        btnToggle.setOnClickListener(v -> {
            if (!Settings.canDrawOverlays(this)) {
                Toast.makeText(this, "Conceda a permissão de sobreposição primeiro", Toast.LENGTH_SHORT).show();
                return;
            }
            mPanelRunning = !mPanelRunning;
            if (mPanelRunning) {
                startService(new Intent(this, FloatingService.class));
                btnToggle.setText("PARAR PAINEL");
                setStatus("PAINEL ATIVO", true);
            } else {
                stopService(new Intent(this, FloatingService.class));
                btnToggle.setText("INICIAR PAINEL");
                setStatus("PAINEL INATIVO", false);
            }
        });
    }

    private void setupPermissionButton() {
        btnPermission.setOnClickListener(v -> {
            if (Settings.canDrawOverlays(this)) {
                Toast.makeText(this, "Permissão já concedida ✓", Toast.LENGTH_SHORT).show();
            } else {
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + getPackageName()));
                startActivityForResult(intent, REQ_OVERLAY);
            }
        });
    }

    private void setupEspToggles() {
        bindToggle(R.id.itemBox,       "BOX ESP",    "BOX");
        bindToggle(R.id.itemHealth,    "HEALTH BAR", "HEALTH");
        bindToggle(R.id.itemName,      "NAME ESP",   "NAME");
        bindToggle(R.id.itemLine,      "LINE ESP",   "LINE");
        bindToggle(R.id.itemSkeleton,  "SKELETON",   "SKELETON");
        bindToggle(R.id.itemDistance,  "DISTANCE",   "DISTANCE");
    }

    private void setupAimToggles() {
        bindToggle(R.id.itemAimHead, "AIMBOT HEAD", "HEAD");
        bindToggle(R.id.itemAimBody, "AIMBOT BODY", "BODY");
    }

    private void setupMiscToggles() {
        bindToggle(R.id.itemNoRecoil,   "NO RECOIL",   "NO_RECOIL");
        bindToggle(R.id.itemNoSpread,   "NO SPREAD",   "NO_SPREAD");
        bindToggle(R.id.itemFastSwitch, "FAST SWITCH", "FAST");
    }

    private void setupFovItem() {
        View fovItem = findViewById(R.id.itemFov);
        if (fovItem == null) return;

        TextView tvTitle = fovItem.findViewById(R.id.tvTitle);
        TextView tvVal   = fovItem.findViewById(R.id.tvFovValue);
        Switch sw        = fovItem.findViewById(R.id.swToggle);
        SeekBar seek     = fovItem.findViewById(R.id.seekFov);

        FeatureState state = FeatureState.get();
        if (tvTitle != null) tvTitle.setText("FOV CIRCLE");
        if (sw != null) sw.setChecked(state.isFovEnabled());
        if (seek != null) seek.setProgress(state.getFovRadius());
        if (tvVal != null) tvVal.setText("Tamanho: " + state.getFovRadius());

        if (sw != null) {
            sw.setOnCheckedChangeListener((v, checked) -> {
                state.setFovEnabled(checked);
                mBridge.sendToggle("IXYZFOV3", checked);
            });
        }

        if (seek != null && tvVal != null) {
            seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override public void onProgressChanged(SeekBar bar, int progress, boolean fromUser) {
                    int r = Math.max(10, progress);
                    state.setFovRadius(r);
                    tvVal.setText("Tamanho: " + r);
                    if (fromUser) mBridge.sendValue("FOV_VALUE", r);
                }
                @Override public void onStartTrackingTouch(SeekBar b) {}
                @Override public void onStopTrackingTouch(SeekBar b) {}
            });
        }
    }

    private void bindToggle(int itemId, String label, String featureKey) {
        View item = findViewById(itemId);
        if (item == null) return;
        TextView tv = item.findViewById(R.id.tvTitle);
        Switch sw   = item.findViewById(R.id.swToggle);
        if (tv != null) tv.setText(label);
        if (sw != null) {
            sw.setChecked(FeatureState.get().getToggle(featureKey));
            sw.setOnCheckedChangeListener((v, checked) -> {
                FeatureState.get().setToggle(featureKey, checked);
                mBridge.sendToggle(featureKey, checked);
            });
        }
    }

    private void startStatusPolling() {
        mStatusPoller = new Runnable() {
            @Override
            public void run() {
                mBridge.checkStatus(new ModuleBridge.Callback() {
                    @Override public void onSuccess(String r) {
                        if (mPanelRunning) setStatus("MÓDULO OK · CONECTADO", true);
                    }
                    @Override public void onError(String e) {
                        if (mPanelRunning) setStatus("MÓDULO OFFLINE", false);
                    }
                });
                mPollHandler.postDelayed(this, 5000);
            }
        };
        mPollHandler.postDelayed(mStatusPoller, 3000);
    }

    private void setStatus(String text, boolean active) {
        if (tvStatus != null) tvStatus.setText(text);
        if (statusDot != null) {
            statusDot.setBackgroundColor(active
                    ? getColor(R.color.accent_red)
                    : getColor(R.color.text_dim));
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mPollHandler.removeCallbacks(mStatusPoller);
        mBridge.shutdown();
    }
}
