import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.*;

public class ChatClient extends JFrame {

    private static final String HOST = "localhost";
    private static final int PORT = 5000;

    private final JTextArea chatArea;
    private final JTextField messageField;
    private final JButton sendButton;
    private final JButton connectButton;
    private final JButton disconnectButton;
    private final JLabel statusLabel;

    private Socket socket;
    private BufferedReader input;
    private PrintWriter output;
    private Thread receiveThread;

    public ChatClient() {
        chatArea = new JTextArea();
        messageField = new JTextField();
        sendButton = new JButton("Send");
        connectButton = new JButton("Connect");
        disconnectButton = new JButton("Disconnect");
        statusLabel = new JLabel("Disconnected");

        initializeUI();
    }

    private void initializeUI() {
        setTitle("Chat Client");
        setSize(700, 550);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        chatArea.setEditable(false);
        chatArea.setFont(new Font("SansSerif", Font.PLAIN, 14));

        JScrollPane scrollPane = new JScrollPane(chatArea);

        JPanel topPanel = new JPanel();
        topPanel.add(connectButton);
        topPanel.add(disconnectButton);

        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.add(messageField, BorderLayout.CENTER);
        bottomPanel.add(sendButton, BorderLayout.EAST);

        JPanel southPanel = new JPanel(new BorderLayout());
        southPanel.add(bottomPanel, BorderLayout.CENTER);
        southPanel.add(statusLabel, BorderLayout.SOUTH);

        add(topPanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
        add(southPanel, BorderLayout.SOUTH);

        connectButton.addActionListener(e -> connect());
        disconnectButton.addActionListener(e -> disconnect());
        sendButton.addActionListener(e -> sendMessage());
        messageField.addActionListener(e -> sendMessage());

        disconnectButton.setEnabled(false);
        setVisible(true);
    }

    private void connect() {
        if (isConnected()) {
            JOptionPane.showMessageDialog(this, "Already connected!");
            return;
        }

        try {
            socket = new Socket(HOST, PORT);
            input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            output = new PrintWriter(socket.getOutputStream(), true);

            updateStatus("Connected", Color.GREEN);
            appendToChat("✅ Connected to server!");

            connectButton.setEnabled(false);
            disconnectButton.setEnabled(true);

            startMessageReceiver();

        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Connection failed: " + e.getMessage());
        }
    }

    private boolean isConnected() {
        return socket != null && socket.isConnected() && !socket.isClosed();
    }

    private void startMessageReceiver() {
        receiveThread = new Thread(() -> {
            try {
                String msg;
                while ((msg = input.readLine()) != null) {
                    appendToChat(msg);
                }
            } catch (IOException e) {
                if (isConnected()) {
                    appendToChat("⚠️ Connection lost");
                }
            }
        });
        receiveThread.start();
    }

    private void appendToChat(String text) {
        SwingUtilities.invokeLater(() -> {
            chatArea.append(text + "\n");
            chatArea.setCaretPosition(chatArea.getDocument().getLength());
        });
    }

    private void sendMessage() {
        String message = messageField.getText().trim();
        if (message.isEmpty() || output == null) return;

        output.println(message);
        appendToChat("You: " + message);
        messageField.setText("");
    }

    private void disconnect() {
        if (output != null) {
            output.println("exit");   // Signal server to clean up
        }

        closeResources();
        updateStatus("Disconnected", Color.RED);
        appendToChat("Disconnected from server.");

        connectButton.setEnabled(true);
        disconnectButton.setEnabled(false);
    }

    private void closeResources() {
        closeQuietly(output);
        closeQuietly(input);
        closeQuietly(socket);

        output = null;
        input = null;
        socket = null;
    }

    private void closeQuietly(AutoCloseable resource) {
        try {
            if (resource != null) {
                resource.close();
            }
        } catch (Exception ignored) {}
    }

    private void updateStatus(String text, Color color) {
        statusLabel.setText(text);
        statusLabel.setForeground(color);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(ChatClient::new);
    }
}
