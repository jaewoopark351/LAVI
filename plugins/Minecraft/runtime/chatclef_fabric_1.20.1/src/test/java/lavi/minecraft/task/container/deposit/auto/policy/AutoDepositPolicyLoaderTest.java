package lavi.minecraft.task.container.deposit.auto.policy;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class AutoDepositPolicyLoaderTest {
    @Test
    void loadsTheDedicatedAutomaticPolicyResource() {
        AutoDepositPolicyDefinition definition = new AutoDepositPolicyLoader().loadOrFailClosed();

        assertTrue(definition.loaded());
        assertTrue(definition.isValuable("minecraft:diamond"));
        assertTrue(definition.isKnownGeneral("minecraft:oak_log"));
    }
}
