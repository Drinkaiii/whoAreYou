package eth.whoAreYou.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class InteractionInfoDto {
    private boolean interacted;
    private int interactionCount;
    private String lastInteractionHash;
    private long lastTimestamp;
}
