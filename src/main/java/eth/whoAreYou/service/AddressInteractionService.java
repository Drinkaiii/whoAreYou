package eth.whoAreYou.service;

import eth.whoAreYou.client.NoditApiClient;
import eth.whoAreYou.dto.InteractionInfoDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AddressInteractionService {

    private final NoditApiClient noditApiClient;

    /**
     * 檢查 selfAddress 是否曾與 targetAddress 有過互動（支援指定鏈）
     */
    public InteractionInfoDto getInteractionInfo(String selfAddress, String targetAddress, String chain) {
        // 簡單映射鏈名稱到協議名稱（小寫）
        String protocol = chain.toLowerCase();
        return noditApiClient.checkInteraction(selfAddress, targetAddress, protocol);
    }
}