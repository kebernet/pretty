/**
 * Copyright 2013, Robert Cooper, Reach Health
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 */
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
