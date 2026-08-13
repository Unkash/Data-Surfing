package es.unkash.surfmalaga.data;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONObject;

public class AlertStorage {

    private static final String PREFS = "surf_alerts";
    private static final String KEY = "alert_config";

    public static void save(Context ctx, AlertConfig config) {
        try {
            JSONObject json = new JSONObject();
            json.put("enabled", config.enabled);
            json.put("waveAlertEnabled", config.waveAlertEnabled);
            json.put("waveMinHeight", config.waveMinHeight);
            json.put("waveMaxHeight", config.waveMaxHeight);
            json.put("wavePeriodMin", config.wavePeriodMin);
            json.put("windAlertEnabled", config.windAlertEnabled);
            json.put("windMaxSpeed", config.windMaxSpeed);
            json.put("windMinSpeed", config.windMinSpeed);
            json.put("swellAlertEnabled", config.swellAlertEnabled);
            json.put("swellMinHeight", config.swellMinHeight);
            json.put("windDirectionEnabled", config.windDirectionEnabled);
            json.put("windDirMin", config.windDirMin);
            json.put("windDirMax", config.windDirMax);
            json.put("checkHour", config.checkHour);
            json.put("checkInterval", config.checkInterval);

            ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
               .edit().putString(KEY, json.toString()).apply();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static AlertConfig load(Context ctx) {
        AlertConfig config = new AlertConfig();
        try {
            SharedPreferences prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
            String json = prefs.getString(KEY, null);
            if (json == null) return config;

            JSONObject obj = new JSONObject(json);
            config.enabled              = obj.optBoolean("enabled", false);
            config.waveAlertEnabled     = obj.optBoolean("waveAlertEnabled", false);
            config.waveMinHeight        = (float) obj.optDouble("waveMinHeight", 0.5);
            config.waveMaxHeight        = (float) obj.optDouble("waveMaxHeight", 3.0);
            config.wavePeriodMin        = (float) obj.optDouble("wavePeriodMin", 8.0);
            config.windAlertEnabled     = obj.optBoolean("windAlertEnabled", false);
            config.windMaxSpeed         = (float) obj.optDouble("windMaxSpeed", 20.0);
            config.windMinSpeed         = (float) obj.optDouble("windMinSpeed", 0.0);
            config.swellAlertEnabled    = obj.optBoolean("swellAlertEnabled", false);
            config.swellMinHeight       = (float) obj.optDouble("swellMinHeight", 0.3);
            config.windDirectionEnabled = obj.optBoolean("windDirectionEnabled", false);
            config.windDirMin           = obj.optInt("windDirMin", 270);
            config.windDirMax           = obj.optInt("windDirMax", 340);
            config.checkHour            = obj.optInt("checkHour", 7);
            config.checkInterval        = obj.optInt("checkInterval", 3);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return config;
    }
}
