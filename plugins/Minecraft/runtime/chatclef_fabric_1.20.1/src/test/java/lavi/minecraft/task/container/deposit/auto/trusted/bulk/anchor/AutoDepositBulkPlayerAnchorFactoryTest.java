package lavi.minecraft.task.container.deposit.auto.trusted.bulk.anchor;

import adris.altoclef.util.Dimension;
import lavi.minecraft.task.container.deposit.auto.trusted.bulk.scan.AutoDepositBulkWorldProvenance;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

//20260904_kpopmodder: Added deterministic player-anchor tests without mutating the MinecraftClient singleton.
final class AutoDepositBulkPlayerAnchorFactoryTest {
    private final AutoDepositBulkPlayerAnchorFactory factory =
            new AutoDepositBulkPlayerAnchorFactory();

    @Test
    void capturesMinecraftFlooredFeetBlockWithoutApplyingAGroundOffset() {
        Object world = new Object();
        Object player = new Object();
        BlockPos feetBlock = new BlockPos(
                MathHelper.floor(-0.20),
                MathHelper.floor(64.90),
                MathHelper.floor(-8.01)
        );

        AutoDepositBulkAnchorReadResult result = factory.create(
                true,
                world,
                world,
                player,
                player,
                world,
                feetBlock
        );

        assertEquals(AutoDepositBulkAnchorReadStatus.AVAILABLE, result.status());
        assertEquals(new BlockPos(-1, 64, -9), result.anchor().orElseThrow().position());
        assertFalse(result.anchor().orElseThrow().position().equals(feetBlock.down()));
    }

    @Test
    void copiesAMutableFeetPositionIntoTheImmutableAnchorSnapshot() {
        Object world = new Object();
        Object player = new Object();
        BlockPos.Mutable mutableFeetBlock = new BlockPos.Mutable(-1, 64, -9);

        AutoDepositBulkPlayerAnchor anchor = factory.create(
                true,
                world,
                world,
                player,
                player,
                world,
                mutableFeetBlock
        ).anchor().orElseThrow();
        mutableFeetBlock.set(20, 80, 20);

        assertEquals(new BlockPos(-1, 64, -9), anchor.position());
    }

    @Test
    void rejectsWrongThreadMissingStateAndClientModIdentityMismatches() {
        Object world = new Object();
        Object player = new Object();
        BlockPos position = new BlockPos(2, 65, -4);

        assertUnavailable(factory.create(
                false, world, world, player, player, world, position
        ));
        assertUnavailable(factory.create(
                true, null, world, player, player, world, position
        ));
        assertUnavailable(factory.create(
                true, world, null, player, player, world, position
        ));
        assertUnavailable(factory.create(
                true, world, world, null, player, world, position
        ));
        assertUnavailable(factory.create(
                true, world, world, player, null, world, position
        ));
        assertUnavailable(factory.create(
                true, world, world, player, player, null, position
        ));
        assertUnavailable(factory.create(
                true, world, world, player, player, world, null
        ));
        assertUnavailable(factory.create(
                true, world, new Object(), player, player, world, position
        ));
        assertUnavailable(factory.create(
                true, world, world, player, new Object(), world, position
        ));
    }

    @Test
    void distinguishesPlayerWorldMembershipMismatchFromGeneralUnavailability() {
        Object world = new Object();
        Object player = new Object();

        AutoDepositBulkAnchorReadResult result = factory.create(
                true,
                world,
                world,
                player,
                player,
                new Object(),
                new BlockPos(0, 65, 0)
        );

        assertEquals(
                AutoDepositBulkAnchorReadStatus.PLAYER_WORLD_MEMBERSHIP_MISMATCH,
                result.status()
        );
        assertTrue(result.anchor().isEmpty());
    }

    @Test
    void comparesPlayerAndWorldBindingsByReferenceIdentity() {
        Object world = new Object();
        Object player = new Object();
        BlockPos position = new BlockPos(4, 70, 9);
        AutoDepositBulkPlayerAnchor anchor = factory.create(
                true,
                world,
                world,
                player,
                player,
                world,
                position
        ).anchor().orElseThrow();

        assertTrue(anchor.samePlayerIdentity(new AutoDepositBulkPlayerAnchor(
                position,
                player,
                world
        )));
        assertFalse(anchor.samePlayerIdentity(new AutoDepositBulkPlayerAnchor(
                position,
                new Object(),
                world
        )));
        assertTrue(anchor.belongsToWorld(provenance(world)));
        assertFalse(anchor.belongsToWorld(provenance(new Object())));
    }

    private static void assertUnavailable(AutoDepositBulkAnchorReadResult result) {
        assertEquals(AutoDepositBulkAnchorReadStatus.UNAVAILABLE, result.status());
        assertTrue(result.anchor().isEmpty());
    }

    private static AutoDepositBulkWorldProvenance provenance(Object worldIdentity) {
        return new AutoDepositBulkWorldProvenance(
                "singleplayer:anchor-factory-test",
                Dimension.OVERWORLD,
                "minecraft:overworld",
                worldIdentity
        );
    }
}
