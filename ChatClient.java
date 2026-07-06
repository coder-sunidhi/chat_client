import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.*;

public class ChatClient extends JFrame {

    private static final String HOST = "localhost";
    private static final int PORT = 5000;

    private JTextArea chatArea;
    private JTextField messageField;
    private JButton sendButton;
    private JButton connectButton;
    private JButton disconnectButton;
    private JLabel statusLabel;

    private Socket socket;
    private BufferedReader input;
    private PrintWriter output;
    private Thread receiveThread;

    public ChatClient() {
        initializeUI();
    }

    private void initializeUI() {
        setTitle("Chat Client");
        setSize(700, 500);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        chatArea = new JTextArea();
        chatArea.setEditable(false);
        chatArea.setFont(new Font("SansSerif", Font.PLAIN, 14));

        JScrollPane scrollPane = new JScrollPane(chatArea);

        messageField = new JTextField();
        sendButton = new JButton("Send");
        connectButton = new JButton("Connect");
        disconnectButton = new JButton("Disconnect");
        disconnectButton.setEnabled(false);

        statusLabel = new JLabel("Disconnected");
        statusLabel.setHorizontalAlignment(JLabel.CENTER);

        // Top panel
        JPanel topPanel = new JPanel();
        topPanel.add(connectButton);
        topPanel.add(disconnectButton);

        // Bottom panel
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.add(messageField, BorderLayout.CENTER);
        bottomPanel.add(sendButton, BorderLayout.EAST);

        JPanel southPanel = new JPanel(new BorderLayout());
        southPanel.add(bottomPanel, BorderLayout.CENTER);
        southPanel.add(statusLabel, BorderLayout.SOUTH);

        add(topPanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
        add(southPanel, BorderLayout.SOUTH);

        // Action listeners
        connectButton.addActionListener(e -> connect());
        disconnectButton.addActionListener(e -> disconnect());
        sendButton.addActionListener(e -> sendMessage());
        messageField.addActionListener(e -> sendMessage());

        setVisible(true);
    }

    private void connect() {
        if (socket != null && socket.isConnected()) {
            JOptionPane.showMessageDialog(this, "Already connected!");
            return;
        }

        try {
            socket = new Socket(HOST, PORT);
            input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            output = new PrintWriter(socket.getOutputStream(), true);

            statusLabel.setText("Connected");
            statusLabel.setForeground(Color.GREEN);
            chatArea.append("✅ Connected to server!\n");
            connectButton.setEnabled(false);
            disconnectButton.setEnabled(true);

            receiveMessages();

        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Connection failed: " + e.getMessage());
        }
    }

    private void sendMessage() {
        String message = messageField.getText().trim();
        if (message.isEmpty() || output == null) return;

        try {
            output.println(message);
            chatArea.append("You: " + message + "\n");
            messageField.setText("");
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Failed to send message");
        }
    }

    private void receiveMessages() {
        receiveThread = new Thread(() -> {
            try {
                String msg;
                while ((msg = input.readLine()) != null) {
                    chatArea.append(msg + "\n");
                }
            } catch (IOException e) {
                if (socket != null && !socket.isClosed()) {
                    chatArea.append("⚠️ Connection lost\n");
                }
            }
        });
        receiveThread.start();
    }

    private void disconnect() {
        try {
            if (output != null) output.close();
            if (input != null) input.close();
            if (socket != null && !socket.isClosed()) socket.close();

            chatArea.append("Disconnected from server.\n");
            statusLabel.setText("Disconnected");
            statusLabel.setForeground(Color.RED);

        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Error while disconnecting");
        } finally {
            connectButton.setEnabled(true);
            disconnectButton.setEnabled(false);
            output = null;
            input = null;
            socket = null;
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(ChatClient::new);
    }
}
