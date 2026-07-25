package adris.altoclef.lavibridge.commands;

//20260725_kpopmodder: Added this class to keep HTTP request validation and AltoClef command text creation isolated.

import java.util.Map;

public class LaviCommandRequestFactory {

    private final LaviCommandFactoryRegistry registry;

    public LaviCommandRequestFactory() {
        this(LaviCommandFactoryRegistry.defaults());
    }

    public LaviCommandRequestFactory(LaviCommandFactoryRegistry registry) {
        this.registry = registry;
    }

    public LaviCommandSpec getItem(Map<String, Object> request) {
        return registry.build("get-item", request);
    }

    public LaviCommandSpec gotoTarget(Map<String, Object> request) {
        return registry.build("goto", request);
    }

    public LaviCommandSpec equip(Map<String, Object> request) {
        return registry.build("equip", request);
    }
}
