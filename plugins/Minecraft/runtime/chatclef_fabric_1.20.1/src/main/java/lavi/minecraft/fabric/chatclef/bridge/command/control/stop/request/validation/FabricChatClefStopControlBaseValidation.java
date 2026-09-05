package lavi.minecraft.fabric.chatclef.bridge.command.control.stop.request.validation;

//20260905_kpopmodder: Distinguish no-wire base-identity failure from a safely echoable STOP request.
public final class FabricChatClefStopControlBaseValidation {
    private final FabricChatClefStopControlBaseRequest request;
    private final String diagnosticDisposition;

    private FabricChatClefStopControlBaseValidation(
            FabricChatClefStopControlBaseRequest request,
            String diagnosticDisposition
    ) {
        this.request = request;
        this.diagnosticDisposition = diagnosticDisposition;
    }

    public static FabricChatClefStopControlBaseValidation valid(FabricChatClefStopControlBaseRequest request) {
        return new FabricChatClefStopControlBaseValidation(request, "");
    }

    public static FabricChatClefStopControlBaseValidation invalid(String diagnosticDisposition) {
        return new FabricChatClefStopControlBaseValidation(null, diagnosticDisposition);
    }

    public boolean valid() {
        return request != null;
    }

    public FabricChatClefStopControlBaseRequest request() {
        return request;
    }

    public String diagnosticDisposition() {
        return diagnosticDisposition;
    }
}
