import java.io.*;
import java.net.*;
import java.util.Set;
import java.util.concurrent.*;

/**
 * Multi-client Chat Server
 */
public class ChatServer {

    private static final int DEFAULT_PORT = 5000;

    public static void main(String[] args) {

        int port = args.length > 0
                ? Integer.parseInt(args[0])
                : DEFAULT_PORT;

        System.out.println("Starting server on port " + port);

        ExecutorService executor = Executors.newCachedThreadPool();
        ClientManager clientManager = new ClientManager();

        try (ServerSocket serverSocket = new ServerSocket(port)) {

            System.out.println("✅ Server started successfully.");

            while (true) {
                Socket socket = serverSocket.accept();
                executor.execute(
                        new ClientHandler(socket, clientManager));
            }

        } catch (IOException e) {
            System.err.println("Server error: " + e.getMessage());
        } finally {
            executor.shutdown();

            try {
                if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }
}

/**
 * Thread-safe client manager
 */
class ClientManager {

    private final Set<PrintWriter> clientWriters =
            new CopyOnWriteArraySet<>();

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
 * Handles a single client
 */
class ClientHandler implements Runnable {

    private final Socket socket;
    private final ClientManager clientManager;

    private PrintWriter output;
    private String clientName;

    public ClientHandler(
            Socket socket,
            ClientManager clientManager) {

        this.socket = socket;
        this.clientManager = clientManager;
    }

    @Override
    public void run() {

        try (
                BufferedReader input =
                        new BufferedReader(
                                new InputStreamReader(
                                        socket.getInputStream()));

                PrintWriter writer =
                        new PrintWriter(
                                socket.getOutputStream(),
                                true)
        ) {

            output = writer;
            clientManager.addClient(output);

            clientName =
                    "Client-" +
                    (System.currentTimeMillis() % 10000);

            clientManager.broadcast(
                    clientName + " joined the chat.");

            String message;

            while ((message = input.readLine()) != null) {

                if ("exit".equalsIgnoreCase(
                        message.trim())) {
                    break;
                }

                clientManager.broadcast(
                        clientName + ": " + message);
            }

        } catch (IOException e) {
            System.out.println(
                    clientName +
                    " disconnected unexpectedly.");
        } finally {
            cleanup();
        }
    }

    private void cleanup() {

        if (output != null) {
            clientManager.removeClient(output);
        }

        if (clientName != null) {
            clientManager.broadcast(
                    clientName + " left the chat.");
        }

        closeQuietly(socket);
    }

    private void closeQuietly(AutoCloseable resource) {
        try {
            if (resource != null)
                resource.close();
        } catch (Exception ignored) {
        }
    }
}
