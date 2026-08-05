package lavi.minecraft.integration.carryon;

//20260730_kpopmodder: Normalize optional Carry On state reads without reporting fabricated success.
public final class CarryOnObservation {
    private final boolean loaded;
    private final String version;
    private final CarryOnCarryState state;
    private final String exceptionType;
    private final CarryOnCarriedBlockIdentity carriedBlockIdentity;

    private CarryOnObservation(boolean loaded,
                               String version,
                               CarryOnCarryState state,
                               String exceptionType,
                               CarryOnCarriedBlockIdentity carriedBlockIdentity) {
        this.loaded = loaded;
        this.version = version;
        this.state = state;
        this.exceptionType = exceptionType;
        this.carriedBlockIdentity = carriedBlockIdentity == null
                ? CarryOnCarriedBlockIdentity.unavailable()
                : carriedBlockIdentity;
    }

    public static CarryOnObservation absent() {
        return new CarryOnObservation(false, "absent", CarryOnCarryState.ABSENT, "none", CarryOnCarriedBlockIdentity.unavailable());
    }

    public static CarryOnObservation observed(String version, boolean carrying) {
        return observed(version, carrying, carrying ? CarryOnCarriedBlockIdentity.unavailable() : CarryOnCarriedBlockIdentity.notCarrying());
    }

    public static CarryOnObservation observed(String version,
                                              boolean carrying,
                                              CarryOnCarriedBlockIdentity carriedBlockIdentity) {
        return new CarryOnObservation(
                true,
                version,
                carrying ? CarryOnCarryState.AVAILABLE_CARRYING : CarryOnCarryState.AVAILABLE_NOT_CARRYING,
                "none",
                carriedBlockIdentity
        );
    }

    public static CarryOnObservation incompatible(String version, Throwable throwable) {
        return new CarryOnObservation(true, version, CarryOnCarryState.INCOMPATIBLE, exceptionType(throwable), CarryOnCarriedBlockIdentity.unavailable());
    }

    public static CarryOnObservation unreadable(String version, Throwable throwable) {
        return new CarryOnObservation(true, version, CarryOnCarryState.STATE_UNREADABLE, exceptionType(throwable), CarryOnCarriedBlockIdentity.unavailable());
    }

    public static CarryOnObservation failed(String version, Throwable throwable) {
        return new CarryOnObservation(true, version, CarryOnCarryState.OBSERVATION_FAILED, exceptionType(throwable), CarryOnCarriedBlockIdentity.unavailable());
    }

    public boolean loaded() {
        return loaded;
    }

    public String version() {
        return version;
    }

    public CarryOnCarryState state() {
        return state;
    }

    public String exceptionType() {
        return exceptionType;
    }

    public String carriedBlockId() {
        return carriedBlockIdentity.blockId();
    }

    public String carriedBlockDescription() {
        return carriedBlockIdentity.blockDescription();
    }

    public String carriedBlockState() {
        return carriedBlockIdentity.blockState();
    }

    public String carriedBlockExceptionType() {
        return carriedBlockIdentity.exceptionType();
    }

    private static String exceptionType(Throwable throwable) {
        return throwable == null ? "none" : throwable.getClass().getSimpleName();
    }
}
