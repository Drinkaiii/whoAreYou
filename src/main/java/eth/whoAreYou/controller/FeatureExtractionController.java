package eth.whoAreYou.controller;

import eth.whoAreYou.client.AnomalyDetectorClient;
import eth.whoAreYou.dto.WalletFeatureDto;
import eth.whoAreYou.service.FeatureExtractor;
import eth.whoAreYou.service.TransactionFetcher;
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
        var txList = fetcher.fetchTransactions(walletAddress, 1000, 1);
        WalletFeatureDto features = extractor.extract(txList, walletAddress);
        return detector.predict(features);
    }
}
