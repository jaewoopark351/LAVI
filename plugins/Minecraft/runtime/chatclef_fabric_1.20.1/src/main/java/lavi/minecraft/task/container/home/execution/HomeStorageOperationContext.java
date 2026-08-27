package lavi.minecraft.task.container.home.execution;

import adris.altoclef.AltoClef;
import adris.altoclef.util.Dimension;
import adris.altoclef.util.helpers.WorldHelper;
import lavi.minecraft.task.container.deposit.auto.trusted.AutoDepositWorldKeyReader;

import java.util.Objects;
import java.util.Optional;

//20260827_kpopmodder: Bind one manual store-home operation to its original world and dimension.
public final class HomeStorageOperationContext {
    private final Object worldIdentity;
    private final String worldKey;
    private final Dimension dimension;

    private HomeStorageOperationContext(
            Object worldIdentity,
            String worldKey,
            Dimension dimension) {
        this.worldIdentity = Objects.requireNonNull(worldIdentity, "worldIdentity");
        this.worldKey = Objects.requireNonNull(worldKey, "worldKey");
        this.dimension = Objects.requireNonNull(dimension, "dimension");
    }

    public static Optional<HomeStorageOperationContext> capture(
            AltoClef mod,
            AutoDepositWorldKeyReader worldKeyReader) {
        if (mod == null || mod.getWorld() == null || mod.getPlayer() == null) {
            return Optional.empty();
        }
        return worldKeyReader.read().map(worldKey -> new HomeStorageOperationContext(
                mod.getWorld(), worldKey, WorldHelper.getCurrentDimension()
        ));
    }

    public boolean matches(AltoClef mod, AutoDepositWorldKeyReader worldKeyReader) {
        return mod != null
                && mod.getWorld() == worldIdentity
                && mod.getPlayer() != null
                && WorldHelper.getCurrentDimension() == dimension
                && worldKeyReader.read().filter(worldKey::equals).isPresent();
    }

    public String worldKey() {
        return worldKey;
    }

    public Dimension dimension() {
        return dimension;
    }
}
