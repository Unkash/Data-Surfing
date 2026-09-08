package es.unkash.surfmalaga.ui;

import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import es.unkash.surfmalaga.databinding.ActivitySettingsBinding;

public class SettingsActivity extends AppCompatActivity {

    public static final String PREFS = "surf_settings";
    public static final String KEY_THEME = "theme_mode";
    public static final int THEME_AUTO   = 0;
    public static final int THEME_LIGHT  = 1;
    public static final int THEME_DARK   = 2;

    private ActivitySettingsBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySettingsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Ajustes");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // Cargar tema actual
        int current = getSharedPreferences(PREFS, MODE_PRIVATE)
                .getInt(KEY_THEME, THEME_AUTO);
        switch (current) {
            case THEME_LIGHT: binding.rbLight.setChecked(true); break;
            case THEME_DARK:  binding.rbDark.setChecked(true);  break;
            default:          binding.rbAuto.setChecked(true);  break;
        }

        binding.rgTheme.setOnCheckedChangeListener((group, checkedId) -> {
            int mode;
            int pref;
            if (checkedId == binding.rbLight.getId()) {
                mode = AppCompatDelegate.MODE_NIGHT_NO;
                pref = THEME_LIGHT;
            } else if (checkedId == binding.rbDark.getId()) {
                mode = AppCompatDelegate.MODE_NIGHT_YES;
                pref = THEME_DARK;
            } else {
                mode = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
                pref = THEME_AUTO;
            }
            getSharedPreferences(PREFS, MODE_PRIVATE).edit()
                    .putInt(KEY_THEME, pref).apply();
            AppCompatDelegate.setDefaultNightMode(mode);
        });
    }

    /** Aplicar tema guardado al arrancar la app */
    public static void applyTheme(android.content.Context ctx) {
        int pref = ctx.getSharedPreferences(PREFS, android.content.Context.MODE_PRIVATE)
                .getInt(KEY_THEME, THEME_AUTO);
        switch (pref) {
            case THEME_LIGHT:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO); break;
            case THEME_DARK:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES); break;
            default:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM); break;
        }
    }

    @Override public boolean onSupportNavigateUp() { finish(); return true; }
}
