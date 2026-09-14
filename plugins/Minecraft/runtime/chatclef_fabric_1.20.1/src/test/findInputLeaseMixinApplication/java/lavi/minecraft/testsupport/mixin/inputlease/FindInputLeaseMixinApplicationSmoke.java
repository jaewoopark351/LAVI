package lavi.minecraft.testsupport.mixin.inputlease;

import lavi.minecraft.testsupport.mixin.IsolatedMixinService;
import java.util.List;
import org.spongepowered.asm.launch.MixinBootstrap;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.Mixins;
import org.spongepowered.asm.service.MixinService;

//20260914_kpopmodder: Apply real Sponge to one actual Baritone handler in an isolated bytecode host.
public final class FindInputLeaseMixinApplicationSmoke {
    public static void main(String[] args) throws Exception {
        boolean intermediary = List.of(args).contains("--intermediary-artifact");
        if (args.length != (intermediary ? 1 : 0)) throw new IllegalArgumentException("Unexpected smoke arguments");
        MixinBootstrap.init();
        MixinEnvironment.getDefaultEnvironment().setSide(MixinEnvironment.Side.CLIENT);
        if (!(MixinService.getService() instanceof IsolatedMixinService service))
            throw new AssertionError("Unexpected game/loader Mixin service");
        var transformer = service.createTransformer();
        var inputs = FindInputLeaseMixinInputs.read(service);
        FindInputLeaseMixinConfiguration.prepare(inputs);
        Mixins.addConfiguration(FindInputLeaseMixinConfiguration.RESOURCE);
        byte[] after = transformer.transformClassBytes(FindInputLeaseMixinInputs.TARGET, FindInputLeaseMixinInputs.TARGET, inputs.target());
        FindInputLeaseTransformationVerifier.verify(inputs, after);
        System.out.println("FIND_INPUT_LEASE_MIXIN_APPLIED target=" + FindInputLeaseMixinInputs.TARGET
                + " inputSha256=" + FindInputLeaseMixinInputs.sha(inputs.target()) + " outputSha256=" + FindInputLeaseMixinInputs.sha(after)
                + " mixinSha256=" + FindInputLeaseMixinInputs.sha(inputs.mixin()) + " ledgerSha256=" + FindInputLeaseMixinInputs.sha(inputs.ledger())
                + " applicationConfigSha256=" + FindInputLeaseMixinInputs.sha(inputs.applicationConfig()));
        System.out.println("FIND_INPUT_LEASE_MIXIN_RESULT targets=1 failed=0 mode="
                + (intermediary ? "ISOLATED_PACKAGED_INTERMEDIARY_1201" : "ISOLATED_NAMED_1201_BYTECODE")
                + "; LIVE_MINECRAFT=NOT_RUN; LIVE_INPUT_OWNERSHIP=NOT_RUN");
    }
}
