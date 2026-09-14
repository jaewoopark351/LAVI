package lavi.minecraft.test.bootstrap;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.lang.instrument.ClassFileTransformer;
import java.io.IOException;
import java.io.InputStream;
import java.security.ProtectionDomain;
import java.util.Map;
import java.util.Set;

//20260914_kpopmodder: Match exact members used by SimpleRegistry after named mappings split their packages.
final class NamedRegistryAccessTransformer implements ClassFileTransformer {
    private static final Map<String, Set<String>> MEMBERS = Map.of(
            "net/minecraft/registry/entry/RegistryEntry$Reference", Set.of(
                    "setRegistryKey(Lnet/minecraft/registry/RegistryKey;)V",
                    "setValue(Ljava/lang/Object;)V",
                    "setTags(Ljava/util/Collection;)V"),
            "net/minecraft/registry/entry/RegistryEntryList$Named", Set.of(
                    "<init>(Lnet/minecraft/registry/entry/RegistryEntryOwner;Lnet/minecraft/registry/tag/TagKey;)V",
                    "copyOf(Ljava/util/List;)V"),
            "net/minecraft/entity/LivingEntity", Set.of(
                    "getAttackPos()Lnet/minecraft/util/math/Vec3d;"));

    // Instrumentation may ignore transformer exceptions. Validate the exact classpath resources in
    // premain so an unknown mapping or missing target fails the JVM rather than silently bypassing it.
    void validateTargets() {
        for (String target : MEMBERS.keySet()) {
            try (InputStream input = ClassLoader.getSystemResourceAsStream(target + ".class")) {
                if (input == null) throw new IllegalStateException("Missing named registry class: " + target);
                transform(null, target, null, null, input.readAllBytes());
            } catch (IOException error) {
                throw new IllegalStateException("Cannot validate named registry class: " + target, error);
            }
        }
    }

    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> redefining,
                            ProtectionDomain domain, byte[] original) {
        Set<String> expected = MEMBERS.get(className);
        if (expected == null) return null;
        ClassReader reader = new ClassReader(original);
        ClassWriter writer = new ClassWriter(reader, 0);
        int[] matched = { 0 };
        reader.accept(new ClassVisitor(Opcodes.ASM9, writer) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String descriptor,
                                             String signature, String[] exceptions) {
                if (expected.contains(name + descriptor)) {
                    if ((access & Opcodes.ACC_PRIVATE) != 0) {
                        throw new IllegalStateException("Unexpected private registry member: " + className + "." + name);
                    }
                    matched[0]++;
                    access = (access & ~(Opcodes.ACC_PUBLIC | Opcodes.ACC_PROTECTED)) | Opcodes.ACC_PUBLIC;
                }
                return super.visitMethod(access, name, descriptor, signature, exceptions);
            }
        }, 0);
        if (matched[0] != expected.size()) {
            throw new IllegalStateException("Named registry mapping mismatch: " + className + " matched=" + matched[0]);
        }
        System.out.println("AUTO_DEPOSIT_TEST_AGENT: " + className + " public members=" + matched[0]);
        return writer.toByteArray();
    }
}
