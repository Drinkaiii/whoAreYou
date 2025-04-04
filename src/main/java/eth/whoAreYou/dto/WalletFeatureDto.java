package eth.whoAreYou.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class WalletFeatureDto {
    private long txCount;
    private double avgValue;
    private double maxValue;
    private double medianGasPrice;
    private long uniqueReceivers;
    private long uniqueSenders;
    private double txPerDay;
    private long inTxCount;
    private long outTxCount;
    private double inOutRatio;
    private double selfTxRatio;

    @JsonProperty("max_tx_in_1h")
    private long maxTxIn1h;

    @JsonProperty("max_tx_in_5min")
    private long maxTxIn5min;
    private double txTimeStd;
    private double zeroValueTxRatio;
    private double highValueTxRatio;
    private double shortTimeRepeatedTx;
}
