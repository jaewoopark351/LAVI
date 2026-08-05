package lavi.minecraft.integration.carryon;

//20260805_kpopmodder: Record optional carried-block identity without turning missing identity into state success.
public final class CarryOnCarriedBlockIdentity {
    private static final CarryOnCarriedBlockIdentity NOT_CARRYING = new CarryOnCarriedBlockIdentity(
            "not_carrying",
            "not_carrying",
            "not_carrying",
            "none"
    );
    private static final CarryOnCarriedBlockIdentity UNAVAILABLE = new CarryOnCarriedBlockIdentity(
            "unavailable",
            "unavailable",
            "unavailable",
            "none"
    );

    private final String blockId;
    private final String blockDescription;
    private final String blockState;
    private final String exceptionType;

    private CarryOnCarriedBlockIdentity(String blockId,
                                        String blockDescription,
                                        String blockState,
                                        String exceptionType) {
        this.blockId = nullToFallback(blockId);
        this.blockDescription = nullToFallback(blockDescription);
        this.blockState = nullToFallback(blockState);
        this.exceptionType = exceptionType == null ? "none" : exceptionType;
    }

    public static CarryOnCarriedBlockIdentity notCarrying() {
        return NOT_CARRYING;
    }

    public static CarryOnCarriedBlockIdentity unavailable() {
        return UNAVAILABLE;
    }

    public static CarryOnCarriedBlockIdentity unavailable(Throwable throwable) {
        return new CarryOnCarriedBlockIdentity(
                "unavailable",
                "unavailable",
                "unavailable",
                throwable == null ? "none" : throwable.getClass().getSimpleName()
        );
    }

    public static CarryOnCarriedBlockIdentity observed(String blockId,
                                                       String blockDescription,
                                                       String blockState) {
        return new CarryOnCarriedBlockIdentity(blockId, blockDescription, blockState, "none");
    }

    public String blockId() {
        return blockId;
    }

    public String blockDescription() {
        return blockDescription;
    }

    public String blockState() {
        return blockState;
    }

    public String exceptionType() {
        return exceptionType;
    }

    private static String nullToFallback(String value) {
        return value == null || value.isBlank() ? "unavailable" : value;
    }
}
