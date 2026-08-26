package es.unkash.surfmalaga.data;

import android.util.Log;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Obtiene el estado de todas las banderas de playas de Málaga capital
 * usando getTodosEstados() del WS SOAP del Ayuntamiento.
 * Sin parámetros — devuelve todas las playas de una vez.
 */
public class BanderaRepository {

    private static final String TAG = "BanderaRepo";
    private static final String ENDPOINT =
            "http://gestmovil.malaga.eu/BanderasPlayasSW/Service1.asmx";

    // Caché 5 minutos
    private static Map<String, EstadoBandera> cache = null;
    private static long cacheTimestamp = 0;
    private static final long CACHE_TTL_MS = 5 * 60 * 1000;

    // Para debug en UI
    public static String lastRawResponse = null;

    public enum Color { VERDE, AMARILLA, ROJA, BLANCA, SIN_DATOS }

    public static class EstadoBandera {
        public Color color = Color.SIN_DATOS;
        public boolean medusas = false;
        public boolean salvamento = false;
        public String hora = null;

        public String toTexto() {
            if (color == Color.SIN_DATOS) return "Sin datos de bandera";
            StringBuilder sb = new StringBuilder();
            switch (color) {
                case VERDE:    sb.append("🟢 Bandera VERDE — Baño permitido"); break;
                case AMARILLA: sb.append("🟡 Bandera AMARILLA — Precaución"); break;
                case ROJA:     sb.append("🔴 Bandera ROJA — Baño prohibido"); break;
                case BLANCA:   sb.append("⚪ Bandera BLANCA — Fuera de temporada"); break;
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

    public static void getEstado(String nombreAyuntamiento, Callback callback) {
        if (nombreAyuntamiento == null) {
            callback.onResult(new EstadoBandera());
            return;
        }
        new Thread(() -> {
            try {
                Map<String, EstadoBandera> banderas = getAllEstados();
                EstadoBandera estado = null;
                if (banderas != null) {
                    for (Map.Entry<String, EstadoBandera> e : banderas.entrySet()) {
                        String key = e.getKey().toLowerCase()
                                .replace("á","a").replace("é","e")
                                .replace("í","i").replace("ó","o").replace("ú","u");
                        String search = nombreAyuntamiento.toLowerCase()
                                .replace("á","a").replace("é","e")
                                .replace("í","i").replace("ó","o").replace("ú","u");
                        if (key.equalsIgnoreCase(search) ||
                            key.contains(search) || search.contains(key)) {
                            estado = e.getValue();
                            break;
                        }
                    }
                    if (estado == null) {
                        lastRawResponse = "Spot '" + nombreAyuntamiento +
                            "' no encontrado.\nClaves disponibles: " +
                            banderas.keySet().toString() +
                            "\n\nRAW:\n" + lastRawResponse;
                    }
                }
                callback.onResult(estado != null ? estado : new EstadoBandera());
            } catch (Exception e) {
                Log.e(TAG, "Error: " + e.getMessage(), e);
                lastRawResponse = "EXCEPCIÓN: " + e.getMessage();
                callback.onResult(new EstadoBandera());
            }
        }).start();
    }

    private static Map<String, EstadoBandera> getAllEstados() throws Exception {
        long now = System.currentTimeMillis();
        if (cache != null && (now - cacheTimestamp) < CACHE_TTL_MS) {
            return cache;
        }

        String soapBody =
            "<?xml version=\"1.0\" encoding=\"utf-8\"?>" +
            "<soap:Envelope " +
            "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" " +
            "xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" " +
            "xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">" +
            "<soap:Body>" +
            "<getTodosEstados xmlns=\"http://localhost/WebService\" />" +
            "</soap:Body>" +
            "</soap:Envelope>";

        HttpURLConnection conn = (HttpURLConnection)
                new URL(ENDPOINT).openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "text/xml; charset=utf-8");
        conn.setRequestProperty("SOAPAction",
                "\"http://localhost/WebService/getTodosEstados\"");
        conn.setRequestProperty("Host", "gestmovil.malaga.eu");
        conn.setRequestProperty("User-Agent",
                "Mozilla/5.0 (Android) SurfMalaga/4.7");
        conn.setDoOutput(true);
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(10000);

        OutputStream os = conn.getOutputStream();
        os.write(soapBody.getBytes(StandardCharsets.UTF_8));
        os.flush();
        os.close();

        int code = conn.getResponseCode();
        if (code != 200) {
            lastRawResponse = "HTTP ERROR: " + code;
            return null;
        }

        BufferedReader br = new BufferedReader(
                new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) sb.append(line);
        br.close();

        String xml = sb.toString();
        lastRawResponse = xml.length() > 800 ? xml.substring(0, 800) + "..." : xml;
        Log.d(TAG, "getTodosEstados raw: " + lastRawResponse);

        cache = parseEstados(xml);
        cacheTimestamp = now;
        return cache;
    }

    /**
     * La respuesta viene como grupos de <string> por playa.
     * Basándonos en el patrón visto en getBanderas (id, color, descripción),
     * getTodosEstados probablemente devuelve: nombre_playa, color, descripcion, fecha, hora
     * Parseo flexible: detecta color y extras por palabras clave en cada string.
     */
    private static Map<String, EstadoBandera> parseEstados(String xml) {
        Map<String, EstadoBandera> result = new HashMap<>();

        Pattern p = Pattern.compile("<string>([^<]*)</string>", Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(xml);

        List<String> strings = new ArrayList<>();
        while (m.find()) {
            String val = m.group(1).trim();
            // Decodificar entidades HTML básicas
            val = val.replace("&lt;", "<").replace("&gt;", ">")
                     .replace("&amp;", "&").replace("&quot;", "\"")
                     .replace("<br>", " ").replace("<BR>", " ");
            if (!val.isEmpty()) strings.add(val);
        }

        Log.d(TAG, "Strings recibidos: " + strings.size());
        for (String s : strings) Log.d(TAG, "  → [" + s + "]");

        // Estrategia: agrupar en bloques donde el primer string parece un nombre de playa
        // (no contiene Verde/Roja/Amarilla/Blanca ni parece fecha/hora)
        String currentName = null;
        EstadoBandera currentEstado = null;

        for (String s : strings) {
            String lower = s.toLowerCase()
                    .replace("á","a").replace("é","e")
                    .replace("í","i").replace("ó","o").replace("ú","u");

            boolean hasColor = lower.contains("verde") || lower.contains("roja") ||
                               lower.contains("amarilla") || lower.contains("blanca");
            boolean looksLikeDate = s.matches(".*\\d{2}/\\d{2}/\\d{4}.*") ||
                                    s.matches("\\d{2}:\\d{2}.*");
            boolean looksLikeId = s.matches("\\d+");

            if (looksLikeId) continue; // saltar IDs numéricos

            if (!hasColor && !looksLikeDate && s.length() > 2 && s.length() < 60) {
                // Probablemente es un nombre de playa
                if (currentName != null && currentEstado != null) {
                    result.put(currentName, currentEstado);
                }
                currentName = s;
                currentEstado = new EstadoBandera();
            } else if (hasColor && currentEstado != null) {
                // Detectar color
                if      (lower.contains("verde"))    currentEstado.color = Color.VERDE;
                else if (lower.contains("amarilla")) currentEstado.color = Color.AMARILLA;
                else if (lower.contains("roja"))     currentEstado.color = Color.ROJA;
                else if (lower.contains("blanca"))   currentEstado.color = Color.BLANCA;

                currentEstado.medusas    = lower.contains("medusa");
                currentEstado.salvamento = lower.contains("naranja") ||
                                           lower.contains("salvamento");
            } else if (looksLikeDate && currentEstado != null) {
                // Fecha/hora de actualización
                Matcher mFecha = Pattern.compile(
                    "(\\d{2}/\\d{2}/\\d{4}\\s+\\d{2}:\\d{2}:\\d{2})").matcher(s);
                if (mFecha.find()) currentEstado.hora = mFecha.group(1);
                else {
                    Matcher mHora = Pattern.compile("(\\d{2}:\\d{2})").matcher(s);
                    if (mHora.find()) currentEstado.hora = mHora.group(1);
                }
            }
        }
        // Último bloque
        if (currentName != null && currentEstado != null) {
            result.put(currentName, currentEstado);
        }

        Log.d(TAG, "Playas parseadas: " + result.keySet());
        return result;
    }

    public static void invalidateCache() {
        cache = null;
        cacheTimestamp = 0;
        lastRawResponse = null;
    }
}
