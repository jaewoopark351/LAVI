package lavi.minecraft.test.bootstrap;

import java.lang.instrument.Instrumentation;

//20260914_kpopmodder: Supply only missing named-mapping registry visibility in the isolated test JVM.
public final class NamedRegistryAccessAgent {
    private NamedRegistryAccessAgent() { }

    public static void premain(String arguments, Instrumentation instrumentation) {
        NamedRegistryAccessTransformer transformer = new NamedRegistryAccessTransformer();
        transformer.validateTargets();
        instrumentation.addTransformer(transformer, false);
        System.out.println("AUTO_DEPOSIT_TEST_AGENT: exact named registry access enabled; no gameplay transforms");
    }
}
