import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.*;

public class ChatClient extends JFrame {

    // Constants (removes hardcoded values)
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

    public ChatClient() {
        initializeUI();
    }

    // UI Design
    private void initializeUI() {

        setTitle("Simple Chat Client");
        setSize(600,500);

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        chatArea = new JTextArea();
        chatArea.setEditable(false);

        JScrollPane scrollPane =
                new JScrollPane(chatArea);

        messageField = new JTextField();

        sendButton = new JButton("Send");
        connectButton = new JButton("Connect");
        disconnectButton = new JButton("Disconnect");

        statusLabel =
                new JLabel("Disconnected");

        JPanel topPanel =
                new JPanel();

        topPanel.add(connectButton);
        topPanel.add(disconnectButton);

        JPanel bottomPanel =
                new JPanel(new BorderLayout());

        bottomPanel.add(
                messageField,
                BorderLayout.CENTER
        );

        bottomPanel.add(
                sendButton,
                BorderLayout.EAST
        );

        JPanel mainBottomPanel =
                new JPanel(new BorderLayout());

        mainBottomPanel.add(
                bottomPanel,
                BorderLayout.CENTER
        );

        mainBottomPanel.add(
                statusLabel,
                BorderLayout.SOUTH
        );

        add(topPanel,
                BorderLayout.NORTH);

        add(scrollPane,
                BorderLayout.CENTER);

        add(mainBottomPanel,
                BorderLayout.SOUTH);

        connectButton.addActionListener(
                e -> connect()
        );

        disconnectButton.addActionListener(
                e -> disconnect()
        );

        sendButton.addActionListener(
                e -> sendMessage()
        );

        setVisible(true);
    }

    // Connection
    private void connect() {

        try {

            if(socket != null &&
                    socket.isConnected()) {

                JOptionPane.showMessageDialog(
                        this,
                        "Already Connected"
                );
                return;
            }

            socket =
                    new Socket(HOST,PORT);

            input =
                    new BufferedReader(
                            new InputStreamReader(
                                    socket.getInputStream()
                            )
                    );

            output =
                    new PrintWriter(
                            socket.getOutputStream(),
                            true
                    );

            statusLabel.setText(
                    "Connected"
            );

            chatArea.append(
                    "Connected to server\n"
            );

            receiveMessages();

        }

        catch(IOException e){

            JOptionPane.showMessageDialog(
                    this,
                    "Connection Failed"
            );
        }
    }

    // Send message
    private void sendMessage() {

        try {

            String message =
                    messageField.getText().trim();

            if(message.isEmpty()) {
                return;
            }

            if(output == null) {

                JOptionPane.showMessageDialog(
                        this,
                        "Not connected"
                );

                return;
            }

            output.println(message);

            chatArea.append(
                    "You: " +
                    message +
                    "\n"
            );

            messageField.setText("");

        }

        catch(Exception e){

            JOptionPane.showMessageDialog(
                    this,
                    "Sending Failed"
            );
        }
    }

    // Receive messages
    private void receiveMessages() {

        Thread thread =
                new Thread(() -> {

                    try {

                        String msg;

                        while(
                                (msg=input.readLine())
                                        != null
                        ) {

                            chatArea.append(
                                    "Server: "
                                    + msg
                                    + "\n"
                            );
                        }

                    }

                    catch(IOException e){

                        chatArea.append(
                                "Connection closed\n"
                        );
                    }

                });

        thread.start();
    }

    // Proper disconnect method
    private void disconnect() {

        try {

            if(output != null){
                output.close();
            }

            if(input != null){
                input.close();
            }

            if(socket != null &&
                    !socket.isClosed()) {

                socket.close();
            }

            statusLabel.setText(
                    "Disconnected"
            );

            chatArea.append(
                    "Disconnected\n"
            );

        }

        catch(IOException e){

            JOptionPane.showMessageDialog(
                    this,
                    "Error during disconnect"
            );
        }

        finally {

            output = null;
            input = null;
            socket = null;
        }
    }

    public static void main(String[] args) {

        SwingUtilities.invokeLater(
                ChatClient::new
        );

    }
}
