
package com.paradigms.chat.client;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;

import java.io.IOException;

public class ChatClientUI extends Application {

    private final TextArea chatArea = new TextArea();
    private final TextField usernameField = new TextField();
    private final TextField messageField = new TextField();

    private final Button connectButton = new Button("Connect");
    private final Button sendButton = new Button("SEND");
    private final Button leaveButton = new Button("LEAVE");

    private final Circle statusCircle = new Circle(7);
    private final Label statusLabel = new Label("Offline");

    private volatile ChatClient client;
    private volatile boolean connected = false;
    private volatile boolean readOnly = false;

    private String serverHost = "localhost";
    private int serverPort = 3000;

    @Override
    public void init() {
        var args = getParameters().getRaw();

        if (!args.isEmpty() && !args.get(0).isBlank()) {
            serverHost = args.get(0);
        }

        if (args.size() >= 2) {
            try {
                serverPort = Integer.parseInt(args.get(1));

                if (serverPort < 1 || serverPort > 65535) {
                    throw new IllegalArgumentException(
                            "Port must be between 1 and 65535."
                    );
                }
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException(
                        "Invalid server port: " + args.get(1), e
                );
            }
        }

        if (serverPort < 1 || serverPort > 65535) {
            throw new IllegalArgumentException("Invalid server port.");
        }
    }

    @Override
    public void start(Stage stage) {
        stage.setTitle("Paradigms TCP Chat");

        chatArea.setEditable(false);
        chatArea.setWrapText(true);
        chatArea.setPromptText("Chat messages will appear here...");

        usernameField.setPromptText("Enter your username (optional)");
        messageField.setPromptText("Type your message...");
        messageField.setDisable(true);

        sendButton.setDisable(true);
        leaveButton.setDisable(true);

        statusCircle.setFill(Color.GRAY);

        connectButton.setOnAction(event -> connectToServer());
        sendButton.setOnAction(event -> sendMessage());
        leaveButton.setOnAction(event -> disconnect());

        // Press Enter to send a message.
        messageField.setOnAction(event -> sendMessage());

        HBox connectionBox = new HBox(
                10,
                new Label("Username:"),
                usernameField,
                connectButton
        );
        connectionBox.setPadding(new Insets(10));

        HBox statusBox = new HBox(8, statusCircle, statusLabel);
        statusBox.setPadding(new Insets(5));

        HBox messageBox = new HBox(
                10,
                messageField,
                sendButton,
                leaveButton
        );
        messageBox.setPadding(new Insets(10));

        VBox bottomBox = new VBox(5, statusBox, messageBox);

        BorderPane root = new BorderPane();
        root.setTop(connectionBox);
        root.setCenter(chatArea);
        root.setBottom(bottomBox);
        root.setPadding(new Insets(10));

        stage.setScene(new Scene(root, 700, 480));
        stage.setOnCloseRequest(event -> disconnect());
        stage.show();
    }

    private void updateStatus(String message, Color color) {
        statusLabel.setText(message);
        statusCircle.setFill(color);
    }

    private void connectToServer() {
        if (connected) {
            return;
        }

        String username = usernameField.getText().trim();
        boolean requestedReadOnly = username.isEmpty();

        connectButton.setDisable(true);
        usernameField.setDisable(true);
        updateStatus("Connecting to " + serverHost + ":" + serverPort,
                Color.ORANGE);

        Thread.ofVirtual().start(() -> {
            ChatClient newClient = new ChatClient(serverHost, serverPort);

            try {
                // Empty username requests read-only mode from the server.
                newClient.connect(username);

                Platform.runLater(() -> {
                    client = newClient;
                    connected = true;
                    readOnly = requestedReadOnly;

                    if (readOnly) {
                        updateStatus("Online — READ-ONLY MODE",
                                Color.LIMEGREEN);
                        chatArea.appendText(
                                "Connected as a read-only client.\n"
                        );
                    } else {
                        updateStatus("Online", Color.LIMEGREEN);
                        chatArea.appendText(
                                "Connected to the chat server.\n"
                        );
                    }

                    messageField.setDisable(readOnly);
                    sendButton.setDisable(readOnly);
                    leaveButton.setDisable(false);

                    Thread.ofVirtual().start(
                            () -> receiveMessages(newClient)
                    );
                });

            } catch (IOException e) {
                newClient.disconnect();

                Platform.runLater(() -> {
                    updateStatus("Offline — connection failed", Color.RED);
                    chatArea.appendText(
                            "Connection failed: " + e.getMessage() + "\n"
                    );

                    connectButton.setDisable(false);
                    usernameField.setDisable(false);
                });
            }
        });
    }

    private void receiveMessages(ChatClient activeClient) {
        try {
            String message;

            while ((message = activeClient.receiveMessage()) != null) {
                String receivedMessage = message;

                Platform.runLater(() ->
                        chatArea.appendText(receivedMessage + "\n")
                );
            }

        } catch (IOException e) {
            if (connected && client == activeClient) {
                Platform.runLater(() ->
                        chatArea.appendText("Connection to server lost.\n")
                );
            }

        } finally {
            Platform.runLater(() -> {
                if (client == activeClient) {
                    connected = false;
                    client = null;
                    readOnly = false;

                    updateStatus("Offline", Color.GRAY);
                    connectButton.setDisable(false);
                    usernameField.setDisable(false);
                    messageField.setDisable(true);
                    sendButton.setDisable(true);
                    leaveButton.setDisable(true);
                }
            });
        }
    }

    private void sendMessage() {
        String message = messageField.getText().trim();
        ChatClient activeClient = client;

        if (!connected || activeClient == null || message.isEmpty()) {
            return;
        }

        if (readOnly) {
            chatArea.appendText(
                    "READ-ONLY MODE: You cannot send messages.\n"
            );
            return;
        }

        // The server already handles bye and end as disconnect commands.
        // Display the command locally before the server closes the connection.
        if (message.equalsIgnoreCase("bye")
                || message.equalsIgnoreCase("end")) {
            chatArea.appendText(message + "\n");
            messageField.clear();
            activeClient.sendMessage(message);
            return;
        }

        activeClient.sendMessage(message);
        messageField.clear();
    }

    private void disconnect() {
        ChatClient activeClient = client;

        if (activeClient == null) {
            return;
        }

        // LEAVE disconnects through the existing ChatClient implementation.
        connected = false;
        client = null;
        readOnly = false;

        activeClient.disconnect();

        updateStatus("Offline", Color.GRAY);
        connectButton.setDisable(false);
        usernameField.setDisable(false);
        messageField.setDisable(true);
        sendButton.setDisable(true);
        leaveButton.setDisable(true);
    }

    @Override
    public void stop() {
        disconnect();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
