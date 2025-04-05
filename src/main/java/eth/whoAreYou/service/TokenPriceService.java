package eth.whoAreYou.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class TokenPriceService {

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public Map<String, Double> getTokenPrice(String address) {
        String url = String.format("https://api.dexscreener.com/latest/dex/tokens/%s", address);

        try {
            String response = restTemplate.getForObject(url, String.class);
            JsonNode root = objectMapper.readTree(response);
            JsonNode pairs = root.path("pairs");

            if (pairs.isArray() && pairs.size() > 0) {
                JsonNode firstPair = pairs.get(0);
                JsonNode priceNode = firstPair.path("priceUsd");

                if (!priceNode.isMissingNode() && priceNode.isTextual()) {
                    double price = Double.parseDouble(priceNode.asText());
                    return Collections.singletonMap("price", price);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return Collections.singletonMap("price", -1.0);
    }
}
