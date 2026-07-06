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

        try (ServerSocket serverSocket =
         new ServerSocket(port)) {

    System.out.println(
            "Server started on port "
                    + port);

    while (true) {

        try {

            Socket socket =
                    serverSocket.accept();

            executor.execute(
                    new ClientHandler(
                            socket,
                            clientManager));

        } catch (IOException e) {

            System.err.println(
                    "Client accept error: "
                            + e.getMessage());
        }
    }

} catch (IOException e) {

    System.err.println(
            "Server startup error: "
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
}} catch (IOException e) {

            System.err.println(
                    "Server error: "
                            + e.getMessage());

        } finally {

            executor.shutdown();
        }
    }
}
