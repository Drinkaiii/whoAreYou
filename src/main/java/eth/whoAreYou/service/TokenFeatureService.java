package eth.whoAreYou.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class TokenFeatureService {

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper;

    @Value("${nodit.api.key}")
    private String apiKey;

    @Value("${ml.model.base-url}")
    private String mlModelBaseUrl;

    public Map<String, Object> fetchTokenFeatureAndPredict(String contractAddress, String chain) {
        // 獲取代幣特徵
        Map<String, Object> tokenFeatures = fetchTokenFeatureAndRisk(contractAddress, chain);
        if (tokenFeatures == null) {
            return Map.of("error", "無法獲取代幣特徵數據");
        }

        // 根據區塊鏈選擇預測端點
        String predictEndpoint = String.format("%s/%sTokenPredict",
                mlModelBaseUrl,
                chain.toLowerCase());

        try {
            // 創建符合API期望的請求格式
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("features", tokenFeatures);  // 包裝在features字段中

            // 發送特徵數據到預測端點
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestBody, headers);
            ResponseEntity<String> predictResponse = restTemplate.exchange(
                    predictEndpoint,
                    HttpMethod.POST,
                    requestEntity,
                    String.class
            );

            // 解析預測結果
            JsonNode predictionData = objectMapper.readTree(predictResponse.getBody());

            // 將預測結果添加到原始特徵中
            tokenFeatures.put("prediction", Map.of(
                    "amplitude_class", predictionData.get("amplitude_class").asText(),
                    "direction_class", predictionData.get("direction_class").asText()
            ));

            return tokenFeatures;

        } catch (Exception e) {
            System.out.println("❌ 預測失敗: " + e.getMessage());
            e.printStackTrace();

            // 返回特徵數據但添加錯誤信息
            tokenFeatures.put("prediction_error", e.getMessage());
            return tokenFeatures;
        }
    }

    private Map<String, Object> fetchTokenFeatureAndRisk(String contractAddress, String chain) {
        if (chain == null || chain.isEmpty()) {
            chain = "ethereum";
        }

        // 設置HTTP請求頭
        HttpHeaders headers = new HttpHeaders();
        headers.set("accept", "application/json");
        headers.set("content-type", "application/json");
        headers.set("X-API-KEY", apiKey);

        try {
            // Step 1: 獲取代幣價格與市值等資料
            String priceUrl = String.format("https://web3.nodit.io/v1/%s/mainnet/token/getTokenPricesByContracts", chain);
            Map<String, Object> pricePayload = new HashMap<>();
            pricePayload.put("contractAddresses", List.of(contractAddress));
            pricePayload.put("currency", "USD");

            HttpEntity<Map<String, Object>> priceRequestEntity = new HttpEntity<>(pricePayload, headers);
            ResponseEntity<String> priceResponse = restTemplate.exchange(
                    priceUrl,
                    HttpMethod.POST,
                    priceRequestEntity,
                    String.class
            );

            JsonNode priceData = objectMapper.readTree(priceResponse.getBody());
            if (priceData.isEmpty()) {
                System.out.println("⚠️ 找不到此合約的價格資料");
                return null;
            }

            JsonNode token = priceData.get(0);

            // Step 2: 查詢持有者數量
            String holdersUrl = String.format("https://web3.nodit.io/v1/%s/mainnet/token/getTokenHoldersByContract", chain);
            Map<String, Object> holdersPayload = new HashMap<>();
            holdersPayload.put("contractAddress", contractAddress);
            holdersPayload.put("withCount", true);

            HttpEntity<Map<String, Object>> holdersRequestEntity = new HttpEntity<>(holdersPayload, headers);
            ResponseEntity<String> holdersResponse = restTemplate.exchange(
                    holdersUrl,
                    HttpMethod.POST,
                    holdersRequestEntity,
                    String.class
            );

            JsonNode holdersData = objectMapper.readTree(holdersResponse.getBody());
            int holdersCount = holdersData.has("count") ? holdersData.get("count").asInt() : 0;

            // 提取特徵數據
            double volume24h = getDoubleValue(token, "volumeFor24h");
            double volumeChangeFor24h = getDoubleValue(token, "volumeChangeFor24h");
            double percentChangeFor1h = getDoubleValue(token, "percentChangeFor1h");
            double percentChangeFor24h = getDoubleValue(token, "percentChangeFor24h");
            double percentChangeFor7d = getDoubleValue(token, "percentChangeFor7d");
            double marketCap = getDoubleValue(token, "marketCap");
            double price = getDoubleValue(token, "price");

            // 整理並返回結果
            Map<String, Object> result = new HashMap<>();
            result.put("contract", contractAddress);
            result.put("volumeFor24h", volume24h);
            result.put("volumeChangeFor24h", volumeChangeFor24h);
            result.put("percentChangeFor1h", percentChangeFor1h);
            result.put("percentChangeFor24h", percentChangeFor24h);
            result.put("percentChangeFor7d", percentChangeFor7d);
            result.put("holdersCount", holdersCount);
            result.put("marketCap", marketCap);
            result.put("price", price);

            return result;

        } catch (Exception e) {
            System.out.println("❌ 獲取代幣特徵數據失敗: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    // 輔助方法：安全地從JsonNode獲取double值
    private double getDoubleValue(JsonNode node, String fieldName) {
        if (node.has(fieldName) && !node.get(fieldName).isNull()) {
            try {
                return node.get(fieldName).asDouble();
            } catch (Exception e) {
                return 0.0;
            }
        }
        return 0.0;
    }
}