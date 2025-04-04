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

    public WalletFeatureDto extract(List<TransactionDto> txList, String walletAddress) {
        if (txList.isEmpty()) return WalletFeatureDto.builder().build();

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

        long txCount = txList.size();
        double timeRange = Duration.between(timestamps.get(0), timestamps.get(timestamps.size() - 1)).getSeconds();
        double txPerDay = timeRange > 0 ? txCount / (timeRange / 86400.0) : 0;

        List<Long> diffs = new ArrayList<>();
        for (int i = 1; i < timestamps.size(); i++) {
            diffs.add(Duration.between(timestamps.get(i - 1), timestamps.get(i)).getSeconds());
        }

        // 分桶計數：1小時與5分鐘
        Map<Long, Long> hourlyBuckets = timestamps.stream().collect(Collectors.groupingBy(ts -> ts.getEpochSecond() / 3600, Collectors.counting()));
        Map<Long, Long> fiveMinBuckets = timestamps.stream().collect(Collectors.groupingBy(ts -> ts.getEpochSecond() / 300, Collectors.counting()));

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
                .selfTxRatio(selfTxCount / (double) txCount)
                .maxTxIn1h(Collections.max(hourlyBuckets.values()))
                .maxTxIn5min(Collections.max(fiveMinBuckets.values()))
                .txTimeStd(stddev(diffs))
                .zeroValueTxRatio(values.stream().filter(v -> v == 0).count() / (double) txCount)
                .highValueTxRatio(values.stream().filter(v -> v > quantile(values, 0.99)).count() / (double) txCount)
                .shortTimeRepeatedTx(0) // 可補上 groupBy "to" 差距 < 60 秒者
                .build();
    }

    private double median(List<Double> list) {
        if (list.isEmpty()) return 0;
        List<Double> sorted = new ArrayList<>(list);
        Collections.sort(sorted);
        int n = sorted.size();
        return n % 2 == 0 ? (sorted.get(n / 2 - 1) + sorted.get(n / 2)) / 2.0 : sorted.get(n / 2);
    }

    private double stddev(List<Long> list) {
        if (list.isEmpty()) return 0;
        double avg = list.stream().mapToDouble(Long::doubleValue).average().orElse(0);
        double sumSq = list.stream().mapToDouble(i -> Math.pow(i - avg, 2)).sum();
        return Math.sqrt(sumSq / list.size());
    }

    private double quantile(List<Double> list, double q) {
        List<Double> sorted = new ArrayList<>(list);
        Collections.sort(sorted);
        int index = (int) (q * sorted.size());
        return sorted.get(Math.min(index, sorted.size() - 1));
    }
}
