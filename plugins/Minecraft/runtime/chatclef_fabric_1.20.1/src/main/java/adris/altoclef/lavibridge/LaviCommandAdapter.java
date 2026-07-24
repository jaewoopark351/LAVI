package adris.altoclef.lavibridge;

//20260725_kpopmodder: Added this adapter to translate LAVI actions into existing AltoClef commands.

import adris.altoclef.AltoClef;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;

public class LaviCommandAdapter {

    private static final Pattern ITEM_NAME_PATTERN = Pattern.compile("[a-z0-9_:.\\-]+");
    private static final int MAX_ITEM_COUNT = 4096;

    private final AltoClef mod;
    private final MinecraftThreadDispatcher dispatcher;
    private final LaviActionRegistry actionRegistry;
    private final LaviStopController stopController;

    public LaviCommandAdapter(
            AltoClef mod,
            MinecraftThreadDispatcher dispatcher,
            LaviActionRegistry actionRegistry,
            LaviStopController stopController
    ) {
        this.mod = mod;
        this.dispatcher = dispatcher;
        this.actionRegistry = actionRegistry;
        this.stopController = stopController;
    }

    public Map<String, Object> getItem(Map<String, Object> request) throws Exception {
        String item = normalizeItemName(readRequiredString(request, "item"));
        int count = readCount(request);
        String command = "get " + item + " " + count;

        return dispatcher.call(() -> {
            ensureInGame();
            ensureNoRunningAction();
            ensureNoManualTask();

            Map<String, Object> action = actionRegistry.createAction("get-item", command, request);
            String actionId = (String) action.get("action_id");
            actionRegistry.markRunning(actionId);

            AtomicBoolean failed = new AtomicBoolean(false);
            String commandLine = mod.getCommandExecutor().getCommandPrefix() + command;

            mod.getCommandExecutor().execute(commandLine, () -> {
                if (!failed.get()) {
                    actionRegistry.markSucceeded(actionId, "AltoClef command finished.");
                }
            }, exception -> {
                failed.set(true);
                actionRegistry.markFailed(actionId, exception.getMessage());
            });

            return accepted(actionRegistry.currentActionSnapshotOnly());
        });
    }

    public Map<String, Object> stop() throws Exception {
        return dispatcher.call(() -> {
            ensureInGame();
            Map<String, Object> request = new LinkedHashMap<>();
            Map<String, Object> cancelledAction = actionRegistry.cancelCurrentIfRunning("Cancelled by stop request.");
            if (cancelledAction != null) {
                request.put("cancelled_action", cancelledAction);
            }
            Map<String, Object> action = actionRegistry.createAction("stop", "stop", request);
            String actionId = (String) action.get("action_id");
            actionRegistry.markRunning(actionId);
            stopController.stopAutomation();
            actionRegistry.markSucceeded(actionId, "Stop requested.");
            return accepted(actionRegistry.currentActionSnapshotOnly());
        });
    }

    private Map<String, Object> accepted(Map<String, Object> action) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("ok", true);
        result.put("accepted", true);
        result.put("action", action);
        return result;
    }

    private void ensureInGame() {
        if (!AltoClef.inGame()) {
            throw new IllegalStateException("Minecraft player is not in game.");
        }
    }

    private void ensureNoRunningAction() {
        if (actionRegistry.hasRunningAction()) {
            throw new IllegalStateException("A LAVI action is already running.");
        }
    }

    private void ensureNoManualTask() {
        if (mod.getUserTaskChain().isActive() && !mod.getUserTaskChain().isRunningIdleTask()) {
            throw new IllegalStateException("An AltoClef user task is already active.");
        }
    }

    private static String readRequiredString(Map<String, Object> request, String key) {
        Object value = request.get(key);
        if (!(value instanceof String text) || text.isBlank()) {
            throw new IllegalArgumentException("Missing required string field: " + key);
        }
        return text.trim();
    }

    private static int readCount(Map<String, Object> request) {
        Object value = request.getOrDefault("count", 1);
        int count;
        if (value instanceof Number number) {
            count = number.intValue();
        } else if (value instanceof String text) {
            count = Integer.parseInt(text);
        } else {
            throw new IllegalArgumentException("count must be a number.");
        }

        if (count < 1 || count > MAX_ITEM_COUNT) {
            throw new IllegalArgumentException("count must be between 1 and " + MAX_ITEM_COUNT + ".");
        }
        return count;
    }

    private static String normalizeItemName(String item) {
        String normalized = item.toLowerCase();
        if (normalized.startsWith("minecraft:")) {
            normalized = normalized.substring("minecraft:".length());
        }
        if (!ITEM_NAME_PATTERN.matcher(normalized).matches()) {
            throw new IllegalArgumentException("item contains unsupported characters.");
        }
        return normalized;
    }
}
