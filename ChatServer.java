import java.io.*;
import java.net.*;
import java.util.concurrent.*;

public class ChatServer {

    public static void main(String[] args) {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 5000;

        System.out.println("Server starting on port " + port + "...");

        ExecutorService executor = Executors.newCachedThreadPool();
        Set<PrintWriter> clientWriters = new CopyOnWriteArraySet<>();

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("✅ Server started successfully!");

            while (true) {
                Socket clientSocket = serverSocket.accept();
                executor.execute(new ClientHandler(clientSocket, clientWriters));
            }
        } catch (IOException e) {
            System.err.println("Server error: " + e.getMessage());
        } finally {
            executor.shutdown();
        }
    }

    public static void broadcast(String message, Set<PrintWriter> writers) {
        for (PrintWriter writer : writers) {
            writer.println(message);
        }
    }
}

class ClientHandler implements Runnable {
    private final Socket socket;
    private final Set<PrintWriter> clientWriters;
    private PrintWriter out;
    private String clientName;

    public ClientHandler(Socket socket, Set<PrintWriter> clientWriters) {
        this.socket = socket;
        this.clientWriters = clientWriters;
    }

    @Override
    public void run() {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter writer = new PrintWriter(socket.getOutputStream(), true)) {

            out = writer;
            clientWriters.add(out);
            clientName = "Client-" + clientWriters.size();

            System.out.println(clientName + " connected");
            ChatServer.broadcast(clientName + " has joined the chat.", clientWriters);

            String message;
            while ((message = in.readLine()) != null) {
                if ("exit".equalsIgnoreCase(message.trim())) {
                    break;
                }
                ChatServer.broadcast(clientName + ": " + message, clientWriters);
            }
        } catch (Exception e) {                    // ← Proper broad exception handling
            System.out.println("Error with " + clientName + ": " + e.getMessage());
        } finally {
            cleanup();
        }
    }

    private void cleanup() {
        if (out != null) {
            clientWriters.remove(out);
        }
        if (clientName != null) {
            ChatServer.broadcast(clientName + " has left the chat.", clientWriters);
        }
        closeQuietly(socket);
    }

    private void closeQuietly(AutoCloseable resource) {
        try {
            if (resource != null) resource.close();
        } catch (Exception ignored) {}
    }
}
