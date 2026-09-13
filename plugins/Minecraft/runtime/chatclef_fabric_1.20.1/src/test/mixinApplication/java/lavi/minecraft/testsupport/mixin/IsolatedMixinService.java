package lavi.minecraft.testsupport.mixin;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.launch.platform.container.ContainerHandleVirtual;
import org.spongepowered.asm.launch.platform.container.IContainerHandle;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.transformer.IMixinTransformer;
import org.spongepowered.asm.mixin.transformer.IMixinTransformerFactory;
import org.spongepowered.asm.service.*;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Collection;
import java.util.List;

/** Bytecode-only host: target classes are resources and are never initialized. */
public final class IsolatedMixinService extends MixinServiceAbstract
        implements IClassProvider, IClassBytecodeProvider, ITransformerProvider, IClassTracker {
    private final ClassLoader loader = IsolatedMixinService.class.getClassLoader();
    public String getName() { return "LAVI isolated bytecode smoke"; }
    public boolean isValid() { return true; }
    public MixinEnvironment.Phase getInitialPhase() { return MixinEnvironment.Phase.DEFAULT; }
    public IClassProvider getClassProvider() { return this; }
    public IClassBytecodeProvider getBytecodeProvider() { return this; }
    public ITransformerProvider getTransformerProvider() { return this; }
    public IClassTracker getClassTracker() { return this; }
    public IMixinAuditTrail getAuditTrail() { return null; }
    public IFeatureValidator getFeatureValidator() { return null; }
    public IAdviceProvider getAdviceProvider() { return null; }
    public Collection<String> getPlatformAgents() { return List.of(); }
    public IContainerHandle getPrimaryContainer() { return new ContainerHandleVirtual("lavi-bytecode-smoke"); }
    public InputStream getResourceAsStream(String name) { return loader.getResourceAsStream(name); }
    public URL[] getClassPath() { return new URL[0]; }
    public Class<?> findClass(String name) throws ClassNotFoundException { return findClass(name, false); }
    public Class<?> findClass(String name, boolean initialize) throws ClassNotFoundException {
        if (name.startsWith("net.minecraft.") || name.startsWith("baritone.")) {
            throw new ClassNotFoundException("Game class loading is forbidden in this bytecode-only host: " + name);
        }
        return Class.forName(name, initialize, loader);
    }
    public Class<?> findAgentClass(String name, boolean initialize) throws ClassNotFoundException {
        return findClass(name, initialize);
    }
    public ClassNode getClassNode(String name) throws ClassNotFoundException, IOException {
        return getClassNode(name, false, 0);
    }
    public ClassNode getClassNode(String name, boolean runTransformers) throws ClassNotFoundException, IOException {
        return getClassNode(name, runTransformers, 0);
    }
    public ClassNode getClassNode(String name, boolean runTransformers, int flags)
            throws ClassNotFoundException, IOException {
        try (InputStream input = getResourceAsStream(name.replace('.', '/') + ".class")) {
            if (input == null) throw new ClassNotFoundException(name);
            ClassNode node = new ClassNode();
            new ClassReader(input).accept(node, flags);
            return node;
        }
    }
    public Collection<ITransformer> getTransformers() { return List.of(); }
    public Collection<ITransformer> getDelegatedTransformers() { return List.of(); }
    public void addTransformerExclusion(String name) { }
    public void registerInvalidClass(String name) { }
    public boolean isClassLoaded(String name) { return false; }
    public String getClassRestrictions(String name) { return ""; }
    public IMixinTransformer createTransformer() {
        IMixinTransformerFactory factory = getInternal(IMixinTransformerFactory.class);
        if (factory == null) throw new IllegalStateException("Sponge did not offer its transformer factory");
        return factory.createTransformer();
    }
}
