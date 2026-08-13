package es.unkash.surfmalaga.data;

/**
 * Configuración de una alerta surf.
 * Se guarda en SharedPreferences como JSON.
 */
public class AlertConfig {

    public boolean enabled = false;

    // Olas
    public boolean waveAlertEnabled = false;
    public float waveMinHeight = 0.5f;   // metros mínimos
    public float waveMaxHeight = 3.0f;   // metros máximos (0 = sin límite)
    public float wavePeriodMin = 8.0f;   // segundos mínimos de período

    // Viento
    public boolean windAlertEnabled = false;
    public float windMaxSpeed = 20.0f;   // km/h máximo
    public float windMinSpeed = 0.0f;    // km/h mínimo

    // Swell
    public boolean swellAlertEnabled = false;
    public float swellMinHeight = 0.3f;

    // Dirección de viento (offshore para Málaga = NNO-O, ≈ 270-340°)
    public boolean windDirectionEnabled = false;
    public int windDirMin = 270;
    public int windDirMax = 340;

    // Horario de comprobación
    public int checkHour = 7;    // hora del día para comprobar (formato 24h)
    public int checkInterval = 3; // cada cuántas horas revisar (1, 3, 6, 12)

    /**
     * Evalúa si los datos actuales cumplen las condiciones de alerta.
     * Devuelve mensaje de alerta o null si no hay alerta.
     */
    public String evaluate(SurfData data) {
        if (!enabled) return null;

        StringBuilder sb = new StringBuilder();

        if (waveAlertEnabled) {
            boolean heightOk = data.waveHeight >= waveMinHeight
                    && (waveMaxHeight <= 0 || data.waveHeight <= waveMaxHeight);
            boolean periodOk = data.wavePeriod >= wavePeriodMin;
            if (heightOk && periodOk) {
                sb.append(String.format("🌊 Olas: %.1fm / %.0fs  ", data.waveHeight, data.wavePeriod));
            } else {
                if (waveAlertEnabled) return null; // condición obligatoria no cumplida
            }
        }

        if (windAlertEnabled) {
            boolean windOk = data.windSpeed <= windMaxSpeed && data.windSpeed >= windMinSpeed;
            if (windOk) {
                sb.append(String.format("💨 Viento: %.0f km/h  ", data.windSpeed));
            } else {
                if (windAlertEnabled) return null;
            }
        }

        if (windDirectionEnabled) {
            boolean dirOk = isDirectionInRange(data.windDirection, windDirMin, windDirMax);
            if (dirOk) {
                sb.append(String.format("🧭 Dir: %s  ", SurfData.directionToText(data.windDirection)));
            } else {
                return null;
            }
        }

        if (swellAlertEnabled) {
            if (data.swellHeight >= swellMinHeight) {
                sb.append(String.format("🌊 Swell: %.1fm  ", data.swellHeight));
            } else {
                return null;
            }
        }

        return sb.length() > 0 ? "¡Buenas condiciones! " + sb.toString().trim() : null;
    }

    private boolean isDirectionInRange(int dir, int min, int max) {
        if (min <= max) {
            return dir >= min && dir <= max;
        } else {
            // Rango que cruza los 360°
            return dir >= min || dir <= max;
        }
    }
}
