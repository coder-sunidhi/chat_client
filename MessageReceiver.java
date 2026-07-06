import java.io.BufferedReader;
import java.io.IOException;

/**
 * Receives messages from the server.
 */
public class MessageReceiver implements Runnable {

    private final BufferedReader input;

    private final ChatClient client;

    public MessageReceiver(
            BufferedReader input,
            ChatClient client) {

        this.input = input;
        this.client = client;
    }

    @Override
    public void run() {

        try {

            receiveMessages();

        } catch (IOException e) {

            LoggerUtil.error(
                    "Connection lost.",
                    e);

            client.appendMessage(
                    "⚠ Connection lost.");

        } finally {

            client.serverDisconnected();
        }
    }

    /**
     * Continuously receives messages.
     */
    private void receiveMessages()
            throws IOException {

        String message;

        while ((message =
                input.readLine()) != null) {

            client.appendMessage(message);
        }
    }
}
