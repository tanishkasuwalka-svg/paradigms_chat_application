
package com.paradigms.chat.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class TCPServerApp {

    private static final int PORT = 3000;

    private static final Map<ClientHandler, String> CLIENTS =
            new ConcurrentHashMap<>();

    public static void main(String[] args) {
        System.out.println("Starting TCP Chat Server...");

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Server Started on port " + PORT);
            System.out.println("Waiting for clients...");

            while (true) {
                Socket clientSocket = serverSocket.accept();

                System.out.println(
                        "New connection: " + clientSocket.getRemoteSocketAddress()
                );

                Thread.ofVirtual()
                        .start(new ClientHandler(clientSocket));
            }

        } catch (IOException e) {
            System.err.println("Server error: " + e.getMessage());
        }
    }

    public static void registerClient(
            ClientHandler client, String username, boolean readOnly) {

        CLIENTS.put(client, username);

        String identity = readOnly ? "READ-ONLY CLIENT" : username;
        System.out.println("Welcome " + identity);

        client.sendMessage(
                readOnly
                        ? "Connected in READ-ONLY MODE."
                        : "Welcome " + username + "!"
        );

        broadcastSystemMessage(identity + " joined the chat.");
    }

    public static void removeClient(ClientHandler client) {
        String username = CLIENTS.remove(client);

        if (username != null) {
            String identity = username.isBlank()
                    ? "READ-ONLY CLIENT" : username;

            System.out.println(identity + " disconnected.");
            broadcastSystemMessage(identity + " left the chat.");
        }
    }

    public static void broadcast(String message) {
        System.out.println(message);

        CLIENTS.keySet().forEach(client -> {
            if (!client.isReadOnly()) {
                client.sendMessage(message);
            }
        });
    }

    private static void broadcastSystemMessage(String message) {
        CLIENTS.keySet().forEach(client -> client.sendMessage(message));
    }

    public static String getActiveUsers() {
        StringBuilder result = new StringBuilder("Active users: ");

        CLIENTS.values().stream()
                .filter(name -> !name.isBlank())
                .forEach(name -> result.append(name).append(", "));

        return result.toString();
    }
}
