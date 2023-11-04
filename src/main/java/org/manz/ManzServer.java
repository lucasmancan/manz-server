package org.manz;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.Function;

public class ManzServer {
    private int port = 3000;
    private int maxThreads = 200;
    private final Logger logger = LoggerFactory.getLogger(ManzServer.class);
    private final Executor manzWorkersPoll = Executors.newFixedThreadPool(maxThreads);

    public void setServerPort(int port) {
        this.port = port;
    }

    public void setServerMaxThreads(int maxThreads) {
        this.maxThreads = maxThreads;
    }

    public void registerRoute(HttpMethod method, String path, Function<Request, Response> handler) {

    }

    public void start() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            while (true) {
                Socket client = serverSocket.accept();
                manzWorkersPoll.execute(new ManzWorker(client));
            }
        } catch (IOException exception) {
            logger.error("Internal error running Manz server", exception);
        }
    }
}