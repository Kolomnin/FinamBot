package work.finamapibot.service;

import org.springframework.stereotype.Service;
import work.finamapibot.DataSet;
import work.finamapibot.entity.Candle;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

@Service
public class DataSetService {

    public List<DataSet> prepareData(List<Candle> candles, int windowSize) {
        if (candles == null || candles.isEmpty() || windowSize <= 0 || candles.size() < windowSize) {
            return new ArrayList<>(); // Return empty list for invalid input
        }

        return IntStream.range(0, candles.size() - windowSize)
                .mapToObj(i -> {
                    List<Candle> window = candles.subList(i, i + windowSize);
                    double[] features = new double[windowSize * 4];
                    for (int j = 0; j < windowSize; j++) {
                        Candle candle = window.get(j); // Get the candle once and reuse it
                        features[j * 4] = candle.getOpen();
                        features[j * 4 + 1] = candle.getHigh();
                        features[j * 4 + 2] = candle.getLow();
                        features[j * 4 + 3] = candle.getClose();
                    }
                    Candle previousCandle = window.get(windowSize - 2);
                    Candle currentCandle = window.get(windowSize - 1);
                    String target = createTargetLabel(previousCandle.getClose(), currentCandle.getClose());
                    return new DataSet(features, target);
                })
                .toList(); // Use toList() for Java 16+ or collect(Collectors.toList()) for older versions
    }

    public String createTargetLabel(double previousClose, double currentClose) {
        return currentClose > previousClose ? "up" : "down";
    }
}
