public void serverDisconnected() {

    SwingUtilities.invokeLater(() -> {

        disconnectBtn.setEnabled(false);
        connectBtn.setEnabled(true);

        updateStatus(
                "Disconnected",
                Color.RED);

        appendToChat(
                "Server disconnected.");
    });
}
