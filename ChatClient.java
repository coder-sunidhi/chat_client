import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.*;

public class ChatClient extends JFrame {

    private static final String DEFAULT_HOST =
            "localhost";

    private static final int DEFAULT_PORT =
            5000;

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

    private volatile boolean running =
            false;

    public ChatClient(
            String host,
            int port) {

        this.host = host;
        this.port = port;

        initUI();
    }

    private void initUI() {

        setTitle("Chat Client");
        setSize(700, 550);
        setDefaultCloseOperation(
                JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        chatArea = createChatArea();
        messageField =
                new JTextField();

        connectBtn =
                new JButton("Connect");

        disconnectBtn =
                new JButton("Disconnect");

        disconnectBtn.setEnabled(false);

        statusLabel =
                new JLabel("Disconnected");

        add(
                new JScrollPane(chatArea),
                BorderLayout.CENTER);

        add(
                createTopPanel(),
                BorderLayout.NORTH);

        add(
                createBottomPanel(),
                BorderLayout.SOUTH);

        setupListeners();

        setVisible(true);
    }

    private JTextArea createChatArea() {

        JTextArea area =
                new JTextArea();

        area.setEditable(false);

        area.setFont(
                new Font(
                        "SansSerif",
                        Font.PLAIN,
                        14));

        return area;
    }

    private JPanel createTopPanel() {

        JPanel panel =
                new JPanel();

        panel.add(connectBtn);
        panel.add(disconnectBtn);

        return panel;
    }

    private JPanel createBottomPanel() {

        JButton sendBtn =
                new JButton("Send");

        sendBtn.addActionListener(
                e -> sendMessage());

        JPanel inputPanel =
                new JPanel(
                        new BorderLayout());

        inputPanel.add(
                messageField,
                BorderLayout.CENTER);

        inputPanel.add(
                sendBtn,
                BorderLayout.EAST);

        JPanel panel =
                new JPanel(
                        new BorderLayout());

        panel.add(
                inputPanel,
                BorderLayout.CENTER);

        panel.add(
                statusLabel,
                BorderLayout.SOUTH);

        return panel;
    }

    private void setupListeners() {

        connectBtn.addActionListener(
                e -> connectToServer());

        disconnectBtn.addActionListener(
                e -> disconnectFromServer());

        messageField.addActionListener(
                e -> sendMessage());
    }

    private void connectToServer() {

        if (isConnected())
            return;

        try {

            socket =
                    new Socket(
                            host,
                            port);

            input =
                    new BufferedReader(
                            new InputStreamReader(
                                    socket.getInputStream()));

            output =
                    new PrintWriter(
                            socket.getOutputStream(),
                            true);

            running = true;

            setConnectedState(true);

            appendToChat(
                    "✅ Connected to server.");

            startReceiverThread();

        } catch (IOException e) {

            JOptionPane.showMessageDialog(
                    this,
                    "Connection failed: "
                            + e.getMessage());
        }
    }

    private synchronized void disconnectFromServer() {

        running = false;

        try {

            if (output != null) {

                output.println("exit");
                output.flush();
            }

            closeAllResources();

            if (receiveThread != null &&
                    receiveThread.isAlive()) {

                receiveThread.interrupt();

                receiveThread.join(1000);

                receiveThread = null;
            }

        } catch (InterruptedException e) {

            Thread.currentThread().interrupt();

        } finally {

            SwingUtilities.invokeLater(() -> {

                setConnectedState(false);

                appendToChat(
                        "Disconnected from server.");
            });
        }
    }

    private void startReceiverThread() {

        receiveThread =
                new Thread(() -> {

                    try {

                        String message;

                        while (running &&
                                !Thread.currentThread()
                                        .isInterrupted()) {

                            message =
                                    input.readLine();

                            if (message == null)
                                break;

                            appendToChat(
                                    message);
                        }

                    } catch (IOException e) {

                        if (running) {

                            appendToChat(
                                    "⚠ Connection lost");
                        }

                    } finally {

                        running = false;

                        closeAllResources();

                        SwingUtilities.invokeLater(
                                () -> setConnectedState(false));
                    }
                });

        receiveThread.setDaemon(true);

        receiveThread.start();
    }

    private void sendMessage() {

        String message =
                messageField
                        .getText()
                        .trim();

        if (message.isEmpty() ||
                output == null)
            return;

        output.println(message);

        appendToChat(
                "You: "
                        + message);

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

    private void closeQuietly(
            AutoCloseable resource) {

        if (resource != null) {
            try {
                resource.close();
            } catch (Exception e) {
                System.err.println(
                        "Close error: "
                                + e.getMessage());
            }
        }
    }

    private boolean isConnected() {

        return socket != null
                && !socket.isClosed();
    }

    private void setConnectedState(
            boolean connected) {

        connectBtn.setEnabled(
                !connected);

        disconnectBtn.setEnabled(
                connected);

        updateStatus(
                connected
                        ? "Connected"
                        : "Disconnected",

                connected
                        ? Color.GREEN
                        : Color.RED);
    }

    private void appendToChat(
            String text) {

        SwingUtilities.invokeLater(
                () -> {

                    chatArea.append(
                            text + "\n");

                    chatArea.setCaretPosition(
                            chatArea
                                    .getDocument()
                                    .getLength());
                });
    }

    private void updateStatus(
            String text,
            Color color) {

        statusLabel.setText(text);
        statusLabel.setForeground(color);
    }

    public static void main(
            String[] args) {

        String host =
                args.length > 0
                        ? args[0]
                        : DEFAULT_HOST;

        int port =
                args.length > 1
                        ? Integer.parseInt(
                        args[1])
                        : DEFAULT_PORT;

        SwingUtilities.invokeLater(
                () -> new ChatClient(
                        host,
                        port));
    }
}
