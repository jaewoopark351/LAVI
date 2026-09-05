package lavi.minecraft.fabric.chatclef.bridge.command.control.stop.result;

//20260905_kpopmodder: Own only the synchronized registry of live STOP result deliveries.

import java.util.ArrayList;
import java.util.List;

public final class FabricChatClefStopControlResultDeliveryRegistry {
    private final List<FabricChatClefStopControlResultDelivery> deliveries = new ArrayList<>();

    public synchronized void add(FabricChatClefStopControlResultDelivery delivery) {
        deliveries.add(delivery);
    }

    public synchronized void remove(FabricChatClefStopControlResultDelivery delivery) {
        deliveries.remove(delivery);
    }

    public synchronized List<FabricChatClefStopControlResultDelivery> snapshot() {
        return new ArrayList<>(deliveries);
    }

    public synchronized int deliveryCount() {
        return deliveries.size();
    }

    public synchronized void reset() {
        deliveries.clear();
    }
}
