package lavi.minecraft.fabric.chatclef.bridge.command.control.stop.request.validation;

//20260905_kpopmodder: Keep STOP validation precedence and its first terminal reason explicit.
public final class FabricChatClefStopControlValidationDecision {
    private final FabricChatClefStopControlRequest request;
    private final String rejectionReason;
    private final FabricChatClefStopControlTargetScope resultTargetScope;
    private final String invalidTargetFieldsMask;

    private FabricChatClefStopControlValidationDecision(
            FabricChatClefStopControlRequest request,
            String rejectionReason,
            FabricChatClefStopControlTargetScope resultTargetScope,
            String invalidTargetFieldsMask
    ) {
        this.request = request;
        this.rejectionReason = rejectionReason;
        this.resultTargetScope = resultTargetScope;
        this.invalidTargetFieldsMask = invalidTargetFieldsMask;
    }

    public static FabricChatClefStopControlValidationDecision accepted(
            FabricChatClefStopControlRequest request
    ) {
        return new FabricChatClefStopControlValidationDecision(request, "", request.targetScope(), "none");
    }

    public static FabricChatClefStopControlValidationDecision rejected(
            String reason,
            FabricChatClefStopControlTargetScope resultTargetScope
    ) {
        return rejected(reason, resultTargetScope, "none");
    }

    public static FabricChatClefStopControlValidationDecision rejected(
            String reason,
            FabricChatClefStopControlTargetScope resultTargetScope,
            String invalidTargetFieldsMask
    ) {
        return new FabricChatClefStopControlValidationDecision(
                null,
                reason,
                resultTargetScope,
                invalidTargetFieldsMask == null || invalidTargetFieldsMask.isBlank()
                        ? "none"
                        : invalidTargetFieldsMask
        );
    }

    public boolean accepted() {
        return request != null;
    }

    public FabricChatClefStopControlRequest request() {
        return request;
    }

    public String rejectionReason() {
        return rejectionReason;
    }

    public FabricChatClefStopControlTargetScope resultTargetScope() {
        return resultTargetScope;
    }

    public String invalidTargetFieldsMask() {
        return invalidTargetFieldsMask;
    }
}
