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

/**
 * Obtiene datos de olas y viento desde Open-Meteo (gratuito, sin API key).
 * Coordenadas: Guadalmar, Málaga  lat=36.68  lon=-4.51
 */
public class OpenMeteoRepository {

    private static final String TAG = "OpenMeteoRepo";

    // Coordenadas de Guadalmar (playa de surf en Málaga)
    private static final double LAT = 36.68;
    private static final double LON = -4.51;

    private static final String MARINE_URL =
            "https://marine-api.open-meteo.com/v1/marine" +
            "?latitude=" + LAT +
            "&longitude=" + LON +
            "&hourly=wave_height,wave_period,wave_direction,swell_wave_height,swell_wave_period,swell_wave_direction" +
            "&forecast_days=3" +
            "&timezone=Europe%2FMadrid";

    private static final String WEATHER_URL =
            "https://api.open-meteo.com/v1/forecast" +
            "?latitude=" + LAT +
            "&longitude=" + LON +
            "&hourly=wind_speed_10m,wind_gusts_10m,wind_direction_10m" +
            "&forecast_days=3" +
            "&wind_speed_unit=kmh" +
            "&timezone=Europe%2FMadrid";

    private final OkHttpClient client = new OkHttpClient();

    public interface Callback {
        void onSuccess(SurfData data);
        void onError(String message);
    }

    public void fetchData(Callback callback) {
        new Thread(() -> {
            try {
                String marineJson = fetch(MARINE_URL);
                String weatherJson = fetch(WEATHER_URL);

                if (marineJson == null || weatherJson == null) {
                    callback.onError("Error de red al obtener datos");
                    return;
                }

                SurfData data = parse(marineJson, weatherJson);
                callback.onSuccess(data);

            } catch (Exception e) {
                Log.e(TAG, "Error fetching data", e);
                callback.onError("Error: " + e.getMessage());
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

        JSONObject marine = new JSONObject(marineJson);
        JSONObject weather = new JSONObject(weatherJson);

        JSONObject marineHourly = marine.getJSONObject("hourly");
        JSONObject weatherHourly = weather.getJSONObject("hourly");

        JSONArray times        = marineHourly.getJSONArray("time");
        JSONArray waveH        = marineHourly.getJSONArray("wave_height");
        JSONArray waveP        = marineHourly.getJSONArray("wave_period");
        JSONArray waveDir      = marineHourly.getJSONArray("wave_direction");
        JSONArray swellH       = marineHourly.getJSONArray("swell_wave_height");
        JSONArray swellP       = marineHourly.getJSONArray("swell_wave_period");
        JSONArray swellDir     = marineHourly.getJSONArray("swell_wave_direction");
        JSONArray windSpd      = weatherHourly.getJSONArray("wind_speed_10m");
        JSONArray windGust     = weatherHourly.getJSONArray("wind_gusts_10m");
        JSONArray windDir      = weatherHourly.getJSONArray("wind_direction_10m");

        // Condiciones actuales = primer índice disponible
        data.waveHeight    = (float) getDouble(waveH, 0);
        data.wavePeriod    = (float) getDouble(waveP, 0);
        data.waveDirection = (int) getDouble(waveDir, 0);
        data.swellHeight   = (float) getDouble(swellH, 0);
        data.swellPeriod   = (float) getDouble(swellP, 0);
        data.swellDirection= (int) getDouble(swellDir, 0);
        data.windSpeed     = (float) getDouble(windSpd, 0);
        data.windGusts     = (float) getDouble(windGust, 0);
        data.windDirection = (int) getDouble(windDir, 0);
        data.fetchTime     = times.getString(0);

        // Previsión horaria: próximas 48 horas
        List<SurfData.HourlyEntry> forecast = new ArrayList<>();
        int limit = Math.min(48, times.length());
        for (int i = 0; i < limit; i++) {
            forecast.add(new SurfData.HourlyEntry(
                    times.getString(i),
                    (float) getDouble(waveH, i),
                    (float) getDouble(waveP, i),
                    (int) getDouble(waveDir, i),
                    (float) getDouble(swellH, i),
                    (float) getDouble(windSpd, i),
                    (float) getDouble(windGust, i),
                    (int) getDouble(windDir, i)
            ));
        }
        data.hourlyForecast = forecast;

        return data;
    }

    private double getDouble(JSONArray arr, int i) {
        try {
            if (arr.isNull(i)) return 0.0;
            return arr.getDouble(i);
        } catch (Exception e) {
            return 0.0;
        }
    }
}
