package eth.whoAreYou.controller;

import eth.whoAreYou.client.AnomalyDetectorClient;
import eth.whoAreYou.dto.WalletFeatureDto;
import eth.whoAreYou.service.FeatureExtractor;
import eth.whoAreYou.service.TransactionFetcher;
import eth.whoAreYou.service.TransactionFetcher.FetchResult;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/feature")
public class FeatureExtractionController {

    private final TransactionFetcher fetcher;
    private final FeatureExtractor extractor;
    private final AnomalyDetectorClient detector;

    public FeatureExtractionController(
            TransactionFetcher fetcher,
            FeatureExtractor extractor,
            AnomalyDetectorClient detector
    ) {
        this.fetcher = fetcher;
        this.extractor = extractor;
        this.detector = detector;
    }

    @GetMapping("/{walletAddress}")
    public Map<String, Object> extract(@PathVariable String walletAddress) throws Exception {
        FetchResult result = fetcher.fetchTransactions(walletAddress, 1000, 1); // ✅ 更新為 FetchResult
        WalletFeatureDto features = extractor.extract(result.transactions(), walletAddress, result.totalCount()); // ✅ 傳入 count
        return detector.predict(features);
    }
}
