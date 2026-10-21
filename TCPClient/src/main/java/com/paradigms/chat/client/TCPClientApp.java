
package com.paradigms.chat.client;

import java.io.IOException;

public class TCPClientApp {

    public static void main(String[] args) {
        String serverAddress =
                args.length > 0 ? args[0] : "localhost";

        int serverPort = args.length > 1
                ? Integer.parseInt(args[1])
                : 3000;

        ChatClient client = new ChatClient(serverAddress, serverPort);

        try {
            client.connect("TestUser");

            // Read messages on a separate virtual thread.
            Thread.ofVirtual().start(() -> {
                try {
                    String message;
                    while ((message = client.receiveMessage()) != null) {
                        System.out.println(message);
                    }
                } catch (IOException e) {
                    System.out.println("Disconnected from server.");
                }
            });

            System.out.println("Client connected. Type bye to leave.");
            System.out.println(
                    "This is a temporary client test; the JavaFX UI comes next."
            );

        } catch (IOException e) {
            System.err.println("Could not connect: " + e.getMessage());
        }
    }
}
