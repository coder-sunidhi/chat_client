import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.*;

/**
 * GUI Chat Client with robust connection management
 */
public class ChatClient extends JFrame {

    private final String serverHost;
    private final int serverPort;

    private JTextArea chatDisplay;
    private JTextField messageInput;
    private JButton connectBtn;
    private JButton disconnectBtn;
    private JLabel statusLabel;

    private Socket socket;
    private PrintWriter output;
    private BufferedReader input;
    private Thread messageReceiverThread;
    private volatile boolean isRunning = false;

    public ChatClient(String host, int port) {
        this.serverHost = host;
        this.serverPort = port;
        initializeUserInterface();
    }

    private void initializeUserInterface() {
        setTitle("Chat Client");
        setSize(700, 550);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        chatDisplay = new JTextArea();
        chatDisplay.setEditable(false);
        chatDisplay.setFont(new Font("SansSerif", Font.PLAIN, 14));

        messageInput = new JTextField();
        connectBtn = new JButton("Connect");
        disconnectBtn = new JButton("Disconnect");
        statusLabel = new JLabel("Disconnected");

        add(new JScrollPane(chatDisplay), BorderLayout.CENTER);
        add(createTopPanel(), BorderLayout.NORTH);
        add(createInputPanel(), BorderLayout.SOUTH);

        setupEventListeners();
        setVisible(true);
    }

    private JPanel createTopPanel() {
        JPanel panel = new JPanel();
        panel.add(connectBtn);
        panel.add(disconnectBtn);
        disconnectBtn.setEnabled(false);
        return panel;
    }

    private JPanel createInputPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(messageInput, BorderLayout.CENTER);
        panel.add(new JButton("Send") {{ addActionListener(e -> sendMessage()); }}, BorderLayout.EAST);

        JPanel container = new JPanel(new BorderLayout());
        container.add(panel, BorderLayout.CENTER);
        container.add(statusLabel, BorderLayout.SOUTH);
        return container;
    }

    private void setupEventListeners() {
        connectBtn.addActionListener(e -> connectToServer());
        disconnectBtn.addActionListener(e -> disconnectFromServer());
        messageInput.addActionListener(e -> sendMessage());
    }

    private void connectToServer() {
        if (isConnected()) {
            JOptionPane.showMessageDialog(this, "Already connected!");
            return;
        }

        try {
            socket = new Socket(serverHost, serverPort);
            input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            output = new PrintWriter(socket.getOutputStream(), true);

            isRunning = true;
            updateStatus("Connected", Color.GREEN);
            appendMessage("✅ Connected to server!");

            connectBtn.setEnabled(false);
            disconnectBtn.setEnabled(true);

            startMessageReceiver();

        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Connection failed: " + e.getMessage());
        }
    }

    private void startMessageReceiver() {
        messageReceiverThread = new Thread(() -> {
            try {
                String serverMessage;
                while (isRunning && (serverMessage = input.readLine()) != null) {
                    appendMessage(serverMessage);
                }
            } catch (IOException e) {
                if (isRunning) {
                    appendMessage("⚠️ Connection lost");
                }
            }
        });
        messageReceiverThread.start();
    }

    private void disconnectFromServer() {
        isRunning = false;

        if (output != null) {
            try { output.println("exit"); } catch (Exception ignored) {}
        }

        if (messageReceiverThread != null) {
            try {
                messageReceiverThread.interrupt();
                messageReceiverThread.join(800);
            } catch (Exception e) {
                // Thread stop handled safely
            }
        }

        closeAllResources();

        disconnectBtn.setEnabled(false);
        connectBtn.setEnabled(true);

        updateStatus("Disconnected", Color.RED);
        appendMessage("Disconnected from server.");
    }

    private void closeAllResources() {
        closeQuietly(output);
        closeQuietly(input);
        closeQuietly(socket);
        output = null;
        input = null;
        socket = null;
    }

    private void closeQuietly(AutoCloseable resource) {
        try {
            if (resource != null) resource.close();
        } catch (Exception ignored) {}
    }

    private boolean isConnected() {
        return socket != null && !socket.isClosed();
    }

    private void appendMessage(String message) {
        SwingUtilities.invokeLater(() -> {
            chatDisplay.append(message + "\n");
            chatDisplay.setCaretPosition(chatDisplay.getDocument().getLength());
        });
    }

    private void sendMessage() {
        String text = messageInput.getText().trim();
        if (text.isEmpty() || output == null) return;

        output.println(text);
        appendMessage("You: " + text);
        messageInput.setText("");
    }

    private void updateStatus(String status, Color color) {
        statusLabel.setText(status);
        statusLabel.setForeground(color);
    }

    public static void main(String[] args) {
        String host = args.length > 0 ? args[0] : "localhost";
        int port = args.length > 1 ? Integer.parseInt(args[1]) : 5000;

        SwingUtilities.invokeLater(() -> new ChatClient(host, port));
    }
}
