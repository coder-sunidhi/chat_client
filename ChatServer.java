import java.io.*;
import java.net.*;
import java.util.Set;
import java.util.concurrent.*;

public class ChatServer {

    private static final int DEFAULT_PORT = 5000;

    public static void main(String[] args) {

        int port = args.length > 0
                ? Integer.parseInt(args[0])
                : DEFAULT_PORT;

        System.out.println("Starting server on port " + port);

        ExecutorService executor =
                Executors.newFixedThreadPool(20);

        ClientManager clientManager =
                new ClientManager();

        try (ServerSocket serverSocket =
                     new ServerSocket(port)) {

            System.out.println(
                    "✅ Server started successfully");

            while (true) {
                Socket socket =
                        serverSocket.accept();

                executor.submit(
                        new ClientHandler(
                                socket,
                                clientManager));
            }

        } catch (IOException e) {

            System.err.println(
                    "Server error: "
                            + e.getMessage());

        } finally {

            executor.shutdown();

            try {
                if (!executor.awaitTermination(
                        5,
                        TimeUnit.SECONDS)) {

                    executor.shutdownNow();
                }

            } catch (InterruptedException e) {

                executor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }
}

class ClientManager {

    private final Set<PrintWriter> clients =
            new CopyOnWriteArraySet<>();

    public void addClient(
            PrintWriter writer) {

        clients.add(writer);
    }

    public void removeClient(
            PrintWriter writer) {

        clients.remove(writer);
    }

    public void broadcast(
            String message) {

        for (PrintWriter writer : clients) {
            writer.println(message);
        }
    }
}

class ClientHandler implements Runnable {

    private final Socket socket;
    private final ClientManager manager;

    private PrintWriter output;
    private String clientName;

    public ClientHandler(
            Socket socket,
            ClientManager manager) {

        this.socket = socket;
        this.manager = manager;
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

            manager.addClient(output);

            clientName =
                    "Client-"
                            + (System.currentTimeMillis()
                            % 10000);

            manager.broadcast(
                    clientName
                            + " joined the chat.");

            String message;

            while ((message =
                    input.readLine()) != null) {

                if ("exit".equalsIgnoreCase(
                        message.trim())) {
                    break;
                }

                manager.broadcast(
                        clientName
                                + ": "
                                + message);
            }

        } catch (IOException e) {

            System.err.println(
                    clientName
                            + " disconnected: "
                            + e.getMessage());

        } finally {

            cleanup();
        }
    }

    private void cleanup() {

        if (output != null) {
            manager.removeClient(output);
        }

        if (clientName != null) {
            manager.broadcast(
                    clientName
                            + " left the chat.");
        }

        closeQuietly(socket);
    }

    private void closeQuietly(
            AutoCloseable resource) {

        if (resource != null) {
            try {
                resource.close();
            } catch (Exception e) {
                System.err.println(
                        "Close error: "
                                + e.getMessage());
            }
        }
    }
}
