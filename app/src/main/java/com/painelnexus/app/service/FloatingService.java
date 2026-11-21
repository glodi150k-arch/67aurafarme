package com.painelnexus.app.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.IBinder;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import androidx.core.app.NotificationCompat;
import com.painelnexus.app.R;
import com.painelnexus.app.esp.FovCircleOverlay;
import com.painelnexus.app.module.ModuleBridge;
import com.painelnexus.app.util.FeatureState;

/**
 * FloatingService — overlay arrastável sobre o jogo.
 * Menu compacto vermelho/preto + círculo FOV branco separado.
 * Hardware layer em ambos os views para não impactar o render do jogo.
 */
public class FloatingService extends Service {

    private static final String CHANNEL_ID = "nexus_fg";
    private WindowManager mWm;
    private View mPanelView;
    private View mFabView;
    private FovCircleOverlay mFovOverlay;
    private ModuleBridge mBridge;
    private boolean mPanelVisible = true;

    // Window params do menu
    private WindowManager.LayoutParams mPanelParams;
    // Window params do FAB (botão flutuante quando minimizado)
    private WindowManager.LayoutParams mFabParams;
    // Window params do FOV circle (tela inteira, não intercepta toque)
    private WindowManager.LayoutParams mFovParams;

    @Override
    public void onCreate() {
        super.onCreate();
        mWm = (WindowManager) getSystemService(WINDOW_SERVICE);
        mBridge = new ModuleBridge();
        startForeground(1, buildNotification());
        inflatePanel();
        inflateFovOverlay();
    }

    private void inflatePanel() {
        mPanelView = LayoutInflater.from(this).inflate(R.layout.layout_floating_panel, null);

        mPanelParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);
        mPanelParams.gravity = Gravity.TOP | Gravity.START;
        mPanelParams.x = 20;
        mPanelParams.y = 100;

        setupDrag(mPanelView.findViewById(R.id.dragHandle), mPanelParams, mPanelView);
        setupToggles();

        mWm.addView(mPanelView, mPanelParams);
    }

    private void inflateFovOverlay() {
        mFovOverlay = new FovCircleOverlay(this);

        mFovParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                // FLAG_NOT_TOUCHABLE + FLAG_NOT_FOCUSABLE = não intercepta NENHUM toque
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                        | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT);

        mWm.addView(mFovOverlay, mFovParams);
    }

    private void setupToggles() {
        FeatureState state = FeatureState.get();

        // ESP
        bindToggle(mPanelView, R.id.fBoxEsp,       "BOX ESP",    "BOX");
        bindToggle(mPanelView, R.id.fHealthEsp,     "HEALTH",     "HEALTH");
        bindToggle(mPanelView, R.id.fNameEsp,       "NAME",       "NAME");
        bindToggle(mPanelView, R.id.fLineEsp,       "LINE",       "LINE");
        bindToggle(mPanelView, R.id.fSkeletonEsp,   "SKELETON",   "SKELETON");
        bindToggle(mPanelView, R.id.fDistanceEsp,   "DISTANCE",   "DISTANCE");

        // AIMBOT
        bindToggle(mPanelView, R.id.fAimHead,       "AIM HEAD",   "HEAD");
        bindToggle(mPanelView, R.id.fAimBody,       "AIM BODY",   "BODY");

        // MISC
        bindToggle(mPanelView, R.id.fNoRecoil,      "NO RECOIL",  "NO_RECOIL");
        bindToggle(mPanelView, R.id.fNoSpread,      "NO SPREAD",  "NO_SPREAD");
        bindToggle(mPanelView, R.id.fFastSwitch,    "FAST SWITCH","FAST");

        // FOV CIRCLE
        Switch swFov = mPanelView.findViewById(R.id.swFovFloat);
        SeekBar seekFov = mPanelView.findViewById(R.id.seekFovFloat);

        swFov.setChecked(state.isFovEnabled());
        seekFov.setProgress(state.getFovRadius());
        mFovOverlay.setFovRadius(state.getFovRadius());
        mFovOverlay.setFovVisible(state.isFovEnabled());

        swFov.setOnCheckedChangeListener((v, checked) -> {
            state.setFovEnabled(checked);
            mFovOverlay.setFovVisible(checked);
            mBridge.sendToggle("IXYZFOV3", checked);
        });

        seekFov.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar bar, int progress, boolean fromUser) {
                if (fromUser) {
                    int r = Math.max(10, progress);
                    state.setFovRadius(r);
                    mFovOverlay.setFovRadius(r);
                    mBridge.sendValue("FOV_VALUE", r);
                }
            }
            @Override public void onStartTrackingTouch(SeekBar b) {}
            @Override public void onStopTrackingTouch(SeekBar b) {}
        });

        // MINIMIZE
        mPanelView.findViewById(R.id.btnMinimize).setOnClickListener(v -> togglePanel(false));
    }

    private void bindToggle(View root, int viewId, String label, String featureKey) {
        View item = root.findViewById(viewId);
        if (item == null) return;
        TextView tv = item.findViewById(R.id.tvTitle);
        Switch sw = item.findViewById(R.id.swToggle);
        if (tv != null) tv.setText(label);
        if (sw != null) {
            sw.setChecked(FeatureState.get().getToggle(featureKey));
            sw.setOnCheckedChangeListener((v, checked) -> {
                FeatureState.get().setToggle(featureKey, checked);
                mBridge.sendToggle(featureKey, checked);
            });
        }
    }

    private void togglePanel(boolean show) {
        mPanelVisible = show;
        mPanelView.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    /** Drag suave no handle — sem vibrar, sem boundar */
    private void setupDrag(View handle, WindowManager.LayoutParams params, View target) {
        final int[] startTouchXY = new int[2];
        final int[] startParamXY = new int[2];

        handle.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    startTouchXY[0] = (int) event.getRawX();
                    startTouchXY[1] = (int) event.getRawY();
                    startParamXY[0] = params.x;
                    startParamXY[1] = params.y;
                    return true;
                case MotionEvent.ACTION_MOVE:
                    params.x = startParamXY[0] + (int)(event.getRawX() - startTouchXY[0]);
                    params.y = startParamXY[1] + (int)(event.getRawY() - startTouchXY[1]);
                    mWm.updateViewLayout(target, params);
                    return true;
            }
            return false;
        });
    }

    private Notification buildNotification() {
        NotificationChannel ch = new NotificationChannel(
                CHANNEL_ID, "Painel Nexus", NotificationManager.IMPORTANCE_LOW);
        ((NotificationManager) getSystemService(NOTIFICATION_SERVICE)).createNotificationChannel(ch);

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Painel Nexus")
                .setContentText("Menu ativo")
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setOngoing(true)
                .build();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mPanelView != null && mPanelView.isAttachedToWindow()) mWm.removeView(mPanelView);
        if (mFovOverlay != null && mFovOverlay.isAttachedToWindow()) mWm.removeView(mFovOverlay);
        if (mBridge != null) mBridge.shutdown();
    }

    @Override
    public IBinder onBind(Intent intent) { return null; }
}
