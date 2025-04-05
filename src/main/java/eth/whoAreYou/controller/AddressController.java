package eth.whoAreYou.controller;

import eth.whoAreYou.dto.AddressInfoDto;
import eth.whoAreYou.service.SortingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/1.0/address")
@RequiredArgsConstructor
public class AddressController {

    private final SortingService sortingService;

    @GetMapping("/{address}")
    public ResponseEntity<?> classifyAddress(@PathVariable String address) {
        try {
            AddressInfoDto result = sortingService.classify(address);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("分類失敗：" + e.getMessage());
        }
    }
}


