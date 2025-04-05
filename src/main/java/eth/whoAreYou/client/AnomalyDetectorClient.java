package eth.whoAreYou.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import eth.whoAreYou.dto.WalletFeatureDto;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import java.util.Map;

@Service
public class AnomalyDetectorClient {

    private final WebClient webClient;
    private final ObjectMapper snakeCaseMapper;

    public AnomalyDetectorClient() {
        this.webClient = WebClient.builder()
                .baseUrl("http://localhost:8000") // 你的 FastAPI server
                .build();

        // 自動把 Java camelCase → JSON snake_case
        this.snakeCaseMapper = new ObjectMapper();
        this.snakeCaseMapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
    }

    /**
     * 原有方法 - 預設使用通用端點
     */
    public Map<String, Object> predict(WalletFeatureDto dto) throws Exception {
        return predict(dto, null); // 使用默認端點
    }

    /**
     * 支援多鏈的預測方法
     * @param dto 錢包特徵
     * @param chain 鏈識別符（ETHEREUM 或 BASE）
     * @return 預測結果
     */
    public Map<String, Object> predict(WalletFeatureDto dto, String chain) throws Exception {
        // 1. 將 Java 物件轉為 snake_case 的 Map
        Map<String, Object> snakeCaseMap = snakeCaseMapper.convertValue(dto, Map.class);

        // 2. 包裝成 FastAPI 需要的格式：{ "features": { ... } }
        Map<String, Object> requestBody = Map.of("features", snakeCaseMap);

        // 3. 根據鏈選擇端點
        String endpoint = getPredictEndpoint(chain);

        // 4. 發送 POST 請求
        return webClient.post()
                .uri(endpoint)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(Map.class)
                .block();
    }

    /**
     * 根據鏈獲取對應的預測端點
     */
    private String getPredictEndpoint(String chain) {
        if (chain == null) {
            return "/predict";  // 預設端點
        }

        return switch (chain.toUpperCase()) {
            case "ETHEREUM" -> "/ethereumWalletPredict";
            case "BASE" -> "/baseWalletPredict";
            default -> "/predict";  // 未知鏈使用預設端點
        };
    }
}