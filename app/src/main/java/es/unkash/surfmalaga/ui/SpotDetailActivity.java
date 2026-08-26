package es.unkash.surfmalaga.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;

import java.util.ArrayList;
import java.util.List;

import es.unkash.surfmalaga.R;
import es.unkash.surfmalaga.data.BanderaRepository;
import es.unkash.surfmalaga.data.SpotRepository;
import es.unkash.surfmalaga.data.SpotStorage;
import es.unkash.surfmalaga.data.SurfData;
import es.unkash.surfmalaga.data.SurfSpot;
import es.unkash.surfmalaga.databinding.ActivitySpotDetailBinding;

public class SpotDetailActivity extends AppCompatActivity {

    private ActivitySpotDetailBinding binding;
    private SurfSpot spot;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySpotDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        String spotId = getIntent().getStringExtra("spot_id");
        spot = SpotRepository.getInstance().getSpotById(spotId);
        if (spot == null) { finish(); return; }

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(spot.name);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        SpotStorage.loadSpot(this, spot);
        binding.btnRefresh.setOnClickListener(v -> loadData());

        // Botón webcam — URL específica por spot
        if (spot.webcamUrl != null) {
            binding.btnWebcam.setVisibility(View.VISIBLE);
            binding.btnWebcam.setOnClickListener(v ->
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(spot.webcamUrl))));
        } else {
            binding.btnWebcam.setVisibility(View.GONE);
        }

        if (spot.currentData != null) {
            updateUI(spot.currentData);
        } else {
            loadData();
        }

        loadBandera();
    }

    @Override
    protected void onResume() {
        super.onResume();
        SpotStorage.loadSpot(this, spot);
    }

    private void loadBandera() {
        if (spot.nombreAyuntamiento == null) {
            binding.cardBandera.setVisibility(View.GONE);
            return;
        }
        binding.cardBandera.setVisibility(View.VISIBLE);
        binding.tvBanderaEstado.setText("🏳 Consultando...");
        binding.tvBanderaHora.setText("");
        BanderaRepository.invalidateCache();
        BanderaRepository.getEstado(spot.nombreAyuntamiento, estado -> {
            spot.bandera = estado.color;
            spot.banderaHora = estado.hora;
            runOnUiThread(() -> updateBanderaUI(estado));
        });
    }

    private void updateBanderaUI(BanderaRepository.EstadoBandera estado) {
        binding.tvBanderaEstado.setText(estado.toTexto());
        binding.tvBanderaEstado.setTextColor(estado.toColor());
        binding.tvBanderaHora.setText(estado.hora != null ? "Actualizado: " + estado.hora : "");
        binding.btnRefreshBandera.setOnClickListener(v -> loadBandera());
    }

    private void loadData() {
        binding.progressBar.setVisibility(View.VISIBLE);
        binding.cardCurrent.setVisibility(View.GONE);
        binding.cardChart.setVisibility(View.GONE);
        binding.tvError.setVisibility(View.GONE);

        SpotRepository.getInstance().fetchSpotData(spot, new SpotRepository.Callback() {
            @Override public void onSuccess(SurfData data) {
                runOnUiThread(() -> updateUI(data));
            }
            @Override public void onError(String message) {
                runOnUiThread(() -> {
                    binding.progressBar.setVisibility(View.GONE);
                    binding.tvError.setVisibility(View.VISIBLE);
                    binding.tvError.setText("Error: " + message);
                });
            }
        });
    }

    private void updateUI(SurfData d) {
        binding.progressBar.setVisibility(View.GONE);
        binding.tvError.setVisibility(View.GONE);
        binding.cardCurrent.setVisibility(View.VISIBLE);
        binding.cardChart.setVisibility(View.VISIBLE);

        binding.tvWaveHeight.setText(String.format("%.1f m", d.waveHeight));
        binding.tvWavePeriod.setText(String.format("%.0f s", d.wavePeriod));
        binding.tvWaveDir.setText(SurfData.directionToText(d.waveDirection));
        binding.tvSwellHeight.setText(String.format("%.1f m", d.swellHeight));
        binding.tvSwellPeriod.setText(String.format("%.0f s", d.swellPeriod));
        binding.tvSwellDir.setText(SurfData.directionToText(d.swellDirection));
        binding.tvWindSpeed.setText(String.format("%.0f km/h", d.windSpeed));
        binding.tvWindGusts.setText(String.format("%.0f km/h", d.windGusts));
        binding.tvWindDir.setText(SurfData.directionToText(d.windDirection));
        binding.tvLastUpdate.setText("Actualizado: " + formatTime(d.fetchTime));

        boolean isOffshore = spot.alertCheckWindDir &&
                isInRange(d.windDirection, spot.offshoreWindMin, spot.offshoreWindMax);
        binding.tvOffshoreIndicator.setText(isOffshore ? "✅ Offshore" : "❌ No offshore");
        binding.tvOffshoreIndicator.setTextColor(isOffshore ?
                ContextCompat.getColor(this, R.color.wave_small) :
                ContextCompat.getColor(this, R.color.error));

        boolean swellOk = spot.alertCheckSwellDir &&
                isInRange(d.swellDirection, spot.swellDirMin, spot.swellDirMax);
        binding.tvSwellIndicator.setText(swellOk ? "✅ Swell bueno" : "❌ Swell fuera");
        binding.tvSwellIndicator.setTextColor(swellOk ?
                ContextCompat.getColor(this, R.color.wave_small) :
                ContextCompat.getColor(this, R.color.error));

        if (d.hourlyForecast != null) drawChart(d.hourlyForecast);
    }

    private void drawChart(List<SurfData.HourlyEntry> forecast) {
        List<Entry> waveEntries = new ArrayList<>();
        List<Entry> windEntries = new ArrayList<>();
        List<String> labels = new ArrayList<>();

        for (int i = 0; i < forecast.size(); i++) {
            SurfData.HourlyEntry e = forecast.get(i);
            waveEntries.add(new Entry(i, e.waveHeight));
            windEntries.add(new Entry(i, e.windSpeed / 10f));
            labels.add(formatTimeShort(e.time));
        }

        LineDataSet waveSet = new LineDataSet(waveEntries, "Olas (m)");
        waveSet.setColor(ContextCompat.getColor(this, R.color.wave_blue));
        waveSet.setLineWidth(2f);
        waveSet.setDrawCircles(false);
        waveSet.setDrawValues(false);

        LineDataSet windSet = new LineDataSet(windEntries, "Viento (÷10 km/h)");
        windSet.setColor(ContextCompat.getColor(this, R.color.wind_green));
        windSet.setLineWidth(2f);
        windSet.setDrawCircles(false);
        windSet.setDrawValues(false);
        windSet.enableDashedLine(10f, 5f, 0f);

        int textColor = ContextCompat.getColor(this, R.color.text_primary);
        int gridColor = 0x44888888;

        binding.chart.setData(new LineData(waveSet, windSet));
        binding.chart.getDescription().setEnabled(false);
        binding.chart.setBackgroundColor(android.graphics.Color.TRANSPARENT);
        binding.chart.getXAxis().setTextColor(textColor);
        binding.chart.getXAxis().setGridColor(gridColor);
        binding.chart.getAxisLeft().setTextColor(textColor);
        binding.chart.getAxisLeft().setGridColor(gridColor);
        binding.chart.getAxisRight().setEnabled(false);
        binding.chart.getLegend().setTextColor(textColor);
        binding.chart.getXAxis().setValueFormatter(new ValueFormatter() {
            @Override public String getFormattedValue(float value) {
                int idx = (int) value;
                return idx >= 0 && idx < labels.size() ? labels.get(idx) : "";
            }
        });
        binding.chart.getXAxis().setGranularity(4f);
        binding.chart.getXAxis().setLabelRotationAngle(-45f);
        binding.chart.animateX(400);
        binding.chart.invalidate();
    }

    private boolean isInRange(int dir, int min, int max) {
        if (min <= max) return dir >= min && dir <= max;
        return dir >= min || dir <= max;
    }

    private String formatTime(String iso) {
        if (iso == null || iso.length() < 16) return "";
        return iso.substring(11, 16);
    }

    private String formatTimeShort(String iso) {
        if (iso == null || iso.length() < 16) return "";
        return iso.substring(8, 10) + "/" + iso.substring(11, 16);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.spot_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_spot_alert) {
            Intent intent = new Intent(this, SpotAlertActivity.class);
            intent.putExtra("spot_id", spot.id);
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onSupportNavigateUp() { finish(); return true; }
}
