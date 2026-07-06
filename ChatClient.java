import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;

/**
 * Simple Chat Client
 */
public class ChatClient extends JFrame {

    private static final String HOST = "localhost";
    private static final int PORT = 5000;

    private JTextArea chatArea;
    private JTextField messageField;

    private JButton connectButton;
    private JButton disconnectButton;
    private JButton sendButton;

    private JLabel statusLabel;

    private Socket socket;
    private BufferedReader input;
    private PrintWriter output;

    private Thread receiveThread;

    private boolean connected;

    public ChatClient() {

        connected = false;

        initializeUI();
    }

    /**
     * Creates GUI.
     */
    private void initializeUI() {

        setTitle("Java Chat Client");

        setSize(700,550);

        setLocationRelativeTo(null);

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        chatArea = new JTextArea();

        chatArea.setEditable(false);

        chatArea.setFont(
                new Font("Arial",
                        Font.PLAIN,
                        14));

        JScrollPane scrollPane =
                new JScrollPane(chatArea);

        messageField =
                new JTextField();

        connectButton =
                new JButton("Connect");

        disconnectButton =
                new JButton("Disconnect");

        sendButton =
                new JButton("Send");

        disconnectButton.setEnabled(false);

        statusLabel =
                new JLabel("Disconnected");

        JPanel topPanel =
                new JPanel();

        topPanel.add(connectButton);

        topPanel.add(disconnectButton);

        JPanel bottomPanel =
                new JPanel(
                        new BorderLayout());

        bottomPanel.add(
                messageField,
                BorderLayout.CENTER);

        bottomPanel.add(
                sendButton,
                BorderLayout.EAST);

        bottomPanel.add(
                statusLabel,
                BorderLayout.SOUTH);

        add(topPanel,
                BorderLayout.NORTH);

        add(scrollPane,
                BorderLayout.CENTER);

        add(bottomPanel,
                BorderLayout.SOUTH);

        connectButton.addActionListener(
                e -> connectToServer());

        disconnectButton.addActionListener(
                e -> disconnectFromServer());

        sendButton.addActionListener(
                e -> sendMessage());

        messageField.addActionListener(
                e -> sendMessage());

        setVisible(true);
    }

    /**
     * Connects to server.
     */
    private void connectToServer() {

        if (connected) {

            return;
        }

        try {

            socket =
                    new Socket(HOST, PORT);

            input =
                    new BufferedReader(
                            new InputStreamReader(
                                    socket.getInputStream()));

            output =
                    new PrintWriter(
                            socket.getOutputStream(),
                            true);

            connected = true;

            connectButton.setEnabled(false);

            disconnectButton.setEnabled(true);

            statusLabel.setText("Connected");

            statusLabel.setForeground(Color.GREEN);

            appendMessage(
                    "Connected to Server.");

            startReceiverThread();

        } catch (UnknownHostException e) {

            appendMessage(
                    "Unknown Host.");

        } catch (ConnectException e) {

            appendMessage(
                    "Server is Offline.");

        } catch (SocketTimeoutException e) {

            appendMessage(
                    "Connection Timed Out.");

        } catch (IOException e) {

            appendMessage(
                    "Connection Failed.");

        } finally {

            if (!connected) {

                closeResources();
            }
        }
    }
    /**
     * Disconnects from the server.
     */
    private void disconnectFromServer() {

        connected = false;

        try {

            if (output != null) {

                output.println("exit");
                output.flush();
            }

        } catch (Exception ignored) {
        }

        closeResources();

        connectButton.setEnabled(true);
        disconnectButton.setEnabled(false);

        statusLabel.setText("Disconnected");
        statusLabel.setForeground(Color.RED);

        appendMessage("Disconnected from server.");
    }

    /**
     * Starts receiver thread.
     */
    private void startReceiverThread() {

        receiveThread =
                new Thread(
                        new MessageReceiver(
                                input,
                                this));

        receiveThread.start();
    }

    /**
     * Sends message.
     */
    private void sendMessage() {

        if (!connected) {

            JOptionPane.showMessageDialog(
                    this,
                    "Please connect first.");

            return;
        }

        String message =
                messageField.getText().trim();

        if (message.isEmpty()) {

            return;
        }

        try {

            output.println(message);

            appendMessage(
                    "You : " + message);

            messageField.setText("");

        } catch (Exception e) {

            appendMessage(
                    "Unable to send message.");
        }
    }

    /**
     * Displays message.
     */
    public void appendMessage(
            String message) {

        SwingUtilities.invokeLater(() -> {

            chatArea.append(
                    message + "\n");

            chatArea.setCaretPosition(
                    chatArea.getDocument()
                            .getLength());

        });
    }

    /**
     * Called by MessageReceiver when server disconnects.
     */
    public void serverDisconnected() {

        connected = false;

        SwingUtilities.invokeLater(() -> {

            connectButton.setEnabled(true);

            disconnectButton.setEnabled(false);

            statusLabel.setText(
                    "Disconnected");

            statusLabel.setForeground(
                    Color.RED);
        });

        closeResources();
    }

    /**
     * Closes all resources.
     */
    private void closeResources() {

        try {

            if (input != null) {

                input.close();
            }

        } catch (Exception ignored) {
        }

        if (output != null) {

            output.close();
        }

        try {

            if (socket != null &&
                    !socket.isClosed()) {

                socket.close();
            }

        } catch (Exception ignored) {
        }

        input = null;
        output = null;
        socket = null;
    }

    /**
     * Starts application.
     */
    public static void main(
            String[] args) {

        SwingUtilities.invokeLater(
                ChatClient::new);
    }
}

