package eth.whoAreYou.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AddressInfoDto {
    private String addressType; // EOA, ERC-20, ERC-721, ERC-1155, Proxy, Unknown
    private String resolvedAddress;
    private Object details;
}

