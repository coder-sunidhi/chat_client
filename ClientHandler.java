import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.UUID;

/**
 * Handles communication with a single client.
 */
public class ClientHandler implements Runnable {

    private final Socket socket;
    private final ClientManager clientManager;

    private BufferedReader input;
    private PrintWriter output;

    private String clientId;

    public ClientHandler(Socket socket,
                         ClientManager clientManager) {

        this.socket = socket;
        this.clientManager = clientManager;
    }

    @Override
    public void run() {

        try {

            initializeClient();

            processMessages();

        } catch (IOException e) {

            LoggerUtil.error(
                    "Client communication failed.",
                    e);

        } finally {

            disconnectClient();
        }
    }

    /**
     * Initializes the client.
     */
    private void initializeClient()
            throws IOException {

        input = new BufferedReader(
                new InputStreamReader(
                        socket.getInputStream()));

        output = new PrintWriter(
                socket.getOutputStream(),
                true);

        clientId = UUID.randomUUID()
                .toString()
                .substring(0, 8);

        clientManager.addClient(
                clientId,
                output);

        String joinMessage =
        clientId + " joined the chat.";

clientManager.broadcast(
        joinMessage);

LoggerUtil.info(
        joinMessage);

        LoggerUtil.info(
                clientId + " connected.");
    }

    /**
     * Reads incoming messages.
     */
    private void processMessages()
            throws IOException {

        String message;

        while ((message = input.readLine()) != null) {

            if ("exit".equalsIgnoreCase(
                    message.trim())) {

                break;
            }

            clientManager.broadcast(
                    clientId + " : " + message);
        }
    }

    /**
     * Disconnects the client safely.
     */
    private void disconnectClient() {

        clientManager.removeClient(
                clientId);

        String leaveMessage =
        clientId + " left the chat.";

clientManager.broadcast(
        leaveMessage);

LoggerUtil.info(
        leaveMessage);

        closeInput();

        closeOutput();

        closeSocket();

        LoggerUtil.info(
                clientId + " disconnected.");
    }

    /**
     * Closes BufferedReader.
     */
    private void closeInput() {

        if (input != null) {

            try {

                input.close();

            } catch (IOException e) {

                LoggerUtil.error(
                        "Unable to close input.",
                        e);
            }
        }
    }

    /**
     * Closes PrintWriter.
     */
    private void closeOutput() {

        if (output != null) {

            output.close();
        }
    }

    /**
     * Closes Socket.
     */
    private void closeSocket() {

        if (socket != null &&
                !socket.isClosed()) {

            try {

                socket.close();

            } catch (IOException e) {

                LoggerUtil.error(
                        "Unable to close socket.",
                        e);
            }
        }
    }
}
