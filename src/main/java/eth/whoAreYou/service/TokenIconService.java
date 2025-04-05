package eth.whoAreYou.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
@RequiredArgsConstructor
public class TokenIconService {

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public String getTokenIcon(String address) {
        String url = String.format(
                "https://api.coingecko.com/api/v3/coins/ethereum/contract/%s",
                address
        );

        try {
            String response = restTemplate.getForObject(url, String.class);
            JsonNode jsonNode = objectMapper.readTree(response);
            return jsonNode.path("image").path("small").asText(); // thumb, small, large
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
