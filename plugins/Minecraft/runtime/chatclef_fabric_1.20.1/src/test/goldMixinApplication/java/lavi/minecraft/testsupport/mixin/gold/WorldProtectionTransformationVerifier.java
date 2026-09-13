package lavi.minecraft.testsupport.mixin.gold;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import java.security.MessageDigest;
import java.util.List;

//20260914_kpopmodder: Verify the real target invokes its merged handler and that handler invokes the new update exactly once.
public final class WorldProtectionTransformationVerifier {
    private static final String BRIDGE_OWNER = "adris/altoclef/AltoClef";
    private static final String BRIDGE_METHOD = "onUserProtectionBlockChanged";
    private WorldProtectionTransformationVerifier() { }

    public static void verify(WorldProtectionNames names, GoldMixinInputs inputs, byte[] after) {
        if (after == null || MessageDigest.isEqual(inputs.world(), after))
            throw new AssertionError("World target bytecode was not transformed");
        ClassNode before = node(inputs.world());
        ClassNode transformed = node(after);
        if (!before.name.equals(names.world()) || !transformed.name.equals(names.world()))
            throw new AssertionError("Unexpected target namespace: " + transformed.name);
        if (bridgeCalls(before, names) != 0 || bridgeCalls(node(inputs.mixin()), names) != 1
                || bridgeCalls(transformed, names) != 1)
            throw new AssertionError("Expected exactly one new protection update call from the selected production Mixin");
        method(node(inputs.altoClef()), BRIDGE_METHOD, names.bridgeDescriptor());
        MethodNode original = method(before, names.changedMethod(), names.changedDescriptor());
        MethodNode injected = method(transformed, names.changedMethod(), names.changedDescriptor());
        List<MethodNode> handlers = transformed.methods.stream().filter(value -> bridgeCalls(value, names) == 1).toList();
        if (handlers.size() != 1 || !handlers.get(0).name.contains("onBlockWasChanged"))
            throw new AssertionError("New protection update is not in the actual WorldBlockModifiedMixin handler");
        MethodNode handler = handlers.get(0);
        if (calls(injected, transformed.name, handler.name, handler.desc) != 1
                || calls(original, transformed.name, handler.name, handler.desc) != 0)
            throw new AssertionError("onBlockChanged does not invoke the merged protection handler exactly once");
        System.out.println("GOLD_PROTECTION_HANDLER target=" + names.targetName() + " method=" + injected.name
                + injected.desc + " handler=" + handler.name + handler.desc + " handlerCalls=1 protectionUpdateCalls=1");
    }
    private static ClassNode node(byte[] bytes) {
        ClassNode node = new ClassNode();
        new ClassReader(bytes).accept(node, 0);
        return node;
    }
    private static MethodNode method(ClassNode node, String name, String descriptor) {
        return node.methods.stream().filter(value -> value.name.equals(name) && value.desc.equals(descriptor))
                .findFirst().orElseThrow(() -> new AssertionError("Required actual method missing: " + node.name + "." + name + descriptor));
    }
    private static long bridgeCalls(ClassNode node, WorldProtectionNames names) {
        return node.methods.stream().mapToLong(method -> bridgeCalls(method, names)).sum();
    }
    private static int bridgeCalls(MethodNode method, WorldProtectionNames names) {
        int count = 0;
        for (var instruction : method.instructions) if (instruction instanceof MethodInsnNode call
                && call.getOpcode() == Opcodes.INVOKEVIRTUAL && call.owner.equals(BRIDGE_OWNER)
                && call.name.equals(BRIDGE_METHOD) && call.desc.equals(names.bridgeDescriptor())) count++;
        return count;
    }
    private static int calls(MethodNode method, String owner, String name, String descriptor) {
        int count = 0;
        for (var instruction : method.instructions) if (instruction instanceof MethodInsnNode call
                && call.owner.equals(owner) && call.name.equals(name) && call.desc.equals(descriptor)) count++;
        return count;
    }
}
