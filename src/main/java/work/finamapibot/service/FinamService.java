package work.finamapibot.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.jfree.data.time.Minute;
import org.json.JSONObject;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Flux;
import reactor.util.retry.Retry;
import work.finamapibot.entity.Candle;
import work.finamapibot.TimeUtils;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.ZoneId;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class FinamService {

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    private static final DateTimeFormatter INPUT_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");
    private static final DateTimeFormatter FINAM_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");

    public FinamService(WebClient webClient, ObjectMapper objectMapper) {
        this.webClient = webClient;
        this.objectMapper = objectMapper;
    }

    // Метод для парсинга свечей из ответа
    private List<Candle> parseCandles(String responseBody) {
        try {
            JsonNode rootNode = objectMapper.readTree(responseBody);
            JsonNode candlesNode = rootNode.path("data").path("candles");

            List<Candle> candles = new ArrayList<>();
            if (candlesNode.isArray()) {
                for (JsonNode node : candlesNode) {
                    try {
                        Candle candle = new Candle(new JSONObject(node.toString()));
                        if (candle.getTimestamp() != null) {
                            candles.add(candle);
                        }
                    } catch (Exception e) {
                        System.err.println("Ошибка создания Candle из JSON: " + e.getMessage());
                    }
                }
            }
            return candles;
        } catch (Exception e) {
            System.err.println("Ошибка парсинга JSON: " + e.getMessage());
            throw new RuntimeException("Ошибка при парсинге JSON", e);
        }
    }

    // Метод для получения данных по свечам
    public Mono<List<Candle>> getIntradayCandles(String securityBoard, String securityCode, String timeFrame, String from, String to, int count) {
        System.out.println("getIntradayCandles called with:");
        System.out.println("securityBoard: " + securityBoard);
        System.out.println("securityCode: " + securityCode);
        System.out.println("timeFrame: " + timeFrame);
        System.out.println("from: " + from);
        System.out.println("to: " + to);
        System.out.println("count: " + count);

        if (securityBoard == null || securityBoard.isEmpty() ||
                securityCode == null || securityCode.isEmpty() ||
                timeFrame == null || timeFrame.isEmpty() ||
                from == null || from.isEmpty() ||
                to == null || to.isEmpty() ||
                count <= 0) {
            return Mono.error(new IllegalArgumentException("Некорректные входные параметры"));
        }

        LocalDateTime startDate = LocalDateTime.parse(from, INPUT_DATE_FORMATTER);
        LocalDateTime endDate = LocalDateTime.parse(to, INPUT_DATE_FORMATTER);

        List<Mono<List<Candle>>> requests = new ArrayList<>();

        while (startDate.isBefore(endDate)) {
            LocalDateTime nextDate = startDate.plusDays(7);
            if (nextDate.isAfter(endDate)) {
                nextDate = endDate;
            }

            String formattedFrom = FINAM_FORMATTER.format(startDate.atZone(ZoneId.of("UTC")));
            String formattedTo = FINAM_FORMATTER.format(nextDate.atZone(ZoneId.of("UTC")));

            String url = UriComponentsBuilder.fromPath("/public/api/v1/intraday-candles")
                    .queryParam("SecurityBoard", securityBoard)
                    .queryParam("SecurityCode", securityCode)
                    .queryParam("TimeFrame", timeFrame)
                    .queryParam("Interval.From", formattedFrom)
                    .queryParam("Interval.To", formattedTo)
                    .queryParam("Interval.Count", count)
                    .toUriString();

            System.out.println("URL запроса: " + url);

            Mono<List<Candle>> request = webClient.get()
                    .uri(url)
                    .retrieve()
                    .bodyToMono(String.class)
                    .doOnNext(response -> System.out.println("Response body: " + response))
                    .retryWhen(Retry.fixedDelay(5, Duration.ofSeconds(5)).filter(this::isTooManyRequests))
                    .onErrorResume(WebClientResponseException.class, this::handleError)
                    .map(this::parseCandles);

            requests.add(request);
            startDate = nextDate;
        }

        return Flux.concat(requests)
                .flatMapSequential(Flux::fromIterable)
                .collectList()
                .map(candles -> {
                    candles.sort(Comparator.comparing(Candle::getTimestamp));
                    List<Candle> filteredCandles = filterTradingCandles(candles, securityBoard.equals("TQBR") ? "stock" : "futures");
                    displayChart(createChart(filteredCandles));
                    return filteredCandles;
                });
    }

    private List<Candle> filterTradingCandles(List<Candle> candles, String marketType) {
        return candles.stream()
                .filter(candle -> TimeUtils.isWithinTradingHours(candle.getTimestamp(), marketType))
                .toList();
    }

    // Метод для создания графика с использованием JFreeChart
    private JFreeChart createChart(List<Candle> candles) {
        TimeSeries series = new TimeSeries("Цена закрытия");

        for (Candle candle : candles) {
            LocalDateTime timestamp = candle.getTimestamp();
            if (timestamp != null) {
                Minute minute = new Minute(timestamp.getMinute(), new org.jfree.data.time.Hour(timestamp.getHour(), new org.jfree.data.time.Day(timestamp.getDayOfMonth(), timestamp.getMonthValue(), timestamp.getYear())));
                series.add(minute, candle.getClose());
            }
        }

        TimeSeriesCollection dataset = new TimeSeriesCollection(series);

        return ChartFactory.createTimeSeriesChart(
                "График цен",
                "Время",
                "Цена",
                dataset,
                true,
                true,
                false
        );
    }

    // Метод для отображения графика с использованием Swing
    private void displayChart(JFreeChart chart) {
        if (chart == null) {
            System.out.println("Не удалось создать график.");
            return;
        }

        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("График цен");
            frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            ChartPanel panel = new ChartPanel(chart);
            frame.getContentPane().add(panel, BorderLayout.CENTER);
            frame.pack();
            frame.setVisible(true);
        });
    }

    // Дополнительные вспомогательные методы
    private boolean isTooManyRequests(Throwable throwable) {
        return throwable instanceof WebClientResponseException.TooManyRequests;
    }

    private Mono<String> handleError(WebClientResponseException ex) {
        System.err.println("Ошибка при запросе к API Finam: " + ex.getStatusCode() + " " + ex.getMessage());
        return Mono.justOrEmpty(ex.getResponseBodyAsString())
                .flatMap(errorBody -> {
                    if (!errorBody.isEmpty()) {
                        System.err.println("Тело ошибки: " + errorBody);
                        return Mono.error(new RuntimeException("Ошибка при запросе к API Finam: " + errorBody, ex));
                    } else {
                        return Mono.error(new RuntimeException("Ошибка при запросе к API Finam: " + ex.getMessage(), ex));
                    }
                });
    }
}