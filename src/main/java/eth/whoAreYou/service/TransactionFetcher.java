package eth.whoAreYou.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import eth.whoAreYou.dto.TransactionDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
public class TransactionFetcher {

    @Value("${blockchain.api.key}")
    private String apiKey;

    private static final String BASE_URL = "https://web3.nodit.io/v1/ethereum/mainnet/blockchain/getInternalTransactionsByAccount";
    private static final int MAX_RETRIES = 5;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public List<TransactionDto> fetchTransactions(String walletAddress, int maxTransactions) {
        return fetchTransactions(walletAddress, maxTransactions, Integer.MAX_VALUE);
    }

    public List<TransactionDto> fetchTransactions(String walletAddress, int maxTransactions, int maxPages) {
        final String externalApiUrl = "https://web3.nodit.io/v1/ethereum/mainnet/blockchain/getTransactionsByAccount";
        List<TransactionDto> allTransactions = new ArrayList<>();
        String cursor = null;
        int page = 1;
        int retryCount = 0;

        while (page <= maxPages) {
            Map<String, Object> payload = new HashMap<>();
            payload.put("accountAddress", walletAddress);
            payload.put("withCount", false);
            payload.put("withLogs", false);
            payload.put("withDecode", false);
            payload.put("rpp", 1000);
            if (cursor != null) payload.put("cursor", cursor);

            HttpHeaders headers = new HttpHeaders();
            headers.set("X-API-KEY", apiKey);
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);

            try {
                ResponseEntity<Map> response = restTemplate.exchange(externalApiUrl, HttpMethod.POST, entity, Map.class);

                if (response.getStatusCode() == HttpStatus.OK) {
                    retryCount = 0;

                    Map<String, Object> body = response.getBody();
                    List<Map<String, Object>> items = (List<Map<String, Object>>) body.get("items");

                    for (Map<String, Object> item : items) {
                        TransactionDto tx = objectMapper.convertValue(item, TransactionDto.class);
                        allTransactions.add(tx);
                    }

                    System.out.printf("[%s] Page %d - Fetched %d tx%n", walletAddress, page, items.size());
                    page++;

                    if (allTransactions.size() >= maxTransactions) {
                        System.out.printf("[%s] Reached %d transactions. Stopping.%n", walletAddress, maxTransactions);
                        break;
                    }

                    cursor = (String) body.get("cursor");
                    if (cursor == null) {
                        System.out.printf("[%s] No more transactions. Stopping.%n", walletAddress);
                        break;
                    }

                    Thread.sleep(1000);

                } else if (response.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS) {
                    retryCount++;
                    if (retryCount > MAX_RETRIES) {
                        System.out.printf("[%s] ⚠️ Retry limit reached. Stopping.%n", walletAddress);
                        break;
                    }
                    System.out.printf("[%s] Rate limit hit. Waiting 10 seconds... (retry %d/%d)%n", walletAddress, retryCount, MAX_RETRIES);
                    Thread.sleep(10_000);
                    continue;

                } else {
                    System.out.printf("[%s] Error %s: %s%n", walletAddress, response.getStatusCode(), response.getBody());
                    break;
                }

            } catch (Exception e) {
                System.out.printf("[%s] ❌ Exception: %s%n", walletAddress, e.getMessage());
                break;
            }
        }

        return allTransactions.subList(0, Math.min(maxTransactions, allTransactions.size()));
    }
}