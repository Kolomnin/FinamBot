package work.finamapibot;

import work.finamapibot.entity.Candle;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class TimeUtils {

    private static final ZoneId MOSCOW_ZONE = ZoneId.of("Europe/Moscow");
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // Market Open/Close Times (using constants for better readability and maintainability)
    private static final LocalTime STOCK_MORNING_START = LocalTime.of(6, 50);
    private static final LocalTime STOCK_MORNING_END = LocalTime.of(9, 50);
    private static final LocalTime STOCK_MAIN_START = LocalTime.of(10, 0);
    private static final LocalTime STOCK_MAIN_END = LocalTime.of(18, 45);
    private static final LocalTime STOCK_EVENING_START = LocalTime.of(19, 0);
    private static final LocalTime STOCK_EVENING_END = LocalTime.of(23, 50);

    private static final LocalTime FUTURES_MORNING_START = LocalTime.of(7, 0); // Corrected start time
    private static final LocalTime FUTURES_MORNING_END = LocalTime.of(9, 50);
    private static final LocalTime FUTURES_MAIN_START = LocalTime.of(10, 0);
    private static final LocalTime FUTURES_MAIN_END = LocalTime.of(18, 45);
    private static final LocalTime FUTURES_EVENING_START = LocalTime.of(19, 0);
    private static final LocalTime FUTURES_EVENING_END = LocalTime.of(23, 50);

    private static final LocalTime CLEARING_START1 = LocalTime.of(14, 0);
    private static final LocalTime CLEARING_END1 = LocalTime.of(14, 5);
    private static final LocalTime CLEARING_START2 = LocalTime.of(18, 45);
    private static final LocalTime CLEARING_END2 = LocalTime.of(19, 0);


    private static boolean isTradingTime(LocalTime now, LocalTime morningStart, LocalTime morningEnd, LocalTime mainStart, LocalTime mainEnd, LocalTime eveningStart, LocalTime eveningEnd) {
        return (now.isAfter(morningStart) && now.isBefore(morningEnd)) ||
                (now.isAfter(mainStart) && now.isBefore(mainEnd)) ||
                (now.isAfter(eveningStart) && now.isBefore(eveningEnd));
    }

    public static List<String> splitIntoIntervals(LocalDateTime startDate, LocalDateTime endDate, int maxIntervalDays) {
        List<String> intervals = new ArrayList<>();

        while (startDate.isBefore(endDate)) {
            LocalDateTime nextDate = startDate.plusDays(maxIntervalDays);
            if (nextDate.isAfter(endDate)) {
                nextDate = endDate;
            }

            intervals.add(startDate.format(DATE_TIME_FORMATTER) + "," + nextDate.format(DATE_TIME_FORMATTER));
            startDate = nextDate;
        }

        return intervals;
    }

    public static boolean isWithinTradingHours(LocalDateTime timestamp, String marketType) {
        LocalTime now = timestamp.toLocalTime();
        if (marketType.equalsIgnoreCase("stock")) {
            return isTradingTime(now, STOCK_MORNING_START, STOCK_MORNING_END, STOCK_MAIN_START, STOCK_MAIN_END, STOCK_EVENING_START, STOCK_EVENING_END);
        } else if (marketType.equalsIgnoreCase("futures")) {
            return isTradingTime(now, FUTURES_MORNING_START, FUTURES_MORNING_END, FUTURES_MAIN_START, FUTURES_MAIN_END, FUTURES_EVENING_START, FUTURES_EVENING_END);
        } else {
            throw new IllegalArgumentException("Invalid market type. Use 'stock' or 'futures'.");
        }
    }

    // Проверка, является ли дата выходным днем
    private static boolean isWeekend(LocalDateTime timestamp) {
        return timestamp.getDayOfWeek() == java.time.DayOfWeek.SATURDAY || timestamp.getDayOfWeek() == java.time.DayOfWeek.SUNDAY;
    }

    // Проверка, попадает ли время в период клиринга
    private static boolean isClearingTime(LocalDateTime timestamp) {
        LocalTime time = timestamp.toLocalTime();
        return (time.isAfter(CLEARING_START1) && time.isBefore(CLEARING_END1)) ||
                (time.isAfter(CLEARING_START2) && time.isBefore(CLEARING_END2));
    }

    // Метод для фильтрации свечей, исключая выходные и время клиринга
    public static List<Candle> filterTradingCandles(List<Candle> candles, String marketType) {
        return candles.stream()
                .filter(candle -> isWithinTradingHours(candle.getTimestamp(), marketType) && !isWeekend(candle.getTimestamp()) && !isClearingTime(candle.getTimestamp()))
                .toList();
    }
}
