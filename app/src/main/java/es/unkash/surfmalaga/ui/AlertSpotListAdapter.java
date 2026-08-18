package es.unkash.surfmalaga.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import es.unkash.surfmalaga.R;
import es.unkash.surfmalaga.data.SurfSpot;

public class AlertSpotListAdapter extends RecyclerView.Adapter<AlertSpotListAdapter.VH> {

    public interface OnClick { void onClick(SurfSpot spot); }

    private final List<SurfSpot> spots;
    private final OnClick listener;

    public AlertSpotListAdapter(List<SurfSpot> spots, OnClick listener) {
        this.spots = spots;
        this.listener = listener;
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_alert_spot, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        SurfSpot spot = spots.get(position);
        h.tvName.setText(spot.name);
        h.tvStatus.setText(spot.alertEnabled ? "🔔 Activa" : "🔕 Sin alerta");
        h.tvStatus.setTextColor(spot.alertEnabled ? 0xFF388E3C : 0xFF9E9E9E);
        h.itemView.setOnClickListener(v -> listener.onClick(spot));
    }

    @Override public int getItemCount() { return spots.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvName, tvStatus;
        VH(View v) {
            super(v);
            tvName   = v.findViewById(R.id.tvAlertSpotName);
            tvStatus = v.findViewById(R.id.tvAlertSpotStatus);
        }
    }
}
