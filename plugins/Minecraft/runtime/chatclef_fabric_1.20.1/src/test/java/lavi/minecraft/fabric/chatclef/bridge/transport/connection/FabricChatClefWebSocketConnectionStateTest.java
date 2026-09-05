package lavi.minecraft.fabric.chatclef.bridge.transport.connection;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.net.http.WebSocket;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class FabricChatClefWebSocketConnectionStateTest {
    @Test
    void ownsRunningConnectingAndExactSocketGenerationState() {
        FabricChatClefWebSocketConnectionState state = new FabricChatClefWebSocketConnectionState();
        WebSocket first = socket();
        WebSocket second = socket();

        assertTrue(state.beginRunning());
        assertFalse(state.beginRunning());
        assertTrue(state.beginConnecting());
        assertFalse(state.beginConnecting());

        assertEquals(1L, state.acceptOpen(first));
        assertSame(first, state.webSocket());
        assertTrue(state.isCurrentSocket(first));
        assertFalse(state.isCurrentSocket(second));
        assertEquals(1L, state.detachCurrent());
        assertEquals(0L, state.activeConnectionGeneration());

        assertTrue(state.beginConnecting());
        assertEquals(2L, state.acceptOpen(second));
        assertTrue(state.isCurrentSocket(second));
        assertTrue(state.endRunning());
        assertFalse(state.endRunning());
    }

    @Test
    void deduplicatesOnlyConsecutiveIdenticalConnectFailures() {
        FabricChatClefWebSocketConnectionState state = new FabricChatClefWebSocketConnectionState();

        assertTrue(state.shouldLogConnectFailure("failure-a"));
        assertFalse(state.shouldLogConnectFailure("failure-a"));
        assertTrue(state.shouldLogConnectFailure("failure-b"));
    }

    private WebSocket socket() {
        return (WebSocket) Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class<?>[]{WebSocket.class},
                (proxy, method, arguments) -> {
                    Class<?> returnType = method.getReturnType();
                    if (returnType == boolean.class) {
                        return false;
                    }
                    if (returnType == long.class) {
                        return 0L;
                    }
                    return null;
                }
        );
    }
}
