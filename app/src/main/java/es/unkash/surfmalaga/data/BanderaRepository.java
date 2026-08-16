package es.unkash.surfmalaga.data;

import android.util.Log;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * Consulta el estado de la bandera de una playa al webservice SOAP
 * del Ayuntamiento de Málaga.
 * Endpoint: gestmovil.malaga.eu/BanderasPlayasSW/Service1.asmx
 *
 * IMPORTANTE: Solo cubre playas de Málaga capital.
 * Para el resto de municipios devuelve null.
 */
public class BanderaRepository {

    private static final String TAG = "BanderaRepo";
    private static final String ENDPOINT =
            "http://gestmovil.malaga.eu/BanderasPlayasSW/Service1.asmx";

    public enum Bandera {
        VERDE, AMARILLA, ROJA, SIN_DATOS
    }

    public interface Callback {
        void onResult(Bandera bandera, String ultimaActualizacion);
    }

    public static void getEstado(String nombrePlaya, Callback callback) {
        if (nombrePlaya == null) {
            callback.onResult(Bandera.SIN_DATOS, null);
            return;
        }

        new Thread(() -> {
            try {
                String soapBody =
                    "<?xml version=\"1.0\" encoding=\"utf-8\"?>" +
                    "<soap:Envelope xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" " +
                    "xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" " +
                    "xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">" +
                    "<soap:Body>" +
                    "<getEstadoPlaya xmlns=\"http://localhost/WebService\">" +
                    "<nom_playa>" + nombrePlaya + "</nom_playa>" +
                    "</getEstadoPlaya>" +
                    "</soap:Body>" +
                    "</soap:Envelope>";

                URL url = new URL(ENDPOINT);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "text/xml; charset=utf-8");
                conn.setRequestProperty("SOAPAction",
                        "\"http://localhost/WebService/getEstadoPlaya\"");
                conn.setRequestProperty("Host", "gestmovil.malaga.eu");
                conn.setDoOutput(true);
                conn.setConnectTimeout(8000);
                conn.setReadTimeout(8000);

                OutputStream os = conn.getOutputStream();
                os.write(soapBody.getBytes(StandardCharsets.UTF_8));
                os.flush();

                BufferedReader br = new BufferedReader(
                        new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) response.append(line);
                br.close();

                String xml = response.toString();
                Log.d(TAG, "Respuesta SOAP: " + xml);

                // Extraer el resultado — viene como array de strings separados por ;
                // Formato esperado: "VERDE;15/08/2026 10:31:17" o similar
                String resultado = extractTag(xml, "string");
                if (resultado == null || resultado.isEmpty()) {
                    callback.onResult(Bandera.SIN_DATOS, null);
                    return;
                }

                // El servicio devuelve múltiples <string>, cogemos el primero
                String[] partes = resultado.split(";");
                String estado = partes[0].trim().toUpperCase();
                String hora = partes.length > 1 ? partes[1].trim() : null;

                Bandera bandera;
                if (estado.contains("VERDE") || estado.contains("GREEN")) {
                    bandera = Bandera.VERDE;
                } else if (estado.contains("AMARILL") || estado.contains("YELLOW")
                        || estado.contains("NARANJ")) {
                    bandera = Bandera.AMARILLA;
                } else if (estado.contains("ROJA") || estado.contains("RED")) {
                    bandera = Bandera.ROJA;
                } else {
                    bandera = Bandera.SIN_DATOS;
                }

                callback.onResult(bandera, hora);

            } catch (Exception e) {
                Log.e(TAG, "Error consultando bandera de " + nombrePlaya, e);
                callback.onResult(Bandera.SIN_DATOS, null);
            }
        }).start();
    }

    private static String extractTag(String xml, String tag) {
        try {
            String open  = "<" + tag + ">";
            String close = "</" + tag + ">";
            int start = xml.indexOf(open);
            if (start < 0) return null;
            start += open.length();
            int end = xml.indexOf(close, start);
            if (end < 0) return null;
            return xml.substring(start, end).trim();
        } catch (Exception e) {
            return null;
        }
    }
}
