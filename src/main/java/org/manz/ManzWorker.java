package org.manz;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static java.lang.String.format;

public class ManzWorker implements Runnable {

    private final Socket client;

    public ManzWorker(Socket client) {
        this.client = client;
    }

    private final Logger logger = LoggerFactory.getLogger(ManzServer.class);

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

    private static Response handle(Request request) {
        return new Response(200, new HashMap<>(), new Payload("{\"message\":\"blaaa\"}", "application/json"));
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
        String path = firstRequestLine[1];

        Map<String, String> headers = mapRequestHeaders(requestsLines);

        Optional<Payload> requestBody = mapRequestBody(requestsLines, headers);

        return new Request(method, path, headers, requestBody);
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
