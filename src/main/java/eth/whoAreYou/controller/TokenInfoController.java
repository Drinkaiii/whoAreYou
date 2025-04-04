package eth.whoAreYou.controller;

import eth.whoAreYou.service.TokenInfoService;
import eth.whoAreYou.service.TokenPriceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/token")
@RequiredArgsConstructor
public class TokenInfoController {

    private final TokenInfoService tokenInfoService;
    private final TokenPriceService tokenPriceService;

    @GetMapping("/info/{address}")
    public ResponseEntity<?> getTokenInfo(@PathVariable String address) {
        try {
            TokenInfoService.TokenInfo info = tokenInfoService.getTokenInfo(address);
            return ResponseEntity.ok(info);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("查詢失敗：" + e.getMessage());
        }
    }

    @GetMapping("/price/{address}")
    public Map<String, Double> getTokenPrice(@PathVariable String address) {
        return tokenPriceService.getTokenPrice(address);
    }
}


