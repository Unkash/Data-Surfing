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
 * Temperatura del agua del mar cerca de Málaga.
 * Fuente: Open-Meteo Marine API (sea_surface_temperature)
 * Coordenadas: Puerto de Málaga (36.71, -4.42)
 * Actualización: cada hora, sin API key.
 */
public class BoyaRepository {

    private static final String TAG = "BoyaRepo";
    // Coordenadas del Puerto de Málaga
    private static final String URL_TEMP =
        "https://marine-api.open-meteo.com/v1/marine" +
        "?latitude=36.71&longitude=-4.42" +
        "&hourly=sea_surface_temperature" +
        "&forecast_days=1&timezone=Europe%2FMadrid";

    private static Float cachedTemp = null;
    private static long cacheTimestamp = 0;
    private static final long CACHE_TTL_MS = 60 * 60 * 1000; // 1 hora

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
                        new URL(URL_TEMP).openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(8000);
                conn.setReadTimeout(8000);

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
                Log.e(TAG, "Error temperatura agua: " + e.getMessage());
                callback.onResult(null);
            }
        }).start();
    }

    private static Float parseTemp(String json) {
        try {
            JSONObject obj = new JSONObject(json);
            JSONObject hourly = obj.getJSONObject("hourly");
            JSONArray temps = hourly.getJSONArray("sea_surface_temperature");

            // Obtener la hora actual para coger el valor más cercano
            java.util.Calendar cal = java.util.Calendar.getInstance();
            int horaActual = cal.get(java.util.Calendar.HOUR_OF_DAY);

            // El índice corresponde a la hora del día
            int idx = Math.min(horaActual, temps.length() - 1);

            // Buscar el último valor no nulo en torno a la hora actual
            for (int i = idx; i >= 0; i--) {
                if (!temps.isNull(i)) {
                    return (float) temps.getDouble(i);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error parseando temperatura: " + e.getMessage());
        }
        return null;
    }

    public static void invalidateCache() {
        cachedTemp = null;
        cacheTimestamp = 0;
    }
}
