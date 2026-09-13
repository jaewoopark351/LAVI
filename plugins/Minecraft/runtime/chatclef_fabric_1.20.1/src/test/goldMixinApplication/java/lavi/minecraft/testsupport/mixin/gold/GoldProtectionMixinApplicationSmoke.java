package lavi.minecraft.testsupport.mixin.gold;

import lavi.minecraft.testsupport.mixin.IsolatedMixinService;
import org.spongepowered.asm.launch.MixinBootstrap;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.Mixins;
import org.spongepowered.asm.service.MixinService;
import java.util.List;

//20260914_kpopmodder: Apply real Sponge to the actual named or packaged intermediary World class without starting Minecraft.
public final class GoldProtectionMixinApplicationSmoke {
    public static void main(String[] args) throws Exception {
        boolean intermediary = List.of(args).contains("--intermediary-artifact");
        if (args.length != (intermediary ? 1 : 0)) throw new IllegalArgumentException("Unexpected smoke arguments");
        MixinBootstrap.init();
        MixinEnvironment.getDefaultEnvironment().setSide(MixinEnvironment.Side.CLIENT);
        if (!(MixinService.getService() instanceof IsolatedMixinService service))
            throw new AssertionError("Unexpected game/loader Mixin service");
        var transformer = service.createTransformer();
        var names = intermediary ? WorldProtectionNames.intermediary() : WorldProtectionNames.named();
        var inputs = GoldMixinInputs.read(service, names);
        var configuration = GoldMixinConfiguration.prepare(inputs);
        GoldMixinRefmap.activate(service, configuration, names, intermediary);
        Mixins.addConfiguration(GoldMixinConfiguration.RESOURCE);
        byte[] transformed = transformer.transformClassBytes(names.targetName(), names.targetName(), inputs.world());
        WorldProtectionTransformationVerifier.verify(names, inputs, transformed);
        System.out.println("GOLD_PROTECTION_MIXIN_APPLIED target=" + names.targetName()
                + " inputSha256=" + GoldMixinInputs.sha(inputs.world()) + " outputSha256=" + GoldMixinInputs.sha(transformed)
                + " mixinSha256=" + GoldMixinInputs.sha(inputs.mixin()) + " altoClefSha256=" + GoldMixinInputs.sha(inputs.altoClef())
                + " applicationConfigSha256=" + GoldMixinInputs.sha(inputs.applicationConfig()));
        System.out.println("GOLD_PROTECTION_MIXIN_RESULT targets=1 failed=0 mode="
                + (intermediary ? "ISOLATED_PACKAGED_INTERMEDIARY_1201" : "ISOLATED_NAMED_1201_BYTECODE")
                + "; LIVE_MINECRAFT=NOT_RUN; LIVE_CALLBACK=NOT_RUN");
    }
}
