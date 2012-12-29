package com.reachcall.pretty.shell;

import groovy.lang.Binding;

import java.io.IOException;

import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;

import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 *
 * @author kebernet
 */
public class Acceptor implements Runnable {
    private static final Logger LOG = Logger.getLogger(Acceptor.class
            .getCanonicalName());
    private static int clientSequence = 0;
    private final Binding binding;
    private final ServerSocket serverSocket;
    private List<ShellThread> clientThreads = new LinkedList<ShellThread>();
    private List<String> defaultScripts;

    public Acceptor(int port, Binding binding, List<String> defaultScripts)
        throws IOException {
        assert port > 0 : "Port must be positive.";
        
        serverSocket = new ServerSocket(port, 10,
                InetAddress.getByName("127.0.0.1"));
        serverSocket.setSoTimeout(1000);
        this.binding = binding;
        this.defaultScripts = defaultScripts;
    }

    public synchronized void killAllClients() {
        for (ShellThread thread : clientThreads) {
            thread.kill();
        }

        clientThreads.clear();
    }

    @Override
    public void run() {
        try {
            LOG.log(Level.INFO, "Groovy shell started on {0}", serverSocket);

            while (!Thread.currentThread()
                              .isInterrupted()) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    LOG.log(Level.INFO, "Groovy shell client connect: {0}",
                        clientSocket);

                    synchronized (this) {
                        ShellTask clientTask = new ShellTask(clientSocket,
                                binding, defaultScripts);
                        ShellThread clientThread = new ShellThread(clientTask,
                                "GroovyShClient-" + clientSequence++);
                        clientThreads.add(clientThread);

                        clientThread.start();

                        LOG.log(Level.INFO, "Groovy shell started: {0}",
                            clientThread.getName());
                    }
                } catch (SocketTimeoutException e) {
                    // Didn't receive a client connection within the SoTimeout interval ... continue with another
                    // accept call if the thread wasn't interrupted
                    Thread.yield();
                } catch (SocketException e) {
                    LOG.log(Level.WARNING, "Stopping groovy shell", e);

                    break;
                }
            }
        } catch (Exception e) {
            LOG.log(Level.WARNING,
                "Error in shell dispatcher thread.", e);
        } finally {
            killAllClients();
            closeQuietly(serverSocket);
            LOG.info("Groovy shell stopped");
        }
    }

    private static void closeQuietly(ServerSocket socket) {
        try {
            socket.close();
        } catch (IOException e) {
            LOG.log(Level.WARNING, "Error while closing socket", e);
        }
    }
}
