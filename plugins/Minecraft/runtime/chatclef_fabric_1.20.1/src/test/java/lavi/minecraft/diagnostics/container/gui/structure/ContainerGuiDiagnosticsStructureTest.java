package lavi.minecraft.diagnostics.container.gui.structure;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

//20260904_kpopmodder: Lock exception-safe observation ordering and canonical reason literals.
class ContainerGuiDiagnosticsStructureTest {
    @Test
    void transportUsesCanonicalNoActiveGuiListenerReason() throws IOException {
        String source = source(
                "src/main/java/lavi/minecraft/diagnostics/container/gui/screen/"
                        + "ContainerScreenTransportDiagnostics.java"
        );

        assertTrue(source.contains("\"NO_ACTIVE_GUI_LISTENER\""));
        assertFalse(source.contains("\"NO_ACTIVE_EVENT_BUS_LISTENER\""));
    }

    @Test
    void slotBeginPublishesThreadLocalOnlyAfterEmitterReturns() throws IOException {
        String source = source(
                "src/main/java/lavi/minecraft/diagnostics/container/gui/slot/"
                        + "ContainerSlotFlowDiagnostics.java"
        );
        int beginStart = source.indexOf("public ContainerSlotActionProbe begin(");
        int beginEnd = source.indexOf("public void onLocalMutation(", beginStart);
        assertTrue(beginStart >= 0, "begin method must remain present");
        assertTrue(beginEnd > beginStart, "begin method boundary must remain identifiable");

        String begin = source.substring(beginStart, beginEnd);
        int emitterCall = begin.indexOf("active.emitter().detail(");
        int threadLocalPublish = begin.indexOf("activeSlotAction.set(");

        assertTrue(emitterCall >= 0, "slot request must be observed by the flow emitter");
        assertTrue(
                threadLocalPublish > emitterCall,
                "ThreadLocal action must not be published before a possibly-throwing emitter call returns"
        );
        assertFalse(
                begin.substring(0, emitterCall).contains("activeSlotAction.set("),
                "pre-emission ThreadLocal publication can leak when diagnostic emission throws"
        );
    }

    @Test
    void eventBusObservesCallbacksInPlaceAndCompletesAfterTheListenerLoop() throws IOException {
        String source = source("src/main/java/adris/altoclef/eventbus/EventBus.java");
        int publishStart = source.indexOf("public static <T> void publish(T event)");
        int publishEnd = source.indexOf("private static <T> void subscribeInternal(", publishStart);
        assertTrue(publishStart >= 0, "publish method must remain present");
        assertTrue(publishEnd > publishStart, "publish method boundary must remain identifiable");

        String publish = source.substring(publishStart, publishEnd);
        int listenerLoop = publish.indexOf("for (Subscription subRaw : subscribers)");
        int listenerStarted = publish.indexOf("screenDispatch.listenerStarted(", listenerLoop);
        int callback = publish.indexOf("sub.accept(event);", listenerStarted);
        int listenerCompleted = publish.indexOf("screenDispatch.listenerCompleted(", callback);
        int listenerLoopReleased = publish.indexOf("lock = false;", listenerCompleted);
        int dispatchCompleted = publish.indexOf("screenDispatch.dispatchCompleted();", listenerLoopReleased);

        assertTrue(listenerLoop >= 0, "listener loop must remain present");
        assertTrue(listenerStarted > listenerLoop, "listener start observation must be inside the loop");
        assertTrue(callback > listenerStarted, "existing callback must run after start observation");
        assertTrue(listenerCompleted > callback, "completion must follow a normally returned callback");
        assertTrue(listenerLoopReleased > listenerCompleted, "listener loop must end before dispatch completion");
        assertTrue(
                dispatchCompleted > listenerLoopReleased,
                "dispatch completion must remain after the whole listener loop"
        );
    }

    @Test
    void eventBusCapturesListenerSnapshotOnlyWhenScreenTransportObservationIsEnabled()
            throws IOException {
        String source = source("src/main/java/adris/altoclef/eventbus/EventBus.java");
        int publishStart = source.indexOf("public static <T> void publish(T event)");
        int publishEnd = source.indexOf("private static <T> void subscribeInternal(", publishStart);
        assertTrue(publishStart >= 0, "publish method must remain present");
        assertTrue(publishEnd > publishStart, "publish method boundary must remain identifiable");

        String publish = source.substring(publishStart, publishEnd);
        int enabledCheck = publish.indexOf(
                "&& ContainerGuiDiagnostics.screenTransportObservationEnabled();"
        );
        int snapshotDeclaration = publish.indexOf(
                "ContainerScreenListenerSnapshot screenListeners = observeScreenTransport"
        );
        int capture = publish.indexOf(
                "? ContainerScreenListenerSnapshot.capture(subscribers)",
                snapshotDeclaration
        );
        int disabledEmpty = publish.indexOf(
                ": ContainerScreenListenerSnapshot.empty();",
                capture
        );
        int dispatchStart = publish.indexOf(
                "ContainerScreenDispatchProbe screenDispatch = observeScreenTransport",
                disabledEmpty
        );

        assertTrue(enabledCheck >= 0, "screen transport must have an explicit OFF predicate");
        assertTrue(snapshotDeclaration > enabledCheck, "the predicate must be resolved before capture");
        assertTrue(capture > snapshotDeclaration, "enabled transport must capture one listener snapshot");
        assertTrue(disabledEmpty > capture, "disabled transport must use an empty snapshot");
        assertTrue(dispatchStart > disabledEmpty, "dispatch must reuse the conditionally captured snapshot");
        assertEquals(
                1,
                countOccurrences(publish, "ContainerScreenListenerSnapshot.capture(subscribers)")
        );
    }

