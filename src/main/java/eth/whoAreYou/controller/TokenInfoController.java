package eth.whoAreYou.controller;

import eth.whoAreYou.dto.NFTInfoDto;
import eth.whoAreYou.dto.TokenDetailsDto;
import eth.whoAreYou.service.NFTInfoService;
import eth.whoAreYou.service.TokenDetailsService;
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

    private final TokenDetailsService tokenInfoService;
    private final TokenPriceService tokenPriceService;
    private final NFTInfoService nftInfoService;

    @GetMapping("/info/{address}")
    public ResponseEntity<?> getTokenInfo(@PathVariable String address) {
        try {
            TokenDetailsDto tokenDetailsDto = tokenInfoService.getTokenDetails(address);
            return ResponseEntity.ok(tokenDetailsDto);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("查詢失敗：" + e.getMessage());
        }
    }

    @GetMapping("/price/{address}")
    public Map<String, Double> getTokenPrice(@PathVariable String address) {
        return tokenPriceService.getTokenPrice(address);
    }

    @GetMapping("/nft/{address}")
    public ResponseEntity<?> getNftInfo(@PathVariable String address) {
        try {
            NFTInfoDto info = nftInfoService.getNFTInfo(address);
            return ResponseEntity.ok(info);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("NFT 查詢失敗：" + e.getMessage());
        }
    }

}


