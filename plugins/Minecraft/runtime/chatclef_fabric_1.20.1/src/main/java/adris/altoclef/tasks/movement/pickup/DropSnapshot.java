package adris.altoclef.tasks.movement.pickup;

import net.minecraft.entity.ItemEntity;

//20260728_kpopmodder: Added this type file to preserve dropped item log state after Minecraft clears an item entity.
final class DropSnapshot {
    private final String uuid;
    private final String itemKey;
    private final int count;
    private final String blockPos;

    private DropSnapshot(String uuid, String itemKey, int count, String blockPos) {
        this.uuid = uuid;
        this.itemKey = itemKey;
        this.count = count;
        this.blockPos = blockPos;
    }

    static DropSnapshot from(ItemEntity drop) {
        if (drop == null || drop.getStack().isEmpty()) {
            return null;
        }
        return new DropSnapshot(
                drop.getUuid().toString(),
                drop.getStack().getItem().getTranslationKey(),
                drop.getStack().getCount(),
                drop.getBlockPos().toShortString()
        );
    }

    boolean matches(ItemEntity drop) {
        return drop != null && uuid.equals(drop.getUuid().toString());
    }

    boolean shouldAnnotate(ItemEntity drop) {
        return drop == null
                || !drop.isAlive()
                || drop.getStack().isEmpty()
                || !itemKey.equals(drop.getStack().getItem().getTranslationKey())
                || count != drop.getStack().getCount()
                || !blockPos.equals(drop.getBlockPos().toShortString());
    }

    String describeAsLastKnown() {
        return "lastKnownItem=" + itemKey + " x " + count
                + ", lastKnownDropPos=" + blockPos;
    }
}
