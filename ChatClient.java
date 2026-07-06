import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.*;
import java.util.concurrent.*;

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
    private Thread receiverThread;
    private volatile boolean running = false;
    private ScheduledExecutorService heartbeatScheduler;

    public ChatClient(String host, int port) {
        this.host = host;
        this.port = port;
        initUI();
    }

    private void initUI() { /* Same UI as before - abbreviated */ 
        setTitle("Chat Client");
        setSize(700, 550);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        chatArea = new JTextArea();
        chatArea.setEditable(false);
        add(new JScrollPane(chatArea), BorderLayout.CENTER);
        // ... add panels and buttons (use previous version's UI code)
        setupListeners();
        setVisible(true);
    }

    private void setupListeners() {
        connectBtn.addActionListener(e -> connect());
        disconnectBtn.addActionListener(e -> disconnect());
        messageField.addActionListener(e -> sendMessage());
    }

    private void connect() {
        if (isConnected()) return;
        try {
            socket = new Socket(host, port);
            socket.setSoTimeout(10000); // 10 sec timeout
            input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            output = new PrintWriter(socket.getOutputStream(), true);

            running = true;
            updateStatus("Connected", Color.GREEN);
            append("✅ Connected");

            startReceiver();
            startHeartbeat();

            connectBtn.setEnabled(false);
            disconnectBtn.setEnabled(true);

        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Connect failed: " + e.getMessage());
        }
    }

    private void startReceiver() {
        receiverThread = new Thread(() -> {
            try {
                String msg;
                while (running && (msg = input.readLine()) != null) {
                    append(msg);
                }
            } catch (IOException e) {
                if (running) append("⚠️ Server disconnected");
            }
        });
        receiverThread.start();
    }

    private void startHeartbeat() {
        heartbeatScheduler = Executors.newSingleThreadScheduledExecutor();
        heartbeatScheduler.scheduleAtFixedRate(() -> {
            if (output != null && running) {
                try { output.println("HEARTBEAT"); } catch (Exception e) {
                    if (running) disconnect();
                }
            }
        }, 5, 5, TimeUnit.SECONDS);
    }

    private void disconnect() {
        running = false;
        if (output != null) try { output.println("exit"); } catch (Exception ignored) {}

        if (heartbeatScheduler != null) heartbeatScheduler.shutdownNow();
        if (receiverThread != null) {
            receiverThread.interrupt();
        }

        closeResources();

        disconnectBtn.setEnabled(false);
        connectBtn.setEnabled(true);
        updateStatus("Disconnected", Color.RED);
        append("Disconnected");
    }

    private void closeResources() {
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

    private void append(String text) {
        SwingUtilities.invokeLater(() -> chatArea.append(text + "\n"));
    }

    private void sendMessage() {
        String msg = messageField.getText().trim();
        if (msg.isEmpty() || output == null) return;
        output.println(msg);
        append("You: " + msg);
        messageField.setText("");
    }

    private void updateStatus(String text, Color c) {
        statusLabel.setText(text);
        statusLabel.setForeground(c);
    }

    public static void main(String[] args) {
        String h = args.length > 0 ? args[0] : "localhost";
        int p = args.length > 1 ? Integer.parseInt(args[1]) : 5000;
        SwingUtilities.invokeLater(() -> new ChatClient(h, p));
    }
}
