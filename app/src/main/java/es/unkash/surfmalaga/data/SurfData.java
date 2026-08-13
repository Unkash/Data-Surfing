package es.unkash.surfmalaga.data;

import java.util.List;

public class SurfData {

    // Condiciones actuales
    public float waveHeight;        // metros
    public float wavePeriod;        // segundos
    public int waveDirection;       // grados
    public float swellHeight;       // metros
    public float swellPeriod;       // segundos
    public int swellDirection;      // grados
    public float windSpeed;         // km/h
    public float windGusts;         // km/h
    public int windDirection;       // grados
    public String fetchTime;        // hora de la última actualización

    // Previsión horaria (próximas 48h)
    public List<HourlyEntry> hourlyForecast;

    public static class HourlyEntry {
        public String time;
        public float waveHeight;
        public float wavePeriod;
        public int waveDirection;
        public float swellHeight;
        public float windSpeed;
        public float windGusts;
        public int windDirection;

        public HourlyEntry(String time, float waveHeight, float wavePeriod,
                           int waveDirection, float swellHeight,
                           float windSpeed, float windGusts, int windDirection) {
            this.time = time;
            this.waveHeight = waveHeight;
            this.wavePeriod = wavePeriod;
            this.waveDirection = waveDirection;
            this.swellHeight = swellHeight;
            this.windSpeed = windSpeed;
            this.windGusts = windGusts;
            this.windDirection = windDirection;
        }
    }

    // Convierte grados a texto de dirección
    public static String directionToText(int degrees) {
        String[] dirs = {"N", "NNE", "NE", "ENE", "E", "ESE", "SE", "SSE",
                         "S", "SSO", "SO", "OSO", "O", "ONO", "NO", "NNO"};
        int index = (int) ((degrees + 11.25) / 22.5) % 16;
        return dirs[index];
    }
}
