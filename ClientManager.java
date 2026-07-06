import java.io.PrintWriter;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Manages all connected clients.
 */
public class ClientManager {

    private final Set<PrintWriter> clients;

    public ClientManager() {

        clients = new CopyOnWriteArraySet<>();
    }

    /**
     * Adds a client.
     */
    public void addClient(PrintWriter writer) {

        if (writer != null) {

            clients.add(writer);
        }
    }

    /**
     * Removes a client.
     */
    public void removeClient(PrintWriter writer) {

        if (writer != null) {

            clients.remove(writer);
        }
    }

    /**
     * Broadcasts a message to every client.
     */
    public void broadcast(String message) {

        for (PrintWriter writer : clients) {

            try {

                writer.println(message);

            } catch (Exception e) {

                System.out.println(
                        "Broadcast Error : "
                                + e.getMessage());
            }
        }
    }

    /**
     * Returns number of connected clients.
     */
    public int getClientCount() {

        return clients.size();
    }
}
