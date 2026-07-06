import java.io.PrintWriter;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Manages connected clients.
 */
static class ClientManager {

    private final Set<PrintWriter> clients =
            new CopyOnWriteArraySet<>();

    private final Object lock =
            new Object();

    public void addClient(
            PrintWriter writer) {

        synchronized (lock) {
            clients.add(writer);
        }
    }

    public void removeClient(
            PrintWriter writer) {

        synchronized (lock) {
            clients.remove(writer);
        }
    }

    public void broadcast(
            String message) {

        synchronized (lock) {

            for (PrintWriter writer : clients) {

                try {

                    writer.println(message);

                } catch (Exception e) {

                    System.err.println(
                            "Broadcast error: "
                                    + e.getMessage());
                }
            }
        }
    }
}
