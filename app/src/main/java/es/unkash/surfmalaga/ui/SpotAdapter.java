package es.unkash.surfmalaga.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import es.unkash.surfmalaga.R;
import es.unkash.surfmalaga.data.SurfData;
import es.unkash.surfmalaga.data.SurfSpot;

public class SpotAdapter extends RecyclerView.Adapter<SpotAdapter.ViewHolder> {

    public interface OnSpotClick { void onClick(SurfSpot spot); }

    private final List<SurfSpot> spots;
    private final OnSpotClick listener;

    public SpotAdapter(List<SurfSpot> spots, OnSpotClick listener) {
        this.spots = spots;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_spot, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int position) {
        SurfSpot spot = spots.get(position);
        h.tvName.setText(spot.name);

        if (spot.alertEnabled) {
            h.tvAlertBadge.setVisibility(View.VISIBLE);
        } else {
            h.tvAlertBadge.setVisibility(View.GONE);
        }

        if (spot.isLoading) {
            h.tvWave.setText("Cargando…");
            h.tvWind.setText("");
            h.tvSwell.setText("");
        } else if (spot.errorMsg != null) {
            h.tvWave.setText("Error");
            h.tvWind.setText("");
            h.tvSwell.setText("");
        } else if (spot.currentData != null) {
            SurfData d = spot.currentData;
            h.tvWave.setText(String.format("🌊 %.1fm  %.0fs  %s",
                    d.waveHeight, d.wavePeriod, SurfData.directionToText(d.waveDirection)));
            h.tvWind.setText(String.format("💨 %.0f km/h  %s  (rachas %.0f)",
                    d.windSpeed, SurfData.directionToText(d.windDirection), d.windGusts));
            h.tvSwell.setText(String.format("↗ Swell %.1fm  %.0fs  %s",
                    d.swellHeight, d.swellPeriod, SurfData.directionToText(d.swellDirection)));

            // Color de fondo según condición
            int color;
            switch (spot.getConditionLevel()) {
                case 3: color = 0xFFE8F5E9; break; // verde claro
                case 2: color = 0xFFFFF9C4; break; // amarillo claro
                case 1: color = 0xFFFFF3E0; break; // naranja claro
                default: color = 0xFFF5F5F5; break; // gris
            }
            h.card.setCardBackgroundColor(color);
        } else {
            h.tvWave.setText("Sin datos");
            h.tvWind.setText("");
            h.tvSwell.setText("");
        }

        h.itemView.setOnClickListener(v -> listener.onClick(spot));
    }

    @Override
    public int getItemCount() { return spots.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        CardView card;
        TextView tvName, tvWave, tvWind, tvSwell, tvAlertBadge;

        ViewHolder(View v) {
            super(v);
            card         = v.findViewById(R.id.card);
            tvName       = v.findViewById(R.id.tvSpotName);
            tvWave       = v.findViewById(R.id.tvWave);
            tvWind       = v.findViewById(R.id.tvWind);
            tvSwell      = v.findViewById(R.id.tvSwell);
            tvAlertBadge = v.findViewById(R.id.tvAlertBadge);
        }
    }
}
