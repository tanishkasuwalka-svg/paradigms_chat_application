
package com.paradigms.chat.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class ChatClient {

    private final String serverAddress;
    private final int serverPort;

    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;

    public ChatClient(String serverAddress, int serverPort) {
        this.serverAddress = serverAddress;
        this.serverPort = serverPort;
    }

    public void connect(String username) throws IOException {
        socket = new Socket(serverAddress, serverPort);

        in = new BufferedReader(
                new InputStreamReader(
                        socket.getInputStream(),
                        StandardCharsets.UTF_8
                )
        );

        out = new PrintWriter(
                socket.getOutputStream(),
                true,
                StandardCharsets.UTF_8
        );

        // The server expects the username as the first line.
        out.println(username == null ? "" : username.trim());

        System.out.println("Connected to " + serverAddress + ":" + serverPort);
    }

    public void sendMessage(String message) {
        if (out != null) {
            out.println(message);
        }
    }

    public String receiveMessage() throws IOException {
        if (in == null) {
            return null;
        }

        return in.readLine();
    }

    public void disconnect() {
        try {
            if (out != null) {
                out.println("bye");
            }

            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            System.err.println("Error disconnecting: " + e.getMessage());
        }
    }
}
