package lavi.minecraft.testsupport.mixin.inputlease;

import java.util.ArrayList;
import java.util.List;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.*;

//20260914_kpopmodder: Prove observer application while preserving both native write bodies and exception paths.
public final class FindInputLeaseTransformationVerifier {
    private static final String INPUT = "Lbaritone/api/utils/input/Input;";
    private FindInputLeaseTransformationVerifier() { }
    public static void verify(FindInputLeaseMixinInputs inputs, byte[] transformed) {
        ClassNode before = read(inputs.target()), after = read(transformed);
        if (!after.interfaces.contains("lavi/minecraft/integration/input/lease/ForcedInputLeaseChannel"))
            throw new AssertionError("Actual transformation did not add explicit input lease channel");
        for (String name : List.of("claim", "release", "owns"))
            method(after, "lavi$" + name + "ForcedInput", "(" + INPUT + "Ljava/lang/Object;)Z");
        method(after, "lavi$writeForcedInput", "(" + INPUT + "Ljava/lang/Object;Z)Z");
        preserveNativeWrite(before, after, "setInputForceState", "(" + INPUT + "Z)V", "lavi$observeUnownedWrite");
        preserveNativeWrite(before, after, "clearAllKeys", "()V", "lavi$observeUnownedClear");
        for (MethodNode method : after.methods) {
            if (!method.name.contains("lavi$observeUnowned")) continue;
            for (var instruction : method.instructions) {
                if (instruction instanceof MethodInsnNode call && call.owner.endsWith("/CallbackInfo")
                        && (call.name.equals("cancel") || call.name.equals("isCancelled")))
                    throw new AssertionError("Observational handler acquired cancellation behavior");
            }
        }
    }
    private static void preserveNativeWrite(ClassNode before, ClassNode after, String name, String descriptor, String observer) {
        MethodNode original = method(before, name, descriptor), changed = method(after, name, descriptor);
        List<String> nativeBody = executable(original), injectedBody = executable(changed);
        if (injectedBody.size() <= nativeBody.size()
                || !injectedBody.subList(injectedBody.size() - nativeBody.size(), injectedBody.size()).equals(nativeBody))
            throw new AssertionError("Native write instruction sequence changed: " + name);
        if (!original.tryCatchBlocks.isEmpty() || !changed.tryCatchBlocks.isEmpty())
            throw new AssertionError("Unexpected native exception interception: " + name);
        int observerCalls = 0;
        for (var instruction : changed.instructions) {
            if (instruction instanceof JumpInsnNode || instruction instanceof TableSwitchInsnNode || instruction instanceof LookupSwitchInsnNode)
                throw new AssertionError("Observer inserted a native write branch: " + name);
            if (instruction instanceof MethodInsnNode call && call.name.contains(observer)) observerCalls++;
        }
        if (observerCalls != 1) throw new AssertionError("Expected exactly one actual observer call: " + name);
        System.out.println("FIND_INPUT_LEASE_NATIVE_BODY_PRESERVED method=" + name + " descriptor=" + descriptor
                + " nativeInstructions=" + nativeBody.size() + " observerCalls=" + observerCalls
                + " newBranches=0 exceptionInterception=0");
    }
    private static List<String> executable(MethodNode method) {
        var result = new ArrayList<String>();
        for (var instruction : method.instructions) {
            if (instruction.getOpcode() < 0) continue;
            String value = Integer.toString(instruction.getOpcode());
            if (instruction instanceof VarInsnNode variable) value += ":" + variable.var;
            else if (instruction instanceof FieldInsnNode field) value += ":" + field.owner + ":" + field.name + ":" + field.desc;
            else if (instruction instanceof MethodInsnNode call) value += ":" + call.owner + ":" + call.name + ":" + call.desc + ":" + call.itf;
            else if (instruction instanceof TypeInsnNode type) value += ":" + type.desc;
            else if (instruction instanceof IntInsnNode integer) value += ":" + integer.operand;
            else if (instruction instanceof LdcInsnNode constant) value += ":" + constant.cst;
            result.add(value);
        }
        return result;
    }
    private static MethodNode method(ClassNode node, String name, String descriptor) {
        return node.methods.stream().filter(method -> method.name.equals(name) && method.desc.equals(descriptor)).findFirst()
                .orElseThrow(() -> new AssertionError("Missing selected native/API method: " + name + descriptor));
    }
    private static ClassNode read(byte[] bytes) {
        var node = new ClassNode(); new ClassReader(bytes).accept(node, 0); return node;
    }
}
