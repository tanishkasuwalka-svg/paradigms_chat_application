
package com.paradigms.chat.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class ClientHandler implements Runnable {

    private final Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private String username = "";
    private boolean readOnly = true;

    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try (
                Socket clientSocket = socket;
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(
                                clientSocket.getInputStream(),
                                StandardCharsets.UTF_8
                        )
                );
                PrintWriter writer = new PrintWriter(
                        clientSocket.getOutputStream(), true,
                        StandardCharsets.UTF_8
                )
        ) {
            in = reader;
            out = writer;

            // The first line sent by the client is its username.
            String requestedName = in.readLine();

            if (requestedName != null && !requestedName.isBlank()) {
                username = requestedName.trim();
                readOnly = false;
            }

            TCPServerApp.registerClient(this, username, readOnly);

            String message;
            while ((message = in.readLine()) != null) {
                message = message.trim();

                if (message.isEmpty()) {
                    continue;
                }

                if (message.equalsIgnoreCase("bye")
                        || message.equalsIgnoreCase("end")) {
                    break;
                }

                if (message.equalsIgnoreCase("allUsers")) {
                    out.println(TCPServerApp.getActiveUsers());
                    continue;
                }

                if (readOnly) {
                    out.println("READ-ONLY MODE: You cannot send messages.");
                    continue;
                }

                String time = LocalTime.now().format(
                        DateTimeFormatter.ofPattern("HH:mm:ss")
                );

                TCPServerApp.broadcast(
                        "[" + time + "] " + username + ": " + message
                );
            }

        } catch (IOException e) {
            System.out.println("Client connection ended: " + e.getMessage());
        } finally {
            TCPServerApp.removeClient(this);
        }
    }

    public void sendMessage(String message) {
        PrintWriter writer = out;
        if (writer != null) {
            writer.println(message);
        }
    }

    public String getUsername() {
        return username;
    }

    public boolean isReadOnly() {
        return readOnly;
    }
}
