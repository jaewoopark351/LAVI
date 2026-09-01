package lavi.minecraft.task.control.command;

import adris.altoclef.AltoClef;
import adris.altoclef.commands.StopCommand;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

//20260831_kpopmodder: Verify the manual stop command owns one stop request and one completion callback.
class StopCommandTest {
    @Test
    void acceptedCommandStopsAutomationAndFinishesExactlyOnce() throws Exception {
        RecordingAltoClef mod = new RecordingAltoClef();
        AtomicInteger completions = new AtomicInteger();

        new StopCommand().run(mod, "stop", completions::incrementAndGet);

        assertEquals(1, mod.stopCalls);
        assertEquals(1, completions.get());
    }

    private static final class RecordingAltoClef extends AltoClef {
        private int stopCalls;

        @Override
        public void stop() {
            stopCalls++;
        }
    }
}
