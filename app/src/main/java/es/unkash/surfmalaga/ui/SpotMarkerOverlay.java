package es.unkash.surfmalaga.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Typeface;

import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Overlay;

import java.util.List;

import es.unkash.surfmalaga.data.SurfData;
import es.unkash.surfmalaga.data.SurfSpot;

/**
 * Overlay que dibuja un marcador por spot con:
 *  - Círculo coloreado según nivel de condiciones
 *  - Altura de ola
 *  - Flecha de dirección de viento
 *  - Nombre del spot
 */
public class SpotMarkerOverlay extends Overlay {

    private final List<SurfSpot> spots;
    private OnSpotClick listener;

    private final Paint circlePaint  = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint borderPaint  = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint    = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint labelPaint   = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint arrowPaint   = new Paint(Paint.ANTI_ALIAS_FLAG);

    public interface OnSpotClick { void onClick(SurfSpot spot); }

    public SpotMarkerOverlay(Context ctx, List<SurfSpot> spots, OnSpotClick listener) {
        this.spots    = spots;
        this.listener = listener;

        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(3f);
        borderPaint.setColor(Color.WHITE);

        textPaint.setColor(Color.WHITE);
        textPaint.setTypeface(Typeface.DEFAULT_BOLD);
        textPaint.setTextAlign(Paint.Align.CENTER);

        labelPaint.setColor(Color.DKGRAY);
        labelPaint.setTextAlign(Paint.Align.CENTER);
        labelPaint.setTypeface(Typeface.DEFAULT_BOLD);

        arrowPaint.setStyle(Paint.Style.FILL);
        arrowPaint.setColor(Color.WHITE);
    }

    @Override
    public void draw(Canvas canvas, MapView mapView, boolean shadow) {
        if (shadow) return;

        float density = mapView.getContext().getResources().getDisplayMetrics().density;
        float radius   = 22 * density;
        float textSize = 10 * density;
        float lblSize  = 9  * density;

        textPaint.setTextSize(textSize);
        labelPaint.setTextSize(lblSize);

        for (SurfSpot spot : spots) {
            // Convertir coordenadas geo a píxeles
            org.osmdroid.api.IProjection proj = mapView.getProjection();
            android.graphics.Point p = new android.graphics.Point();
            proj.toPixels(new GeoPoint(spot.lat, spot.lon), p);

            // Color según condición
            circlePaint.setStyle(Paint.Style.FILL);
            circlePaint.setColor(conditionColor(spot));

            // Círculo principal
            canvas.drawCircle(p.x, p.y, radius, circlePaint);
            canvas.drawCircle(p.x, p.y, radius, borderPaint);

            if (spot.currentData != null) {
                SurfData d = spot.currentData;

                // Altura de ola en el centro
                String waveText = String.format("%.1f", d.waveHeight) + "m";
                canvas.drawText(waveText, p.x, p.y + textSize / 3, textPaint);

                // Flecha de dirección de viento
                drawWindArrow(canvas, p.x, p.y, radius, d.windDirection, density);
            } else {
                canvas.drawText("?", p.x, p.y + textSize / 3, textPaint);
            }

            // Nombre del spot debajo del círculo
            canvas.drawText(spot.name, p.x, p.y + radius + lblSize + 2 * density, labelPaint);
        }
    }

    /**
     * Dibuja una pequeña flecha en el borde del círculo indicando dirección del viento.
     * El viento meteorológico indica DE DONDE VIENE, la flecha apunta hacia donde VA.
     */
    private void drawWindArrow(Canvas canvas, float cx, float cy,
                                float radius, int windDirFrom, float density) {
        // La flecha apunta hacia donde va el viento (dirección + 180°)
        double angleDeg = (windDirFrom + 180) % 360;
        double angleRad = Math.toRadians(angleDeg - 90); // ajuste para sistema Canvas

        float arrowLen   = 8 * density;
        float arrowWidth = 4 * density;

        // Punta de la flecha: en el borde del círculo
        float tipX = cx + (float)(radius * Math.cos(angleRad));
        float tipY = cy + (float)(radius * Math.sin(angleRad));

        // Base de la flecha: hacia el interior
        float baseX = cx + (float)((radius - arrowLen) * Math.cos(angleRad));
        float baseY = cy + (float)((radius - arrowLen) * Math.sin(angleRad));

        // Perpendicular para la anchura
        float perpX = (float)(arrowWidth * Math.cos(angleRad + Math.PI / 2));
        float perpY = (float)(arrowWidth * Math.sin(angleRad + Math.PI / 2));

        Path arrow = new Path();
        arrow.moveTo(tipX, tipY);
        arrow.lineTo(baseX + perpX, baseY + perpY);
        arrow.lineTo(baseX - perpX, baseY - perpY);
        arrow.close();

        canvas.drawPath(arrow, arrowPaint);
    }

    private int conditionColor(SurfSpot spot) {
        switch (spot.getConditionLevel()) {
            case 3: return Color.parseColor("#388E3C"); // verde — buenas condiciones
            case 2: return Color.parseColor("#F57C00"); // naranja — condiciones medias
            case 1: return Color.parseColor("#1565C0"); // azul — pocas condiciones
            default: return Color.parseColor("#757575"); // gris — sin datos
        }
    }

    @Override
    public boolean onSingleTapConfirmed(android.view.MotionEvent e, MapView mapView) {
        if (listener == null) return false;

        float density = mapView.getContext().getResources().getDisplayMetrics().density;
        float radius  = 22 * density;

        org.osmdroid.api.IProjection proj = mapView.getProjection();
        android.graphics.Point tap = new android.graphics.Point((int) e.getX(), (int) e.getY());

        for (SurfSpot spot : spots) {
            android.graphics.Point p = new android.graphics.Point();
            proj.toPixels(new GeoPoint(spot.lat, spot.lon), p);

            float dist = (float) Math.hypot(tap.x - p.x, tap.y - p.y);
            if (dist <= radius * 1.5f) {
                listener.onClick(spot);
                return true;
            }
        }
        return false;
    }
}
