//20260914_kpopmodder: Explicit Jupiter selectors and complete failure/abort accounting for gold behavior verification.
package lavi.minecraft.test;

import org.junit.jupiter.engine.JupiterTestEngine;
import org.junit.platform.engine.ConfigurationParameters;
import org.junit.platform.engine.DiscoveryFilter;
import org.junit.platform.engine.DiscoverySelector;
import org.junit.platform.engine.EngineDiscoveryRequest;
import org.junit.platform.engine.EngineExecutionListener;
import org.junit.platform.engine.ExecutionRequest;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.discovery.ClassSelector;
import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.engine.support.descriptor.ClassSource;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/** Isolated Jupiter-engine runner: explicit class selectors only, never classpath or package scans. */
public final class GoldFocusedTestRunner {
    private GoldFocusedTestRunner() { }

    public static void main(String[] args) {
        try {
            run(args);
        } catch (Throwable failure) {
            failure.printStackTrace(System.err);
            System.exit(1);
        }
    }

    private static void run(String[] args) {
        if (args.length == 0) throw new IllegalArgumentException("Supply explicit fully-qualified test class names");
        List<ClassSelector> selectors = new ArrayList<>();
        Set<String> names = new HashSet<>();
        for (String name : args) {
            if (name.isBlank() || !names.add(name)) {
                throw new IllegalArgumentException("Blank or duplicate test class: " + name);
            }
            selectors.add(DiscoverySelectors.selectClass(name));
        }
        ConfigurationParameters parameters = new ConfigurationParameters() {
            @Override public Optional<String> get(String key) {
                return "junit.jupiter.execution.parallel.enabled".equals(key) ? Optional.of("false") : Optional.empty();
            }
            @Override public Optional<Boolean> getBoolean(String key) {
                return get(key).map(Boolean::parseBoolean);
            }
            @Override public int size() { return 1; }
            @Override public Set<String> keySet() { return Set.of("junit.jupiter.execution.parallel.enabled"); }
        };
        EngineDiscoveryRequest request = new EngineDiscoveryRequest() {
            @Override public <T extends DiscoverySelector> List<T> getSelectorsByType(Class<T> type) {
                return selectors.stream().filter(type::isInstance).map(type::cast).toList();
            }
            @Override public <T extends DiscoveryFilter<?>> List<T> getFiltersByType(Class<T> type) {
                return List.of();
            }
            @Override public ConfigurationParameters getConfigurationParameters() { return parameters; }
        };
        JupiterTestEngine engine = new JupiterTestEngine();
        TestDescriptor root = engine.discover(request, UniqueId.forEngine(engine.getId()));
        for (String name : names) {
            boolean discovered = root.getDescendants().stream().anyMatch(descriptor ->
                    descriptor.getSource().filter(source -> source instanceof ClassSource classSource
                            && name.equals(classSource.getClassName())).isPresent()
                            && TestDescriptor.containsTests(descriptor));
            if (!discovered) throw new IllegalStateException("No Jupiter tests discovered for explicitly selected class: " + name);
        }
        AtomicInteger started = new AtomicInteger();
        AtomicInteger succeeded = new AtomicInteger();
        AtomicInteger failed = new AtomicInteger();
        AtomicInteger aborted = new AtomicInteger();
        AtomicInteger skipped = new AtomicInteger();
        AtomicInteger containerFailures = new AtomicInteger();
        EngineExecutionListener listener = new EngineExecutionListener() {
            @Override public void executionStarted(TestDescriptor descriptor) {
                if (descriptor.isTest()) {
                    started.incrementAndGet();
                    System.out.println("STARTED " + descriptor.getUniqueId());
                }
            }
            @Override public void executionSkipped(TestDescriptor descriptor, String reason) {
                skipped.incrementAndGet();
                System.out.println("SKIPPED " + descriptor.getUniqueId() + " reason=" + reason);
            }
            @Override public void executionFinished(TestDescriptor descriptor, TestExecutionResult result) {
                if (descriptor.isTest()) {
                    switch (result.getStatus()) {
                        case SUCCESSFUL -> succeeded.incrementAndGet();
                        case FAILED -> failed.incrementAndGet();
                        case ABORTED -> aborted.incrementAndGet();
                    }
                    System.out.println(result.getStatus() + " " + descriptor.getUniqueId());
                } else if (result.getStatus() != TestExecutionResult.Status.SUCCESSFUL) {
                    containerFailures.incrementAndGet();
                    System.err.println("CONTAINER_" + result.getStatus() + " " + descriptor.getUniqueId());
                }
                result.getThrowable().ifPresent(failure -> failure.printStackTrace(System.err));
            }
        };
        engine.execute(ExecutionRequest.create(root, listener, parameters));
        System.out.println("JUPITER_FOCUSED_RESULT started=" + started.get() + " successful=" + succeeded.get()
                + " failed=" + failed.get() + " aborted=" + aborted.get() + " skipped=" + skipped.get()
                + " containerFailures=" + containerFailures.get());
        if (started.get() == 0 || succeeded.get() != started.get() || failed.get() != 0
                || aborted.get() != 0 || skipped.get() != 0 || containerFailures.get() != 0) {
            throw new AssertionError("Focused Jupiter verification did not fully pass");
        }
    }
}
