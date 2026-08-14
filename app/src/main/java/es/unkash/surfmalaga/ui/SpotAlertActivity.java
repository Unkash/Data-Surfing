package es.unkash.surfmalaga.ui;

import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import es.unkash.surfmalaga.data.SpotRepository;
import es.unkash.surfmalaga.data.SpotStorage;
import es.unkash.surfmalaga.data.SurfSpot;
import es.unkash.surfmalaga.databinding.ActivitySpotAlertBinding;

public class SpotAlertActivity extends AppCompatActivity {

    private ActivitySpotAlertBinding binding;
    private SurfSpot spot;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySpotAlertBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        String spotId = getIntent().getStringExtra("spot_id");
        spot = SpotRepository.getInstance().getSpotById(spotId);
        if (spot == null) { finish(); return; }

        SpotStorage.loadSpot(this, spot);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Alertas — " + spot.name);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        populateUI();
        binding.btnSave.setOnClickListener(v -> saveAndClose());
        binding.switchEnabled.setOnCheckedChangeListener((btn, checked) -> setFieldsEnabled(checked));
    }

    private void populateUI() {
        binding.switchEnabled.setChecked(spot.alertEnabled);
        binding.etWaveMin.setText(String.valueOf(spot.alertWaveMinHeight));
        binding.etWaveMax.setText(String.valueOf(spot.alertWaveMaxHeight));
        binding.etWavePeriod.setText(String.valueOf(spot.alertWavePeriodMin));
        binding.etSwellMin.setText(String.valueOf(spot.alertSwellMinHeight));
        binding.etWindMax.setText(String.valueOf(spot.alertWindMaxSpeed));
        binding.switchWindDir.setChecked(spot.alertCheckWindDir);
        binding.switchSwellDir.setChecked(spot.alertCheckSwellDir);
        binding.etOffshoreMin.setText(String.valueOf(spot.offshoreWindMin));
        binding.etOffshoreMax.setText(String.valueOf(spot.offshoreWindMax));
        binding.etSwellDirMin.setText(String.valueOf(spot.swellDirMin));
        binding.etSwellDirMax.setText(String.valueOf(spot.swellDirMax));
        setFieldsEnabled(spot.alertEnabled);
    }

    private void setFieldsEnabled(boolean enabled) {
        binding.etWaveMin.setEnabled(enabled);
        binding.etWaveMax.setEnabled(enabled);
        binding.etWavePeriod.setEnabled(enabled);
        binding.etSwellMin.setEnabled(enabled);
        binding.etWindMax.setEnabled(enabled);
        binding.switchWindDir.setEnabled(enabled);
        binding.switchSwellDir.setEnabled(enabled);
        binding.etOffshoreMin.setEnabled(enabled);
        binding.etOffshoreMax.setEnabled(enabled);
        binding.etSwellDirMin.setEnabled(enabled);
        binding.etSwellDirMax.setEnabled(enabled);
    }

    private void saveAndClose() {
        try {
            spot.alertEnabled        = binding.switchEnabled.isChecked();
            spot.alertWaveMinHeight  = Float.parseFloat(binding.etWaveMin.getText().toString());
            spot.alertWaveMaxHeight  = Float.parseFloat(binding.etWaveMax.getText().toString());
            spot.alertWavePeriodMin  = Float.parseFloat(binding.etWavePeriod.getText().toString());
            spot.alertSwellMinHeight = Float.parseFloat(binding.etSwellMin.getText().toString());
            spot.alertWindMaxSpeed   = Float.parseFloat(binding.etWindMax.getText().toString());
            spot.alertCheckWindDir   = binding.switchWindDir.isChecked();
            spot.alertCheckSwellDir  = binding.switchSwellDir.isChecked();
            spot.offshoreWindMin     = Integer.parseInt(binding.etOffshoreMin.getText().toString());
            spot.offshoreWindMax     = Integer.parseInt(binding.etOffshoreMax.getText().toString());
            spot.swellDirMin         = Integer.parseInt(binding.etSwellDirMin.getText().toString());
            spot.swellDirMax         = Integer.parseInt(binding.etSwellDirMax.getText().toString());

            SpotStorage.saveSpot(this, spot);
            Toast.makeText(this, "Guardado", Toast.LENGTH_SHORT).show();
            finish();
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Comprueba los valores", Toast.LENGTH_SHORT).show();
        }
    }

    @Override public boolean onSupportNavigateUp() { finish(); return true; }
}
