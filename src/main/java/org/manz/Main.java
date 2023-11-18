package org.manz;

import org.manz.model.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.function.Function;

public class Main {

    private static Function<Request, Response> helloWorldHandler() {
        return (Request request) -> new Response(200, new HashMap<>() {
        }, new Payload("Hello World", "text/plain"));
    }

    public static void main(String[] args) throws Exception {
        var server = new ManzServer();

        server.setServerMaxThreads(200);
        server.setServerPort(3000);

        var exampleRoute = new Route(HttpMethod.GET, "/", Collections.emptyList(), Collections.emptyList());

        server.registerRoute(exampleRoute, helloWorldHandler());

        server.start();
    }
}
