package es.unkash.surfmalaga.data;

import android.util.Log;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Obtiene el estado de todas las banderas de playas de Málaga capital
 * mediante la API SOAP del Ayuntamiento:
 *   http://gestmovil.malaga.eu/BanderasPlayasSW/Service1.asmx
 *   Método: getBanderas(idioma: "es")
 *
 * La respuesta es un array de <string> con formato:
 *   "NombrePlaya;ColorBandera;Fecha;Hora" o similar
 *
 * Se hace UNA sola llamada para todas las playas y se cachea 5 minutos.
 */
public class BanderaRepository {

    private static final String TAG = "BanderaRepo";
    private static final String ENDPOINT =
            "http://gestmovil.malaga.eu/BanderasPlayasSW/Service1.asmx";
    private static final String SOAP_ACTION =
            "\"http://localhost/WebService/getBanderas\"";

    // Caché: mapa de nombreAyuntamiento → EstadoBandera
    private static Map<String, EstadoBandera> cache = null;
    private static long cacheTimestamp = 0;
    private static final long CACHE_TTL_MS = 5 * 60 * 1000;

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

    // Última respuesta raw del WS (para debug en UI)
    public static String lastRawResponse = null;

    public interface Callback {
        void onResult(EstadoBandera estado);
    }

    /**
     * Obtiene el estado de bandera para un spot concreto.
     * @param nombreAyuntamiento Nombre de la playa tal como lo devuelve el WS
     */
    public static void getEstado(String nombreAyuntamiento, Callback callback) {
        if (nombreAyuntamiento == null) {
            callback.onResult(new EstadoBandera());
            return;
        }
        new Thread(() -> {
            try {
                Map<String, EstadoBandera> banderas = getAllBanderas();
                EstadoBandera estado = null;
                if (banderas != null) {
                    // Buscar por nombre exacto primero, luego por contenido
                    for (Map.Entry<String, EstadoBandera> e : banderas.entrySet()) {
                        if (e.getKey().equalsIgnoreCase(nombreAyuntamiento) ||
                            e.getKey().toLowerCase().contains(nombreAyuntamiento.toLowerCase()) ||
                            nombreAyuntamiento.toLowerCase().contains(e.getKey().toLowerCase())) {
                            estado = e.getValue();
                            break;
                        }
                    }
                }
                callback.onResult(estado != null ? estado : new EstadoBandera());
            } catch (Exception e) {
                Log.e(TAG, "Error obteniendo bandera de " + nombreAyuntamiento, e);
                callback.onResult(new EstadoBandera());
            }
        }).start();
    }

    /**
     * Obtiene todas las banderas en una sola llamada SOAP.
     * Resultado cacheado 5 minutos.
     */
    private static Map<String, EstadoBandera> getAllBanderas() throws Exception {
        long now = System.currentTimeMillis();
        if (cache != null && (now - cacheTimestamp) < CACHE_TTL_MS) {
            return cache;
        }

        String soapBody =
            "<?xml version=\"1.0\" encoding=\"utf-8\"?>" +
            "<soap:Envelope " +
            "  xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" " +
            "  xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" " +
            "  xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">" +
            "  <soap:Body>" +
            "    <getBanderas xmlns=\"http://localhost/WebService\">" +
            "      <idioma>es</idioma>" +
            "    </getBanderas>" +
            "  </soap:Body>" +
            "</soap:Envelope>";

        HttpURLConnection conn = (HttpURLConnection)
                new URL(ENDPOINT).openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "text/xml; charset=utf-8");
        conn.setRequestProperty("SOAPAction", SOAP_ACTION);
        conn.setRequestProperty("Host", "gestmovil.malaga.eu");
        conn.setRequestProperty("User-Agent",
                "Mozilla/5.0 (Android) SurfMalaga/4.5");
        conn.setDoOutput(true);
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(10000);

