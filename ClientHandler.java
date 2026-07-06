import java.io.*;
import java.net.Socket;

public class ClientHandler implements Runnable {

    private final Socket socket;
    private final ClientManager manager;

    public ClientHandler(
            Socket socket,
            ClientManager manager) {

        this.socket = socket;
        this.manager = manager;
    }

    @Override
    public void run() {

        PrintWriter output = null;
        String clientName = null;

        try (
                BufferedReader input =
                        new BufferedReader(
                                new InputStreamReader(
                                        socket.getInputStream()))
        ) {

            output =
                    new PrintWriter(
                            socket.getOutputStream(),
                            true);

            manager.addClient(output);

            clientName =
                    "Client-"
                            + System.currentTimeMillis()
                            % 10000;

            manager.broadcast(
                    clientName
                            + " joined");

            String message;

            while ((message =
                    input.readLine())
                    != null) {

                if ("exit".equalsIgnoreCase(
                        message.trim()))
                    break;

                manager.broadcast(
                        clientName
                                + ": "
                                + message);
            }

        } catch (IOException e) {

            System.err.println(
                    "Client disconnected");

        } finally {

            if (output != null)
                manager.removeClient(output);

            if (clientName != null)
                manager.broadcast(
                        clientName
                                + " left");

            try {
                socket.close();
            } catch (IOException ignored) {
            }
        }
    }
}
