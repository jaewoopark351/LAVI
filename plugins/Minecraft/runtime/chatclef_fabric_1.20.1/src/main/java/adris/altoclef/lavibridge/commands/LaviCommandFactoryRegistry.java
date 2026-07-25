package adris.altoclef.lavibridge.commands;

//20260725_kpopmodder: Added this registry to keep action factory selection out of request facade code.

import java.util.LinkedHashMap;
import java.util.Map;

public class LaviCommandFactoryRegistry {

    private final Map<String, LaviTypedCommandFactory> factories;

    public LaviCommandFactoryRegistry(Map<String, LaviTypedCommandFactory> factories) {
        this.factories = factories;
    }

    public static LaviCommandFactoryRegistry defaults() {
        Map<String, LaviTypedCommandFactory> factories = new LinkedHashMap<>();
        register(factories, new LaviGetItemCommandFactory());
        register(factories, new LaviGotoCommandFactory());
        register(factories, new LaviEquipCommandFactory());
        return new LaviCommandFactoryRegistry(factories);
    }

    public LaviCommandSpec build(String actionType, Map<String, Object> request) {
        LaviTypedCommandFactory factory = factories.get(actionType);
        if (factory == null) {
            throw new IllegalArgumentException("Unsupported LAVI command action: " + actionType);
        }
        return factory.build(request);
    }

    private static void register(Map<String, LaviTypedCommandFactory> factories, LaviTypedCommandFactory factory) {
        factories.put(factory.getActionType(), factory);
    }
}
