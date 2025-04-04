package eth.whoAreYou.dto;

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
    private long maxTxIn1h;
    private long maxTxIn5min;
    private double txTimeStd;
    private double zeroValueTxRatio;
    private double highValueTxRatio;
    private double shortTimeRepeatedTx;
}
