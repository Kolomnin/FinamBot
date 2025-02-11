package work.finamapibot.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.json.JSONObject;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;
import work.finamapibot.entity.Candle;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

@Service
public class FinamService {

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    public FinamService(WebClient webClient, ObjectMapper objectMapper) {
        this.webClient = webClient;
        this.objectMapper = objectMapper;
    }

    private List<Candle> parseCandles(String responseBody) {
        try {
            JsonNode rootNode = objectMapper.readTree(responseBody);
            JsonNode dataNode = rootNode.path("data");
            JsonNode candlesNode = dataNode.path("candles");

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

    public Mono<List<Candle>> getIntradayCandles(String securityBoard, String securityCode, String timeFrame, String from, String to, int count) {

        System.out.println("getIntradayCandles called with:"); // Выводим входные параметры
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
            System.err.println("Некорректные входные параметры!");
            return Mono.error(new IllegalArgumentException("Некорректные входные параметры"));
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        LocalDateTime startDate = LocalDateTime.parse(from);
        LocalDateTime endDate = LocalDateTime.parse(to);

        List<Mono<List<Candle>>> requests = new ArrayList<>();

        while (startDate.isBefore(endDate)) {
            LocalDateTime nextDate = startDate.plusDays(7);
            if (nextDate.isAfter(endDate)) {
                nextDate = endDate;
            }

            String formattedFrom = formatter.format(startDate) + "T00:00:00.000Z";
            String formattedTo = formatter.format(nextDate) + "T00:00:00.000Z";

            String url = UriComponentsBuilder.fromPath("/public/api/v1/intraday-candles")
                    .queryParam("SecurityBoard", securityBoard)
                    .queryParam("SecurityCode", securityCode)
                    .queryParam("TimeFrame", timeFrame)
                    .queryParam("Interval.From", formattedFrom)
                    .queryParam("Interval.To", formattedTo)
                    .queryParam("Interval.Count", count)
                    .toUriString();

            System.out.println("URL запроса: " + url); // Выводим URL ПЕРЕД запросом!!!

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
                    return candles;
                });
    }

    private boolean isTooManyRequests(Throwable throwable) {
        return throwable instanceof WebClientResponseException.TooManyRequests;
    }

    private Mono<String> handleError(WebClientResponseException ex) {
        System.err.println("Ошибка при запросе к API Finam: " + ex.getStatusCode() + " " + ex.getMessage());
        return Mono.error(new RuntimeException("Ошибка при запросе к API Finam", ex));
    }
}