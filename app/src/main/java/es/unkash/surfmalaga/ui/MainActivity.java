package es.unkash.surfmalaga.ui;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;

import java.util.ArrayList;
import java.util.List;

import es.unkash.surfmalaga.R;
import es.unkash.surfmalaga.data.OpenMeteoRepository;
import es.unkash.surfmalaga.data.SurfData;
import es.unkash.surfmalaga.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private final OpenMeteoRepository repo = new OpenMeteoRepository();
    private SurfData currentData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.toolbar);

        requestNotificationPermission();

        binding.btnRefresh.setOnClickListener(v -> loadData());
        loadData();
    }

    private void loadData() {
        binding.progressBar.setVisibility(View.VISIBLE);
        binding.cardCurrent.setVisibility(View.GONE);
        binding.cardChart.setVisibility(View.GONE);
        binding.tvError.setVisibility(View.GONE);

        repo.fetchData(new OpenMeteoRepository.Callback() {
            @Override
            public void onSuccess(SurfData data) {
                currentData = data;
                runOnUiThread(() -> updateUI(data));
            }
            @Override
            public void onError(String message) {
                runOnUiThread(() -> {
                    binding.progressBar.setVisibility(View.GONE);
                    binding.tvError.setVisibility(View.VISIBLE);
                    binding.tvError.setText("Error al obtener datos: " + message);
                });
            }
        });
    }

    private void updateUI(SurfData data) {
        binding.progressBar.setVisibility(View.GONE);
        binding.cardCurrent.setVisibility(View.VISIBLE);
        binding.cardChart.setVisibility(View.VISIBLE);

        // --- Condiciones actuales ---
        binding.tvWaveHeight.setText(String.format("%.1f m", data.waveHeight));
        binding.tvWavePeriod.setText(String.format("%.0f s", data.wavePeriod));
        binding.tvWaveDir.setText(SurfData.directionToText(data.waveDirection));
        binding.tvSwellHeight.setText(String.format("%.1f m", data.swellHeight));
        binding.tvSwellPeriod.setText(String.format("%.0f s", data.swellPeriod));
        binding.tvSwellDir.setText(SurfData.directionToText(data.swellDirection));
        binding.tvWindSpeed.setText(String.format("%.0f km/h", data.windSpeed));
        binding.tvWindGusts.setText(String.format("%.0f km/h", data.windGusts));
        binding.tvWindDir.setText(SurfData.directionToText(data.windDirection));
        binding.tvLastUpdate.setText("Actualizado: " + formatTime(data.fetchTime));

        // Color indicativo según altura de ola
        int waveColor = getWaveColor(data.waveHeight);
        binding.tvWaveHeight.setTextColor(waveColor);

        // --- Gráfica de previsión horaria ---
        if (data.hourlyForecast != null && !data.hourlyForecast.isEmpty()) {
            drawChart(data.hourlyForecast);
        }
    }

    private void drawChart(List<SurfData.HourlyEntry> forecast) {
        List<Entry> waveEntries = new ArrayList<>();
        List<Entry> windEntries = new ArrayList<>();
        List<String> labels = new ArrayList<>();

        for (int i = 0; i < forecast.size(); i++) {
            SurfData.HourlyEntry e = forecast.get(i);
            waveEntries.add(new Entry(i, e.waveHeight));
            windEntries.add(new Entry(i, e.windSpeed / 10f)); // escalar para coincidir
            labels.add(formatTimeShort(e.time));
        }

        LineDataSet waveSet = new LineDataSet(waveEntries, "Olas (m)");
        waveSet.setColor(ContextCompat.getColor(this, R.color.wave_blue));
        waveSet.setCircleColor(ContextCompat.getColor(this, R.color.wave_blue));
        waveSet.setLineWidth(2f);
        waveSet.setDrawCircles(false);
        waveSet.setDrawValues(false);

        LineDataSet windSet = new LineDataSet(windEntries, "Viento (×10 km/h)");
        windSet.setColor(ContextCompat.getColor(this, R.color.wind_green));
        windSet.setCircleColor(ContextCompat.getColor(this, R.color.wind_green));
        windSet.setLineWidth(2f);
        windSet.setDrawCircles(false);
        windSet.setDrawValues(false);
        windSet.enableDashedLine(10f, 5f, 0f);

        LineChart chart = binding.chart;
        chart.setData(new LineData(waveSet, windSet));
        chart.getDescription().setEnabled(false);
        chart.getXAxis().setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                int idx = (int) value;
                if (idx >= 0 && idx < labels.size()) return labels.get(idx);
                return "";
            }
        });
        chart.getXAxis().setGranularity(4f);
        chart.getXAxis().setLabelRotationAngle(-45f);
        chart.getAxisRight().setEnabled(false);
        chart.animateX(500);
        chart.invalidate();
    }

    private int getWaveColor(float height) {
        if (height < 0.5f) return getColor(R.color.wave_flat);
        if (height < 1.0f) return getColor(R.color.wave_small);
        if (height < 1.5f) return getColor(R.color.wave_medium);
        return getColor(R.color.wave_big);
    }

    private String formatTime(String iso) {
        // "2024-01-15T07:00" → "07:00"
        if (iso == null || iso.length() < 16) return iso;
        return iso.substring(11, 16);
    }

    private String formatTimeShort(String iso) {
        if (iso == null || iso.length() < 16) return "";
        return iso.substring(8, 10) + "/" + iso.substring(11, 16);
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
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_alerts) {
            startActivity(new Intent(this, AlertsActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
