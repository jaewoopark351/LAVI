package adris.altoclef.lavibridge.server;

//20260725_kpopmodder: Added this factory to isolate HttpServer socket binding.

import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;

public class LaviHttpServerFactory {

    public HttpServer create(LaviBridgeServerConfig config) throws IOException {
        return HttpServer.create(
                new InetSocketAddress(
                        InetAddress.getByName(config.getHost()),
                        config.getPort()
                ),
                0
        );
    }
}
