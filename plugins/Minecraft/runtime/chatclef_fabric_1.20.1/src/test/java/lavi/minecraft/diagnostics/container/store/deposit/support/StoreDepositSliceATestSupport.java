package lavi.minecraft.diagnostics.container.store.deposit.support;

import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.ItemTarget;
import adris.altoclef.util.slots.PlayerSlot;
import lavi.minecraft.diagnostics.container.store.deposit.binding.StoreDepositBindingRegistry;
import lavi.minecraft.diagnostics.container.store.deposit.binding.StoreDepositBindingRegistry.TrackerBinding;
import lavi.minecraft.diagnostics.container.store.deposit.budget.StoreDepositEmissionGate;
import lavi.minecraft.diagnostics.container.store.deposit.context.StoreDepositOperationContext;
import lavi.minecraft.diagnostics.container.store.deposit.event.StoreDepositEventFields;
import lavi.minecraft.diagnostics.container.store.deposit.terminal.StoreDepositAutomaticContext;
import lavi.minecraft.diagnostics.container.store.deposit.terminal.StoreDepositAutomaticLifecycleLedger;
import lavi.minecraft.diagnostics.container.store.deposit.terminal.StoreDepositAutomaticTerminalDiagnostics;
import lavi.minecraft.diagnostics.container.store.deposit.transfer.StoreDepositSlotActionDiagnostics;
import lavi.minecraft.diagnostics.container.store.deposit.transfer.StoreDepositTransferAttemptRegistry;
import lavi.minecraft.diagnostics.container.store.deposit.transfer.StoreDepositTransferSelectionSnapshot;
import net.minecraft.item.Item;
import net.minecraft.util.math.BlockPos;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

//20260907_kpopmodder: Centralize only the reusable fixtures shared by the responsibility-split Slice A tests.
public final class StoreDepositSliceATestSupport {
    private StoreDepositSliceATestSupport() {
    }

    public static SlotHarness automaticSlotHarness() {
        StoreDepositBindingRegistry bindings = new StoreDepositBindingRegistry();
        StoreDepositEmissionGate gate = new StoreDepositEmissionGate();
        StoreDepositAutomaticTerminalDiagnostics terminals = new StoreDepositAutomaticTerminalDiagnostics(
                new StoreDepositAutomaticLifecycleLedger(), gate
        );
        StoreDepositTransferAttemptRegistry attempts = new StoreDepositTransferAttemptRegistry(bindings, terminals);
        StoreDepositSlotActionDiagnostics diagnostics = new StoreDepositSlotActionDiagnostics(
                attempts, gate, terminals
        );
        Task parent = new TestTask("store-parent");
        Task transfer = new TestTask("transfer");
        bindings.registerRoot(parent, "AUTO_DEPOSIT_ALL_CHAIN", automaticContext());
        attempts.stage(parent, transfer, selection(target(10), 64));
        bindings.bindChild(parent, transfer);
        StoreDepositTransferAttemptRegistry.Reconciliation reconciliation = attempts.reconcile(
                parent, null, transfer, false, true, transfer
        );
        if (!reconciliation.available() || reconciliation.promoted() == null) {
            throw new AssertionError("automatic transfer candidate was not promoted");
        }
        return new SlotHarness(diagnostics, transfer, reconciliation.promoted());
    }

    public static StoreDepositTransferSelectionSnapshot selection(ItemTarget aggregate, int sourceCount) {
        return new StoreDepositTransferSelectionSnapshot(
                new BlockPos(1, 64, 2),
                aggregate,
                new PlayerSlot(9),
                "fixture:physical-stack-" + sourceCount,
                sourceCount,
                new PlayerSlot(10),
                "EMPTY_SLOT",
                0,
                64,
                false,
                2
        );
    }

    public static ItemTarget target(int count) {
        return new ItemTarget(new Item[]{null}, count);
    }

    public static StoreDepositAutomaticContext automaticContext() {
        return new StoreDepositAutomaticContext(
                true,
                1L,
                77L,
                "auto-deposit-1",
                "auto-deposit-1-maintenance-1",
                "auto-deposit-1-child-1",
                0,
                "auto-deposit-1-pressure-run-1"
        );
    }

    public static StoreDepositOperationContext context(String source) {
        return new StoreDepositOperationContext(
                "operation-" + source, source, new TestTask(source), 0L, 0L
        );
    }

    public static Object[] effectFields(TrackerBinding binding, boolean predicateResult) {
        return StoreDepositEventFields.effectObservationFields(
                null,
                binding,
                null,
                null,
                null,
                null,
                false,
                true,
                predicateResult,
                predicateResult ? "MATCH" : "POSITION_MISMATCH",
                true,
                new BlockPos(1, 64, 2),
                predicateResult ? "ACCEPT_POSITIVE_DELTA" : "REJECT_TARGET_PREDICATE"
        );
    }

    public static Map<String, Object> fields(Object[] values) {
        Map<String, Object> result = new LinkedHashMap<>();
        if (values == null) {
            return result;
        }
        for (int index = 0; index + 1 < values.length; index += 2) {
            result.put(String.valueOf(values[index]), values[index + 1]);
        }
        return result;
    }

    public static Object field(Object[] values, String name) {
        Map<String, Object> result = fields(values);
        if (!result.containsKey(name)) {
            throw new AssertionError("Missing field: " + name);
        }
        return result.get(name);
    }

    public static String source(String relativePath) throws IOException {
        Path current = Path.of(System.getProperty("user.dir")).toAbsolutePath().normalize();
        while (current != null) {
            Path candidate = current.resolve(relativePath);
            if (Files.isRegularFile(candidate)) {
                return Files.readString(candidate)
                        .replace("\r\n", "\n")
                        .replace('\r', '\n');
            }
            current = current.getParent();
        }
        throw new IOException("Unable to locate source file: " + relativePath);
    }

    public static int occurrences(String text, String token) {
        int count = 0;
        int index = 0;
        while ((index = text.indexOf(token, index)) >= 0) {
            count++;
            index += token.length();
        }
        return count;
    }

    public static synchronized String captureOutput(Runnable action) {
        PrintStream original = System.out;
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        System.setOut(new PrintStream(output, true, StandardCharsets.UTF_8));
        try {
            action.run();
        } finally {
            System.setOut(original);
        }
        return output.toString(StandardCharsets.UTF_8);
    }

    public record SlotHarness(StoreDepositSlotActionDiagnostics diagnostics,
                              Task transferTask,
                              StoreDepositTransferAttemptRegistry.ActiveTransfer activeTransfer) {
    }

    public static final class TestTask extends Task {
        private final String name;

        public TestTask(String name) {
            this.name = name;
        }

        @Override
        protected void onStart() {
        }

        @Override
        protected Task onTick() {
            return null;
        }

        @Override
        protected void onStop(Task interruptTask) {
        }

        @Override
        protected boolean isEqual(Task other) {
            return this == other;
        }

        @Override
        protected String toDebugString() {
            return name;
        }
    }
}
