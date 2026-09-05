package lavi.minecraft.fabric.chatclef.bridge.transport.inbound;

//20260905_kpopmodder: Assemble WebSocket text fragments into one complete inbound message.

import java.util.Optional;

public final class FabricChatClefInboundTextFrameAssembler {
    private final StringBuilder incomingText = new StringBuilder();

    public void reset() {
        incomingText.setLength(0);
    }

    public Optional<String> append(CharSequence data, boolean last) {
        incomingText.append(data);
        if (!last) {
            return Optional.empty();
        }
        String message = incomingText.toString();
        incomingText.setLength(0);
        return Optional.of(message);
    }
}
