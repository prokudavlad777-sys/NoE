package ua.noe.resourcepack;

import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;

/** Tiny built-in HTTP server that serves the generated pack. Runs on its own daemon thread. */
public final class PackHost {

    private final HttpServer server;
    private final ExecutorService executor;
    private volatile Path file;

    public PackHost(String bindAddress, int port, Path initialFile) throws IOException {
        this.file = initialFile;
        this.server = HttpServer.create(new InetSocketAddress(bindAddress, port), 0);
        this.executor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "NoE-PackHost");
            t.setDaemon(true);
            return t;
        });
        server.setExecutor(executor);
        server.createContext("/noe-pack.zip", exchange -> {
            try (exchange) {
                Path current = file;
                if (current == null || !Files.isRegularFile(current)) {
                    exchange.sendResponseHeaders(404, -1);
                    return;
                }
                byte[] data = Files.readAllBytes(current);
                exchange.getResponseHeaders().add("Content-Type", "application/zip");
                exchange.sendResponseHeaders(200, data.length);
                try (OutputStream out = exchange.getResponseBody()) {
                    out.write(data);
                }
            }
        });
        server.start();
    }

    public void setFile(Path file) {
        this.file = file;
    }

    public void stop() {
        server.stop(0);
        executor.shutdownNow();
    }
}
