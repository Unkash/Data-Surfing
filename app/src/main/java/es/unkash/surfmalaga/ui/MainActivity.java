package es.unkash.surfmalaga.ui;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import java.util.List;

import es.unkash.surfmalaga.R;
import es.unkash.surfmalaga.data.BanderaRepository;
import es.unkash.surfmalaga.data.SpotRepository;
import es.unkash.surfmalaga.data.SpotStorage;
import es.unkash.surfmalaga.data.SurfSpot;
import es.unkash.surfmalaga.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private SpotAdapter adapter;
    private List<SurfSpot> spots;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.toolbar);

        requestNotificationPermission();

        spots = SpotRepository.getInstance().getSpots();
        SpotStorage.loadAll(this, spots);

        adapter = new SpotAdapter(spots, spot -> {
            Intent intent = new Intent(this, SpotDetailActivity.class);
            intent.putExtra("spot_id", spot.id);
            startActivity(intent);
        });

        binding.recyclerView.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerView.setAdapter(adapter);

        binding.btnRefreshAll.setOnClickListener(v -> refreshAll());
        refreshAll();
        loadBanderasBackground();
    }

    private void loadBanderasBackground() {
        new Thread(() -> {
            for (SurfSpot spot : spots) {
                if (spot.nombreAyuntamiento != null) {
                    BanderaRepository.getEstado(spot.nombreAyuntamiento, estado -> {
                        spot.bandera = estado.color;
                        spot.banderaHora = estado.hora;
                        runOnUiThread(() -> adapter.notifyDataSetChanged());
                    });
                }
            }
        }).start();
    }

    @Override
    protected void onResume() {
        super.onResume();
        SpotStorage.loadAll(this, spots);
        adapter.notifyDataSetChanged();
    }

    private void refreshAll() {
        binding.progressBar.setVisibility(View.VISIBLE);
        SpotRepository repo = SpotRepository.getInstance();
        final int[] remaining = {spots.size()};

        for (SurfSpot spot : spots) {
            repo.fetchSpotData(spot, new SpotRepository.Callback() {
                @Override
                public void onSuccess(es.unkash.surfmalaga.data.SurfData data) {
                    runOnUiThread(() -> {
                        adapter.notifyDataSetChanged();
                        remaining[0]--;
                        if (remaining[0] <= 0)
                            binding.progressBar.setVisibility(View.GONE);
                    });
                }
                @Override
                public void onError(String message) {
                    runOnUiThread(() -> {
                        adapter.notifyDataSetChanged();
                        remaining[0]--;
                        if (remaining[0] <= 0)
                            binding.progressBar.setVisibility(View.GONE);
                    });
                }
            });
        }
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS}, 100);
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_alerts) {
            startActivity(new Intent(this, AlertsActivity.class));
            return true;
        }
        if (item.getItemId() == R.id.action_map) {
            startActivity(new Intent(this, MapActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
