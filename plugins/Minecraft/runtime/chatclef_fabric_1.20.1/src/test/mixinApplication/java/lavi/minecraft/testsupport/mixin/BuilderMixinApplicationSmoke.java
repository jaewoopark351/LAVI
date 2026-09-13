package lavi.minecraft.testsupport.mixin;

import com.llamalad7.mixinextras.MixinExtrasBootstrap;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.launch.MixinBootstrap;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.Mixins;
import org.spongepowered.asm.mixin.transformer.IMixinTransformer;
import org.spongepowered.asm.service.MixinService;
import java.io.InputStream;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.nio.charset.StandardCharsets;

/** Applies real Sponge/MixinExtras transformations, without launching Fabric or Minecraft. */
public final class BuilderMixinApplicationSmoke {
    private static final List<String> TARGETS = List.of(
            "baritone.utils.BaritoneProcessHelper", "baritone.process.BuilderProcess", "baritone.pathing.calc.Path",
            "baritone.pathing.path.CutoffPath", "baritone.pathing.path.SplicedPath",
            "baritone.pathing.path.PathExecutor", "baritone.behavior.PathingBehavior",
            "baritone.pathing.calc.AbstractNodeCostSearch");
    private static final List<String> MIXINS = List.of(
            "BaritoneProcessHelperDiagnosticAccessor", "BuilderProcessDiagnosticMixin", "PathDiagnosticMixin",
            "CutoffPathDiagnosticMixin", "SplicedPathDiagnosticMixin", "PathExecutorDiagnosticMixin", "PathingBehaviorDiagnosticMixin",
            "AbstractNodeCostSearchDiagnosticMixin");
    public static void main(String[] args) throws Throwable {
        MixinBootstrap.init();
        MixinEnvironment.getDefaultEnvironment().setSide(MixinEnvironment.Side.CLIENT);
        if (!(MixinService.getService() instanceof IsolatedMixinService service)) {
            throw new AssertionError("Unexpected game/loader Mixin service");
        }
        IMixinTransformer transformer = service.createTransformer();
        MixinExtrasBootstrap.init();
        boolean previous = List.of(args).contains("--previous-artifact");
        boolean intermediary = List.of(args).contains("--intermediary-artifact");
        try (InputStream resource = service.getResourceAsStream("altoclef.mixins.json")) {
            if (resource == null) throw new AssertionError("Actual application Mixin configuration missing");
            String applicationConfig = new String(resource.readAllBytes(), StandardCharsets.UTF_8);
            for (String mixin : MIXINS) {
                if (previous && !mixin.equals("BuilderProcessDiagnosticMixin")) continue;
                if (!applicationConfig.contains("\"diagnostics." + mixin + "\"")) {
                    throw new AssertionError("Actual application does not register " + mixin);
                }
            }
        }
        Mixins.addConfiguration(previous ? "lavi-builder-previous.mixins.json" : "lavi-builder-smoke.mixins.json");
        int applied = 0;
        for (String target : TARGETS) {
            if (previous && !target.equals("baritone.process.BuilderProcess")) continue;
            byte[] before;
            try (InputStream input = service.getResourceAsStream(target.replace('.', '/') + ".class")) {
                if (input == null) throw new AssertionError("Missing real 1.20.1 target: " + target);
                before = input.readAllBytes();
            }
            byte[] after;
            try {
                after = transformer.transformClassBytes(target, target, before);
            } catch (Throwable failure) {
                if (!previous || !isKnownShadowFailure(failure)) throw failure;
                failure.printStackTrace(System.out);
                System.out.println("MIXIN_PREVIOUS_ARTIFACT_CONTROL=EXPECTED_SHADOW_FAILURE; TARGET=baritone.process.BuilderProcess; LIVE_MINECRAFT=NOT_RUN");
                return;
            }
            if (previous) throw new AssertionError("Previous artifact unexpectedly passed the Shadow regression control");
            ClassNode node = new ClassNode();
            new ClassReader(after).accept(node, 0);
            long handlers = node.methods.stream().filter(method -> method.name.contains("lavi$")).count();
            if (MessageDigest.isEqual(before, after) || handlers == 0) {
                throw new AssertionError("No actual diagnostic transformation: " + target);
            }
            applied++;
            System.out.println("MIXIN_APPLIED target=" + target + " handlers=" + handlers
                    + " inputSha256=" + sha(before) + " outputSha256=" + sha(after));
        }
        System.out.println("MIXIN_APPLICATION_RESULT targets=" + applied + " failed=0 mode="
                + (intermediary ? "ISOLATED_PACKAGED_INTERMEDIARY_1201" : "ISOLATED_NAMED_1201_BYTECODE")
                + "; LIVE_MINECRAFT=NOT_RUN");
    }
    private static boolean isKnownShadowFailure(Throwable failure) {
        for (Throwable cause = failure; cause != null; cause = cause.getCause()) {
            if (cause.getClass().getName().endsWith(".InvalidMixinException") && cause.getMessage() != null
                    && cause.getMessage().contains("@Shadow field baritone was not located in the target class baritone.process.BuilderProcess")) {
                return true;
            }
        }
        return false;
    }
    private static String sha(byte[] bytes) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
    }
}
