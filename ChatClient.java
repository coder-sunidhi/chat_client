import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.*;

/**
 * Swing-based Chat Client
 */
public class ChatClient extends JFrame {

    private final String host;
    private final int port;

    private JTextArea chatArea;
    private JTextField messageField;
    private JButton connectBtn;
    private JButton disconnectBtn;
    private JLabel statusLabel;

    private Socket socket;
    private PrintWriter output;
    private BufferedReader input;
    private Thread receiveThread;
    private volatile boolean running = false;

    public ChatClient(String host, int port) {
        this.host = host;
        this.port = port;
        initUI();
    }

    /** Initializes the user interface */
    private void initUI() {
        setTitle("Chat Client");
        setSize(700, 550);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        setupComponents();
        layoutComponents();
        setupListeners();
        setVisible(true);
    }

    private void setupComponents() {
        chatArea = new JTextArea();
        chatArea.setEditable(false);
        chatArea.setFont(new Font("SansSerif", Font.PLAIN, 14));

        messageField = new JTextField();
        connectBtn = new JButton("Connect");
        disconnectBtn = new JButton("Disconnect");
        statusLabel = new JLabel("Disconnected");
        disconnectBtn.setEnabled(false);
    }

    private void layoutComponents() {
        add(new JScrollPane(chatArea), BorderLayout.CENTER);
        add(createTopPanel(), BorderLayout.NORTH);
        add(createBottomPanel(), BorderLayout.SOUTH);
    }

    private JPanel createTopPanel() {
        JPanel panel = new JPanel();
        panel.add(connectBtn);
        panel.add(disconnectBtn);
        return panel;
    }

    private JPanel createBottomPanel() {
        JPanel inputPanel = new JPanel(new BorderLayout());
        inputPanel.add(messageField, BorderLayout.CENTER);
        inputPanel.add(new JButton("Send"){{addActionListener(e -> sendMessage());}}, BorderLayout.EAST);

        JPanel panel = new JPanel(new BorderLayout());
        panel.add(inputPanel, BorderLayout.CENTER);
        panel.add(statusLabel, BorderLayout.SOUTH);
        return panel;
    }

    private void setupListeners() {
        connectBtn.addActionListener(e -> connectToServer());
        disconnectBtn.addActionListener(e -> disconnectFromServer());
        messageField.addActionListener(e -> sendMessage());
    }

    /** Connects to the server */
    private void connectToServer() {
        if (isConnected()) {
            JOptionPane.showMessageDialog(this, "Already connected!");
            return;
        }

        try {
            socket = new Socket(host, port);
            input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            output = new PrintWriter(socket.getOutputStream(), true);

            running = true;
            updateStatus("Connected", Color.GREEN);
            appendToChat("✅ Connected to server!");

            connectBtn.setEnabled(false);
            disconnectBtn.setEnabled(true);

            startReceiverThread();

        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Connection failed: " + e.getMessage());
        }
    }

    /** Disconnects from the server with proper cleanup */
    private void disconnectFromServer() {
        running = false;

        if (output != null) {
            try { output.println("exit"); } catch (Exception ignored) {}
        }

        if (receiveThread != null) {
            try {
                receiveThread.interrupt();
                receiveThread.join(500);
            } catch (Exception ignored) {}
        }

        closeAllResources();

        disconnectBtn.setEnabled(false);
        connectBtn.setEnabled(true);
        updateStatus("Disconnected", Color.RED);
        appendToChat("Disconnected from server.");
    }

    private void startReceiverThread() {
        receiveThread = new Thread(() -> {
            try {
                String msg;
                while (running && (msg = input.readLine()) != null) {
                    appendToChat(msg);
                }
            } catch (IOException e) {
                if (running) appendToChat("⚠️ Connection lost");
            }
        });
        receiveThread.start();
    }

    private void sendMessage() {
        String message = messageField.getText().trim();
        if (message.isEmpty() || output == null) return;

        output.println(message);
        appendToChat("You: " + message);
        messageField.setText("");
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

    private void appendToChat(String text) {
        SwingUtilities.invokeLater(() -> {
            chatArea.append(text + "\n");
            chatArea.setCaretPosition(chatArea.getDocument().getLength());
        });
    }

    private void updateStatus(String text, Color color) {
        statusLabel.setText(text);
        statusLabel.setForeground(color);
    }

    public static void main(String[] args) {
        String h = args.length > 0 ? args[0] : "localhost";
        int p = args.length > 1 ? Integer.parseInt(args[1]) : 5000;
        SwingUtilities.invokeLater(() -> new ChatClient(h, p));
    }
}
