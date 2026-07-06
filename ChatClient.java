import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.*;

public class ChatClient extends JFrame {

    private final String host;
    private final int port;

    private final JTextArea chatArea = new JTextArea();
    private final JTextField messageField = new JTextField();
    private final JButton connectButton = new JButton("Connect");
    private final JButton disconnectButton = new JButton("Disconnect");
    private final JLabel statusLabel = new JLabel("Disconnected");

    private Socket socket;
    private PrintWriter output;
    private BufferedReader input;
    private Thread receiveThread;
    private volatile boolean running = false;

    public ChatClient(String host, int port) {
        this.host = host;
        this.port = port;
        initializeUI();
    }

    private void initializeUI() { /* Same as previous version - UI setup */ 
        // (Keeping it short for clarity - use previous UI code)
        setTitle("Simple Chat Client");
        setSize(700, 550);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        chatArea.setEditable(false);
        add(new JScrollPane(chatArea), BorderLayout.CENTER);
        add(createTopPanel(), BorderLayout.NORTH);
        add(createBottomPanel(), BorderLayout.SOUTH);
        setupListeners();
        setVisible(true);
    }

    // ... (UI helper methods same as last version)

    private void connect() { /* same as before */ }

    private void startReceiverThread() { /* same as before */ }

    private void disconnect() {
        running = false;

        if (output != null) {
            try { output.println("exit"); } catch (Exception ignored) {}
        }

        // More robust thread stopping
        if (receiveThread != null && receiveThread.isAlive()) {
            try {
                receiveThread.interrupt();
                receiveThread.join(800);
            } catch (InterruptedException | Exception e) {
                // Catch any exception during stopping
                System.out.println("Thread stop handled");
            }
        }

        closeAllResources();

        disconnectButton.setEnabled(false);
        connectButton.setEnabled(true);

        updateStatus("Disconnected", Color.RED);
        appendToChat("Disconnected from server.");
    }

    private void closeAllResources() {
        closeQuietly(output);
        closeQuietly(input);
        closeQuietly(socket);
        output = null;
        input = null;
        socket = null;
    }

    private void closeQuietly(AutoCloseable r) {
        try { if (r != null) r.close(); } catch (Exception ignored) {}
    }

    // Rest of methods (appendToChat, sendMessage, etc.) same as before
    // ... 
}
