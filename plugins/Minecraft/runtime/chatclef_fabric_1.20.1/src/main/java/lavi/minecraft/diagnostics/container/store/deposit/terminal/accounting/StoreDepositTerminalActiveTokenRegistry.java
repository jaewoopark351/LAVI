package lavi.minecraft.diagnostics.container.store.deposit.terminal.accounting;

import java.util.IdentityHashMap;

//20260831_kpopmodder: Bound unsettled terminal-token lifetime without evicting authoritative evidence.
// Every access is serialized by the owning StoreDepositTerminalLedger monitor.
final class StoreDepositTerminalActiveTokenRegistry {
    static final int MAX_ACTIVE_TOKENS = 64;

    private final IdentityHashMap<StoreDepositTerminalAccountingToken, Metadata> activeTokens =
            new IdentityHashMap<>();

    boolean full() {
        return activeTokens.size() >= MAX_ACTIVE_TOKENS;
    }

    void register(StoreDepositTerminalAccountingToken token,
                  String operationId,
                  long observedTick) {
        if (full()) {
            throw new IllegalStateException("Terminal accounting token registry is full.");
        }
        activeTokens.put(token, new Metadata(operationId, observedTick));
    }

    boolean contains(StoreDepositTerminalAccountingToken token) {
        return activeTokens.containsKey(token);
    }

    Metadata metadata(StoreDepositTerminalAccountingToken token) {
        return activeTokens.get(token);
    }

    void remove(StoreDepositTerminalAccountingToken token) {
        activeTokens.remove(token);
    }

    int size() {
        return activeTokens.size();
    }

    record Metadata(String operationId, long observedTick) {
    }
}
