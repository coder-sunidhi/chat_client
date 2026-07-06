import java.io.*;
import java.net.*;
import java.util.concurrent.*;

public class ChatServer {

    private static final int PORT = 5000;
    
    // Best practice: CopyOnWriteArraySet for thread-safe iteration
    private static final Set<PrintWriter> clientWriters = 
            new CopyOnWriteArraySet<>();

    public static void main(String[] args) {
        System.out.println("Server starting...");

        ExecutorService executor = Executors.newCachedThreadPool();

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("✅ Server started on port " + PORT);
            System.out.println("Waiting for clients...");

            while (true) {
                Socket clientSocket = serverSocket.accept();
                executor.execute(new ClientHandler(clientSocket));
            }
        } catch (IOException e) {
            System.err.println("Server error: " + e.getMessage());
        } finally {
            executor.shutdown();
        }
    }

    public static void broadcast(String message) {
        for (PrintWriter writer : clientWriters) {
            writer.println(message);
        }
    }

    private static class ClientHandler implements Runnable {
        private final Socket socket;
        private PrintWriter out;
        private BufferedReader in;
        private String clientName;

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try {
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);

                clientWriters.add(out);
                clientName = "Client-" + clientWriters.size();

                System.out.println(clientName + " connected");
                broadcast(clientName + " has joined the chat.");

                String message;
                while ((message = in.readLine()) != null) {
                    if ("exit".equalsIgnoreCase(message.trim())) {
                        break;
                    }
                    broadcast(clientName + ": " + message);
                }
            } catch (IOException e) {
                // Client disconnected abruptly
            } finally {
                cleanup();
            }
        }

        private void cleanup() {
            clientWriters.remove(out);
            if (clientName != null) {
                broadcast(clientName + " has left the chat.");
            }
            closeQuietly(socket);
        }

        private void closeQuietly(AutoCloseable resource) {
            try {
                if (resource != null) {
                    resource.close();
                }
            } catch (Exception ignored) {}
        }
    }
}
