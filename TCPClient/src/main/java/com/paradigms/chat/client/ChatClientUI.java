
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
import javafx.stage.Stage;

import java.io.IOException;

public class ChatClientUI extends Application {

    private final TextArea chatArea = new TextArea();
    private final TextField usernameField = new TextField();
    private final TextField messageField = new TextField();

    private final Button connectButton = new Button("Connect");
    private final Button sendButton = new Button("Send");
    private final Button disconnectButton = new Button("Disconnect");

    private final Label statusLabel = new Label("Not connected");

    private volatile ChatClient client;
    private volatile boolean connected = false;

    @Override
    public void start(Stage stage) {
        stage.setTitle("Paradigms TCP Chat");

        chatArea.setEditable(false);
        chatArea.setWrapText(true);
        chatArea.setPromptText("Chat messages will appear here...");

        usernameField.setPromptText("Enter your username");
        messageField.setPromptText("Type your message...");
        messageField.setDisable(true);

        sendButton.setDisable(true);
        disconnectButton.setDisable(true);

        connectButton.setOnAction(event -> connectToServer());
        sendButton.setOnAction(event -> sendMessage());
        disconnectButton.setOnAction(event -> disconnect());

        messageField.setOnAction(event -> sendMessage());

        HBox connectionBox = new HBox(
                10, new Label("Username:"), usernameField, connectButton
        );
        connectionBox.setPadding(new Insets(10));

        HBox messageBox = new HBox(10, messageField, sendButton);
        messageBox.setPadding(new Insets(10));

        VBox bottomBox = new VBox(5, statusLabel, messageBox);
        bottomBox.setPadding(new Insets(5));

        BorderPane root = new BorderPane();
        root.setTop(connectionBox);
        root.setCenter(chatArea);
        root.setBottom(bottomBox);
        root.setPadding(new Insets(10));

        Scene scene = new Scene(root, 600, 450);

        stage.setScene(scene);
        stage.setOnCloseRequest(event -> disconnect());
        stage.show();
    }

    private void connectToServer() {
        String username = usernameField.getText().trim();

        if (username.isEmpty()) {
            statusLabel.setText("Please enter a username.");
            return;
        }

        connectButton.setDisable(true);
        usernameField.setDisable(true);
        statusLabel.setText("Connecting to localhost:3000...");

        Thread.ofVirtual().start(() -> {
            ChatClient newClient = new ChatClient("localhost", 3000);

            try {
                newClient.connect(username);

                Platform.runLater(() -> {
                    client = newClient;
                    connected = true;

                    statusLabel.setText("Connected as " + username);
                    chatArea.appendText(
                            "Connected to the chat server.\n"
                    );

                    messageField.setDisable(false);
                    sendButton.setDisable(false);
                    disconnectButton.setDisable(false);

                    Thread.ofVirtual().start(() ->
                            receiveMessages(newClient)
                    );
                });

            } catch (IOException e) {
                newClient.disconnect();

                Platform.runLater(() -> {
                    statusLabel.setText(
                            "Connection failed: " + e.getMessage()
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
                        chatArea.appendText(
                                "Connection to server lost.\n"
                        )
                );
            }
        } finally {
            Platform.runLater(() -> {
                if (client == activeClient) {
                    connected = false;
                    client = null;

                    statusLabel.setText("Disconnected");
                    connectButton.setDisable(false);
                    usernameField.setDisable(false);
                    messageField.setDisable(true);
                    sendButton.setDisable(true);
                    disconnectButton.setDisable(true);
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

        activeClient.sendMessage(message);
        messageField.clear();
    }

    private void disconnect() {
        connected = false;

        ChatClient activeClient = client;
        client = null;

        if (activeClient != null) {
            activeClient.disconnect();
        }

        messageField.setDisable(true);
        sendButton.setDisable(true);
        disconnectButton.setDisable(true);
        connectButton.setDisable(false);
        usernameField.setDisable(false);
        statusLabel.setText("Disconnected");
    }

    @Override
    public void stop() {
        disconnect();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
