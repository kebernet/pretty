package com.reachcall.pretty.websocket;

import org.eclipse.jetty.websocket.WebSocket;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 *
 */
public class RemoteWebSocket implements WebSocket, WebSocket.OnBinaryMessage {
    private static final Logger LOG = Logger.getLogger(RemoteWebSocket.class.getCanonicalName());
    final Connection proxyConnection;
    Connection remoteConnection;

    public RemoteWebSocket(Connection proxyConnection) {
        this.proxyConnection = proxyConnection;
    }

    @Override
    public void onClose(int i, String s) {
        this.proxyConnection.close(i, s);
    }

    @Override
    public void onMessage(byte[] bytes, int i, int i2) {
        try {
            this.proxyConnection.sendMessage(bytes, i, i2);
        } catch (IOException e) {
            LOG.log(Level.WARNING, "Exception sending data", e);
            remoteConnection.close();
        }
    }

    @Override
    public void onOpen(Connection connection) {
        this.remoteConnection = connection;
    }
}
