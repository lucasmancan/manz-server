package org.manz;

import com.fasterxml.jackson.databind.ObjectMapper;
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

    private final Logger logger = LoggerFactory.getLogger(ManzServer.class);

    private final Socket client;

    private final ObjectMapper objectMapper;

    private final Map<Route, Function<Request, Response>> registeredRoutes;

    public ManzWorker(Socket client,
                      ObjectMapper objectMapper,
                      Map<Route, Function<Request, Response>> registeredRoutes) {
        this.client = client;
        this.objectMapper = objectMapper;
        this.registeredRoutes = registeredRoutes;
    }

    @Override
    public void run() {
        try {

            logger.debug("Executing client request on: {}", Thread.currentThread().getName());

            if (socketConnectionIsInvalid(client))
                return;

            var request = mapRequest(client.getInputStream());

            var response = handle(request);

            logger.debug("Request: {}", objectMapper.writeValueAsString(request));
            logger.debug("Response: {}", objectMapper.writeValueAsString(response));

            sendResponse(client.getOutputStream(), response);
        } catch (Exception exception) {
            logger.error("Error processing client request", exception);
        } finally {
            closeQuietly();
        }
    }

    private boolean socketConnectionIsInvalid(Socket socket) throws IOException {
        return socket.getInputStream().available() == 0;
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

    private String[] readMessage(final InputStream inputStream) throws IOException {

        final char[] inBuffer = new char[inputStream.available()];

        InputStreamReader inReader = new InputStreamReader(inputStream);
        int read = inReader.read(inBuffer);
        return new String(inBuffer).split("\r\n");
    }

    private Request mapRequest(final InputStream inputStream) throws IOException {

        String[] requestsLines = readMessage(inputStream);

        String[] firstRequestLine = requestsLines[0].split(" ");
        String method = firstRequestLine[0];
        String fullPath = firstRequestLine[1];

        Map<String, String> queryParameters = mapQueryParameters(fullPath);

        var isolatedPath = isolatePath(fullPath);

        Map<String, String> headers = mapRequestHeaders(requestsLines);

        Optional<Payload> requestBody = mapRequestBody(requestsLines, headers);

        return new Request(HttpMethod.valueOf(method), isolatedPath, queryParameters, headers, requestBody);
    }

    private String isolatePath(String fullPath) {
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
                clientOutput.write(("Content-Type: " + response.responseBody().contentType() + "\r\n").getBytes());
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
