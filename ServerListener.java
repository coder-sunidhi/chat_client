import java.io.BufferedReader;
import java.io.IOException;

public class ServerListener
        implements Runnable {

    private final BufferedReader input;
    private final ChatClient client;

    public ServerListener(
            BufferedReader input,
            ChatClient client) {

        this.input = input;
        this.client = client;
    }

    @Override
    public void run() {

        try {

            String msg;

            while ((msg =
                    input.readLine())
                    != null) {

                client.appendToChat(msg);
            }

        } catch (IOException e) {

            client.serverDisconnected();
        }
    }
}
