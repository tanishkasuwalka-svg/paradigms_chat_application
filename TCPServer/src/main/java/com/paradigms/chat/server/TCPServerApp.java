package com.paradigms.chat.server;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

public class TCPServerApp extends Application {

    private static final String DEFAULT_HOST = "0.0.0.0";
    private static final int DEFAULT_PORT = 3000;

    private static final Map<ClientHandler, String> CLIENTS =
            new ConcurrentHashMap<>();

    private static final Map<ClientHandler, Color> CLIENT_COLORS =
            new ConcurrentHashMap<>();

    private static final ObservableList<String> ACTIVE_USERS =
            FXCollections.observableArrayList();

    private static TextArea logArea;
    private static Label serverStatusLabel;

    private static volatile boolean serverRunning = false;

    @Override
    public void start(Stage stage) {
        stage.setTitle("Paradigms TCP Chat Server");

        Label title = new Label("TCP CHAT SERVER");
        title.setStyle("-fx-font-size: 20px; -fx-font-weight: bold;");

        serverStatusLabel = new Label("Starting server...");
        Label usersHeading = new Label("Connected Clients");

        ListView<String> usersList = new ListView<>(ACTIVE_USERS);
        usersList.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(String username, boolean empty) {
                super.updateItem(username, empty);

                if (empty || username == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(username);

                    ClientHandler matchingClient = CLIENTS.keySet()
                            .stream()
                            .filter(client ->
                                    getDisplayName(client).equals(username))
                            .findFirst()
                            .orElse(null);

                    Color color = matchingClient == null
                            ? Color.web("#455A64")
                            : CLIENT_COLORS.getOrDefault(
                            matchingClient, Color.web("#455A64"));

                    String background = toHex(color);

                    setStyle(
                            "-fx-background-color: " + background + ";" +
                                    "-fx-text-fill: white;" +
                                    "-fx-font-weight: bold;" +
                                    "-fx-padding: 10 12 10 12;" +
                                    "-fx-background-radius: 5px;"
                    );
                }
            }
        });

        Label logHeading = new Label("Server Activity Log");

        logArea = new TextArea();
        logArea.setEditable(false);
        logArea.setWrapText(true);
        logArea.setPromptText("Server activity will appear here...");

        VBox leftPanel = new VBox(8, usersHeading, usersList);
        leftPanel.setPadding(new Insets(10));
        leftPanel.setPrefWidth(230);

        VBox rightPanel = new VBox(8, logHeading, logArea);
        rightPanel.setPadding(new Insets(10));

        BorderPane root = new BorderPane();
        root.setTop(new VBox(8, title, serverStatusLabel));
        root.setLeft(leftPanel);
        root.setCenter(rightPanel);
        root.setPadding(new Insets(12));

        BorderPane.setMargin(root.getTop(), new Insets(5, 10, 5, 10));

        stage.setScene(new Scene(root, 850, 520));
        stage.setOnCloseRequest(event -> {
            serverRunning = false;
            log("Server window closed.");
        });
        stage.show();

        log("Starting TCP Chat Server...");

        Thread.ofVirtual().start(this::startServer);
    }

    private void startServer() {
        Properties properties = loadConfiguration();

        String host = properties.getProperty(
                "server.host", DEFAULT_HOST
        ).trim();

        int port;

        try {
            port = Integer.parseInt(
                    properties.getProperty(
                            "server.port",
                            String.valueOf(DEFAULT_PORT)
                    ).trim()
            );

            if (port < 1 || port > 65535) {
                throw new IllegalArgumentException(
                        "Port must be between 1 and 65535."
                );
            }
        } catch (IllegalArgumentException e) {
            log("Invalid server.port setting: " + e.getMessage());
            updateStatus("Server configuration error");
            return;
        }

        log("Configured host: " + host);
        log("Configured port: " + port);

        try (ServerSocket serverSocket = new ServerSocket()) {
            serverSocket.bind(new InetSocketAddress(host, port));

            serverRunning = true;

            log("Server Started on port " + port);
            log("Waiting for Client...");
            updateStatus("Online — listening on port " + port);

            while (serverRunning) {
                Socket clientSocket = serverSocket.accept();

                log("New connection: "
                        + clientSocket.getRemoteSocketAddress());

                Thread.ofVirtual().start(
                        new ClientHandler(clientSocket)
                );
            }

        } catch (IOException e) {
            if (serverRunning) {
                log("Server error: " + e.getMessage());
                updateStatus("Server error");
            }
        } finally {
            serverRunning = false;
        }
    }

    private static Properties loadConfiguration() {
        Properties properties = new Properties();

        Path[] candidates = {
                Path.of("config", "server.properties"),
                Path.of("TCPServer", "config", "server.properties")
        };

        for (Path path : candidates) {
            if (Files.isRegularFile(path)) {
                try (InputStream input = Files.newInputStream(path)) {
                    properties.load(input);
                    log("Loaded configuration from: "
                            + path.toAbsolutePath());
                    return properties;
                } catch (IOException e) {
                    log("Could not read " + path + ": "
                            + e.getMessage());
                }
            }
        }

        log("Configuration file not found. Using default settings.");
        return properties;
    }

    public static void registerClient(
            ClientHandler client,
            String username,
            boolean readOnly
    ) {
        String displayName = readOnly
                ? "READ-ONLY CLIENT"
                : username;

        CLIENTS.put(client, username);
        CLIENT_COLORS.put(client, randomColor());

        Platform.runLater(() -> {
            ACTIVE_USERS.add(displayName);
        });

        log("Welcome " + displayName);

        client.sendMessage(
                readOnly
                        ? "Connected in READ-ONLY MODE."
                        : "Welcome " + username + "!"
        );

        broadcastSystemMessage(displayName + " joined the chat.");
    }

    public static void removeClient(ClientHandler client) {
        String username = CLIENTS.remove(client);
        CLIENT_COLORS.remove(client);

        if (username != null) {
            String displayName = username.isBlank()
                    ? "READ-ONLY CLIENT"
                    : username;

            Platform.runLater(() -> ACTIVE_USERS.remove(displayName));

            log(displayName + " disconnected.");
            broadcastSystemMessage(displayName + " left the chat.");
        }
    }

    public static void broadcast(String message) {
        log(message);

        CLIENTS.keySet().forEach(client -> client.sendMessage(message));
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

    private static String getDisplayName(ClientHandler client) {
        String username = CLIENTS.get(client);

        if (username == null || username.isBlank()) {
            return "READ-ONLY CLIENT";
        }

        return username;
    }


    private static Color randomColor() {
        // Bright, distinct colours for connected-client entries.
        Color[] colors = {
                Color.web("#008F7A"), // Vibrant teal
                Color.web("#245BDB"), // Electric blue
                Color.web("#7B2CBF"), // Purple
                Color.web("#D81B60"), // Hot pink
                Color.web("#D97706"), // Amber orange
                Color.web("#0077B6"), // Bright blue
                Color.web("#218838"), // Green
                Color.web("#C62828")  // Red
        };

        return colors[java.util.concurrent.ThreadLocalRandom.current()
                .nextInt(colors.length)];
    }


    private static String toHex(Color color) {
        return String.format(
                "#%02X%02X%02X",
                (int) Math.round(color.getRed() * 255),
                (int) Math.round(color.getGreen() * 255),
                (int) Math.round(color.getBlue() * 255)
        );
    }

    private static void log(String message) {
        System.out.println(message);

        TextArea currentLog = logArea;

        if (currentLog != null) {
            Platform.runLater(() ->
                    currentLog.appendText(message + "\n")
            );
        }
    }

    private static void updateStatus(String message) {
        Label currentLabel = serverStatusLabel;

        if (currentLabel != null) {
            Platform.runLater(() -> currentLabel.setText(message));
        }
    }

    @Override
    public void stop() {
        serverRunning = false;
    }

    public static void main(String[] args) {
        launch(args);
    }
}

