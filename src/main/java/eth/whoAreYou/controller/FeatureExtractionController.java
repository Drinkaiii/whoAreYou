package eth.whoAreYou.controller;

import eth.whoAreYou.dto.WalletFeatureDto;
import eth.whoAreYou.service.FeatureExtractor;
import eth.whoAreYou.service.TransactionFetcher;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/feature")
public class FeatureExtractionController {

    private final TransactionFetcher fetcher;
    private final FeatureExtractor extractor;

    public FeatureExtractionController(TransactionFetcher fetcher, FeatureExtractor extractor) {
        this.fetcher = fetcher;
        this.extractor = extractor;
    }

    @GetMapping("/{walletAddress}")
    public WalletFeatureDto extract(@PathVariable String walletAddress) {
        var txList = fetcher.fetchTransactions(walletAddress, 1000,1);
        return extractor.extract(txList, walletAddress);
    }
}
