import java.io.*;
import java.net.*;

public class ChatServer {

    // Constant instead of hardcoding
    private static final int PORT = 5000;

    public static void main(String[] args) {

        System.out.println("Server starting...");

        try (
                ServerSocket serverSocket =
                        new ServerSocket(PORT)
        ) {

            System.out.println(
                    "Server started on port "
                    + PORT
            );

            System.out.println(
                    "Waiting for client..."
            );

            try (

                    Socket socket =
                            serverSocket.accept();

                    BufferedReader input =
                            new BufferedReader(
                                    new InputStreamReader(
                                            socket.getInputStream()
                                    )
                            );

                    PrintWriter output =
                            new PrintWriter(
                                    socket.getOutputStream(),
                                    true
                            )

            ) {

                System.out.println(
                        "Client connected!"
                );

                String message;

                while ((message =
                        input.readLine()) != null) {

                    System.out.println(
                            "Client: "
                            + message
                    );

                    output.println(
                            "Message received: "
                            + message
                    );

                    if(message.equalsIgnoreCase(
                            "exit")) {

                        System.out.println(
                                "Client disconnected"
                        );

                        break;
                    }
                }

            }

        }

        catch(IOException e){

            System.out.println(
                    "Server Error: "
                    + e.getMessage()
            );
        }

        finally {

            System.out.println(
                    "Server stopped."
            );
        }
    }
}
