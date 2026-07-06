import java.io.*;
import java.net.*;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Simple Multi Client Chat Server
 */
public class ChatServer {

    private static final int PORT = 5000;

    private static final ExecutorService executor =
            Executors.newCachedThreadPool();

    private static final ClientManager clientManager =
            new ClientManager();

    public static void main(String[] args) {

        System.out.println("=================================");
        System.out.println("      CHAT SERVER STARTED");
        System.out.println("=================================");

        try (ServerSocket serverSocket =
                     new ServerSocket(PORT)) {

            System.out.println("Listening on Port : " + PORT);

            while (true) {

                Socket clientSocket =
                        serverSocket.accept();

                System.out.println(
                        "New Client Connected : "
                                + clientSocket.getInetAddress());

                executor.execute(
                        new ClientHandler(
                                clientSocket,
                                clientManager));
            }

        } catch (IOException e) {

            System.out.println(
                    "Server Error : "
                            + e.getMessage());

        } finally {

            executor.shutdown();
        }
    }

    /**
     * Stores connected clients.
     */
    static class ClientManager {

        private final Set<PrintWriter> clients =
                new CopyOnWriteArraySet<>();

        /**
         * Adds a client.
         */
        public void addClient(
                PrintWriter writer) {

            clients.add(writer);
        }

        /**
         * Removes a client.
         */
        public void removeClient(
                PrintWriter writer) {

            clients.remove(writer);
        }

        /**
         * Broadcasts message.
         */
        public void broadcast(
                String message) {

            for (PrintWriter writer : clients) {

                writer.println(message);
            }
        }
    }

    /**
     * Handles one connected client.
     */
    static class ClientHandler
            implements Runnable {

        private final Socket socket;

        private final ClientManager manager;

        private PrintWriter output;

        private BufferedReader input;

        private String clientName;

        public ClientHandler(
                Socket socket,
                ClientManager manager) {

            this.socket = socket;
            this.manager = manager;
        }

        @Override
        public void run() {

            try {

                input =
                        new BufferedReader(
                                new InputStreamReader(
                                        socket.getInputStream()));

                output =
                        new PrintWriter(
                                socket.getOutputStream(),
                                true);

                manager.addClient(output);

                clientName =
                        "Client-"
                                + UUID.randomUUID()
                                .toString()
                                .substring(0,8);

                manager.broadcast(
                        clientName
                                + " joined the chat.");

                String message;

                while ((message =
                        input.readLine()) != null) {

                    if (message.equalsIgnoreCase(
                            "exit")) {

                        break;
                    }

                    manager.broadcast(
                            clientName
                                    + " : "
                                    + message);
                }

            } catch (IOException e) {

                System.out.println(
                        clientName
                                + " disconnected.");

            } finally {                if (output != null) {

                    manager.removeClient(output);

                    manager.broadcast(
                            clientName
                                    + " left the chat.");

                    output.close();
                }

                try {

                    if (input != null) {

                        input.close();
                    }

                } catch (IOException e) {

                    System.out.println(
                            "Unable to close input stream.");
                }

                try {

                    if (socket != null &&
                            !socket.isClosed()) {

                        socket.close();
                    }

                } catch (IOException e) {

                    System.out.println(
                            "Unable to close socket.");
                }
            }
        }
    }
}
