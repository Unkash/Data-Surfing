package es.unkash.surfmalaga.ui;

import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import es.unkash.surfmalaga.data.AlertConfig;
import es.unkash.surfmalaga.data.AlertStorage;
import es.unkash.surfmalaga.databinding.ActivityAlertsBinding;
import es.unkash.surfmalaga.utils.WorkerScheduler;

public class AlertsActivity extends AppCompatActivity {

    private ActivityAlertsBinding binding;
    private AlertConfig config;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAlertsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Configurar alertas");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        config = AlertStorage.load(this);
        populateUI();

        binding.btnSave.setOnClickListener(v -> saveConfig());
        binding.switchEnabled.setOnCheckedChangeListener((btn, checked) -> {
            setFieldsEnabled(checked);
        });
    }

    private void populateUI() {
        binding.switchEnabled.setChecked(config.enabled);

        binding.switchWave.setChecked(config.waveAlertEnabled);
        binding.etWaveMin.setText(String.valueOf(config.waveMinHeight));
        binding.etWaveMax.setText(String.valueOf(config.waveMaxHeight));
        binding.etWavePeriod.setText(String.valueOf(config.wavePeriodMin));

        binding.switchWind.setChecked(config.windAlertEnabled);
        binding.etWindMax.setText(String.valueOf(config.windMaxSpeed));
        binding.etWindMin.setText(String.valueOf(config.windMinSpeed));

        binding.switchSwell.setChecked(config.swellAlertEnabled);
        binding.etSwellMin.setText(String.valueOf(config.swellMinHeight));

        binding.switchWindDir.setChecked(config.windDirectionEnabled);
        binding.etWindDirMin.setText(String.valueOf(config.windDirMin));
        binding.etWindDirMax.setText(String.valueOf(config.windDirMax));

        binding.etCheckInterval.setText(String.valueOf(config.checkInterval));

        setFieldsEnabled(config.enabled);
    }

    private void setFieldsEnabled(boolean enabled) {
        binding.switchWave.setEnabled(enabled);
        binding.etWaveMin.setEnabled(enabled);
        binding.etWaveMax.setEnabled(enabled);
        binding.etWavePeriod.setEnabled(enabled);
        binding.switchWind.setEnabled(enabled);
        binding.etWindMax.setEnabled(enabled);
        binding.etWindMin.setEnabled(enabled);
        binding.switchSwell.setEnabled(enabled);
        binding.etSwellMin.setEnabled(enabled);
        binding.switchWindDir.setEnabled(enabled);
        binding.etWindDirMin.setEnabled(enabled);
        binding.etWindDirMax.setEnabled(enabled);
        binding.etCheckInterval.setEnabled(enabled);
    }

    private void saveConfig() {
        try {
            config.enabled = binding.switchEnabled.isChecked();
            config.waveAlertEnabled = binding.switchWave.isChecked();
            config.waveMinHeight = Float.parseFloat(binding.etWaveMin.getText().toString());
            config.waveMaxHeight = Float.parseFloat(binding.etWaveMax.getText().toString());
            config.wavePeriodMin = Float.parseFloat(binding.etWavePeriod.getText().toString());
            config.windAlertEnabled = binding.switchWind.isChecked();
            config.windMaxSpeed = Float.parseFloat(binding.etWindMax.getText().toString());
            config.windMinSpeed = Float.parseFloat(binding.etWindMin.getText().toString());
            config.swellAlertEnabled = binding.switchSwell.isChecked();
            config.swellMinHeight = Float.parseFloat(binding.etSwellMin.getText().toString());
            config.windDirectionEnabled = binding.switchWindDir.isChecked();
            config.windDirMin = Integer.parseInt(binding.etWindDirMin.getText().toString());
            config.windDirMax = Integer.parseInt(binding.etWindDirMax.getText().toString());
            config.checkInterval = Integer.parseInt(binding.etCheckInterval.getText().toString());

            AlertStorage.save(this, config);

            if (config.enabled) {
                WorkerScheduler.schedule(this, config.checkInterval);
                Toast.makeText(this, "Alertas activadas (cada " + config.checkInterval + "h)", Toast.LENGTH_SHORT).show();
            } else {
                WorkerScheduler.cancel(this);
                Toast.makeText(this, "Alertas desactivadas", Toast.LENGTH_SHORT).show();
            }

            finish();
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Comprueba los valores introducidos", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
