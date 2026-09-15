package lavi.minecraft.testsupport.mixin.slotclick;

import lavi.minecraft.testsupport.mixin.IsolatedMixinService;
import org.spongepowered.asm.launch.MixinBootstrap;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.Mixins;
import org.spongepowered.asm.service.MixinService;

import java.io.InputStream;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;

//20260916_kpopmodder: Transform the real selected ScreenHandler bytes without loading Minecraft or executing a click.
public final class SlotClickMixinApplicationSmoke {
    public static void main(String[] args) throws Exception {
        boolean intermediary = List.of(args).contains("--intermediary-artifact");
        if (args.length != (intermediary ? 1 : 0)) throw new IllegalArgumentException("Unexpected smoke arguments");
        MixinBootstrap.init();
        MixinEnvironment.getDefaultEnvironment().setSide(MixinEnvironment.Side.CLIENT);
        if (!(MixinService.getService() instanceof IsolatedMixinService service))
            throw new AssertionError("Unexpected game/loader Mixin service");
        var transformer = service.createTransformer();
        SlotClickTargetNames names = SlotClickTargetNames.forArtifact(intermediary);
        byte[] target = resource(service, names.owner() + ".class");
        byte[] mixin = resource(service, SlotClickMixinConfiguration.MIXIN + ".class");
        byte[] application = resource(service, "altoclef.mixins.json");
        SlotClickMixinConfiguration.prepare(service, application, names, intermediary);
        Mixins.addConfiguration(SlotClickMixinConfiguration.RESOURCE);
        byte[] transformed = transformer.transformClassBytes(names.targetName(), names.targetName(), target);
        SlotClickTransformationVerifier.verify(names, target, transformed);
        System.out.println("SLOT_CLICK_MIXIN_APPLIED target=" + names.targetName()
                + " inputSha256=" + sha(target) + " outputSha256=" + sha(transformed)
                + " mixinSha256=" + sha(mixin) + " applicationConfigSha256=" + sha(application));
        System.out.println("SLOT_CLICK_MIXIN_RESULT targets=1 failed=0 outerRedirectSites=1 nativeDelegateSites=1"
                + " nativeInternalBodyUnchanged=true quickCraftRecursiveSites=1 mode="
                + (intermediary ? "ISOLATED_PACKAGED_INTERMEDIARY_1201" : "ISOLATED_NAMED_1201_BYTECODE")
                + "; LIVE_MINECRAFT=NOT_RUN; LIVE_CALLBACK=NOT_RUN; RUNTIME_EXACT_ONCE=NOT_RUN");
    }

    static byte[] resource(IsolatedMixinService service, String path) throws Exception {
        try (InputStream input = service.getResourceAsStream(path)) {
            if (input == null) throw new AssertionError("Missing selected input resource: " + path);
            return input.readAllBytes();
        }
    }

    static String sha(byte[] bytes) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
    }
}
