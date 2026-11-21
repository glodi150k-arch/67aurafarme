package com.painelnexus.app.esp;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.view.View;

/**
 * Overlay de círculo FOV — desenhado sobre a tela via WindowManager.
 * Apenas um círculo branco, sem bitmap, sem textura, sem sombra.
 * Zero impacto em memória/cache — só Paint.drawCircle no canvas.
 */
public class FovCircleOverlay extends View {

    private final Paint mCirclePaint;
    private int mRadius = 100;
    private boolean mVisible = false;

    public FovCircleOverlay(Context context) {
        super(context);
        setLayerType(LAYER_TYPE_HARDWARE, null); // GPU, sem SW rasterizer

        mCirclePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mCirclePaint.setStyle(Paint.Style.STROKE);
        mCirclePaint.setColor(Color.WHITE);
        mCirclePaint.setStrokeWidth(2.0f);
        mCirclePaint.setXfermode(null);
        // Sem alpha, sem blur, sem shadow — só stroke branco
    }

    public void setFovRadius(int radius) {
        mRadius = Math.max(10, Math.min(radius, 500));
        if (mVisible) invalidate();
    }

    public void setFovVisible(boolean visible) {
        mVisible = visible;
        setVisibility(visible ? VISIBLE : INVISIBLE);
        if (visible) invalidate();
    }

    public int getFovRadius() { return mRadius; }
    public boolean isFovVisible() { return mVisible; }

    @Override
    protected void onDraw(Canvas canvas) {
        if (!mVisible) return;
        float cx = getWidth() / 2f;
        float cy = getHeight() / 2f;
        canvas.drawCircle(cx, cy, mRadius, mCirclePaint);
    }
}
