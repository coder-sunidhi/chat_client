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
    private JButton connectButton;
    private JButton disconnectButton;
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

        initializeUI();
    }

    private void initializeUI() {

        setTitle("Chat Client");
        setSize(700, 550);
        setDefaultCloseOperation(
                JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        chatArea =
                createChatArea();

        messageField =
                new JTextField();

        connectButton =
                new JButton("Connect");

        disconnectButton =
                new JButton("Disconnect");

        disconnectButton.setEnabled(false);

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

        panel.add(connectButton);
        panel.add(disconnectButton);

        return panel;
    }

    private JPanel createBottomPanel() {

        JButton sendButton =
                new JButton("Send");

        sendButton.addActionListener(
                e -> sendMessage());

        JPanel inputPanel =
                new JPanel(
                        new BorderLayout());

        inputPanel.add(
                messageField,
                BorderLayout.CENTER);

        inputPanel.add(
                sendButton,
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

        connectButton.addActionListener(
                e -> connectToServer());

        disconnectButton.addActionListener(
                e -> disconnectFromServer());

        messageField.addActionListener(
                e -> sendMessage());
    }

    private void connectToServer() {

        if (isConnected()) {
            return;
        }

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

            if (receiveThread != null &&
                    receiveThread.isAlive()) {

                receiveThread.interrupt();
            }

            if (output != null) {

                output.println("exit");
                output.flush();
            }

        } catch (Exception e) {

            appendToChat(
                    "Disconnect error: "
                            + e.getMessage());

        } finally {

            closeAllResources();

            try {

                if (receiveThread != null) {

                    receiveThread.join(1000);
                }

            } catch (InterruptedException e) {

                Thread.currentThread().interrupt();
            }

            receiveThread = null;

            SwingUtilities.invokeLater(() -> {

                setConnectedState(false);

                appendToChat(
                        "Disconnected from server.");
            });
        }
    }

    private final ThreadFactory receiverThreadFactory =
        runnable -> {
            Thread thread =
                    new Thread(runnable);

            thread.setDaemon(true);
            thread.setName("MessageReceiver");

            return thread;
        };

private void startReceiverThread() {

    receiveThread =
            receiverThreadFactory.newThread(
                    new MessageReceiver());

    receiveThread.start();
}

    private void sendMessage() {

        String message =
                messageField
                        .getText()
                        .trim();

        if (message.isEmpty()
                || output == null) {

            return;
        }

        try {

            output.println(message);

            appendToChat(
                    "You: "
                            + message);

            messageField.setText("");

        } catch (Exception e) {

            appendToChat(
                    "Send failed: "
                            + e.getMessage());
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

        connectButton.setEnabled(
                !connected);

        disconnectButton.setEnabled(
                connected);

        statusLabel.setText(
                connected
                        ? "Connected"
                        : "Disconnected");

        statusLabel.setForeground(
                connected
                        ? Color.GREEN
                        : Color.RED);
    }

    public void appendToChat(
            String text) {

        SwingUtilities.invokeLater(() -> {

            chatArea.append(
                    text + "\n");

            chatArea.setCaretPosition(
                    chatArea.getDocument()
                            .getLength());
        });
    }


    /**
 * Receives messages from the server.
 */
private class MessageReceiver
        implements Runnable {

    @Override
    public void run() {

        try {

            String message;

            while (running &&
                    !Thread.currentThread()
                            .isInterrupted()) {

                message = input.readLine();

                if (message == null) {

                    SwingUtilities.invokeLater(() -> {

                        appendToChat(
                                "⚠ Server disconnected.");

                        setConnectedState(false);
                    });

                    break;
                }

                appendToChat(message);
            }

        } catch (IOException e) {

            if (running) {

                SwingUtilities.invokeLater(() -> {

                    appendToChat(
                            "⚠ Connection lost.");

                    setConnectedState(false);
                });
            }

        } finally {

            running = false;

            closeAllResources();

            receiveThread = null;

            SwingUtilities.invokeLater(
                    () -> setConnectedState(false));
        }
    }
}
    public static void main(
            String[] args) {

        String host =
                args.length > 0
                        ? args[0]
                        : DEFAULT_HOST;

        int port =
                args.length > 1
                        ? Integer.parseInt(args[1])
                        : DEFAULT_PORT;

        SwingUtilities.invokeLater(
                () -> new ChatClient(
                        host,
                        port));
    }
}
