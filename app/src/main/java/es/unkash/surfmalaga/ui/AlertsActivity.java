package es.unkash.surfmalaga.ui;

import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import java.util.List;

import es.unkash.surfmalaga.data.SpotRepository;
import es.unkash.surfmalaga.data.SpotStorage;
import es.unkash.surfmalaga.data.SurfSpot;
import es.unkash.surfmalaga.databinding.ActivityAlertsBinding;
import es.unkash.surfmalaga.utils.WorkerScheduler;

public class AlertsActivity extends AppCompatActivity {

    private ActivityAlertsBinding binding;
    private List<SurfSpot> spots;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAlertsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Configurar alertas");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        spots = SpotRepository.getInstance().getSpots();
        SpotStorage.loadAll(this, spots);

        binding.switchGlobal.setChecked(SpotStorage.isGlobalEnabled(this));
        binding.etInterval.setText(String.valueOf(SpotStorage.getCheckInterval(this)));

        // Lista de spots con acceso directo a sus alertas
        AlertSpotListAdapter adapter = new AlertSpotListAdapter(spots, spot -> {
            android.content.Intent intent = new android.content.Intent(this, SpotAlertActivity.class);
            intent.putExtra("spot_id", spot.id);
            startActivity(intent);
        });
        binding.recyclerSpots.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerSpots.setAdapter(adapter);

        binding.btnSaveGlobal.setOnClickListener(v -> saveGlobal());
    }

    @Override
    protected void onResume() {
        super.onResume();
        SpotStorage.loadAll(this, spots);
        binding.recyclerSpots.getAdapter().notifyDataSetChanged();
    }

    private void saveGlobal() {
        try {
            boolean enabled = binding.switchGlobal.isChecked();
            int interval = Integer.parseInt(binding.etInterval.getText().toString());
            SpotStorage.saveGlobal(this, enabled, interval);

            if (enabled) {
                WorkerScheduler.schedule(this, interval);
                Toast.makeText(this, "Alertas activadas (cada " + interval + "h)", Toast.LENGTH_SHORT).show();
            } else {
                WorkerScheduler.cancel(this);
                Toast.makeText(this, "Alertas desactivadas", Toast.LENGTH_SHORT).show();
            }
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Intervalo inválido", Toast.LENGTH_SHORT).show();
        }
    }

    @Override public boolean onSupportNavigateUp() { finish(); return true; }
}
