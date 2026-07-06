import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.*;

public class ChatClient extends JFrame {

    private final String host;
    private final int port;

    private JTextArea chatArea;
    private JTextField messageField;
    private JButton connectBtn, disconnectBtn;
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

    private void initUI() {
        setTitle("Chat Client");
        setSize(700, 550);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        chatArea = new JTextArea();
        chatArea.setEditable(false);
        chatArea.setFont(new Font("SansSerif", Font.PLAIN, 14));

        messageField = new JTextField();
        connectBtn = new JButton("Connect");
        disconnectBtn = new JButton("Disconnect");
        statusLabel = new JLabel("Disconnected");

        add(new JScrollPane(chatArea), BorderLayout.CENTER);
        add(createTopPanel(), BorderLayout.NORTH);
        add(createInputPanel(), BorderLayout.SOUTH);

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

    private JPanel createInputPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(messageField, BorderLayout.CENTER);
        panel.add(new JButton("Send"){{addActionListener(e -> sendMessage());}}, BorderLayout.EAST);

        JPanel container = new JPanel(new BorderLayout());
        container.add(panel, BorderLayout.CENTER);
        container.add(statusLabel, BorderLayout.SOUTH);
        return container;
    }

    private void setupListeners() {
        connectBtn.addActionListener(e -> connectToServer());
        disconnectBtn.addActionListener(e -> disconnectFromServer());
        messageField.addActionListener(e -> sendMessage());
    }

    private void connectToServer() {
        if (isConnected()) return;

        try {
            socket = new Socket(host, port);
            input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            output = new PrintWriter(socket.getOutputStream(), true);

            running = true;
            updateStatus("Connected", Color.GREEN);
            appendToChat("✅ Connected to server!");

            connectBtn.setEnabled(false);
            disconnectBtn.setEnabled(true);

            startReceiver();

        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Connection failed: " + e.getMessage());
        }
    }

    private void startReceiver() {
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

    private void disconnectFromServer() {
        running = false;

        if (output != null) {
            try { output.println("exit"); } catch (Exception ignored) {}
        }

        // Robust thread shutdown
        if (receiveThread != null) {
            try {
                receiveThread.interrupt();
                receiveThread.join(700);
            } catch (Exception e) {
                // Thread stopped
            }
        }

        closeAllResources();

        disconnectBtn.setEnabled(false);
        connectBtn.setEnabled(true);
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

    private boolean isConnected() {
        return socket != null && !socket.isClosed();
    }

    private void appendToChat(String text) {
        SwingUtilities.invokeLater(() -> {
            chatArea.append(text + "\n");
            chatArea.setCaretPosition(chatArea.getDocument().getLength());
        });
    }

    private void sendMessage() {
        String msg = messageField.getText().trim();
        if (msg.isEmpty() || output == null) return;
        output.println(msg);
        appendToChat("You: " + msg);
        messageField.setText("");
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
