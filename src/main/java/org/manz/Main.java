package org.manz;

import org.manz.model.HttpMethod;
import org.manz.model.Payload;
import org.manz.model.Request;
import org.manz.model.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.function.Function;

public class Main {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);


    private static Function<Request, Response> helloWorldHandler() {
        return (Request request) -> new Response(200, new HashMap<>() {
        }, new Payload("Hello World", "text/plain"));
    }

    public static void main(String[] args) throws Exception {
        var server = new ManzServer();

        server.setServerMaxThreads(200);
        server.setServerPort(3000);

        server.registerRoute(HttpMethod.GET, "/", helloWorldHandler());

        server.start();
    }
}