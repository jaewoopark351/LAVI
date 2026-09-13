package lavi.minecraft.diagnostics.baritone.builder;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;
import java.io.InputStream;
import java.util.List;
import java.util.stream.StreamSupport;
import static org.junit.jupiter.api.Assertions.*;

/** Checks the bundled target shapes, not runtime Mixin application or file emission. */
class BuilderMixinTargetBytecodeTest {
    @Test void movementAccessHasOneActualListReceiverAndExecutorInSlotOne() throws Exception {
        MethodNode method = method("baritone/process/BuilderProcess", "updateMovement", "()V");
        long gets = instructions(method).filter(n -> n instanceof MethodInsnNode call
                && call.owner.equals("java/util/List") && call.name.equals("get")
                && call.desc.equals("(I)Ljava/lang/Object;")).count();
        assertEquals(1, gets);
        assertTrue(method.localVariables.stream().anyMatch(local -> local.index == 1
                && local.desc.equals("Lbaritone/pathing/path/PathExecutor;")));
        assertEquals(1, instructions(method).filter(n -> n instanceof MethodInsnNode call
                && call.owner.equals("baritone/pathing/path/PathExecutor") && call.name.equals("getPath")).count());
    }
    @Test void schedulingWritesFinderBeforeExecutorCanRunWorker() throws Exception {
        MethodNode method = methods("baritone/behavior/PathingBehavior").stream()
                .filter(m -> m.name.equals("findPathInNewThread")).findFirst().orElseThrow();
        int assigned = -1, scheduled = -1;
        for (int i = 0; i < method.instructions.size(); i++) {
            AbstractInsnNode node = method.instructions.get(i);
            if (node instanceof FieldInsnNode field && field.name.equals("inProgress") && node.getOpcode() == Opcodes.PUTFIELD) assigned = i;
            if (node instanceof MethodInsnNode call && call.owner.equals("java/util/concurrent/Executor") && call.name.equals("execute")) scheduled = i;
        }
        assertTrue(assigned >= 0 && scheduled > assigned);
    }
    @Test void cutoffAndSpliceTransferIndexAfterExecutorConstruction() throws Exception {
        for (String name : List.of("cutIfTooLong", "lambda$trySplice$0")) {
            MethodNode method = methods("baritone/pathing/path/PathExecutor").stream()
                    .filter(m -> m.name.equals(name)).findFirst().orElseThrow();
            int constructed = -1, transferred = -1;
            for (int i = 0; i < method.instructions.size(); i++) {
                AbstractInsnNode node = method.instructions.get(i);
                if (node instanceof MethodInsnNode call && call.owner.equals("baritone/pathing/path/PathExecutor") && call.name.equals("<init>")) constructed = i;
                if (node instanceof FieldInsnNode field && field.name.equals("pathPosition") && node.getOpcode() == Opcodes.PUTFIELD) transferred = i;
            }
            assertTrue(constructed >= 0 && transferred > constructed, name);
        }
    }
    private static java.util.stream.Stream<AbstractInsnNode> instructions(MethodNode method) {
        return StreamSupport.stream(method.instructions.spliterator(), false);
    }
    private static MethodNode method(String owner, String name, String desc) throws Exception {
        return methods(owner).stream().filter(m -> m.name.equals(name) && m.desc.equals(desc)).findFirst().orElseThrow();
    }
    private static List<MethodNode> methods(String owner) throws Exception {
        try (InputStream input = BuilderMixinTargetBytecodeTest.class.getClassLoader().getResourceAsStream(owner + ".class")) {
            assertNotNull(input, owner);
            ClassNode node = new ClassNode();
            new ClassReader(input).accept(node, 0);
            return node.methods;
        }
    }
}
