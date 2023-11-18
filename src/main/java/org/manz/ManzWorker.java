package org.manz;

import org.manz.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.util.*;
import java.util.function.Function;

import static java.lang.String.format;

public class ManzWorker implements Runnable {

    private final Socket client;

    private final Map<Route, Function<Request, Response>> registeredRoutes;

    public ManzWorker(Socket client, Map<Route, Function<Request, Response>> registeredRoutes) {
        this.client = client;
        this.registeredRoutes = registeredRoutes;
    }

    private static final Logger logger = LoggerFactory.getLogger(ManzServer.class);

    @Override
    public void run() {
        logger.info("Executing client request on: {}", Thread.currentThread().getName());
        try {
            var request = mapRequest(client.getInputStream());

            var response = handle(request);

            logger.debug(request + " | " + response);

            sendResponse(client.getOutputStream(), response);
        } catch (Exception exception) {
            logger.error("Error processing client request", exception);
        } finally {
            closeQuietly();
        }
    }

    private void closeQuietly() {
        try {
            client.close();
        } catch (IOException e) {
            logger.error("Error closing client request", e);
        }
    }

    private Response handle(Request request) {
        var routeHandlerRequest = registeredRoutes.entrySet().stream()
                .filter(route -> route.getKey().method().equals(request.method()) && route.getKey().path().equals(request.path()))
                .findFirst();

        if (routeHandlerRequest.isEmpty()) {
            return Response.NOT_FOUND;
        }

        return routeHandlerRequest.get().getValue().apply(request);
    }

    private static String[] readMessage(final InputStream inputStream) throws IOException {

        if (!(inputStream.available() > 0)) {
            return null;
        }

        final char[] inBuffer = new char[inputStream.available()];

        InputStreamReader inReader = new InputStreamReader(inputStream);
        int read = inReader.read(inBuffer);
        return new String(inBuffer).split("\r\n");
    }

    private static Request mapRequest(final InputStream inputStream) throws IOException {

        String[] requestsLines = readMessage(inputStream);

        if (requestsLines == null
                || requestsLines.length == 0) {
            return null;
        }

        String[] firstRequestLine = requestsLines[0].split(" ");
        String method = firstRequestLine[0];
        String fullPath = firstRequestLine[1];

        Map<String, String> queryParameters = mapQueryParameters(fullPath);

        var isolatedPath = isolatePath(fullPath);

        Map<String, String> headers = mapRequestHeaders(requestsLines);

        Optional<Payload> requestBody = mapRequestBody(requestsLines, headers);

        return new Request(HttpMethod.valueOf(method), isolatedPath, queryParameters, headers, requestBody);
    }

    private static String isolatePath(String fullPath) {
        var index = fullPath.indexOf("?");

        // In case query parametes not provided
        if (index == -1)
            return fullPath;

        return fullPath.substring(0, index);
    }

    private static Map<String, String> mapQueryParameters(String path) {
        var index = path.indexOf("?");
        if (index == -1) {
            return Collections.emptyMap();
        }

        var queryParameterMap = new HashMap<String, String>();

        Arrays.stream(path.substring(index + 1)
                .split("&")).forEach(rawQueryParameter -> {

            var rawQueryParameterSplited = rawQueryParameter.split("=");

            queryParameterMap.put(rawQueryParameterSplited[0], rawQueryParameterSplited[1]);
        });

        return queryParameterMap;
    }

    private static Optional<Payload> mapRequestBody(String[] requestsLines, Map<String, String> headers) {
        Payload requestBody = null;
        if (headers.containsKey("Content-Length")) {
            requestBody = new Payload(requestsLines[requestsLines.length - 1], headers.get("Content-Type"));
        }

        return Optional.ofNullable(requestBody);
    }

    private static Map<String, String> mapRequestHeaders(String[] requestsLines) {
        var headers = new HashMap<String, String>();

        for (int h = 1; h < requestsLines.length; h++) {
            String stringHeader = requestsLines[h];

            // reaching the end of header section
            if (stringHeader.isEmpty())
                break;

            var header = stringHeader.split(":");

            headers.put(header[0], header[1]);
        }
        return headers;
    }

    private static void sendResponse(OutputStream clientOutput, Response response) throws IOException {
        try {
            clientOutput.write((format("HTTP/1.1 %s \r\n", response.status())).getBytes());
            if (response.responseBody() != null) {
                clientOutput.write(("Content-Type: " + response.responseBody() + "\r\n").getBytes());
                clientOutput.write("\r\n".getBytes());
                clientOutput.write(response.responseBody().value().getBytes());
            }
            clientOutput.write("\r\n\r\n".getBytes());
            clientOutput.flush();
            clientOutput.close();
        } catch (SocketException socketException) {
            clientOutput.close();
        }
    }
}
