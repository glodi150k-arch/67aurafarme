package com.painelnexus.app.module;

import android.os.Handler;
import android.os.Looper;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import java.util.concurrent.TimeUnit;

/**
 * Bridge HTTP com o módulo no Free Fire — porta 7755 (127.0.0.1)
 * Mesmo protocolo do TH_PAINEL original: GET requests simples.
 * ExecutorService single-thread para serializar os comandos.
 */
public class ModuleBridge {

    private static final String BASE_URL = "http://127.0.0.1:7755/xyz/";
    private static final String STATUS_URL = BASE_URL + "fov3-status";

    private final OkHttpClient mClient;
    private final ExecutorService mExecutor;
    private final Handler mMain;

    public interface Callback {
        void onSuccess(String response);
        void onError(String error);
    }

    public ModuleBridge() {
        mClient = new OkHttpClient.Builder()
                .connectTimeout(1, TimeUnit.SECONDS)
                .readTimeout(1, TimeUnit.SECONDS)
                .writeTimeout(1, TimeUnit.SECONDS)
                .retryOnConnectionFailure(false)
                .build();
        mExecutor = Executors.newSingleThreadExecutor();
        mMain = new Handler(Looper.getMainLooper());
    }

    /** Envia toggle de feature. Ex: BOX=1, HEALTH=0 */
    public void sendToggle(String feature, boolean enabled) {
        sendGet(BASE_URL + feature + "=" + (enabled ? "1" : "0"), null);
    }

    /** Envia valor inteiro. Ex: FOV_VALUE=150 */
    public void sendValue(String feature, int value) {
        sendGet(BASE_URL + feature + "=" + value, null);
    }

    /** Checa status do módulo */
    public void checkStatus(Callback callback) {
        sendGet(STATUS_URL, callback);
    }

    private void sendGet(String url, Callback callback) {
        mExecutor.execute(() -> {
            Request req = new Request.Builder().url(url).get().build();
            try (Response resp = mClient.newCall(req).execute()) {
                String body = resp.body() != null ? resp.body().string() : "";
                if (callback != null) {
                    mMain.post(() -> callback.onSuccess(body));
                }
            } catch (IOException e) {
                if (callback != null) {
                    mMain.post(() -> callback.onError(e.getMessage()));
                }
            }
        });
    }

    public void shutdown() {
        mExecutor.shutdownNow();
    }
}
