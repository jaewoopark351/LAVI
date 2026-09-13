package lavi.minecraft.testsupport.mixin;

import org.spongepowered.asm.service.IGlobalPropertyService;
import org.spongepowered.asm.service.IPropertyKey;
import java.util.HashMap;
import java.util.Map;

/** Mixin bootstrap blackboard, isolated to this short-lived verification JVM. */
public final class IsolatedGlobalProperties implements IGlobalPropertyService {
    private record Key(String name) implements IPropertyKey { }
    private final Map<IPropertyKey, Object> values = new HashMap<>();
    public IPropertyKey resolveKey(String name) { return new Key(name); }
    @SuppressWarnings("unchecked") public <T> T getProperty(IPropertyKey key) { return (T) values.get(key); }
    public void setProperty(IPropertyKey key, Object value) { values.put(key, value); }
    public <T> T getProperty(IPropertyKey key, T fallback) { T value = getProperty(key); return value == null ? fallback : value; }
    public String getPropertyString(IPropertyKey key, String fallback) {
        Object value = values.get(key); return value == null ? fallback : value.toString();
    }
}
