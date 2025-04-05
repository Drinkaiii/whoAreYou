package eth.whoAreYou.service;

import eth.whoAreYou.dto.TransactionDto;
import eth.whoAreYou.dto.WalletFeatureDto;
import org.springframework.stereotype.Service;

import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class FeatureExtractor {

    private static final double ETH_CONVERSION = 1e18;

    public WalletFeatureDto extract(List<TransactionDto> txList, String walletAddress, int txCountOverride) {
        if (txList.isEmpty()) return WalletFeatureDto.builder().build();

        long txCount = txCountOverride > 0 ? txCountOverride : txList.size();

        List<Double> values = txList.stream()
                .map(tx -> Double.parseDouble(tx.value) / ETH_CONVERSION)
                .collect(Collectors.toList());

        List<Double> gasPrices = txList.stream()
                .map(tx -> Double.parseDouble(tx.gasPrice) / 1e9)
                .collect(Collectors.toList());

        Set<String> uniqueReceivers = txList.stream().map(tx -> tx.to).collect(Collectors.toSet());
        Set<String> uniqueSenders = txList.stream().map(tx -> tx.from).collect(Collectors.toSet());

        List<Instant> timestamps = txList.stream()
                .map(tx -> Instant.ofEpochSecond(tx.timeStamp))
                .sorted()
                .collect(Collectors.toList());

        long inTxCount = txList.stream().filter(tx -> walletAddress.equalsIgnoreCase(tx.to)).count();
        long outTxCount = txList.stream().filter(tx -> walletAddress.equalsIgnoreCase(tx.from)).count();
        long selfTxCount = txList.stream().filter(tx -> tx.from.equalsIgnoreCase(tx.to)).count();

        double timeRange = timestamps.size() >= 2
                ? Duration.between(timestamps.get(0), timestamps.get(timestamps.size() - 1)).getSeconds()
                : 0;
        double txPerDay = timeRange > 0 ? txCount / (timeRange / 86400.0) : 0;

        List<Long> diffs = new ArrayList<>();
        for (int i = 1; i < timestamps.size(); i++) {
            diffs.add(Duration.between(timestamps.get(i - 1), timestamps.get(i)).getSeconds());
        }

        Map<Long, Long> hourlyBuckets = timestamps.stream()
                .collect(Collectors.groupingBy(ts -> ts.getEpochSecond() / 3600, Collectors.counting()));
        Map<Long, Long> fiveMinBuckets = timestamps.stream()
                .collect(Collectors.groupingBy(ts -> ts.getEpochSecond() / 300, Collectors.counting()));

        // shortTimeRepeatedTx 統計
        Map<String, List<Instant>> toGroupedTimestamps = txList.stream()
                .collect(Collectors.groupingBy(
                        tx -> tx.to,
                        Collectors.mapping(tx -> Instant.ofEpochSecond(tx.timeStamp), Collectors.toList())
                ));

        long repeatedTxCount = toGroupedTimestamps.values().stream()
                .flatMap(timestampsList -> {
                    timestampsList.sort(Comparator.naturalOrder());
                    List<Long> shortDiffs = new ArrayList<>();
                    for (int i = 1; i < timestampsList.size(); i++) {
                        long diff = Duration.between(timestampsList.get(i - 1), timestampsList.get(i)).getSeconds();
                        if (diff < 60) shortDiffs.add(diff);
                    }
                    return shortDiffs.stream();
                }).count();

        double shortTimeRepeatedRatio = repeatedTxCount / (double) Math.max(txCount, 1);

        return WalletFeatureDto.builder()
                .txCount(txCount)
                .avgValue(values.stream().mapToDouble(v -> v).average().orElse(0))
                .maxValue(values.stream().mapToDouble(v -> v).max().orElse(0))
                .medianGasPrice(median(gasPrices))
                .uniqueReceivers(uniqueReceivers.size())
                .uniqueSenders(uniqueSenders.size())
                .txPerDay(txPerDay)
                .inTxCount(inTxCount)
                .outTxCount(outTxCount)
                .inOutRatio(inTxCount / (double) Math.max(outTxCount, 1))
                .selfTxRatio(selfTxCount / (double) Math.max(txCount, 1))
                .maxTxIn1h(hourlyBuckets.values().stream().max(Long::compare).orElse(0L))
                .maxTxIn5min(fiveMinBuckets.values().stream().max(Long::compare).orElse(0L))
                .txTimeStd(stddev(diffs))
                .zeroValueTxRatio(values.stream().filter(v -> v == 0).count() / (double) Math.max(txCount, 1))
                .highValueTxRatio(values.stream().filter(v -> v > quantile(values, 0.99)).count() / (double) Math.max(txCount, 1))
                .shortTimeRepeatedTx(shortTimeRepeatedRatio)
                .build();
    }

    private double median(List<Double> list) {
        if (list.isEmpty()) return 0;
        List<Double> sorted = new ArrayList<>(list);
        Collections.sort(sorted);
        int n = sorted.size();
        return n % 2 == 0
                ? (sorted.get(n / 2 - 1) + sorted.get(n / 2)) / 2.0
                : sorted.get(n / 2);
    }

    private double stddev(List<Long> list) {
        if (list.isEmpty()) return 0;
        double avg = list.stream().mapToDouble(Long::doubleValue).average().orElse(0);
        double sumSq = list.stream().mapToDouble(i -> Math.pow(i - avg, 2)).sum();
        return Math.sqrt(sumSq / list.size());
    }

    private double quantile(List<Double> list, double q) {
        if (list.isEmpty()) return 0;
        List<Double> sorted = new ArrayList<>(list);
        Collections.sort(sorted);
        int index = (int) (q * sorted.size());
        return sorted.get(Math.min(index, sorted.size() - 1));
    }
}
