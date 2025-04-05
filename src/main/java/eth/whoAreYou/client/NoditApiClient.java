package eth.whoAreYou.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import eth.whoAreYou.dto.InteractionInfoDto;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.util.*;

@Component
@RequiredArgsConstructor
public class NoditApiClient {

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${nodit.api.key}")
    private String apiKey;

    @Value("${blockchain:ethereum}")
    private String protocol;

    @Value("${network:mainnet}")
    private String network;

    private static final String BASE_URL = "https://web3.nodit.io/v1";

    public InteractionInfoDto checkInteraction(String selfAddress, String targetAddress) {
        try {
            String url = String.format("%s/%s/%s/blockchain/getTransactionsByAccount", BASE_URL, protocol, network);

            String fromDate = "2015-07-30T00:00:00+00:00"; // Ethereum Genesis block
            String toDate = LocalDate.now() + "T00:00:00+00:00";

            int count = 0;
            String lastHash = null;
            long lastTimestamp = 0;
            String cursor = null;

            while (true) {
                Map<String, Object> requestBody = new HashMap<>();
                requestBody.put("accountAddress", selfAddress);
                requestBody.put("fromDate", fromDate);
                requestBody.put("toDate", toDate);
                requestBody.put("withDecode", true);
                requestBody.put("withLogs", true);
                requestBody.put("withCount", false);
                if (cursor != null) {
                    requestBody.put("cursor", cursor); // <== 加入分頁 cursor
                }

                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                headers.setAccept(List.of(MediaType.APPLICATION_JSON));
                headers.set("X-API-KEY", apiKey);

                HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
                ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);

                JsonNode root = objectMapper.readTree(response.getBody());
                JsonNode items = root.path("items");

                for (JsonNode tx : items) {
                    String from = tx.path("from").asText();
                    String to = tx.path("to").asText();
                    String txHash = tx.path("transactionHash").asText();
                    String value = tx.path("value").asText();
                    long ts = tx.path("timestamp").asLong();

                    if (from.equalsIgnoreCase(selfAddress) && to.equalsIgnoreCase(targetAddress)) {
                        count++;
                        if (ts > lastTimestamp) {
                            lastTimestamp = ts;
                            lastHash = txHash;
                        }
                    }
                }

                // 取下一頁的 cursor
                JsonNode cursorNode = root.path("cursor");
                if (cursorNode.isMissingNode() || cursorNode.isNull()) {
                    break; // 沒有更多資料就跳出
                }
                cursor = cursorNode.asText();
            }

            return InteractionInfoDto.builder()
                    .interacted(count > 0)
                    .interactionCount(count)
                    .lastInteractionHash(lastHash)
                    .lastTimestamp(lastTimestamp)
                    .build();

        } catch (Exception e) {
            e.printStackTrace();
            return InteractionInfoDto.builder()
                    .interacted(false)
                    .interactionCount(0)
                    .lastInteractionHash(null)
                    .lastTimestamp(0)
                    .build();
        }
    }
}