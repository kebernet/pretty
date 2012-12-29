package com.reachcall.pretty.websocket;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

import com.reachcall.pretty.config.Match;

import org.eclipse.jetty.websocket.WebSocket;


import java.util.Enumeration;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;


/**
 *
 */
public class ProxyWebSocket implements WebSocket, WebSocket.OnFrame {
    private static final Logger LOG = Logger.getLogger(ProxyWebSocket.class.getCanonicalName());
    private Connection connection;
    private final Match destination;
    private final Multimap<String,String> headers = ArrayListMultimap.create();
    private final String protocol;
    private final String url;

    public ProxyWebSocket(HttpServletRequest request, String protocol) {
        this.url = request.getRequestURL()
                          .toString();
        this.protocol = protocol;

        Enumeration<String> headerNames = request.getHeaderNames();

        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            Enumeration<String> values = request.getHeaders(headerName);

            while (values.hasMoreElements()) {
                headers.put(headerName, values.nextElement());
            }
        }

        this.destination = (Match) request.getAttribute("match");

        if (destination == null) {
            LOG.log(Level.WARNING, "Could not resolve web socket match for path {0}", new Object[] { this.url });
            throw new NullPointerException("Resolver match not found on request.");
        }

    }

    @Override
    public void onClose(int i, String s) {

    }

    @Override
    public boolean onFrame(byte b, byte b2, byte[] bytes, int i, int i2) {
        return false;
    }

    @Override
    public void onHandshake(FrameConnection frameConnection) {

    }

    @Override
    public void onOpen(Connection connection) {
        this.connection = connection;
    }
}
