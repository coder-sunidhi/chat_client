import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Chat Server
 * Accepts multiple client connections and broadcasts messages.
 */
public class ChatServer {

    private static final int DEFAULT_PORT = 5000;

    public static void main(String[] args) {

        int port = DEFAULT_PORT;

        if (args.length > 0) {
            port = Integer.parseInt(args[0]);
        }

        ExecutorService executorService =
                Executors.newFixedThreadPool(
                        Runtime.getRuntime().availableProcessors() * 2);

        ClientManager clientManager =
                new ClientManager();

        System.out.println("Starting server...");

        try (ServerSocket serverSocket =
                     new ServerSocket(port)) {

            System.out.println("Server started on port "
                    + port);

            while (true) {

                try {

                    Socket clientSocket =
                            serverSocket.accept();

                    ClientHandler handler =
                            new ClientHandler(
                                    clientSocket,
                                    clientManager);

                    executorService.execute(handler);

                } catch (IOException e) {

                    System.out.println(
                            "Client connection error : "
                                    + e.getMessage());
                }
            }

        } catch (IOException e) {

            System.out.println(
                    "Server error : "
                            + e.getMessage());

        } finally {

            executorService.shutdown();

            try {

                if (!executorService.awaitTermination(
                        5,
                        TimeUnit.SECONDS)) {

                    executorService.shutdownNow();
                }

            } catch (InterruptedException e) {

                executorService.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * Thread-safe client manager.
     */
    static class ClientManager {

        private final List<PrintWriter> clients =
                Collections.synchronizedList(
                        new ArrayList<>());

        /**
         * Adds a client.
         */
        public synchronized void addClient(
                PrintWriter writer) {

            clients.add(writer);
        }

        /**
         * Removes a client.
         */
        public synchronized void removeClient(
                PrintWriter writer) {

            clients.remove(writer);
        }

        /**
         * Broadcasts message to all clients.
         */
        public synchronized void broadcast(
                String message) {

            synchronized (clients) {

                for (PrintWriter writer : clients) {

                    try {

                        writer.println(message);

                    } catch (Exception ignored) {
                    }
                }
            }
        }
    }

    /**
     * Handles one client connection.
     */
    static class ClientHandler
            implements Runnable {

        private final Socket socket;

        private final ClientManager clientManager;

        private PrintWriter outputWriter;

        private String clientName;

        public ClientHandler(
                Socket socket,
                ClientManager clientManager) {

            this.socket = socket;
            this.clientManager = clientManager;
        }

        @Override
        public void run() {

            BufferedReader inputReader = null;

            try {

                inputReader =
                        new BufferedReader(
                                new InputStreamReader(
                                        socket.getInputStream()));

                outputWriter =
                        new PrintWriter(
                                socket.getOutputStream(),
                                true);

                clientManager.addClient(
                        outputWriter);

                clientName =
                        "Client-"
                                + UUID.randomUUID()
                                .toString()
                                .substring(0,8);

                clientManager.broadcast(
                        clientName
                                + " joined the chat.");

                String message;

                while (true) {

                    message =
                            inputReader.readLine();

                    if (message == null) {
                        break;
                    }

                    if (message.equalsIgnoreCase(
                            "exit")) {
                        break;
                    }

                    clientManager.broadcast(
                            clientName
                                    + " : "
                                    + message);
                }

            } catch (IOException e) {

                System.out.println(
                        clientName
                                + " disconnected.");

            } finally {                if (outputWriter != null) {

                    clientManager.removeClient(
                            outputWriter);

                    clientManager.broadcast(
                            clientName
                                    + " left the chat.");
                }

                if (inputReader != null) {

                    try {

                        inputReader.close();

                    } catch (IOException e) {

                        System.out.println(
                                "Error closing input stream.");
                    }
                }

                if (outputWriter != null) {

                    outputWriter.close();
                }

                if (socket != null &&
                        !socket.isClosed()) {

                    try {

                        socket.close();

                    } catch (IOException e) {

                        System.out.println(
                                "Error closing socket.");
                    }
                }
            }
        }
    }
}
