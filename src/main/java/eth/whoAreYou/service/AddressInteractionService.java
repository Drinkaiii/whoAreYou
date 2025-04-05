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
     * 檢查 selfAddress 是否曾與 targetAddress 有過互動
     */
    public InteractionInfoDto getInteractionInfo(String selfAddress, String targetAddress) {
        return noditApiClient.checkInteraction(selfAddress, targetAddress);
    }
}
