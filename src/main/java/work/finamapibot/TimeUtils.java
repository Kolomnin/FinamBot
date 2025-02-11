package work.finamapibot;

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



    public static boolean isMarketOpen(String marketType) {
        LocalTime now = LocalTime.now(MOSCOW_ZONE);

        boolean isClearing = (now.isAfter(CLEARING_START1) && now.isBefore(CLEARING_END1)) ||
                (now.isAfter(CLEARING_START2) && now.isBefore(CLEARING_END2));

        if (marketType.equalsIgnoreCase("stock")) {
            return isTradingTime(now, STOCK_MORNING_START, STOCK_MORNING_END, STOCK_MAIN_START, STOCK_MAIN_END, STOCK_EVENING_START, STOCK_EVENING_END) && !isClearing;
        } else if (marketType.equalsIgnoreCase("futures")) {
            return isTradingTime(now, FUTURES_MORNING_START, FUTURES_MORNING_END, FUTURES_MAIN_START, FUTURES_MAIN_END, FUTURES_EVENING_START, FUTURES_EVENING_END) && !isClearing;
        } else {
            throw new IllegalArgumentException("Invalid market type. Use 'stock' or 'futures'.");
        }
    }

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
}
