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

    public Map<String, Object> predict(WalletFeatureDto dto) throws Exception {
        // 1. 將 Java 物件轉為 snake_case 的 Map
        Map<String, Object> snakeCaseMap = snakeCaseMapper.convertValue(dto, Map.class);

        // 2. 包裝成 FastAPI 需要的格式：{ "features": { ... } }
        Map<String, Object> requestBody = Map.of("features", snakeCaseMap);

        // 3. 發送 POST 請求
        return webClient.post()
                .uri("/predict")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(Map.class)
                .block();
    }
}
