import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.*;

/**
 * Chat Client with improved connection management
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
    private Thread receiverThread;
    private volatile boolean isRunning = false;

    public ChatClient(String host, int port) {
        this.serverHost = host;
        this.serverPort = port;
        initializeUI();
    }

    private void initializeUI() {
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
        add(createBottomPanel(), BorderLayout.SOUTH);

        setupListeners();
        setVisible(true);
    }

    private JPanel createTopPanel() {
        JPanel p = new JPanel();
        p.add(connectBtn);
        p.add(disconnectBtn);
        disconnectBtn.setEnabled(false);
        return p;
    }

    private JPanel createBottomPanel() {
        JPanel inputPanel = new JPanel(new BorderLayout());
        inputPanel.add(messageInput, BorderLayout.CENTER);
        inputPanel.add(new JButton("Send"){{addActionListener(e -> sendMessage());}}, BorderLayout.EAST);

        JPanel panel = new JPanel(new BorderLayout());
        panel.add(inputPanel, BorderLayout.CENTER);
        panel.add(statusLabel, BorderLayout.SOUTH);
        return panel;
    }

    private void setupListeners() {
        connectBtn.addActionListener(e -> connectToServer());
        disconnectBtn.addActionListener(e -> disconnectFromServer());
        messageInput.addActionListener(e -> sendMessage());
    }

    private void connectToServer() {
        if (isConnected()) return;

        try {
            socket = new Socket(serverHost, serverPort);
            input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            output = new PrintWriter(socket.getOutputStream(), true);

            isRunning = true;
            updateStatus("Connected", Color.GREEN);
            appendMessage("✅ Connected to server!");

            connectBtn.setEnabled(false);
            disconnectBtn.setEnabled(true);

            startReceiverThread();

        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Connection failed: " + e.getMessage());
        }
    }

    private void startReceiverThread() {
        receiverThread = new Thread(() -> {
            try {
                String msg;
                while (isRunning && (msg = input.readLine()) != null) {
                    appendMessage(msg);
                }
            } catch (IOException e) {
                if (isRunning) appendMessage("⚠️ Connection lost");
            }
        });
        receiverThread.start();
    }

    private void disconnectFromServer() {
        isRunning = false;

        if (output != null) {
            try { output.println("exit"); } catch (Exception ignored) {}
        }

        if (receiverThread != null) {
            try {
                receiverThread.interrupt();
                receiverThread.join(600);
            } catch (Exception ignored) {}
        }

        // Finally block for resource cleanup
        try {
            closeAllResources();
        } finally {
            disconnectBtn.setEnabled(false);
            connectBtn.setEnabled(true);
            updateStatus("Disconnected", Color.RED);
            appendMessage("Disconnected from server.");
        }
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

    private void appendMessage(String msg) {
        SwingUtilities.invokeLater(() -> {
            chatDisplay.append(msg + "\n");
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

    private void updateStatus(String text, Color color) {
        statusLabel.setText(text);
        statusLabel.setForeground(color);
    }

    public static void main(String[] args) {
        String host = args.length > 0 ? args[0] : "localhost";
        int port = args.length > 1 ? Integer.parseInt(args[1]) : 5000;
        SwingUtilities.invokeLater(() -> new ChatClient(host, port));
    }
}
