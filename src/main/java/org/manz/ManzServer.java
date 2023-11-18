package org.manz;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.manz.model.Request;
import org.manz.model.Response;
import org.manz.model.Route;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.Function;

public class ManzServer {
    private int port = 3000;
    private int maxThreads = 200;
    private ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .registerModule(new Jdk8Module());
    private final Logger logger = LoggerFactory.getLogger(ManzServer.class);
    private final Executor manzWorkersPoll = Executors.newFixedThreadPool(maxThreads);

    private final Map<Route, Function<Request, Response>> registeredRoutes = new HashMap<>();

    public void setServerPort(int port) {
        this.port = port;
    }

    public void setServerMaxThreads(int maxThreads) {
        this.maxThreads = maxThreads;
    }

    public void setObjectMapperImpl(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public void registerRoute(Route route, Function<Request, Response> handler) {
        this.registeredRoutes.put(route, handler);
    }

    public void start() {
        printApplicationBanner();
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            while (true) {
                Socket client = serverSocket.accept();

                var worker = new ManzWorker(client, objectMapper, registeredRoutes);

                manzWorkersPoll.execute(worker);
            }
        } catch (IOException exception) {
            logger.error("Internal error running Manz server", exception);
        }
    }

    private void printApplicationBanner() {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("banner.txt")) {
            try (var fileReader = new InputStreamReader(is)) {

                var buffer = new char[is.available()];
                var read = fileReader.read(buffer);

                var bannerString = new String(buffer);

                System.out.println(bannerString);

            }
        } catch (Exception e) {
            logger.debug("Error loading application banner", e);
        }
    }
}