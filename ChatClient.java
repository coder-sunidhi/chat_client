import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.ConnectException;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

/**
 * Chat Client Application
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

    private Thread receiverThread;

    private boolean connected;

    public ChatClient() {

        connected = false;

        initializeUI();
    }

    /**
     * Builds the Swing UI.
     */
    private void initializeUI() {

        setTitle("Chat Client");

        setSize(700, 550);

        setLocationRelativeTo(null);

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        chatArea = new JTextArea();

        chatArea.setEditable(false);

        chatArea.setFont(
                new Font(
                        "Arial",
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

        add(
                topPanel,
                BorderLayout.NORTH);

        add(
                scrollPane,
                BorderLayout.CENTER);

        add(
                bottomPanel,
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
     * Connects to the chat server.
     */
    private void connectToServer() {

        if (connected) {

            return;
        }

        try {

            socket =
                    new Socket(
                            HOST,
                            PORT);

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
                    "Connected to server.");

            LoggerUtil.info(
                    "Connected to server.");

            startReceiverThread();

        } catch (UnknownHostException e) {

            LoggerUtil.error(
                    "Unknown host.",
                    e);

            appendMessage(
                    "Unknown host.");

            closeResources();

        } catch (ConnectException e) {

            LoggerUtil.error(
                    "Server unavailable.",
                    e);

            appendMessage(
                    "Server unavailable.");

            closeResources();

        } catch (SocketTimeoutException e) {

            LoggerUtil.error(
                    "Connection timed out.",
                    e);

            appendMessage(
                    "Connection timed out.");

            closeResources();

        } catch (IOException e) {

            LoggerUtil.error(
                    "Connection failed.",
                    e);

            appendMessage(
                    "Unable to connect.");

            closeResources();
        }
    }

    /**
     * Starts receiver thread.
     */
    private void startReceiverThread() {

        receiverThread =
                new Thread(
                        new MessageReceiver(
                                input,
                                this));

        receiverThread.setDaemon(true);

        receiverThread.start();
    }
