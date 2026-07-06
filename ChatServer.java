import java.io.*;
import java.net.*;
import java.util.concurrent.*;

/**
 * Main Chat Server - Handles multiple clients using multithreading
 */
public class ChatServer {

    public static void main(String[] args) {
        int port = (args.length > 0) ? Integer.parseInt(args[0]) : 5000;

        System.out.println("Server starting on port " + port + "...");

        ExecutorService executor = Executors.newFixedThreadPool(20);
        ClientManager clientManager = new ClientManager();

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("✅ Server started successfully on port " + port);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                executor.execute(new ClientHandler(clientSocket, clientManager));
            }
        } catch (IOException e) {
            System.err.println("Server failed to start: " + e.getMessage());
        } finally {
            executor.shutdown();
        }
    }
}

/**
 * Manages all connected clients - Thread-safe
 */
class ClientManager {
    private final Set<PrintWriter> clientWriters = ConcurrentHashMap.newKeySet();

    public void addClient(PrintWriter writer) {
        clientWriters.add(writer);
    }

    public void removeClient(PrintWriter writer) {
        clientWriters.remove(writer);
    }

    public void broadcast(String message) {
        for (PrintWriter writer : clientWriters) {
            writer.println(message);
        }
    }
}

/**
 * Handles communication with a single client
 */
class ClientHandler implements Runnable {
    private final Socket socket;
    private final ClientManager clientManager;
    private PrintWriter output;
    private String clientName;

    public ClientHandler(Socket socket, ClientManager clientManager) {
        this.socket = socket;
        this.clientManager = clientManager;
    }

    @Override
    public void run() {
        try (BufferedReader input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter writer = new PrintWriter(socket.getOutputStream(), true)) {

            output = writer;
            clientManager.addClient(output);
            clientName = "Client-" + (System.currentTimeMillis() % 10000);

            clientManager.broadcast(clientName + " has joined the chat.");

            String message;
            while ((message = input.readLine()) != null) {
                if ("exit".equalsIgnoreCase(message.trim())) {
                    break;
                }
                clientManager.broadcast(clientName + ": " + message);
            }
        } catch (IOException e) {
            System.out.println(clientName + " disconnected unexpectedly.");
        } finally {
            cleanupClient();
        }
    }

    private void cleanupClient() {
        if (output != null) {
            clientManager.removeClient(output);
        }
        if (clientName != null) {
            clientManager.broadcast(clientName + " has left the chat.");
        }
        closeQuietly(socket);
    }

    private void closeQuietly(AutoCloseable resource) {
        try {
            if (resource != null) resource.close();
        } catch (Exception ignored) {}
    }
}
