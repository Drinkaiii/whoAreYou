package eth.whoAreYou.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TokenInfoDto {
    private String name;
    private String symbol;
    private double price;
}
