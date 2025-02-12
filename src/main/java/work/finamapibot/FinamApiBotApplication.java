package work.finamapibot;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import reactor.core.publisher.Flux;
import work.finamapibot.entity.Candle;
import work.finamapibot.service.FinamService;
import work.finamapibot.service.DataSetService;
import work.finamapibot.TimeUtils;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@SpringBootApplication
public class FinamApiBotApplication implements CommandLineRunner {

    private final FinamService finamService;
    private final DataSetService datasetService;

    public FinamApiBotApplication(FinamService finamService, DataSetService datasetService) {
        this.finamService = finamService;
        this.datasetService = datasetService;
    }

    public static void main(String[] args) {
        SpringApplication.run(FinamApiBotApplication.class, args);
    }

    @Override
    public void run(String... args) {
        try {
            // Убираем проверку на открытость рынка, так как она не нужна для получения исторических данных

            String timeFrame = "1"; // Проверьте, что timeFrame поддерживается Finam API
            int maxIntervalDays = 30;
            String board = "FUT";
            String code = "SiH5";

            LocalDateTime startDate = LocalDateTime.ofInstant(Instant.parse("2025-02-01T00:00:00Z"), ZoneOffset.UTC);
            LocalDateTime endDate = LocalDateTime.ofInstant(Instant.parse("2025-02-12T00:00:00Z"), ZoneOffset.UTC);

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

            List<String> dateRanges = TimeUtils.splitIntoIntervals(startDate, endDate, maxIntervalDays);

            Flux.fromIterable(dateRanges)
                    .flatMapSequential(range -> {
                        String[] dates = range.split(",");
                        String start = LocalDateTime.parse(dates[0], formatter).toString();
                        String end = LocalDateTime.parse(dates[1], formatter).toString();
                        return finamService.getIntradayCandles(board, code, timeFrame, start, end, 1000); // Проверьте count
                    })
                    .collectList()
                    .subscribe(allCandles -> {
                        // "Разглаживаем" список List<List<Candle>> в List<Candle>
                        List<Candle> flattenedCandles = allCandles.stream()
                                .flatMap(List::stream)
                                .collect(Collectors.toList());

                        if (!flattenedCandles.isEmpty()) {
                            List<DataSet> dataSet = datasetService.prepareData(flattenedCandles, 10);
                            System.out.println("Подготовлено " + dataSet.size() + " обучающих примеров.");
                        } else {
                            System.out.println("Нет данных для данного запроса.");
                        }
                    }, error -> { // Обработка ошибок при запросе данных
                        System.err.println("Ошибка при получении данных: " + error.getMessage());
                        error.printStackTrace(); // Или более подробное логирование ошибки
                    });

        } catch (Exception e) {
            System.err.println("Ошибка в run методе: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
