//#if MC == 12001
package lavi.minecraft.find.command;

import adris.altoclef.commandsystem.CommandException;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

//20260914_kpopmodder: Validate domain-specific namespace and exact player syntax without widening other commands.
class FindRequestResolverTest {
    @Test void namespacedModIdsRemainValidAndPlayersAreLiteral() {
        assertDoesNotThrow(() -> FindRequestResolver.validateSyntax("entity", "some_mod:creature/path", "report"));
        assertDoesNotThrow(() -> FindRequestResolver.validateSyntax("block", "minecraft:chest", "approach"));
        assertDoesNotThrow(() -> FindRequestResolver.validateSyntax("player", "Player_Name", "report"));
    }
    @Test void namespaceAndInjectionSyntaxMustNotReachTaskExecution() {
        for (String target : new String[]{"villager", "Minecraft:villager", "minecraft:villager\nstop", "minecraft:villager;stop", "minecraft:villager extra", "@stop"}) {
            assertThrows(CommandException.class, () -> FindRequestResolver.validateSyntax("entity", target, "report"));
        }
    }
    @Test void itemApproachIsExplicitlyUnsupported() {
        assertThrows(CommandException.class, () -> FindRequestResolver.validateSyntax("item", "minecraft:diamond", "approach"));
        assertThrows(CommandException.class, () -> FindRequestResolver.validateSyntax("structure", "stronghold", "report"));
    }
    @Test void exactPlayerNamesCannotBecomeRegistryEntitiesOrCaseCoercions() {
        for (String name : new String[]{"ab", "minecraft:player", "player name", "01234567890123456"}) {
            assertThrows(CommandException.class, () -> FindRequestResolver.validateSyntax("player", name, "report"));
        }
        assertDoesNotThrow(() -> FindRequestResolver.validateSyntax("player", "ABC", "report"));
    }
}
//#endif
