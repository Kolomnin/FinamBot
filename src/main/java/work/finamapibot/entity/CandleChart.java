package work.finamapibot.entity;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.jfree.data.time.Minute;
import org.springframework.stereotype.Component;
import work.finamapibot.entity.Candle; //

import java.time.LocalDateTime;
import java.util.List;

@Component
public class CandleChart {

    public JFreeChart createChart(List<Candle> candles) {
        if (candles == null || candles.isEmpty()) {
            return null; // Или выбросить исключение, если хотите
        }

        TimeSeries series = new TimeSeries("Цена закрытия");

        for (Candle candle : candles) {
            LocalDateTime timestamp = candle.getTimestamp();
            if (timestamp != null) { // Важная проверка на null!
                Minute minute = new Minute(
                        timestamp.getMinute(),
                        new org.jfree.data.time.Hour(timestamp.getHour(), new org.jfree.data.time.Day(timestamp.getDayOfMonth(), timestamp.getMonthValue(), timestamp.getYear()))
                );
                series.add(minute, candle.getClose()); // Используйте candle.getClose()
            }
        }

        TimeSeriesCollection dataset = new TimeSeriesCollection(series);

        return ChartFactory.createTimeSeriesChart(
                "Котировки акций",
                "Время",
                "Цена",
                dataset,
                true,
                true,
                false
        );
    }
}