    @Test
    void mixinConfigurationRegistersTheServerReconciliationObserverExactlyOnce()
            throws IOException {
        String configuration = source("src/main/resources/altoclef.mixins.json");

        assertEquals(
                1,
                countOccurrences(
                        configuration,
                        "\"diagnostics.ClientScreenHandlerUpdateDiagnosticMixin\""
                )
        );
    }

    @Test
    void serverReconciliationMixinUsesStrictPostApplyHooksAndVersionedStackGetter()
            throws IOException {
        String source = source(
                "src/main/java/adris/altoclef/mixins/diagnostics/"
                        + "ClientScreenHandlerUpdateDiagnosticMixin.java"
        );

        assertTrue(source.contains(
                "method = \"onScreenHandlerSlotUpdate"
                        + "(Lnet/minecraft/network/packet/s2c/play/"
                        + "ScreenHandlerSlotUpdateS2CPacket;)V\""
        ));
        assertTrue(source.contains(
                "target = \"Lnet/minecraft/screen/ScreenHandler;"
                        + "setStackInSlot(IILnet/minecraft/item/ItemStack;)V\""
        ));
        assertTrue(source.contains(
                "method = \"onInventory"
                        + "(Lnet/minecraft/network/packet/s2c/play/InventoryS2CPacket;)V\""
        ));
        assertTrue(source.contains(
                "target = \"Lnet/minecraft/screen/ScreenHandler;"
                        + "updateSlotStacks(ILjava/util/List;Lnet/minecraft/item/ItemStack;)V\""
        ));
        assertEquals(2, countOccurrences(source, "shift = At.Shift.AFTER"));
        assertEquals(2, countOccurrences(source, "require = 1"));
        assertEquals(2, countOccurrences(source, "allow = 1"));

        int versionGuard = source.indexOf("//#if MC > 12001");
        int laterVersionGetter = source.indexOf("return packet.getStack();", versionGuard);
        int alternateBranch = source.indexOf("//#else", laterVersionGetter);
        int oneTwentyOneGetter = source.indexOf(
                "//$$ return packet.getItemStack();",
                alternateBranch
        );
        int branchEnd = source.indexOf("//#endif", oneTwentyOneGetter);

        assertTrue(versionGuard >= 0, "packet getter must remain version guarded");
        assertTrue(laterVersionGetter > versionGuard, "later versions must use getStack");
        assertTrue(alternateBranch > laterVersionGetter, "1.20.1 branch must remain explicit");
        assertTrue(
                oneTwentyOneGetter > alternateBranch,
                "the 1.20.1 preprocessor branch must use getItemStack"
        );
        assertTrue(branchEnd > oneTwentyOneGetter, "versioned getter branch must remain bounded");
    }

    @Test
    void serverReconciliationMatchesOnlyTheSamePendingFlowHandlerSyncIdAndTickWindow()
            throws IOException {
        String source = source(
                "src/main/java/lavi/minecraft/diagnostics/container/gui/slot/"
                        + "ContainerSlotPostTickVerifier.java"
        );
        int matchStart = source.indexOf(
                "public synchronized ContainerSlotActionObservation latestMatchingAction("
        );
        int matchEnd = source.indexOf("public synchronized void clear()", matchStart);
        assertTrue(matchStart >= 0, "latest matching action lookup must remain present");
        assertTrue(matchEnd > matchStart, "latest matching action boundary must be identifiable");

        String matching = source.substring(matchStart, matchEnd);
        assertTrue(source.contains("private static final int MAX_OBSERVATION_TICKS = 20;"));
        assertTrue(matching.contains("verification.flow == activeFlow"));
        assertTrue(matching.contains("verification.handler == handler"));
        assertTrue(matching.contains("verification.action.syncId() == syncId"));
        assertTrue(matching.contains("elapsed >= 0L"));
        assertTrue(matching.contains("elapsed <= MAX_OBSERVATION_TICKS"));
    }

    private static int countOccurrences(String value, String needle) {
        int count = 0;
        int offset = 0;
        while ((offset = value.indexOf(needle, offset)) >= 0) {
            count++;
            offset += needle.length();
        }
        return count;
    }

    private static String source(String relativePath) throws IOException {
        return Files.readString(locate(relativePath))
                .replace("\r\n", "\n")
                .replace('\r', '\n');
    }

    private static Path locate(String relativePath) throws IOException {
        Path current = Path.of(System.getProperty("user.dir")).toAbsolutePath().normalize();
        while (current != null) {
            Path candidate = current.resolve(relativePath);
            if (Files.exists(candidate)) {
                return candidate;
            }
            current = current.getParent();
        }
        throw new IOException("Unable to locate source path: " + relativePath);
    }
}
