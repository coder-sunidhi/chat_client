import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.*;

public class ChatClient extends JFrame {

    private final String host;
    private final int port;

    private final JTextArea chatArea = new JTextArea();
    private final JTextField messageField = new JTextField();
    private final JButton sendButton = new JButton("Send");
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

    private void initializeUI() {
        setTitle("Chat Client");
        setSize(700, 550);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        chatArea.setEditable(false);
        chatArea.setFont(new Font("SansSerif", Font.PLAIN, 14));

        add(new JScrollPane(chatArea), BorderLayout.CENTER);
        add(createTopPanel(), BorderLayout.NORTH);
        add(createBottomPanel(), BorderLayout.SOUTH);

        setupListeners();
        setVisible(true);
    }

    private JPanel createTopPanel() {
        JPanel p = new JPanel();
        p.add(connectButton);
        p.add(disconnectButton);
        return p;
    }

    private JPanel createBottomPanel() {
        JPanel bottom = new JPanel(new BorderLayout());
        bottom.add(messageField, BorderLayout.CENTER);
        bottom.add(sendButton, BorderLayout.EAST);

        JPanel south = new JPanel(new BorderLayout());
        south.add(bottom, BorderLayout.CENTER);
        south.add(statusLabel, BorderLayout.SOUTH);
        return south;
    }

    private void setupListeners() {
        connectButton.addActionListener(e -> connect());
        disconnectButton.addActionListener(e -> disconnect());
        sendButton.addActionListener(e -> sendMessage());
        messageField.addActionListener(e -> sendMessage());
        disconnectButton.setEnabled(false);
    }

    private void connect() {
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

            connectButton.setEnabled(false);
            disconnectButton.setEnabled(true);

            startReceiverThread();

        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Connection failed: " + e.getMessage());
        }
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

    private void disconnect() {
        running = false;
        if (output != null) {
            try { output.println("exit"); } catch (Exception ignored) {}
        }

        if (receiveThread != null) {
            try { receiveThread.join(500); } catch (InterruptedException ignored) {}
        }

        closeResources();

        // Ensure button is disabled
        disconnectButton.setEnabled(false);   // ← Fixed as per feedback
        connectButton.setEnabled(true);

        updateStatus("Disconnected", Color.RED);
        appendToChat("Disconnected from server.");
    }

    private void closeResources() {
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
        String host = args.length > 0 ? args[0] : "localhost";
        int port = args.length > 1 ? Integer.parseInt(args[1]) : 5000;
        SwingUtilities.invokeLater(() -> new ChatClient(host, port));
    }
}
