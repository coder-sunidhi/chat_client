import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.*;

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

    private void initUI() {
        setTitle("Chat Client");
        setSize(700, 550);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        chatArea = createChatArea();
        messageField = new JTextField();
        connectBtn = new JButton("Connect");
        disconnectBtn = new JButton("Disconnect");
        statusLabel = new JLabel("Disconnected");

        add(new JScrollPane(chatArea), BorderLayout.CENTER);
        add(createTopPanel(), BorderLayout.NORTH);
        add(createBottomPanel(), BorderLayout.SOUTH);

        setupListeners();
        setVisible(true);
    }

    private JTextArea createChatArea() {
        JTextArea area = new JTextArea();
        area.setEditable(false);
        area.setFont(new Font("SansSerif", Font.PLAIN, 14));
        return area;
    }

    private JPanel createTopPanel() {
        JPanel panel = new JPanel();
        panel.add(connectBtn);
        panel.add(disconnectBtn);
        disconnectBtn.setEnabled(false);
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

            startReceiverThread();

        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Connection failed: " + e.getMessage());
        }
    }

    private void disconnectFromServer() {
        running = false;

        if (output != null) {
            try { output.println("exit"); } catch (Exception ignored) {}
        }

        // Robust thread shutdown with try-catch
        if (receiveThread != null) {
            try {
                receiveThread.interrupt();
                receiveThread.join(800);
            } catch (Exception e) {
                System.out.println("Thread stop handled");
            }
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
