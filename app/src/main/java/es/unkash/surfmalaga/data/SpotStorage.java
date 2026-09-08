package es.unkash.surfmalaga.data;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONObject;

import java.util.List;

public class SpotStorage {

    private static final String PREFS = "surf_spots";
    // Configuración global de alertas
    private static final String KEY_GLOBAL_ENABLED  = "global_enabled";
    private static final String KEY_CHECK_INTERVAL  = "check_interval";

    // ─── Global ───────────────────────────────────────────────────────────

    public static void saveGlobal(Context ctx, boolean enabled, int intervalHours) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .putBoolean(KEY_GLOBAL_ENABLED, enabled)
                .putInt(KEY_CHECK_INTERVAL, intervalHours)
                .apply();
    }

    public static boolean isGlobalEnabled(Context ctx) {
        return ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getBoolean(KEY_GLOBAL_ENABLED, false);
    }

    public static int getCheckInterval(Context ctx) {
        return ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getInt(KEY_CHECK_INTERVAL, 3);
    }

    // ─── Por spot ─────────────────────────────────────────────────────────

    public static void saveSpot(Context ctx, SurfSpot spot) {
        try {
            JSONObject json = new JSONObject();
            json.put("alertEnabled",       spot.alertEnabled);
            json.put("waveMinHeight",      spot.alertWaveMinHeight);
            json.put("waveMaxHeight",      spot.alertWaveMaxHeight);
            json.put("wavePeriodMin",      spot.alertWavePeriodMin);
            json.put("swellMinHeight",     spot.alertSwellMinHeight);
            json.put("windMaxSpeed",       spot.alertWindMaxSpeed);
            json.put("checkWindDir",       spot.alertCheckWindDir);
            json.put("checkSwellDir",      spot.alertCheckSwellDir);
            // Guardamos también los rangos offshore y swell por si el usuario los personaliza
            json.put("offshoreWindMin",    spot.offshoreWindMin);
            json.put("offshoreWindMax",    spot.offshoreWindMax);
            json.put("swellDirMin",        spot.swellDirMin);
            json.put("swellDirMax",        spot.swellDirMax);

            ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                    .putString("spot_" + spot.id, json.toString())
                    .apply();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void loadSpot(Context ctx, SurfSpot spot) {
        try {
            String raw = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                    .getString("spot_" + spot.id, null);
            if (raw == null) return;

            JSONObject json = new JSONObject(raw);
            spot.alertEnabled       = json.optBoolean("alertEnabled", false);
            spot.alertWaveMinHeight = (float) json.optDouble("waveMinHeight", 0.5);
            spot.alertWaveMaxHeight = (float) json.optDouble("waveMaxHeight", 0);
            spot.alertWavePeriodMin = (float) json.optDouble("wavePeriodMin", 8);
            spot.alertSwellMinHeight= (float) json.optDouble("swellMinHeight", 0.3);
            spot.alertWindMaxSpeed  = (float) json.optDouble("windMaxSpeed", 30);
            spot.alertCheckWindDir  = json.optBoolean("checkWindDir", true);
            spot.alertCheckSwellDir = json.optBoolean("checkSwellDir", true);
            spot.offshoreWindMin    = json.optInt("offshoreWindMin", spot.offshoreWindMin);
            spot.offshoreWindMax    = json.optInt("offshoreWindMax", spot.offshoreWindMax);
            spot.swellDirMin        = json.optInt("swellDirMin", spot.swellDirMin);
            spot.swellDirMax        = json.optInt("swellDirMax", spot.swellDirMax);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void loadAll(Context ctx, List<SurfSpot> spots) {
        for (SurfSpot spot : spots) {
            loadSpot(ctx, spot);
        }
    }
}
