package work.finamapibot.entity;

import org.json.JSONObject;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

public class Candle {
    private LocalDateTime timestamp;
    private double open;
    private double close;
    private double high;
    private double low;
    private double volume;

    public Candle(JSONObject candleJson) {
        try {
            String timestampStr = candleJson.getString("timestamp");
            Instant instant = Instant.parse(timestampStr);
            this.timestamp = LocalDateTime.ofInstant(instant, ZoneId.of("UTC"));

            this.open = parsePrice(candleJson.getJSONObject("open"));
            this.close = parsePrice(candleJson.getJSONObject("close"));
            this.high = parsePrice(candleJson.getJSONObject("high"));
            this.low = parsePrice(candleJson.getJSONObject("low"));

            this.volume = candleJson.getDouble("volume");

        } catch (Exception e) {
            System.err.println("Ошибка парсинга данных свечи: " + e.getMessage());
            this.timestamp = null; // Или другое значение по умолчанию
            this.open = 0;
            this.close = 0;
            this.high = 0;
            this.low = 0;
            this.volume = 0;
        }

    }

    private double parsePrice(JSONObject priceJson) {
        int num = priceJson.getInt("num");
        int scale = priceJson.getInt("scale");
        return num / Math.pow(10, scale);
    }

    // Геттеры для всех полей (timestamp, open, close, high, low, volume)
    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public double getOpen() {
        return open;
    }

    public double getClose() {
        return close;
    }

    public double getHigh() {
        return high;
    }

    public double getLow() {
        return low;
    }

    public double getVolume() {
        return volume;
    }

    @Override
    public String toString() {
        return "Candle{" +
                "timestamp=" + timestamp +
                ", open=" + open +
                ", close=" + close +
                ", high=" + high +
                ", low=" + low +
                ", volume=" + volume +
                '}';
    }
}
