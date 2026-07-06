import java.io.*;
import java.net.*;
import java.util.concurrent.*;

public class ChatServer {

    public static void main(String[] args) {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 5000;

        System.out.println("Server starting on port " + port + "...");

        ExecutorService executor = Executors.newCachedThreadPool();
        ClientManager clientManager = new ClientManager();

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("✅ Server started successfully!");

            while (true) {
                Socket socket = serverSocket.accept();
                executor.execute(new ClientHandler(socket, clientManager));
            }
        } catch (IOException e) {
            System.err.println("Server error: " + e.getMessage());
        } finally {
            executor.shutdown();
        }
    }
}

// Responsible only for managing clients
class ClientManager {
    private final Set<PrintWriter> writers = new CopyOnWriteArraySet<>();

    public synchronized void addClient(PrintWriter writer) {   // Added synchronized
        writers.add(writer);
    }

    public synchronized void removeClient(PrintWriter writer) { // Added synchronized
        writers.remove(writer);
    }

    public void broadcast(String message) {
        for (PrintWriter writer : writers) {
            writer.println(message);
        }
    }
}

// Focused on handling one client
class ClientHandler implements Runnable {
    private final Socket socket;
    private final ClientManager clientManager;
    private PrintWriter out;
    private String clientName;

    public ClientHandler(Socket socket, ClientManager clientManager) {
        this.socket = socket;
        this.clientManager = clientManager;
    }

    @Override
    public void run() {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter writer = new PrintWriter(socket.getOutputStream(), true)) {

            out = writer;
            clientManager.addClient(out);
            clientName = "Client-" + (System.currentTimeMillis() % 10000);

            System.out.println(clientName + " connected");
            clientManager.broadcast(clientName + " has joined the chat.");

            String message;
            while ((message = in.readLine()) != null) {
                if ("exit".equalsIgnoreCase(message.trim())) {
                    break;
                }
                clientManager.broadcast(clientName + ": " + message);
            }
        } catch (Exception e) {
            System.out.println("Error with " + clientName);
        } finally {
            cleanup();
        }
    }

    private void cleanup() {
        if (out != null) {
            clientManager.removeClient(out);
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
