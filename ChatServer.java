import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ChatServer {

    public static final int DEFAULT_PORT = 5000;

    public static void main(String[] args) {

        int port = args.length > 0
                ? Integer.parseInt(args[0])
                : DEFAULT_PORT;

        ExecutorService executor =
                Executors.newFixedThreadPool(
                        Runtime.getRuntime()
                                .availableProcessors() * 2);

        ClientManager manager =
                new ClientManager();

        try (ServerSocket server =
                     new ServerSocket(port)) {

            System.out.println(
                    "Server running on port "
                            + port);

            while (true) {

                Socket socket =
                        server.accept();

                executor.execute(
                        new ClientHandler(
                                socket,
                                manager));
            }

        } catch (IOException e) {

            System.err.println(
                    "Server error: "
                            + e.getMessage());

        } finally {

            executor.shutdown();
        }
    }
}
