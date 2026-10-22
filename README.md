# Java TCP Chat Application

A multi-client chat application developed using Java 21, TCP sockets, virtual threads, JavaFX, and Maven.

## Project Overview

This project implements a TCP client-server chat system. Multiple clients can connect to a central server, exchange messages in real time, and view active users through a graphical interface.

## Features

### Client
- Real-time message sending and receiving.
- Sender names and message timestamps.
- Active-user listing using `allUsers`.
- Read-only connections when the username is blank.
- Disconnection using `bye`, `end`, or the Leave button.
- Online/offline connection status indicator.

### Server
- Multiple concurrent client connections using Java virtual threads.
- Message broadcasting to connected clients.
- Live list of connected clients with vibrant background colours.
- Server activity logging.
- Host and port loaded from a properties configuration file.

## Technologies
- Java 21
- JavaFX
- TCP sockets
- Virtual threads
- Maven
- Git and GitHub

## Project Structure

```text
paradigms_chat_application/
├── TCPClient/
│   ├── src/main/java/com/paradigms/chat/client/
│   └── pom.xml
├── TCPServer/
│   ├── config/server.properties
│   ├── src/main/java/com/paradigms/chat/server/
│   └── pom.xml
├── .gitignore
└── README.md
```

## Requirements
- Java Development Kit (JDK) 21 or later.
- Maven or IntelliJ IDEA with bundled Maven.
- A desktop environment that supports JavaFX.

## Server Configuration

Configuration file: `TCPServer/config/server.properties`

```properties
server.host=0.0.0.0
server.port=3000
```

The server listens on the configured port. Clients on the same computer can connect using `localhost`. Clients on other computers need the server computer's reachable IP address and appropriate network access.

## Build Instructions

Build the server:

```bash
cd TCPServer
mvn clean package
```

Build the client:

```bash
cd TCPClient
mvn clean package
```

The JAR files are generated in each project's `target` directory.

## Run Instructions

Start the server first:

```bash
cd TCPServer
java -jar target/tcp-server-1.0-SNAPSHOT.jar
```

Then open another terminal and start a client:

```bash
cd TCPClient
java -jar target/tcp-client-1.0-SNAPSHOT.jar localhost 3000
```

Replace `localhost` and `3000` with the appropriate server IP address and port when needed.

## Chat Commands

| Command | Description |
|---|---|
| `allUsers` | Requests the list of active named users. |
| `bye` | Disconnects the client. |
| `end` | Disconnects the client. |
| Leave button | Disconnects through the graphical interface. |

## Testing

The application has been manually tested with multiple clients, message exchange, active-user listing, read-only connections, disconnection and reconnection, configuration changes, server activity logging, and launching the packaged applications.

## GitHub Repository

https://github.com/tanishkasuwalka-svg/paradigms_chat_application
