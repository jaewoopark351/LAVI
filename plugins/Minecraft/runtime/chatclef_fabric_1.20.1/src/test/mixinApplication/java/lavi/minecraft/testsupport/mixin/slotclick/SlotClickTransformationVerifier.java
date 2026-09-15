package lavi.minecraft.testsupport.mixin.slotclick;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

//20260916_kpopmodder: Verify the transformed call graph and unchanged native recursion, not merely changed class bytes.
final class SlotClickTransformationVerifier {
    private static final String BRIDGE = "lavi/minecraft/inventory/slotclick/SlotClickEventBridge";

    private SlotClickTransformationVerifier() { }

    static void verify(SlotClickTargetNames names, byte[] original, byte[] transformed) {
        ClassNode before = read(original);
        ClassNode after = read(transformed);
        require(!Arrays.equals(original, transformed), "Sponge returned unchanged target bytes");
        require(before.name.equals(names.owner()) && after.name.equals(names.owner()), "Wrong target namespace");
        MethodNode originalOuter = method(before, names.outer(), names.descriptor());
        MethodNode originalInner = method(before, names.inner(), names.descriptor());
        MethodNode outer = method(after, names.outer(), names.descriptor());
        MethodNode inner = method(after, names.inner(), names.descriptor());
        require(calls(originalOuter, names.owner(), names.inner(), names.descriptor()).size() == 1,
                "Selected vanilla outer method does not have one internal call");
        require(calls(originalInner, names.owner(), names.inner(), names.descriptor()).size() == 1,
                "Selected vanilla internal method does not have the expected QUICK_CRAFT recursive call");
        require((inner.access & Opcodes.ACC_PRIVATE) != 0, "Native private method visibility changed");
        require(instructions(originalInner).equals(instructions(inner)), "Native internal click implementation changed");
        require(calls(outer, names.owner(), names.inner(), names.descriptor()).isEmpty(),
                "Outer native call was not redirected; the legacy recursive hook is still selected");

        List<MethodInsnNode> redirected = new ArrayList<>();
        for (AbstractInsnNode instruction : outer.instructions) {
            if (instruction instanceof MethodInsnNode call && call.owner.equals(names.owner())
                    && call.desc.equals(names.redirectDescriptor())) redirected.add(call);
        }
        require(redirected.size() == 1, "Expected one actual outer redirect call site");
        MethodInsnNode redirect = redirected.get(0);
        MethodNode wrapper = method(after, redirect.name, redirect.desc);
        require(calls(wrapper, BRIDGE, "run", null).size() == 1, "Wrapper does not invoke the existing bridge once");
        int nativeSites = nativeDelegateSites(after, wrapper, names, new HashSet<>());
        require(nativeSites == 1, "Expected one native delegate call site reachable from the wrapper: " + nativeSites);
        require(calls(inner, names.owner(), names.inner(), names.descriptor()).size() == 1,
                "Native QUICK_CRAFT recursive call was replaced");
    }

    private static int nativeDelegateSites(ClassNode target, MethodNode current, SlotClickTargetNames names,
                                          Set<String> visited) {
        require(visited.add(current.name + current.desc), "Cycle or reused callback in wrapper delegate graph");
        int count = 0;
        for (AbstractInsnNode instruction : current.instructions) {
            if (instruction instanceof MethodInsnNode call && call.owner.equals(names.owner())) {
                require(!call.name.equals(names.outer()) || !call.desc.equals(names.descriptor()),
                        "Wrapper calls outer onSlotClick again and would recurse");
                if (call.name.equals(names.inner()) && call.desc.equals(names.descriptor())) count++;
                else count += nativeDelegateSites(target, method(target, call.name, call.desc), names, visited);
            } else if (instruction instanceof InvokeDynamicInsnNode dynamic) {
                for (Object argument : dynamic.bsmArgs) {
                    if (argument instanceof Handle handle && handle.getOwner().equals(names.owner())) {
                        require(!handle.getName().equals(names.outer()) || !handle.getDesc().equals(names.descriptor()),
                                "Callback delegates to outer onSlotClick");
                        if (handle.getName().equals(names.inner()) && handle.getDesc().equals(names.descriptor())) count++;
                        else count += nativeDelegateSites(target, method(target, handle.getName(), handle.getDesc()), names, visited);
                    }
                }
            }
        }
        return count;
    }

    private static List<MethodInsnNode> calls(MethodNode method, String owner, String name, String descriptor) {
        List<MethodInsnNode> result = new ArrayList<>();
        for (AbstractInsnNode instruction : method.instructions)
            if (instruction instanceof MethodInsnNode call && call.owner.equals(owner) && call.name.equals(name)
                    && (descriptor == null || call.desc.equals(descriptor))) result.add(call);
        return result;
    }

    private static MethodNode method(ClassNode target, String name, String descriptor) {
        return target.methods.stream().filter(method -> method.name.equals(name) && method.desc.equals(descriptor))
                .findFirst().orElseThrow(() -> new AssertionError("Missing target method: " + name + descriptor));
    }

    private static ClassNode read(byte[] bytes) {
        ClassNode node = new ClassNode();
        new ClassReader(bytes).accept(node, 0);
        return node;
    }

    /** Compare semantic instructions and branch destinations, ignoring only debug labels, lines and stack-map frames. */
    private static List<String> instructions(MethodNode method) {
        var positions = new IdentityHashMap<LabelNode, Integer>();
        int position = 0;
        for (AbstractInsnNode instruction : method.instructions) {
            if (instruction instanceof LabelNode label) positions.put(label, position);
            if (instruction.getOpcode() >= 0) position++;
        }
        List<String> result = new ArrayList<>();
        for (AbstractInsnNode instruction : method.instructions) {
            if (instruction.getOpcode() < 0) continue;
            String value = "";
            if (instruction instanceof MethodInsnNode call) value = call.owner + call.name + call.desc + call.itf;
            else if (instruction instanceof FieldInsnNode field) value = field.owner + field.name + field.desc;
            else if (instruction instanceof VarInsnNode variable) value = Integer.toString(variable.var);
            else if (instruction instanceof IntInsnNode integer) value = Integer.toString(integer.operand);
            else if (instruction instanceof TypeInsnNode type) value = type.desc;
            else if (instruction instanceof LdcInsnNode constant) value = String.valueOf(constant.cst);
            else if (instruction instanceof IincInsnNode increment) value = increment.var + ":" + increment.incr;
            else if (instruction instanceof JumpInsnNode jump) value = String.valueOf(positions.get(jump.label));
            else if (instruction instanceof InvokeDynamicInsnNode dynamic)
                value = dynamic.name + dynamic.desc + dynamic.bsm + Arrays.toString(dynamic.bsmArgs);
            else if (instruction instanceof TableSwitchInsnNode table)
                value = table.min + ":" + table.max + ":" + positions.get(table.dflt) + ":"
                        + table.labels.stream().map(positions::get).toList();
            else if (instruction instanceof LookupSwitchInsnNode lookup)
                value = lookup.keys + ":" + positions.get(lookup.dflt) + ":"
                        + lookup.labels.stream().map(positions::get).toList();
            else if (instruction instanceof MultiANewArrayInsnNode array) value = array.desc + ":" + array.dims;
            result.add(instruction.getOpcode() + ":" + value);
        }
        for (TryCatchBlockNode handler : method.tryCatchBlocks)
            result.add("catch:" + positions.get(handler.start) + ":" + positions.get(handler.end) + ":"
                    + positions.get(handler.handler) + ":" + handler.type);
        return result;
    }

    private static void require(boolean condition, String failure) {
        if (!condition) throw new AssertionError(failure);
    }
}
