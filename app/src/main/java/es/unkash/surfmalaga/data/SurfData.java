package es.unkash.surfmalaga.data;

import java.util.List;

public class SurfData {

    public float waveHeight;
    public float wavePeriod;
    public int   waveDirection;
    public float swellHeight;
    public float swellPeriod;
    public int   swellDirection;
    public float windSpeed;
    public float windGusts;
    public int   windDirection;
    public String fetchTime;

    public List<HourlyEntry> hourlyForecast;

    public static class HourlyEntry {
        public String time;
        public float waveHeight;
        public float wavePeriod;
        public int   waveDirection;
        public float swellHeight;
        public float windSpeed;
        public float windGusts;
        public int   windDirection;

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

    public static String directionToText(int degrees) {
        String[] dirs = {"N","NNE","NE","ENE","E","ESE","SE","SSE",
                         "S","SSO","SO","OSO","O","ONO","NO","NNO"};
        int index = (int)((degrees + 11.25) / 22.5) % 16;
        return dirs[index];
    }
}
