package es.unkash.surfmalaga.data;

/**
 * Representa un spot de surf con sus características geográficas
 * y parámetros de alerta independientes.
 */
public class SurfSpot {

    public String id;
    public String name;
    public double lat;
    public double lon;
    public String webcamUrl; // null si no hay webcam

    // Viento offshore óptimo para este spot (grados)
    public int offshoreWindMin;
    public int offshoreWindMax;

    // Swell óptimo para este spot (grados de donde viene el swell)
    public int swellDirMin;
    public int swellDirMax;

    // Alertas independientes por spot
    public boolean alertEnabled = false;
    public float alertWaveMinHeight = 0.5f;
    public float alertWaveMaxHeight = 0f;      // 0 = sin límite
    public float alertWavePeriodMin = 8f;
    public float alertSwellMinHeight = 0.3f;
    public float alertWindMaxSpeed = 30f;
    public boolean alertCheckWindDir = true;   // filtrar por dirección offshore
    public boolean alertCheckSwellDir = true;  // filtrar por dirección swell

    // Datos en tiempo real (se rellenan al cargar)
    public SurfData currentData;
    public boolean isLoading = false;
    public String errorMsg = null;

    public SurfSpot(String id, String name, double lat, double lon,
                    int offshoreWindMin, int offshoreWindMax,
                    int swellDirMin, int swellDirMax,
                    String webcamUrl) {
        this.id = id;
        this.name = name;
        this.lat = lat;
        this.lon = lon;
        this.offshoreWindMin = offshoreWindMin;
        this.offshoreWindMax = offshoreWindMax;
        this.swellDirMin = swellDirMin;
        this.swellDirMax = swellDirMax;
        this.webcamUrl = webcamUrl;
    }

    /**
     * Evalúa si las condiciones actuales cumplen los criterios de alerta.
     * Devuelve mensaje de alerta o null si no procede.
     */
    public String evaluateAlert() {
        if (!alertEnabled || currentData == null) return null;

        SurfData d = currentData;

        // 1. Altura de ola mínima
        if (d.waveHeight < alertWaveMinHeight) return null;

        // 2. Altura de ola máxima (si se ha definido)
        if (alertWaveMaxHeight > 0 && d.waveHeight > alertWaveMaxHeight) return null;

        // 3. Período mínimo
        if (d.wavePeriod < alertWavePeriodMin) return null;

        // 4. Viento máximo
        if (d.windSpeed > alertWindMaxSpeed) return null;

        // 5. Dirección de viento offshore
        if (alertCheckWindDir && !isInRange(d.windDirection, offshoreWindMin, offshoreWindMax)) return null;

        // 6. Dirección de swell
        if (alertCheckSwellDir && !isInRange(d.swellDirection, swellDirMin, swellDirMax)) return null;

        // Todas las condiciones OK
        return String.format(
            "🏄 %s: Olas %.1fm / %.0fs · Swell %.1fm · Viento %.0f km/h %s",
            name,
            d.waveHeight, d.wavePeriod,
            d.swellHeight,
            d.windSpeed,
            SurfData.directionToText(d.windDirection)
        );
    }

    private boolean isInRange(int dir, int min, int max) {
        if (min <= max) {
            return dir >= min && dir <= max;
        } else {
            // Rango que cruza los 360° (ej: 340-020)
            return dir >= min || dir <= max;
        }
    }

    /** Calidad de condiciones en 0-3 para mostrar color en la lista */
    public int getConditionLevel() {
        if (currentData == null) return 0;
        SurfData d = currentData;
        int score = 0;
        if (d.waveHeight >= alertWaveMinHeight) score++;
        if (d.wavePeriod >= alertWavePeriodMin) score++;
        if (isInRange(d.windDirection, offshoreWindMin, offshoreWindMax)) score++;
        return score;
    }
}
