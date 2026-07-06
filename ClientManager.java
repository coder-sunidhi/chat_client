import java.io.PrintWriter;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages connected clients.
 */
public class ClientManager {

    private final ConcurrentHashMap<String, PrintWriter> clients;

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

            LoggerUtil.info(
                    "Client Added : "
                            + clientId);
        }
    }

    /**
     * Removes a client.
     */
    public void removeClient(
            String clientId) {

        if (clientId != null) {

            PrintWriter writer =
                    clients.remove(clientId);

            if (writer != null) {

                writer.close();
            }

            LoggerUtil.info(
                    "Client Removed : "
                            + clientId);
        }
    }

    /**
     * Sends a message to every connected client.
     */
    public void broadcast(
            String message) {

        Iterator<Map.Entry<String, PrintWriter>> iterator =
                clients.entrySet().iterator();

        while (iterator.hasNext()) {

            Map.Entry<String, PrintWriter> entry =
                    iterator.next();

            try {

                PrintWriter writer =
                        entry.getValue();

                writer.println(message);

                if (writer.checkError()) {

                    removeClient(
                            entry.getKey());
                }

            } catch (Exception e) {

                LoggerUtil.error(
                        "Broadcast Failed",
                        e);

                removeClient(
                        entry.getKey());
            }
        }
    }

    /**
     * Returns connected client count.
     */
    public int getClientCount() {

        return clients.size();
    }

}
