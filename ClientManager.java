import java.io.PrintWriter;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

public class ClientManager {

    private final Set<PrintWriter> clients =
            new CopyOnWriteArraySet<>();

    public void addClient(
            PrintWriter writer) {

        clients.add(writer);
    }

    public void removeClient(
            PrintWriter writer) {

        clients.remove(writer);
    }

    public void broadcast(
            String message) {

        for (PrintWriter writer : clients) {

            try {
                writer.println(message);
            } catch (Exception ignored) {
            }
        }
    }
}
