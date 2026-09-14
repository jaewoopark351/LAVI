//#if MC == 12001
//$$ package lavi.minecraft.fabric.chatclef.bridge.transport.find;

//$$ import lavi.minecraft.fabric.chatclef.bridge.protocol.FabricChatClefBridgeJson;
//$$ import lavi.minecraft.fabric.chatclef.bridge.transport.connection.FabricChatClefWebSocketConnectionState;
//$$ import lavi.minecraft.fabric.chatclef.bridge.transport.session.FabricChatClefAcceptedSessionIdentity;
//$$ import lavi.minecraft.fabric.chatclef.bridge.transport.session.FabricChatClefSessionGuard;
//$$ import java.net.http.WebSocket;
//$$ import java.util.Map;
//$$ import java.util.concurrent.CompletableFuture;

//$$ //20260914_kpopmodder: Send catalog pages through the existing exact Fabric socket/session owner.
//$$ public final class FabricChatClefFindCatalogPageSender {
//$$     private final FabricChatClefWebSocketConnectionState connection;
//$$     private final FabricChatClefSessionGuard sessions;
//$$     private final FabricChatClefFindCatalogEnvelopeEncoder encoder;
//$$     public FabricChatClefFindCatalogPageSender(FabricChatClefWebSocketConnectionState connection,
//$$             FabricChatClefSessionGuard sessions, FabricChatClefBridgeJson json) {
//$$         this.connection = connection; this.sessions = sessions;
//$$         this.encoder = new FabricChatClefFindCatalogEnvelopeEncoder(json);
//$$     }
//$$     public FabricChatClefAcceptedSessionIdentity identity() {
//$$         var identity = sessions.acceptedIdentity().orElse(null);
//$$         return identity != null && connection.webSocket() != null
//$$                 && identity.javaSocketGeneration() == connection.activeConnectionGeneration() ? identity : null;
//$$     }
//$$     public CompletableFuture<WebSocket> send(FabricChatClefAcceptedSessionIdentity identity,
//$$             String event, Map<String, Object> payload) {
//$$         var socket = connection.webSocket();
//$$         if (socket == null || !sessions.matches(identity.sessionId(), identity.serverConnectionGeneration(),
//$$                 identity.javaSocketGeneration()) || !connection.isCurrentSocket(socket))
//$$             return CompletableFuture.failedFuture(new IllegalStateException("catalog_session_detached"));
//$$         try {
//$$             String encoded = encoder.encode(identity.sessionId(), identity.serverConnectionGeneration(), event, payload);
//$$             return socket.sendText(encoded, true);
//$$         } catch (java.io.IOException | RuntimeException error) {
//$$             return CompletableFuture.failedFuture(error);
//$$         }
//$$     }
//$$ }
//#endif
