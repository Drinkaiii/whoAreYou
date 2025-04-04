package eth.whoAreYou.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class TokenPriceService {

    @Value("${blockchain:ethereum}")
    private String blockchain;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public Map<String, Double> getTokenPrice(String address) {
        String vsCurrency="usd";
        String url = String.format(
                "https://api.coingecko.com/api/v3/simple/token_price/%s?contract_addresses=%s&vs_currencies=%s",
                blockchain, address, vsCurrency
        );

        try {
            String response = restTemplate.getForObject(url, String.class);
            JsonNode jsonNode = objectMapper.readTree(response);
            JsonNode priceNode = jsonNode.get(address.toLowerCase());

            if (priceNode != null && priceNode.has(vsCurrency)) {
                double price = priceNode.get(vsCurrency).asDouble();
                return Collections.singletonMap("price", price);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return Collections.singletonMap("price", -1.0);
    }
}


