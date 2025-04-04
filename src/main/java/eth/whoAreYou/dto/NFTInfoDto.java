package eth.whoAreYou.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigInteger;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NFTInfoDto {
    private String name;
    private BigInteger totalSupply;
    private String imageUrl;
}
