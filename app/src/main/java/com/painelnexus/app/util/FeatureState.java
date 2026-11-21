package com.painelnexus.app.util;

import java.util.HashMap;
import java.util.Map;

/**
 * Estado global das features — singleton simples.
 * SharedPreferences persiste entre sessões.
 */
public class FeatureState {

    private static FeatureState sInstance;
    private final Map<String, Boolean> mToggles = new HashMap<>();
    private int mFovRadius = 100;
    private boolean mFovEnabled = false;

    private FeatureState() {
        // defaults
        mToggles.put("BOX",       false);
        mToggles.put("HEALTH",    false);
        mToggles.put("NAME",      false);
        mToggles.put("LINE",      false);
        mToggles.put("SKELETON",  false);
        mToggles.put("DISTANCE",  false);
        mToggles.put("HEAD",      false);
        mToggles.put("BODY",      false);
        mToggles.put("NO_RECOIL", false);
        mToggles.put("NO_SPREAD", false);
        mToggles.put("FAST",      false);
    }

    public static synchronized FeatureState get() {
        if (sInstance == null) sInstance = new FeatureState();
        return sInstance;
    }

    public boolean getToggle(String key) {
        return Boolean.TRUE.equals(mToggles.get(key));
    }

    public void setToggle(String key, boolean value) {
        mToggles.put(key, value);
    }

    public int getFovRadius() { return mFovRadius; }
    public void setFovRadius(int r) { mFovRadius = r; }

    public boolean isFovEnabled() { return mFovEnabled; }
    public void setFovEnabled(boolean v) { mFovEnabled = v; }
}
