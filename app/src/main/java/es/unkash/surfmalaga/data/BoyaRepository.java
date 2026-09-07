package es.unkash.surfmalaga.data;

import android.util.Log;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Temperatura del agua real de la boya del Puerto de Málaga.
 * Fuente: labouee.app (datos de Puertos del Estado)
 * URL: https://labouee.app/es/buoy/malaga-buoy
 * Actualización: cada hora
 */
public class BoyaRepository {

    private static final String TAG = "BoyaRepo";
    private static final String URL_BOYA =
            "https://labouee.app/es/buoy/malaga-buoy";

    private static Float cachedTemp = null;
    private static long cacheTimestamp = 0;
    private static final long CACHE_TTL_MS = 30 * 60 * 1000; // 30 minutos

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
                conn.setRequestProperty("User-Agent",
                        "Mozilla/5.0 (Android) SurfMalaga/5.1");
                conn.setRequestProperty("Accept-Language", "es-ES,es;q=0.9");

                if (conn.getResponseCode() != 200) {
                    Log.e(TAG, "HTTP " + conn.getResponseCode());
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
                Log.d(TAG, "Temperatura boya Málaga: " + temp + "°C");

                if (temp != null) {
                    cachedTemp = temp;
                    cacheTimestamp = now;
                }
                callback.onResult(temp);

            } catch (Exception e) {
                Log.e(TAG, "Error: " + e.getMessage());
                callback.onResult(null);
            }
        }).start();
    }

    /**
     * El HTML contiene: Agua\n22.7°C
     * Buscamos el patrón de temperatura después de "Agua"
     */
    private static Float parseTemp(String html) {
        try {
            // Buscar patrón "Agua" seguido de temperatura XX.X°C o XX°C
            Pattern p = Pattern.compile(
                    "Agua[^\\d]{0,30}(\\d{1,2}[.,]\\d)\\s*°C",
                    Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
            Matcher m = p.matcher(html);
            if (m.find()) {
                String val = m.group(1).replace(",", ".");
                return Float.parseFloat(val);
            }

            // Patrón alternativo: buscar directamente XX.X°C cerca de "temperatura"
            Pattern p2 = Pattern.compile(
                    "temperatura[^\\d]{0,50}(\\d{1,2}[.,]\\d)\\s*°C",
                    Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
            Matcher m2 = p2.matcher(html);
            if (m2.find()) {
                String val = m2.group(1).replace(",", ".");
                return Float.parseFloat(val);
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
