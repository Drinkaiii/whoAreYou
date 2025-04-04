package eth.whoAreYou.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class TransactionDto {
    public String from;
    public String to;
    public String value;
    public String gasPrice;
    public long timeStamp;
}
