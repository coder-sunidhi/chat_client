import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.Socket;

/**
 * Chat Client Application
 * Connects to the chat server and allows
 * users to send and receive messages.
 */
public class ChatClient extends JFrame {

    private static final String DEFAULT_HOST = "localhost";
    private static final int DEFAULT_PORT = 5000;

    private final String host;
    private final int port;

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

    private volatile boolean running;

    /**
     * Constructor
     */
    public ChatClient(String host, int port) {

        this.host = host;
        this.port = port;

        running = false;

        initializeUserInterface();
    }

    /**
     * Initializes GUI.
     */
    private void initializeUserInterface() {

        setTitle("Chat Client");

        setSize(700,550);

        setLocationRelativeTo(null);

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        chatArea = new JTextArea();

        chatArea.setEditable(false);

        chatArea.setFont(
                new Font(
                        "SansSerif",
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

        statusLabel =
                new JLabel("Disconnected");

        disconnectButton.setEnabled(false);

        JPanel topPanel =
                new JPanel();

        topPanel.add(connectButton);

        topPanel.add(disconnectButton);

        JPanel bottomPanel =
                new JPanel(new BorderLayout());

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
                new ActionListener() {

                    @Override
                    public void actionPerformed(
                            ActionEvent e) {

                        connectToServer();
                    }
                });

        disconnectButton.addActionListener(
                new ActionListener() {

                    @Override
                    public void actionPerformed(
                            ActionEvent e) {

                        disconnectFromServer();
                    }
                });

        sendButton.addActionListener(
                new ActionListener() {

                    @Override
                    public void actionPerformed(
                            ActionEvent e) {

                        sendMessage();
                    }
                });

        messageField.addActionListener(
                new ActionListener() {

                    @Override
                    public void actionPerformed(
                            ActionEvent e) {

                        sendMessage();
                    }
                });

        setVisible(true);
    }
        /**
     * Connects to the chat server.
     */
    private synchronized void connectToServer() {

        if (running) {
            return;
        }

        try {

            socket = new Socket(host, port);

            input = new BufferedReader(
                    new InputStreamReader(
                            socket.getInputStream()));

            output = new PrintWriter(
                    socket.getOutputStream(),
                    true);

            running = true;

            statusLabel.setText("Connected");
            statusLabel.setForeground(Color.GREEN);

            connectButton.setEnabled(false);
            disconnectButton.setEnabled(true);

            appendToChat(
                    "Connected to server.");

            startReceiverThread();

        } catch (IOException e) {

            JOptionPane.showMessageDialog(
                    this,
                    "Connection failed : "
                            + e.getMessage());

            running = false;
        }
    }

    /**
     * Disconnects from server.
     */
    private synchronized void disconnectFromServer() {

        running = false;

        try {

            if (output != null) {

                output.println("exit");
                output.flush();
            }

            if (receiveThread != null) {

                receiveThread.interrupt();

                receiveThread.join(1000);
            }

        } catch (Exception e) {

            appendToChat(
                    "Disconnect Error : "
                            + e.getMessage());

        } finally {

            closeAllResources();

            receiveThread = null;

            connectButton.setEnabled(true);

            disconnectButton.setEnabled(false);

            statusLabel.setText(
                    "Disconnected");

            statusLabel.setForeground(
                    Color.RED);

            appendToChat(
                    "Disconnected from server.");
        }
    }

    /**
     * Starts the receiver thread.
     */
    private void startReceiverThread() {

        receiveThread =
                new Thread(
                        new MessageReceiver());

        receiveThread.setDaemon(true);

        receiveThread.setName(
                "MessageReceiver");

        receiveThread.start();
    }

    /**
     * Receives messages continuously.
     */
    private class MessageReceiver
            implements Runnable {

        @Override
        public void run() {

            try {

                while (running) {

                    String message =
                            input.readLine();

                    if (message == null) {

                        break;
                    }

                    appendToChat(message);
                }

            } catch (IOException e) {

                if (running) {

                    appendToChat(
                            "Connection lost.");
                }

            } finally {

                running = false;

                closeAllResources();

                SwingUtilities.invokeLater(
                        new Runnable() {

                            @Override
                            public void run() {

                                connectButton.setEnabled(
                                        true);

                                disconnectButton.setEnabled(
                                        false);

                                statusLabel.setText(
                                        "Disconnected");

                                statusLabel.setForeground(
                                        Color.RED);
                            }
                        });
            }
        }
    }

    /**
     * Sends a message to the server.
     */
    private synchronized void sendMessage() {

        if (!running) {

            JOptionPane.showMessageDialog(
                    this,
                    "Please connect to the server first.");

            return;
        }

        String message =
                messageField.getText().trim();

        if (message.isEmpty()) {
            return;
        }

        try {

            output.println(message);

            output.flush();

            appendToChat(
                    "You : " + message);

            messageField.setText("");

        } catch (Exception e) {

            appendToChat(
                    "Unable to send message.");
        }
    }

    /**
     * Appends text to chat area.
     */
    private void appendToChat(
            final String text) {

        SwingUtilities.invokeLater(
                new Runnable() {

                    @Override
                    public void run() {

                        chatArea.append(
                                text + "\n");

                        chatArea.setCaretPosition(
                                chatArea.getDocument()
                                        .getLength());
                    }
                });
    }

    /**
     * Closes all network resources.
     */
    private synchronized void closeAllResources() {

        closeQuietly(input);

        if (output != null) {

            output.close();

            output = null;
        }

        closeQuietly(socket);

        input = null;

        socket = null;
    }

    /**
     * Safely closes resources.
     */
    private void closeQuietly(
            AutoCloseable resource) {

        if (resource != null) {

            try {

                resource.close();

            } catch (Exception ignored) {
            }
        }
    }

    /**
     * Checks whether the client
     * is connected.
     */
    private boolean isConnected() {

        return socket != null
                && socket.isConnected()
                && !socket.isClosed();
    }

    /**
     * Main Method.
     */
    public static void main(
            String[] args) {

        String host =
                DEFAULT_HOST;

        int port =
                DEFAULT_PORT;

        if (args.length >= 1) {

            host = args[0];
        }

        if (args.length >= 2) {

            port =
                    Integer.parseInt(
                            args[1]);
        }

        final String finalHost =
                host;

        final int finalPort =
                port;

        SwingUtilities.invokeLater(
                new Runnable() {

                    @Override
                    public void run() {

                        new ChatClient(
                                finalHost,
                                finalPort);
                    }
                });
    }
}
