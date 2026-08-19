package es.unkash.surfmalaga.data;

import android.util.Log;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * Scraping del HTML de gestmovil.malaga.eu/playasdemalaga/
 *
 * Nombres de imagen conocidos (mayúsculas/minúsculas pueden variar):
 *
 * Color base:
 *   BanderaVerde.png
 *   BanderaAmarilla.png
 *   BanderaRoja.png
 *   BanderaBlanca.png  (fuera de temporada)
 *
 * Con medusas:
 *   BanderaVerdeMedusas.png
 *   BanderaAmarillaMedusas.png
 *   BanderaRojaMedusas.png
 *
 * Con dispositivo de salvamento (PuntoNaranja):
 *   BanderaVerde_PuntoNaranja.png
 *   BanderaAmarilla_PuntoNaranja.png
 *   BanderaRoja_PuntoNaranja.png
 *
 * Con medusas + dispositivo de salvamento:
 *   BanderaVerdeMedusas_PuntoNaranja.png
 *   BanderaAmarillaMedusas_PuntoNaranja.png
 *   BanderaRojaMedusas_PuntoNaranja.png
 */
public class BanderaRepository {

    private static final String TAG = "BanderaRepo";
    private static final String URL_PLAYAS =
            "http://gestmovil.malaga.eu/playasdemalaga/";

    // Caché 5 minutos
    private static String cachedHtml = null;
    private static long cacheTimestamp = 0;
    private static final long CACHE_TTL_MS = 5 * 60 * 1000;

    public enum Color { VERDE, AMARILLA, ROJA, BLANCA, SIN_DATOS }

    public static class EstadoBandera {
        public Color color = Color.SIN_DATOS;
        public boolean medusas = false;
        public boolean salvamento = false;
        public String hora = null;

        /** Texto completo para mostrar en la UI */
        public String toTexto() {
            if (color == Color.SIN_DATOS) return "Sin datos de bandera";

            StringBuilder sb = new StringBuilder();
            switch (color) {
                case VERDE:   sb.append("🟢 Bandera VERDE — Baño permitido"); break;
                case AMARILLA:sb.append("🟡 Bandera AMARILLA — Precaución"); break;
                case ROJA:    sb.append("🔴 Bandera ROJA — Baño prohibido"); break;
                case BLANCA:  sb.append("⚪ Bandera BLANCA — Fuera de temporada"); break;
            }
            if (medusas)    sb.append(" · 🪼 Medusas");
            if (salvamento) sb.append(" · 🛟 Salvamento");
            return sb.toString();
        }

        public int toColor() {
            switch (color) {
                case VERDE:    return 0xFF388E3C;
                case AMARILLA: return 0xFFF57F17;
                case ROJA:     return 0xFFD32F2F;
                case BLANCA:   return 0xFF757575;
                default:       return 0xFF9E9E9E;
            }
        }
    }

    public interface Callback {
        void onResult(EstadoBandera estado);
    }

    public static void getEstado(String anchorName, Callback callback) {
        if (anchorName == null) {
            callback.onResult(new EstadoBandera());
            return;
        }
        new Thread(() -> {
            try {
                String html = getHtml();
                if (html == null || html.isEmpty()) {
                    callback.onResult(new EstadoBandera());
                    return;
                }
                EstadoBandera estado = parse(html, anchorName);
                callback.onResult(estado);
            } catch (Exception e) {
                Log.e(TAG, "Error scraping " + anchorName, e);
                callback.onResult(new EstadoBandera());
            }
        }).start();
    }

    private static String getHtml() throws Exception {
        long now = System.currentTimeMillis();
        if (cachedHtml != null && (now - cacheTimestamp) < CACHE_TTL_MS) {
            return cachedHtml;
        }
        HttpURLConnection conn = (HttpURLConnection)
                new URL(URL_PLAYAS).openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(10000);
        conn.setRequestProperty("User-Agent",
                "Mozilla/5.0 (Android; Mobile) SurfMalaga/4.3");

        if (conn.getResponseCode() != 200) return null;

        BufferedReader br = new BufferedReader(
                new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) sb.append(line).append("\n");
        br.close();

        cachedHtml = sb.toString();
        cacheTimestamp = now;
        return cachedHtml;
    }

    private static EstadoBandera parse(String html, String anchorName) {
        EstadoBandera estado = new EstadoBandera();

        // Buscar el anchor href="#anchorName"
        int idx = html.indexOf("href=\"#" + anchorName + "\"");
        if (idx < 0) idx = html.indexOf("href='#" + anchorName + "'");
        if (idx < 0) {
            Log.w(TAG, "Anchor no encontrado: " + anchorName);
            return estado;
        }

        // Extraer bloque de ~800 chars desde el anchor
        String block = html.substring(idx, Math.min(idx + 800, html.length()));

        // Buscar src de la img
        String src = extractImgSrc(block);
        if (src == null) {
            Log.w(TAG, "img src no encontrado para: " + anchorName);
            return estado;
        }

        Log.d(TAG, anchorName + " → src: " + src);
        String s = src.toLowerCase();

        // Detectar color
        if      (s.contains("verde"))   estado.color = Color.VERDE;
        else if (s.contains("amarilla")) estado.color = Color.AMARILLA;
        else if (s.contains("roja"))    estado.color = Color.ROJA;
        else if (s.contains("blanca"))  estado.color = Color.BLANCA;

        // Detectar medusas
        estado.medusas = s.contains("medusa");

        // Detectar dispositivo de salvamento (punto naranja)
        estado.salvamento = s.contains("naranja") || s.contains("salvamento");

        // Buscar hora de actualización DD/MM/YYYY HH:MM:SS
        java.util.regex.Matcher m = java.util.regex.Pattern.compile(
                "(\\d{2}/\\d{2}/\\d{4}\\s+\\d{2}:\\d{2}:\\d{2})").matcher(block);
        if (m.find()) estado.hora = m.group(1);

        return estado;
    }

    private static String extractImgSrc(String block) {
        int imgIdx = block.indexOf("<img");
        if (imgIdx < 0) return null;
        String imgBlock = block.substring(imgIdx, Math.min(imgIdx + 300, block.length()));

        for (String q : new String[]{"src=\"", "src='"}) {
            int i = imgBlock.indexOf(q);
            if (i >= 0) {
                int start = i + q.length();
                char closing = q.endsWith("\"") ? '"' : '\'';
                int end = imgBlock.indexOf(closing, start);
                if (end > start) return imgBlock.substring(start, end);
            }
        }
        return null;
    }

    public static void invalidateCache() {
        cachedHtml = null;
        cacheTimestamp = 0;
    }
}
