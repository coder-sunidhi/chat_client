import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.UUID;

/**
 * Handles communication with one client.
 */
public class ClientHandler implements Runnable {

    private final Socket socket;
    private final ClientManager clientManager;

    private BufferedReader input;
    private PrintWriter output;

    private String clientName;

    public ClientHandler(
            Socket socket,
            ClientManager clientManager) {

        this.socket = socket;
        this.clientManager = clientManager;
    }

    @Override
    public void run() {

        try {

            initializeClient();

            receiveMessages();

        } catch (IOException e) {

            System.out.println(
                    "Connection Error : "
                            + e.getMessage());

        } finally {

            disconnectClient();
        }
    }

    /**
     * Initializes client resources.
     */
    private void initializeClient()
            throws IOException {

        input =
                new BufferedReader(
                        new InputStreamReader(
                                socket.getInputStream()));

        output =
                new PrintWriter(
                        socket.getOutputStream(),
                        true);

        clientManager.addClient(output);

        clientName =
                "Client-"
                        + UUID.randomUUID()
                        .toString()
                        .substring(0, 8);

        clientManager.broadcast(
                clientName
                        + " joined the chat.");

        System.out.println(
                clientName
                        + " connected.");
    }

    /**
     * Receives messages continuously.
     */
    private void receiveMessages()
            throws IOException {

        String message;

        while ((message =
                input.readLine()) != null) {

            if (message.trim()
                    .equalsIgnoreCase("exit")) {

                break;
            }

            clientManager.broadcast(
                    clientName
                            + " : "
                            + message);
        }
    }

    /**
     * Disconnects client safely.
     */
    private void disconnectClient() {

        try {

            if (output != null) {

                clientManager.removeClient(output);

                clientManager.broadcast(
                        clientName
                                + " left the chat.");

                output.close();
            }

            if (input != null) {

                input.close();
            }

            if (socket != null &&
                    !socket.isClosed()) {

                socket.close();
            }

            System.out.println(
                    clientName
                            + " disconnected.");

        } catch (IOException e) {

            System.out.println(
                    "Cleanup Error : "
                            + e.getMessage());
        }
    }
}
