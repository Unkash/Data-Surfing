package es.unkash.surfmalaga.data;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * Temperatura del agua del mar desde la boya del Puerto de Málaga
 * Fuente: Puertos del Estado (portus.puertos.es) — ID boya: 1514
 * Actualización: cada hora
 */
public class BoyaRepository {

    private static final String TAG = "BoyaRepo";
    private static final String URL_BOYA =
        "https://portus.puertos.es/PortusData/rtData?station=1514&params=WaterTemp&locale=es";

    private static Float cachedTemp = null;
    private static long cacheTimestamp = 0;
    private static final long CACHE_TTL_MS = 30 * 60 * 1000;

    public interface Callback {
        void onResult(Float tempCelsius);
    }

    public static void getTemperaturaAgua(Callback callback) {
        long now = System.currentTimeMillis();
        if (cachedTemp != null && (now - cacheTimestamp) < CACHE_TTL_MS) {
            callback.onResult(cachedTemp);
            return;
        }
        new Thread(() -> {
            try {
                HttpURLConnection conn = (HttpURLConnection)
                        new URL(URL_BOYA).openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(8000);
                conn.setReadTimeout(8000);
                conn.setRequestProperty("User-Agent", "Mozilla/5.0 SurfMalaga/5.0");
                conn.setRequestProperty("Accept", "application/json");

                if (conn.getResponseCode() != 200) {
                    callback.onResult(null);
                    return;
                }

                BufferedReader br = new BufferedReader(
                        new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) sb.append(line);
                br.close();

                Float temp = parseTemp(sb.toString());
                if (temp != null) {
                    cachedTemp = temp;
                    cacheTimestamp = now;
                }
                callback.onResult(temp);

            } catch (Exception e) {
                Log.e(TAG, "Error boya: " + e.getMessage());
                callback.onResult(null);
            }
        }).start();
    }

    private static Float parseTemp(String json) {
        try {
            // Formato: [[timestamp_ms, valor], ...]
            JSONArray arr = new JSONArray(json);
            for (int i = arr.length() - 1; i >= 0; i--) {
                JSONArray punto = arr.getJSONArray(i);
                if (punto.length() >= 2 && !punto.isNull(1)) {
                    return (float) punto.getDouble(1);
                }
            }
        } catch (Exception e1) {
            try {
                // Formato alternativo: {"data": [[ts, val], ...]}
                JSONObject obj = new JSONObject(json);
                JSONArray data = obj.getJSONArray("data");
                for (int i = data.length() - 1; i >= 0; i--) {
                    JSONArray punto = data.getJSONArray(i);
                    if (punto.length() >= 2 && !punto.isNull(1)) {
                        return (float) punto.getDouble(1);
                    }
                }
            } catch (Exception e2) {
                Log.e(TAG, "Parse error: " + e2.getMessage());
            }
        }
        return null;
    }

    public static void invalidateCache() {
        cachedTemp = null;
        cacheTimestamp = 0;
    }
}
