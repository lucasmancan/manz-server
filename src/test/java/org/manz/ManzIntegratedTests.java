package org.manz;

import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.RequestBody;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.manz.model.*;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ManzIntegratedTests {

    private static Function<Request, Response> helloWorldHandler() {
        return (Request request) -> new Response(200, new HashMap<>() {
        }, new Payload("Hello World", "text/plain"));
    }

    private static Thread serverThread;

    public static void startLocalServer() {

        serverThread = new Thread(() -> {
            var server = new ManzServer();

            server.setServerMaxThreads(200);
            server.setServerPort(3000);

            var helloRoute = new Route(HttpMethod.GET, "/pessoas",
                    Collections.emptyList(), Collections.emptyList());

            server.registerRoute(helloRoute, helloWorldHandler());

            server.start();
        });

        serverThread.start();
    }

    public static void stopLocalServer() throws InterruptedException {
        serverThread.stop();
    }

    @Test
    public void shouldHandleRequestAnd() throws IOException, InterruptedException {
        startLocalServer();

        OkHttpClient client = new OkHttpClient();

        MediaType mediaType = MediaType.parse("application/json");

        RequestBody requestBody = RequestBody.create(mediaType, "{\n    \"apelido\" : \"josé\",\n    \"nome\" : \"José Roberto\",\n    \"nascimento\" : \"2000-10-01\",\n    \"stack\" : [\"C#\", \"Node\", \"Oracle\"]\n}");

        com.squareup.okhttp.Request request = new com.squareup.okhttp.Request.Builder()
                .url("http://localhost:3000/pessoas?t=lucas&testeVariavel=2341123")
                .method("GET", null)
                .addHeader("Content-Type", "application/json")
                .build();

        com.squareup.okhttp.Response response = client.newCall(request).execute();

        var body = response.body().string();

        assertEquals("Hello World", body);
        assertEquals(200, response.code());

        stopLocalServer();

    }
//    public static void main(String[] args) throws Exception {
//        var server = new ManzServer();
//
//        server.setServerMaxThreads(200);
//        server.setServerPort(3000);
//
//        var helloRoute = new Route(HttpMethod.GET, "/pessoas", Collections.emptyList(), Collections.emptyList());
//
//        server.registerRoute(helloRoute, helloWorldHandler());
//
//        server.start();
//    }
}