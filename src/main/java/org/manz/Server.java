package org.manz;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;

import static java.lang.String.format;

// Read the full article https://dev.to/mateuszjarzyna/build-your-own-http-server-in-java-in-less-than-one-hour-only-get-method-2k02
public class Server {

    public static void main(String[] args) throws Exception {
        try (ServerSocket serverSocket = new ServerSocket(3000, 200)) {
            while (true) {
                try (Socket client = serverSocket.accept()) {
                    handleClient(client);
                }
            }
        }
    }

    private static List<String> readMessage(final InputStream inputStream) {
        try {
            if (!(inputStream.available() > 0)) {
                return Collections.emptyList();
            }

            final char[] inBuffer = new char[inputStream.available()];
            final InputStreamReader inReader = new InputStreamReader(inputStream);
            int read = inReader.read(inBuffer);

            List<String> messageLines = new ArrayList<>();

            try (Scanner sc = new Scanner(new String(inBuffer))) {
                while (sc.hasNextLine()) {
                    String line = sc.nextLine();
                    messageLines.add(line);
                }
            }

            return messageLines;
        } catch (Exception ignored) {
            return Collections.emptyList();
        }
    }

    private static void handleClient(Socket client) throws IOException {

        List<String> requestsLines = readMessage(client.getInputStream());

        if (requestsLines.isEmpty()) {
            client.close();
            return;
        }

        String[] firstRequestLine = requestsLines.get(0).split(" ");
        String method = firstRequestLine[0];
        String path = firstRequestLine[1];
        String version = firstRequestLine[2];

        List<String> headers = new ArrayList<>();
        for (int h = 1; h < requestsLines.size(); h++) {
            String header = requestsLines.get(h);
            headers.add(header);
        }

        String accessLog = format("Client %s, method %s, path %s, version %s, headers %s",
                client.toString(), method, path, version, headers.toString());

        System.out.println(accessLog);

        byte[] messageContent = "{\"message\":\"TESTE\"}".getBytes();

        sendResponse(client, "200 OK", "application/json", messageContent);
    }

    private static void sendResponse(Socket client, String status, String contentType, byte[] content) throws IOException {
        OutputStream clientOutput = client.getOutputStream();
        clientOutput.write((format("HTTP/1.1 %s \r\n", status)).getBytes());
        clientOutput.write(("Content-Type: " + contentType + "\r\n").getBytes());
        clientOutput.write("\r\n".getBytes());
        clientOutput.write(content);
        clientOutput.write("\r\n\r\n".getBytes());
        clientOutput.flush();
        client.close();
    }
}