package adris.altoclef.lavibridge.server;

//20260725_kpopmodder: Added this value object to keep bridge host and port out of lifecycle code.

public class LaviBridgeServerConfig {

    private final String host;
    private final int port;

    public LaviBridgeServerConfig(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public static LaviBridgeServerConfig localhostDefault() {
        return new LaviBridgeServerConfig("127.0.0.1", 4316);
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public String baseUrl() {
        return "http://" + host + ":" + port;
    }
}
