package lavi.minecraft.diagnostics.toolselect;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ToolEquipCallerBytecodeTest {
    @Test
    void priorityKeepsItsOneOriginalEquipCallOutsideDiagnosticHandlersAndCallbacks() throws Exception {
        ClassNode owner = read("adris/altoclef/chains/PlayerInteractionFixChain");
        MethodNode priority = owner.methods.stream().filter(method -> method.name.equals("getPriority")).findFirst().orElseThrow();
        List<MethodInsnNode> calls = calls(priority);
        //20260913_kpopmodder: The requested behavior change delegates once to the exact-source owner.
        assertEquals(1L, calls.stream().filter(call -> call.owner.equals("lavi/minecraft/integration/toolselect/equip/execution/ExactToolEquipController")
                && call.name.equals("equip")).count());
        assertTrue(calls.stream().noneMatch(call -> call.owner.equals("adris/altoclef/control/SlotHandler") && call.name.equals("forceEquipItem")));
        assertTrue(priority.tryCatchBlocks.isEmpty(), "Do not catch the game action or re-run getPriority");
        assertTrue(calls.stream().noneMatch(call -> call.name.equals("getPriority")));
        assertTrue(calls.stream().noneMatch(call -> call.owner.equals("lavi/minecraft/diagnostics/toolselect/ToolEquipDiagnostics")));
        assertEquals(5L, calls.stream().filter(call -> call.owner.equals("lavi/minecraft/diagnostics/toolselect/call/ToolEquipDiagnosticCall")
                && call.name.equals("selection")).count());
        for (MethodNode callback : owner.methods.stream().filter(method -> method.name.startsWith("lambda$")).toList()) {
            assertTrue(calls(callback).stream().noneMatch(call -> call.name.equals("forceEquipItem")));
        }
    }

    @Test
    void exactSwapDoesNotSelectAHandOrScanMatchingItemsAndDefenseGetterDoesNotExecuteDefense() throws Exception {
        MethodNode swap = read("adris/altoclef/control/SlotHandler").methods.stream().filter(method -> method.name.equals("forceSwapPlayerSlot")).findFirst().orElseThrow();
        assertEquals(1L, calls(swap).stream().filter(call -> call.name.equals("clickSlotForce")).count());
        assertTrue(calls(swap).stream().noneMatch(call -> call.name.contains("getSlotsWithItem") || call.name.equals("forceEquipItem")));
        assertTrue(Arrays.stream(swap.instructions.toArray()).noneMatch(instruction -> instruction instanceof org.objectweb.asm.tree.FieldInsnNode field
                && field.name.equals("selectedSlot") && field.getOpcode() == org.objectweb.asm.Opcodes.PUTFIELD));
        MethodNode defense = read("adris/altoclef/chains/MobDefenseChain").methods.stream().filter(method -> method.name.equals("isToolInputClaimed")).findFirst().orElseThrow();
        assertTrue(calls(defense).stream().noneMatch(call -> call.name.equals("getPriority") || call.name.equals("getPriorityInner")
                || call.name.equals("doForceField") || call.name.equals("tickEnd")));
        ClassNode mod = read("adris/altoclef/AltoClef");
        MethodNode registration = mod.methods.stream().filter(method -> calls(method).stream().anyMatch(call -> call.owner.equals("adris/altoclef/chains/PlayerInteractionFixChain") && call.name.equals("<init>"))).findFirst().orElseThrow();
        List<MethodInsnNode> construction = calls(registration);
        int defenseIndex = -1, interactionIndex = -1;
        for (int i = 0; i < construction.size(); i++) {
            if (construction.get(i).owner.equals("adris/altoclef/chains/MobDefenseChain") && construction.get(i).name.equals("<init>")) defenseIndex = i;
            if (construction.get(i).owner.equals("adris/altoclef/chains/PlayerInteractionFixChain") && construction.get(i).name.equals("<init>")) interactionIndex = i;
        }
        assertTrue(defenseIndex >= 0 && defenseIndex < interactionIndex, "Native defense evaluation must precede the interaction selection owner");
    }

    @Test
    void forceEquipReturnBranchesAndSnapshotsUseTheDiagnosticFacade() throws Exception {
        ClassNode owner = read("adris/altoclef/control/SlotHandler");
        MethodNode equip = owner.methods.stream().filter(method -> method.name.equals("forceEquipItem") && method.desc.contains(";J")).findFirst().orElseThrow();
        List<MethodInsnNode> calls = calls(equip);
        assertTrue(equip.tryCatchBlocks.isEmpty(), "Game slot actions must retain their original exception semantics");
        assertEquals(3L, calls.stream().filter(call -> call.owner.equals("lavi/minecraft/diagnostics/toolselect/call/ToolEquipDiagnosticCall")
                && call.name.equals("result")).count());
        assertEquals(1L, calls.stream().filter(call -> call.owner.equals("lavi/minecraft/diagnostics/toolselect/call/ToolEquipDiagnosticCall")
                && call.name.equals("capture")).count());
        assertTrue(calls.stream().noneMatch(call -> call.owner.startsWith("lavi/minecraft/diagnostics/toolselect/snapshot/ToolEquipSnapshotCapture")));
        assertTrue(calls.stream().noneMatch(call -> call.owner.equals("lavi/minecraft/diagnostics/toolselect/ToolEquipDiagnostics")));
    }

    private static List<MethodInsnNode> calls(MethodNode method) {
        return Arrays.stream(method.instructions.toArray()).filter(MethodInsnNode.class::isInstance)
                .map(MethodInsnNode.class::cast).toList();
    }

    private static ClassNode read(String owner) throws Exception {
        try (InputStream stream = ToolEquipCallerBytecodeTest.class.getClassLoader().getResourceAsStream(owner + ".class")) {
            assertNotNull(stream, owner);
            ClassNode node = new ClassNode();
            new ClassReader(stream).accept(node, 0);
            return node;
        }
    }
}
