package lavi.minecraft.diagnostics.postplace;

//20260804_kpopmodder: Type post-place interaction phases while preserving existing diagnostic phase strings.
public enum PostPlaceContainerInteractionPhase {
    HEAD("HEAD"),
    RETURN("RETURN"),
    OTHER("");

    private final String wireValue;

    PostPlaceContainerInteractionPhase(String wireValue) {
        this.wireValue = wireValue;
    }

    public static PostPlaceContainerInteractionPhase fromWireValue(String value) {
        for (PostPlaceContainerInteractionPhase phase : values()) {
            if (phase.wireValue.equals(value)) {
                return phase;
            }
        }
        return OTHER;
    }

    public boolean isHead() {
        return this == HEAD;
    }

    public boolean isReturn() {
        return this == RETURN;
    }
}
