# Manz HTTP Server

A minimal HTTP server built on top of Java Socket API.

## How to use?

```java

public class Main {

    private static Function<Request, Response> helloWorldHandler() {
        return (Request request) -> new Response(200, new HashMap<>() {
        }, new Payload("Hello World", "text/plain"));
    }

    public static void main(String[] args) throws Exception {
        var server = new ManzServer();
        // creating the server instance and setting main server properties
        server.setServerMaxThreads(200);
        server.setServerPort(3000);

        // registering a simple route
        server.registerRoute(HttpMethod.GET, "/", helloWorldHandler());

        // starting the server
        server.start();
    }
}
```

