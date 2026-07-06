import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Main server class.
 */
public class ChatServer {

    private static final int DEFAULT_PORT = 5000;

    private final ExecutorService executorService;
    private final ClientManager clientManager;

    public ChatServer() {

        executorService =
                Executors.newCachedThreadPool();

        clientManager =
                new ClientManager();
    }

    /**
     * Starts the server.
     */
    public void startServer(int port) {

        try (ServerSocket serverSocket =
                     new ServerSocket(port)) {

            LoggerUtil.info(
                    "Server started on port "
                            + port);

            while (true) {

                Socket socket =
                        serverSocket.accept();

                LoggerUtil.info(
                        "Client connected : "
                                + socket.getInetAddress());

                ClientHandler handler =
                        new ClientHandler(
                                socket,
                                clientManager);

                executorService.execute(
                        handler);
            }

        } catch (IOException e) {

            LoggerUtil.error(
                    "Server Error",
                    e);

        } finally {

            shutdownExecutor();
        }
    }

    /**
     * Stops thread pool.
     */
    private void shutdownExecutor() {

        executorService.shutdown();

        LoggerUtil.info(
                "Executor Service Stopped.");
    }

    /**
     * Application entry point.
     */
    public static void main(
            String[] args) {

        int port = DEFAULT_PORT;

        if (args.length > 0) {

            try {

                port =
                        Integer.parseInt(
                                args[0]);

            } catch (NumberFormatException e) {

                LoggerUtil.warning(
                        "Invalid port. Using default.");
            }
        }

        ChatServer server =
                new ChatServer();

        server.startServer(port);
    }
}
