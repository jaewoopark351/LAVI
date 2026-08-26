package lavi.minecraft.task.container.deposit.auto.trusted;

import adris.altoclef.util.Dimension;
import net.minecraft.util.math.BlockPos;

import java.util.Objects;

public final class AutoDepositTrustedDestination {
    private final String worldKey;
    private final Dimension dimension;
    private final BlockPos position;
    private final boolean enabled;

    public AutoDepositTrustedDestination(String worldKey,
                                         Dimension dimension,
                                         BlockPos position,
                                         boolean enabled) {
        this.worldKey = Objects.requireNonNull(worldKey, "worldKey");
        this.dimension = Objects.requireNonNull(dimension, "dimension");
        this.position = Objects.requireNonNull(position, "position").toImmutable();
        this.enabled = enabled;
    }

    public String worldKey() {
        return worldKey;
    }

    public Dimension dimension() {
        return dimension;
    }

    public BlockPos position() {
        return position;
    }

    public boolean enabled() {
        return enabled;
    }

    public String key() {
        return worldKey + ":" + dimension + ":" + position.toShortString();
    }
}
