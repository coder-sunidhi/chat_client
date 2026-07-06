import java.io.*;
import java.net.*;
import java.util.concurrent.*;

public class ChatServer {

    public static void main(String[] args) {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 5000;

        System.out.println("Server starting on port " + port + "...");

        // Dynamic thread pool
        int poolSize = Runtime.getRuntime().availableProcessors() * 2;
        ExecutorService executor = Executors.newFixedThreadPool(poolSize);

        ClientManager clientManager = new ClientManager();

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("✅ Server started successfully!");

            while (true) {
                Socket socket = serverSocket.accept();
                executor.execute(new ClientHandler(socket, clientManager));
            }
        } catch (IOException e) {
            System.err.println("Server error: " + e.getMessage());
        }
    }
}

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
        try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter writer = new PrintWriter(socket.getOutputStream(), true)) {

            output = writer;
            clientManager.addClient(output);
            clientName = "Client-" + (System.currentTimeMillis() % 10000);

            clientManager.broadcast(clientName + " has joined the chat.");

            String message;
            while ((message = in.readLine()) != null) {
                if ("exit".equalsIgnoreCase(message.trim())) break;
                clientManager.broadcast(clientName + ": " + message);
            }
        } catch (Exception e) {
            // Client disconnected
        } finally {
            cleanup();
        }
    }

    private void cleanup() {
        if (output != null) clientManager.removeClient(output);
        if (clientName != null) clientManager.broadcast(clientName + " has left the chat.");
        closeQuietly(socket);
    }

    private void closeQuietly(AutoCloseable r) {
        try { if (r != null) r.close(); } catch (Exception ignored) {}
    }
}
