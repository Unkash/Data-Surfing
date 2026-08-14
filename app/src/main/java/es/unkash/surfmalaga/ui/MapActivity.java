package es.unkash.surfmalaga.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;

import java.util.List;

import es.unkash.surfmalaga.data.SpotRepository;
import es.unkash.surfmalaga.data.SpotStorage;
import es.unkash.surfmalaga.data.SurfSpot;
import es.unkash.surfmalaga.databinding.ActivityMapBinding;

public class MapActivity extends AppCompatActivity {

    private ActivityMapBinding binding;
    private MapView mapView;
    private List<SurfSpot> spots;
    private SpotMarkerOverlay markerOverlay;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // OSMDroid requiere configurar user agent antes de usar el mapa
        Configuration.getInstance().setUserAgentValue(getPackageName());

        binding = ActivityMapBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Mapa de spots");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        spots = SpotRepository.getInstance().getSpots();
        SpotStorage.loadAll(this, spots);

        mapView = binding.mapView;
        mapView.setTileSource(TileSourceFactory.MAPNIK); // OpenStreetMap estándar
        mapView.setMultiTouchControls(true);
        mapView.setBuiltInZoomControls(true);

        // Centrar en Málaga con zoom que muestre toda la costa
        mapView.getController().setZoom(11.0);
        mapView.getController().setCenter(new GeoPoint(36.620, -4.420));

        // Crear overlay de marcadores
        markerOverlay = new SpotMarkerOverlay(this, spots, spot -> {
            Intent intent = new Intent(this, SpotDetailActivity.class);
            intent.putExtra("spot_id", spot.id);
            startActivity(intent);
        });
        mapView.getOverlays().add(markerOverlay);

        // Leyenda
        binding.tvLegend.setText(
            "🟢 Buenas condiciones  🟠 Medias  🔵 Pocas  ⚫ Sin datos\n" +
            "▶ Flecha = dirección del viento  •  Número = altura de ola (m)"
        );

        // Botón actualizar
        binding.btnRefreshMap.setOnClickListener(v -> {
            binding.progressMap.setVisibility(View.VISIBLE);
            binding.btnRefreshMap.setEnabled(false);
            final int[] remaining = {spots.size()};

            for (SurfSpot spot : spots) {
                SpotRepository.getInstance().fetchSpotData(spot, new SpotRepository.Callback() {
                    @Override public void onSuccess(es.unkash.surfmalaga.data.SurfData data) {
                        runOnUiThread(() -> {
                            mapView.invalidate();
                            remaining[0]--;
                            if (remaining[0] <= 0) {
                                binding.progressMap.setVisibility(View.GONE);
                                binding.btnRefreshMap.setEnabled(true);
                            }
                        });
                    }
                    @Override public void onError(String msg) {
                        runOnUiThread(() -> {
                            remaining[0]--;
                            if (remaining[0] <= 0) {
                                binding.progressMap.setVisibility(View.GONE);
                                binding.btnRefreshMap.setEnabled(true);
                            }
                        });
                    }
                });
            }
        });

        // Si ya hay datos cargados (desde MainActivity), mostrarlos directamente
        boolean anyData = false;
        for (SurfSpot s : spots) { if (s.currentData != null) { anyData = true; break; } }
        if (!anyData) binding.btnRefreshMap.performClick();
    }

    @Override public void onResume() { super.onResume(); mapView.onResume(); }
    @Override public void onPause()  { super.onPause();  mapView.onPause();  }
    @Override public boolean onSupportNavigateUp() { finish(); return true; }
}
