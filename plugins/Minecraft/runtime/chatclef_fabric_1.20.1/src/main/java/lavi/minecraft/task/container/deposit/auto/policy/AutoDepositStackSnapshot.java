package lavi.minecraft.task.container.deposit.auto.policy;

import net.minecraft.item.Item;

import java.util.Objects;

//20260827_kpopmodder: Capture immutable slot and metadata facts before automatic deposit planning.
public final class AutoDepositStackSnapshot {
    private final int slotIndex;
    private final AutoDepositStackLocation location;
    private final Item item;
    private final String itemId;
    private final int count;
    private final int maxCount;
    private final boolean selectedMainHand;
    private final boolean botProtected;
    private final boolean specialMetadata;
    private final String metadataFingerprint;
    private final AutoDepositItemRole role;
    private final int equipmentScore;
    private final boolean food;
    private final boolean blockItem;
    private final boolean fallingBlock;

    public AutoDepositStackSnapshot(int slotIndex,
                                    AutoDepositStackLocation location,
                                    Item item,
                                    String itemId,
                                    int count,
                                    int maxCount,
                                    boolean selectedMainHand,
                                    boolean botProtected,
                                    boolean specialMetadata,
                                    String metadataFingerprint,
                                    AutoDepositItemRole role,
                                    int equipmentScore,
                                    boolean food,
                                    boolean blockItem,
                                    boolean fallingBlock) {
        if (slotIndex < 0) {
            throw new IllegalArgumentException("slotIndex must be non-negative");
        }
        if (count <= 0) {
            throw new IllegalArgumentException("count must be positive");
        }
        this.slotIndex = slotIndex;
        this.location = Objects.requireNonNull(location, "location");
        this.item = Objects.requireNonNull(item, "item");
        this.itemId = Objects.requireNonNull(itemId, "itemId");
        this.count = count;
        this.maxCount = Math.max(1, maxCount);
        this.selectedMainHand = selectedMainHand;
        this.botProtected = botProtected;
        this.specialMetadata = specialMetadata;
        this.metadataFingerprint = Objects.requireNonNull(metadataFingerprint, "metadataFingerprint");
        this.role = Objects.requireNonNull(role, "role");
        this.equipmentScore = equipmentScore;
        this.food = food;
        this.blockItem = blockItem;
        this.fallingBlock = fallingBlock;
    }

    public int slotIndex() {
        return slotIndex;
    }

    public AutoDepositStackLocation location() {
        return location;
    }

    public Item item() {
        return item;
    }

    public String itemId() {
        return itemId;
    }

    public int count() {
        return count;
    }

    public int maxCount() {
        return maxCount;
    }

    public boolean selectedMainHand() {
        return selectedMainHand;
    }

    public boolean botProtected() {
        return botProtected;
    }

    public boolean specialMetadata() {
        return specialMetadata;
    }

    public String metadataFingerprint() {
        return metadataFingerprint;
    }

    public AutoDepositItemRole role() {
        return role;
    }

    public int equipmentScore() {
        return equipmentScore;
    }

    public boolean food() {
        return food;
    }

    public boolean blockItem() {
        return blockItem;
    }

    public boolean fallingBlock() {
        return fallingBlock;
    }

    public boolean isMainInventory() {
        return location == AutoDepositStackLocation.MAIN;
    }

    public boolean isEquipped() {
        return location == AutoDepositStackLocation.ARMOR || location == AutoDepositStackLocation.OFFHAND;
    }

    public String semanticIdentity() {
        return location + ":" + slotIndex + ":" + itemId + ":" + metadataFingerprint
                + ":selected=" + selectedMainHand + ":bot=" + botProtected
                + ":role=" + role + ":score=" + equipmentScore;
    }
}