        OutputStream os = conn.getOutputStream();
        os.write(soapBody.getBytes(StandardCharsets.UTF_8));
        os.flush();
        os.close();

        int code = conn.getResponseCode();
        if (code != 200) {
            Log.e(TAG, "HTTP " + code + " del WS de banderas");
            return null;
        }

        BufferedReader br = new BufferedReader(
                new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) sb.append(line);
        br.close();

        String xml = sb.toString();
        Log.d(TAG, "Respuesta SOAP getBanderas: " + xml.substring(0, Math.min(500, xml.length())));
        lastRawResponse = xml;

        cache = parseResponse(xml);
        cacheTimestamp = now;
        return cache;
    }

    /**
     * Parsea la respuesta XML. Cada <string> contiene datos de una playa.
     * Formato esperado basado en la web: campos separados por ; o similares.
     * Ejemplo probable: "La Misericordia;BanderaVerde.png;15/08/2026;10:31:17"
     * o bien texto plano del tipo "La Misericordia|VERDE|10:31"
     *
     * Se detecta el color buscando palabras clave en cada string.
     */
    private static Map<String, EstadoBandera> parseResponse(String xml) {
        Map<String, EstadoBandera> result = new HashMap<>();

        // Extraer todos los <string>...</string>
        Pattern p = Pattern.compile("<string>([^<]*)</string>",
                Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(xml);

        // Los strings vienen en pares o grupos — intentamos leerlos de forma flexible
        // Guardamos todos primero
        java.util.List<String> strings = new java.util.ArrayList<>();
        while (m.find()) {
            String val = m.group(1).trim();
            if (!val.isEmpty()) strings.add(val);
        }

        Log.d(TAG, "Strings recibidos: " + strings.size());
        for (String s : strings) Log.d(TAG, "  → " + s);

        // Intentar parsear: cada string puede ser "nombre;datos" o solo nombre/solo datos
        // Estrategia: si contiene ; asumimos "nombre;bandera[;fecha;hora]"
        for (String s : strings) {
            if (s.contains(";")) {
                String[] parts = s.split(";");
                if (parts.length >= 2) {
                    String nombre = parts[0].trim();
                    EstadoBandera estado = parseEstadoFromString(s);
                    result.put(nombre, estado);
                }
            }
        }

        // Si no había separadores, intentar leer de dos en dos (nombre, estado)
        if (result.isEmpty() && strings.size() >= 2) {
            for (int i = 0; i + 1 < strings.size(); i += 2) {
                String nombre = strings.get(i);
                String datos  = strings.get(i + 1);
                EstadoBandera estado = parseEstadoFromString(datos);
                result.put(nombre, estado);
            }
        }

        return result;
    }

    private static EstadoBandera parseEstadoFromString(String s) {
        EstadoBandera estado = new EstadoBandera();
        String lower = s.toLowerCase();

        // Color
        if      (lower.contains("verde"))    estado.color = Color.VERDE;
        else if (lower.contains("amarilla")) estado.color = Color.AMARILLA;
        else if (lower.contains("roja"))     estado.color = Color.ROJA;
        else if (lower.contains("blanca"))   estado.color = Color.BLANCA;

        // Extras
        estado.medusas    = lower.contains("medusa");
        estado.salvamento = lower.contains("naranja") || lower.contains("salvamento");

        // Hora: buscar patrón HH:MM o DD/MM/YYYY HH:MM:SS
        Matcher mFecha = Pattern.compile(
                "(\\d{2}/\\d{2}/\\d{4}\\s+\\d{2}:\\d{2}:\\d{2})").matcher(s);
        if (mFecha.find()) {
            estado.hora = mFecha.group(1);
        } else {
            Matcher mHora = Pattern.compile("(\\d{2}:\\d{2})").matcher(s);
            if (mHora.find()) estado.hora = mHora.group(1);
        }

        return estado;
    }

    public static void invalidateCache() {
        cache = null;
        cacheTimestamp = 0;
    }
}
