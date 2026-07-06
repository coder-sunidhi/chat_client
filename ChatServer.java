import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ChatServer {

    private static final int PORT = 5000;

    public static void main(String[] args) {

        ExecutorService executor =
                Executors.newCachedThreadPool();

        ClientManager manager =
                new ClientManager();

        System.out.println("=================================");
        System.out.println(" Chat Server Started ");
        System.out.println(" Listening on Port : " + PORT);
        System.out.println("=================================");

        try (ServerSocket serverSocket =
                     new ServerSocket(PORT)) {

            while (true) {

                Socket clientSocket =
                        serverSocket.accept();

                System.out.println(
                        "Client Connected : "
                                + clientSocket.getInetAddress());

                ClientHandler handler =
                        new ClientHandler(
                                clientSocket,
                                manager);

                executor.execute(handler);
            }

        } catch (IOException e) {

            System.out.println(
                    "Server Error : "
                            + e.getMessage());

        } finally {

            executor.shutdown();
        }
    }
}
