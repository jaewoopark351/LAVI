package lavi.minecraft.fabric.chatclef.bridge.command.result.effect.get;

//20260907_kpopmodder: Keep one target-count read and its world binding immutable.
final class FabricChatClefGetItemCountObservation {
    private final Integer count;
    private final Object worldIdentity;
    private final Object playerIdentity;
    private final String unavailableReason;

    private FabricChatClefGetItemCountObservation(
            Integer count,
            Object worldIdentity,
            Object playerIdentity,
            String unavailableReason
    ) {
        this.count = count;
        this.worldIdentity = worldIdentity;
        this.playerIdentity = playerIdentity;
        this.unavailableReason = unavailableReason == null || unavailableReason.isBlank()
                ? "unavailable"
                : unavailableReason;
    }

    static FabricChatClefGetItemCountObservation authoritative(
            int count,
            Object worldIdentity,
            Object playerIdentity
    ) {
        if (count < 0 || worldIdentity == null || playerIdentity == null) {
            return unavailable("invalid_authoritative_observation");
        }
        return new FabricChatClefGetItemCountObservation(
                count,
                worldIdentity,
                playerIdentity,
                ""
        );
    }

    static FabricChatClefGetItemCountObservation unavailable(String reason) {
        return new FabricChatClefGetItemCountObservation(null, null, null, reason);
    }

    boolean authoritative() {
        return count != null && worldIdentity != null && playerIdentity != null;
    }

    Integer countOrNull() {
        return count;
    }

    Object worldIdentity() {
        return worldIdentity;
    }

    Object playerIdentity() {
        return playerIdentity;
    }

    String unavailableReason() {
        return unavailableReason;
    }
}
