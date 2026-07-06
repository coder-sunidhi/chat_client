import java.io.PrintWriter;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages connected clients.
 */
public class ClientManager {

    private final Map<String, PrintWriter> clients;

    public ClientManager() {

        clients = new ConcurrentHashMap<>();
    }

    /**
     * Adds a client.
     */
    public void addClient(
            String clientId,
            PrintWriter writer) {

        if (clientId != null &&
                writer != null) {

            clients.put(
                    clientId,
                    writer);
        }
    }

    /**
     * Removes a client.
     */
    public void removeClient(
            String clientId) {

        if (clientId != null) {

            clients.remove(clientId);
        }
    }

    /**
     * Broadcasts a message.
     */
    public void broadcast(
            String message) {

        for (PrintWriter writer :
                clients.values()) {

            try {

                writer.println(message);

            } catch (Exception ignored) {

            }
        }
    }

    /**
     * Returns number of clients.
     */
    public int getClientCount() {

        return clients.size();
    }
}
