package lavi.minecraft.diagnostics.toolselect.shaping;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

//20260831_kpopmodder: Bound mutable tool-selection channel state with deterministic LRU eviction.
final class ToolSelectionChannelRegistry {
    private final int maximumChannels;
    private final LinkedHashMap<String, ToolSelectionChannelState> channels =
            new LinkedHashMap<>(16, 0.75f, true);

    ToolSelectionChannelRegistry(int maximumChannels) {
        if (maximumChannels <= 0) {
            throw new IllegalArgumentException("maximumChannels must be positive");
        }
        this.maximumChannels = maximumChannels;
    }

    ToolSelectionChannelState get(String channel) {
        return channels.get(channel);
    }

    ToolSelectionChannelState create(String channel, String fingerprint, long tick) {
        if (channels.size() >= maximumChannels) {
            Iterator<Map.Entry<String, ToolSelectionChannelState>> iterator =
                    channels.entrySet().iterator();
            if (iterator.hasNext()) {
                iterator.next();
                iterator.remove();
            }
        }
        ToolSelectionChannelState state = new ToolSelectionChannelState(fingerprint, tick);
        channels.put(channel, state);
        return state;
    }

    int size() {
        return channels.size();
    }

    boolean contains(String channel) {
        return channels.containsKey(channel);
    }

    long totalSuppressedRepeatCount() {
        long total = 0L;
        for (ToolSelectionChannelState state : channels.values()) {
            total = ToolSelectionSuppressionCounter.add(
                    total,
                    state.suppressedRepeatCount()
            );
        }
        return total;
    }

    void clear() {
        channels.clear();
    }
}
