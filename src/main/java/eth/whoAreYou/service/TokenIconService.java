package eth.whoAreYou.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class TokenIconService {

    @Value("${coinmarketcap.key}")
    private String coinmarketcapKey;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Map<String, String> iconCache = new ConcurrentHashMap<>();

    public String getTokenIcon(String address, String chain) {
        chain = chain.toLowerCase();

        String cacheKey = chain + ":" + address;
        if (iconCache.containsKey(cacheKey)) {
            return iconCache.get(cacheKey);
        }

        // Step 1: Dexscreener 嘗試
        String dexIcon = getFromDexscreener(address);
        System.out.println(address);
        if (dexIcon != null) {
            iconCache.put(cacheKey, dexIcon);
            return dexIcon;
        }

        // Step 2: 可擴充 - 針對不同鏈用不同 fallback
        if ("ethereum".equals(chain)) {
            String cgIcon = getFromCoinGecko("ethereum", address);
            if (cgIcon != null) {
                iconCache.put(cacheKey, cgIcon);
                return cgIcon;
            }
        }
//        else if ("base".equals(chain)) {
//            String cgIcon = getFromCoinGecko("base", address); // 注意：base 並不一定全部支援
//            if (cgIcon != null) {
//                iconCache.put(cacheKey, cgIcon);
//                return cgIcon;
//            }
//        }

        // Step 3: fallback - CoinMarketCap
        String cmcIcon = getFromCoinMarketCap(address);
        if (cmcIcon != null) {
            iconCache.put(cacheKey, cmcIcon);
            return cmcIcon;
        }

        return null; // 無結果
    }

    private String getFromDexscreener(String address) {
        try {
            String url = String.format("https://api.dexscreener.com/latest/dex/tokens/%s", address);
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            JsonNode root = objectMapper.readTree(response.getBody());
            JsonNode pairs = root.path("pairs");

            if (pairs.isArray() && pairs.size() > 0) {
                for (JsonNode pair : pairs) {
                    // ✅ 先檢查 info.imageUrl（這才是 Dexscreener 放 icon 的主要位置）
                    JsonNode info = pair.path("info");
                    if (!info.isMissingNode()) {
                        String infoIcon = info.path("imageUrl").asText(null);
                        if (infoIcon != null && !infoIcon.isEmpty()) {
                            return infoIcon;
                        }
                    }

                    // 再 fallback 檢查 base/quote token
                    JsonNode base = pair.path("baseToken");
                    if (base.path("address").asText().equalsIgnoreCase(address)) {
                        String baseIcon = base.path("imageUrl").asText(null);
                        if (baseIcon != null && !baseIcon.isEmpty()) return baseIcon;
                    }

                    JsonNode quote = pair.path("quoteToken");
                    if (quote.path("address").asText().equalsIgnoreCase(address)) {
                        String quoteIcon = quote.path("imageUrl").asText(null);
                        if (quoteIcon != null && !quoteIcon.isEmpty()) return quoteIcon;
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Dexscreener failed: " + e.getMessage());
        }
        return null;
    }


    private String getFromCoinGecko(String chain, String address) {
        try {
            String url = String.format(
                    "https://api.coingecko.com/api/v3/coins/%s/contract/%s",
                    chain,
                    address
            );
            String response = restTemplate.getForObject(url, String.class);
            JsonNode jsonNode = objectMapper.readTree(response);
            return jsonNode.path("image").path("small").asText(null);
        } catch (Exception e) {
            System.err.println("CoinGecko failed (%s): %s".formatted(chain, e.getMessage()));
        }
        return null;
    }

    private String getFromCoinMarketCap(String address) {
        try {
            String url = String.format(
                    "https://pro-api.coinmarketcap.com/v1/cryptocurrency/info?address=%s", address
            );

            HttpHeaders headers = new HttpHeaders();
            headers.set("X-CMC_PRO_API_KEY", coinmarketcapKey);
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    String.class
            );

            JsonNode root = objectMapper.readTree(response.getBody());
            JsonNode data = root.path("data");
            if (!data.isMissingNode()) {
                // 只抓第一個資料（因為 address 對應資料會是 map）
                JsonNode first = data.elements().next();
                return first.path("logo").asText(null);
            }
        } catch (Exception e) {
            System.err.println("CoinMarketCap failed: " + e.getMessage());
        }
        return null;
    }


}
