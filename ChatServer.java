import java.io.*;
import java.net.*;
import java.util.concurrent.*;
import java.util.logging.*;

public class ChatServer {

    private static final Logger logger = Logger.getLogger(ChatServer.class.getName());

    public static void main(String[] args) {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 5000;

        logger.info("Server starting on port " + port);

        int poolSize = Runtime.getRuntime().availableProcessors() * 2;
        ExecutorService executor = Executors.newFixedThreadPool(poolSize);

        ClientManager clientManager = new ClientManager();

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            logger.info("✅ Server started successfully!");

            while (true) {
                Socket socket = serverSocket.accept();
                executor.execute(new ClientHandler(socket, clientManager));
            }
        } catch (IOException e) {
            logger.severe("Server crashed: " + e.getMessage());
        }
    }
}

class ClientManager {
    private final Set<PrintWriter> writers = ConcurrentHashMap.newKeySet();

    public void addClient(PrintWriter w) { writers.add(w); }
    public void removeClient(PrintWriter w) { writers.remove(w); }

    public void broadcast(String msg) {
        for (PrintWriter w : writers) {
            try { w.println(msg); } catch (Exception ignored) {}
        }
    }
}

class ClientHandler implements Runnable {
    private final Socket socket;
    private final ClientManager manager;
    private PrintWriter out;
    private String name;

    public ClientHandler(Socket socket, ClientManager manager) {
        this.socket = socket;
        this.manager = manager;
    }

    @Override
    public void run() {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter writer = new PrintWriter(socket.getOutputStream(), true)) {

            out = writer;
            manager.addClient(out);
            name = "Client-" + (System.currentTimeMillis() % 10000);

            manager.broadcast(name + " joined");
            logger.info(name + " connected");

            String msg;
            while ((msg = in.readLine()) != null) {
                if ("exit".equalsIgnoreCase(msg.trim())) break;
                manager.broadcast(name + ": " + msg);
            }
        } catch (Exception e) {
            logger.warning(name + " error: " + e.getMessage());
        } finally {
            cleanup();
        }
    }

    private void cleanup() {
        if (out != null) manager.removeClient(out);
        if (name != null) manager.broadcast(name + " left");
        closeQuietly(socket);
    }

    private void closeQuietly(AutoCloseable r) {
        try { if (r != null) r.close(); } catch (Exception ignored) {}
    }
}
