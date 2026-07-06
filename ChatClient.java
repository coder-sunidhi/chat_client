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

    /**
     * Constructor
     */
    public ChatClient() {

        connected = false;

        initializeGUI();
    }

    /**
     * Creates GUI.
     */
    private void initializeGUI() {

        setTitle("Java Chat Client");

        setSize(700,550);

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
     * Connects to the server.
     */
    private void connectToServer() {

        if (connected) {
            return;
        }

        try {

            socket = new Socket(HOST, PORT);

            input = new BufferedReader(
                    new InputStreamReader(
                            socket.getInputStream()));

            output = new PrintWriter(
                    socket.getOutputStream(),
                    true);

            connected = true;

            connectButton.setEnabled(false);
            disconnectButton.setEnabled(true);

            statusLabel.setText("Connected");
            statusLabel.setForeground(Color.GREEN);

            appendMessage(
                    "Connected to server.");

            startReceiverThread();

        } catch (IOException e) {

            appendMessage(
                    "Unable to connect to server.");

            JOptionPane.showMessageDialog(
                    this,
                    "Connection Failed : "
                            + e.getMessage());
        }
    }

    /**
     * Disconnects from server.
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

        appendMessage(
                "Disconnected from server.");
    }

    /**
     * Starts receiver thread.
     */
    private void startReceiverThread() {

        receiveThread =
                new Thread(
                        new MessageReceiver());

        receiveThread.start();
    }

    /**
     * Receives messages from server.
     */
    private class MessageReceiver
            implements Runnable {

        @Override
        public void run() {

            try {

                String message;

                while (connected &&
                        (message =
                                input.readLine()) != null) {

                    appendMessage(message);
                }

            } catch (IOException e) {

                if (connected) {

                    appendMessage(
                            "Connection Lost.");
                }

            } finally {

                connected = false;

                SwingUtilities.invokeLater(
                        new Runnable() {

                            @Override
                            public void run() {

                                connectButton.setEnabled(true);

                                disconnectButton.setEnabled(false);

                                statusLabel.setText(
                                        "Disconnected");

                                statusLabel.setForeground(
                                        Color.RED);
                            }
                        });

                closeResources();
            }
        }
    }


    /**
     * Sends a message to the server.
     */
    private void sendMessage() {

        if (!connected) {

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

            appendMessage(
                    "You : " + message);

            messageField.setText("");

        } catch (Exception e) {

            appendMessage(
                    "Unable to send message.");
        }
    }

    /**
     * Appends message to chat area.
     */
    private void appendMessage(
            final String message) {

        SwingUtilities.invokeLater(
                new Runnable() {

                    @Override
                    public void run() {

                        chatArea.append(
                                message + "\n");

                        chatArea.setCaretPosition(
                                chatArea.getDocument()
                                        .getLength());
                    }
                });
    }

    /**
     * Closes all resources.
     */
    private void closeResources() {

        try {

            if (input != null) {

                input.close();
            }

        } catch (IOException ignored) {
        }

        if (output != null) {

            output.close();
        }

        try {

            if (socket != null &&
                    !socket.isClosed()) {

                socket.close();
            }

        } catch (IOException ignored) {
        }

        input = null;
        output = null;
        socket = null;
    }

    /**
     * Starts Client Application.
     */
    public static void main(
            String[] args) {

        SwingUtilities.invokeLater(
                new Runnable() {

                    @Override
                    public void run() {

                        new ChatClient();
                    }
                });
    }
}
