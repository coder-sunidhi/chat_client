import java.io.BufferedReader;
import java.io.IOException;

/**
 * Receives messages from the server.
 */
public class MessageReceiver
        implements Runnable {

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

            String message;

            while ((message =
                    input.readLine()) != null) {

                client.appendMessage(
                        message);
            }

        } catch (IOException e) {

            client.appendMessage(
                    "Connection Lost.");

        } finally {

            client.serverDisconnected();
        }
    }
}
