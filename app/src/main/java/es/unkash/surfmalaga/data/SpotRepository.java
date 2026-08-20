package es.unkash.surfmalaga.data;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class SpotRepository {

    private static final String TAG = "SpotRepository";
    private static SpotRepository instance;
    private final OkHttpClient client = new OkHttpClient();

    // Lista de todos los spots de Málaga
    private final List<SurfSpot> spots = new ArrayList<>();

    private SpotRepository() {
        initSpots();
    }

    public static SpotRepository getInstance() {
        if (instance == null) instance = new SpotRepository();
        return instance;
    }

    private void initSpots() {
        // id, nombre, lat, lon, offshoreMin, offshoreMax, swellDirMin, swellDirMax
        spots.add(new SurfSpot("misericordia","La Misericordia",      36.6912, -4.4406, 290, 340, 100, 160, "https://meteo365.es/livecams/malaga.php",            "La Misericordia"));
        spots.add(new SurfSpot("guadalmar",   "Guadalmar",           36.6627, -4.4579, 290, 340, 100, 160, "https://es.surf-forecast.com/breaks/Guadalmar/webcams/latest", "Guadalmar - San Julian"));
        spots.add(new SurfSpot("losalamos",   "Los Álamos",          36.6375, -4.4833, 290, 340, 100, 160, null,                                                null));
        spots.add(new SurfSpot("malapesquera","Malapesquera",        36.5953, -4.5188, 290, 340, 100, 160, null,                                                null));
        spots.add(new SurfSpot("carihuela",   "La Carihuela",        36.6072, -4.5049, 290, 340, 100, 160, null,                                                null));
        spots.add(new SurfSpot("sunset",      "Sunset / Bil-Bil",    36.5903, -4.5273, 290, 340, 100, 160, "https://meteo365.es/livecams/benalmadena.php",       null));
        spots.add(new SurfSpot("elchino",     "El Chino (Fuengirola)",36.5375,-4.6190, 280, 320, 100, 160, "https://meteo365.es/livecams/fuengirola-puerto.php", null));
        spots.add(new SurfSpot("calaburras",  "Faro Calaburras",     36.5074, -4.6405, 300, 340, 120, 180, null,                                                null));
        spots.add(new SurfSpot("cabopino",    "Cabopino",            36.4840, -4.7430, 350,  30, 100, 140, null,                                                null));
        spots.add(new SurfSpot("malagueta",   "La Malagueta",        36.7164, -4.4083, 310, 360, 100, 160, "https://meteo365.es/livecams/malaga.php",            "La Malagueta"));
        spots.add(new SurfSpot("chanquete",   "El Chanquete / Dedo", 36.7169, -4.3499,  10,  50, 100, 160, null,                                                "El Dedo"));
        spots.add(new SurfSpot("larana",      "La Araña",            36.7127, -4.3242,  10,  50, 100, 160, null,                                                "Playa de la Araña"));
        spots.add(new SurfSpot("benajarafe",  "Benajarafe",          36.7180, -4.1960, 330,  30, 120, 200, null,                                                null));
        spots.add(new SurfSpot("lagos",       "Lagos / Pijil",       36.7420, -4.0020, 330,  30, 120, 200, null,                                                null));
    }

    public List<SurfSpot> getSpots() {
        return spots;
    }

    public SurfSpot getSpotById(String id) {
        for (SurfSpot s : spots) {
            if (s.id.equals(id)) return s;
        }
        return null;
    }

    public interface Callback {
        void onSuccess(SurfData data);
        void onError(String message);
    }

    public void fetchSpotData(SurfSpot spot, Callback callback) {
        spot.isLoading = true;
        spot.errorMsg = null;

        String marineUrl = "https://marine-api.open-meteo.com/v1/marine"
                + "?latitude=" + spot.lat
                + "&longitude=" + spot.lon
                + "&hourly=wave_height,wave_period,wave_direction,"
                + "swell_wave_height,swell_wave_period,swell_wave_direction"
                + "&forecast_days=3&timezone=Europe%2FMadrid";

        String weatherUrl = "https://api.open-meteo.com/v1/forecast"
                + "?latitude=" + spot.lat
                + "&longitude=" + spot.lon
                + "&hourly=wind_speed_10m,wind_gusts_10m,wind_direction_10m"
                + "&forecast_days=3&wind_speed_unit=kmh&timezone=Europe%2FMadrid";

        new Thread(() -> {
            try {
                String marineJson  = fetch(marineUrl);
                String weatherJson = fetch(weatherUrl);

                if (marineJson == null || weatherJson == null) {
                    spot.isLoading = false;
                    spot.errorMsg = "Error de red";
                    callback.onError("Error de red");
                    return;
                }

                SurfData data = parse(marineJson, weatherJson);
                spot.currentData = data;
                spot.isLoading = false;
                callback.onSuccess(data);

            } catch (Exception e) {
                Log.e(TAG, "Error fetching " + spot.id, e);
                spot.isLoading = false;
                spot.errorMsg = e.getMessage();
                callback.onError(e.getMessage());
            }
        }).start();
    }

    private String fetch(String url) throws IOException {
        Request request = new Request.Builder().url(url).build();
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) return null;
            return response.body() != null ? response.body().string() : null;
        }
    }

    private SurfData parse(String marineJson, String weatherJson) throws Exception {
        SurfData data = new SurfData();

        JSONObject marine  = new JSONObject(marineJson);
        JSONObject weather = new JSONObject(weatherJson);

        JSONObject mh = marine.getJSONObject("hourly");
        JSONObject wh = weather.getJSONObject("hourly");

        JSONArray times    = mh.getJSONArray("time");
        JSONArray waveH    = mh.getJSONArray("wave_height");
        JSONArray waveP    = mh.getJSONArray("wave_period");
        JSONArray waveDir  = mh.getJSONArray("wave_direction");
        JSONArray swellH   = mh.getJSONArray("swell_wave_height");
        JSONArray swellP   = mh.getJSONArray("swell_wave_period");
        JSONArray swellDir = mh.getJSONArray("swell_wave_direction");
        JSONArray windSpd  = wh.getJSONArray("wind_speed_10m");
        JSONArray windGust = wh.getJSONArray("wind_gusts_10m");
        JSONArray windDir  = wh.getJSONArray("wind_direction_10m");

        // Condiciones actuales = primer índice
        data.waveHeight    = getF(waveH, 0);
        data.wavePeriod    = getF(waveP, 0);
        data.waveDirection = getI(waveDir, 0);
        data.swellHeight   = getF(swellH, 0);
        data.swellPeriod   = getF(swellP, 0);
        data.swellDirection= getI(swellDir, 0);
        data.windSpeed     = getF(windSpd, 0);
        data.windGusts     = getF(windGust, 0);
        data.windDirection = getI(windDir, 0);
        data.fetchTime     = times.getString(0);

        // Previsión 48h
        List<SurfData.HourlyEntry> forecast = new ArrayList<>();
        int limit = Math.min(48, times.length());
        for (int i = 0; i < limit; i++) {
            forecast.add(new SurfData.HourlyEntry(
                    times.getString(i),
                    getF(waveH, i), getF(waveP, i), getI(waveDir, i),
                    getF(swellH, i),
                    getF(windSpd, i), getF(windGust, i), getI(windDir, i)
            ));
        }
        data.hourlyForecast = forecast;
        return data;
    }

    private float getF(JSONArray a, int i) {
        try { return a.isNull(i) ? 0f : (float) a.getDouble(i); }
        catch (Exception e) { return 0f; }
    }

    private int getI(JSONArray a, int i) {
        try { return a.isNull(i) ? 0 : (int) a.getDouble(i); }
        catch (Exception e) { return 0; }
    }
}
