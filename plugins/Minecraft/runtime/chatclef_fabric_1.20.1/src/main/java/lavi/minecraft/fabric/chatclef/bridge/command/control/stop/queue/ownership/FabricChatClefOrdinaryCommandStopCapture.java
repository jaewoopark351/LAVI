package lavi.minecraft.fabric.chatclef.bridge.command.control.stop.queue.ownership;

//20260905_kpopmodder: Freeze one pending, active, or absent ordinary owner under the STOP barrier lock.

import lavi.minecraft.fabric.chatclef.bridge.command.FabricChatClefCommandContext;

public final class FabricChatClefOrdinaryCommandStopCapture {
    private final FabricChatClefCommandContext context;
    private final String state;

    private FabricChatClefOrdinaryCommandStopCapture(
            FabricChatClefCommandContext context,
            String state
    ) {
        this.context = context;
        this.state = state;
    }

    public static FabricChatClefOrdinaryCommandStopCapture pending(FabricChatClefCommandContext context) {
        return new FabricChatClefOrdinaryCommandStopCapture(context, "pending");
    }

    public static FabricChatClefOrdinaryCommandStopCapture active(FabricChatClefCommandContext context) {
        return new FabricChatClefOrdinaryCommandStopCapture(context, "active");
    }

    public static FabricChatClefOrdinaryCommandStopCapture none() {
        return new FabricChatClefOrdinaryCommandStopCapture(null, "none");
    }

    public FabricChatClefCommandContext context() {
        return context;
    }

    public String state() {
        return state;
    }

    public boolean pending() {
        return "pending".equals(state);
    }

    public boolean active() {
        return "active".equals(state);
    }

    public boolean absent() {
        return context == null;
    }
}
