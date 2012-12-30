/**
 * Copyright 2013, Robert Cooper, Reach Health
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 */
package com.reachcall.pretty.peering;

import com.reachcall.pretty.config.Configuration;

import com.reachcall.util.CompressingJAXB;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;

import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Arrays;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import javax.xml.bind.JAXBException;

/**
 *
 * @author kebernet
 */
public class Agent implements AgentMBean {

    public static final byte MESSAGE_CONFIG_UPDATE = 0;
    public static final byte MESSAGE_HEARTBEAT = 1;
    private static final Logger LOG = Logger.getLogger(Agent.class.getCanonicalName());
    private final CompressingJAXB<Configuration> configurationIO;
    private CopyOnWriteArrayList<AgentListener> listeners = new CopyOnWriteArrayList<AgentListener>();
    private ListenerThread listener;
    private String broadcastAddress;
    private int multicastPort;

    public Agent(String broadcastAddress, int multicastPort)
            throws JAXBException {
        this.broadcastAddress = broadcastAddress;
        this.multicastPort = multicastPort;
        this.configurationIO = new CompressingJAXB<Configuration>(Configuration.class);
    }
    
    public Agent(int multicastPort) throws SocketException, JAXBException{
        this.multicastPort = multicastPort;
        this.configurationIO = new CompressingJAXB<Configuration>(Configuration.class);
        this.broadcastAddress = findBroadcast();
        
    }

    private String findBroadcast() throws SocketException {
        return "224.0.0.1";
    }
    
    
    private String networkInterface;

    /**
     * Get the value of networkInterface
     *
     * @return the value of networkInterface
     */
    public String getNetworkInterface() {
        return this.networkInterface;
    }

    /**
     * Set the value of networkInterface
     *
     * @param newnetworkInterface new value of networkInterface
     */
    public void setNetworkInterface(String newnetworkInterface) {
        this.networkInterface = newnetworkInterface;
    }


    /**
     * Set the value of broadcastAddress
     *
     * @param newbroadcastAddress new value of broadcastAddress
     */
    @Override
    public void setBroadcastAddress(String newbroadcastAddress) {
        this.broadcastAddress = newbroadcastAddress;
    }

    /**
     * Get the value of broadcastAddress
     *
     * @return the value of broadcastAddress
     */
    public String getBroadcastAddress() {
        return this.broadcastAddress;
    }

    public ObjectName getJMXObjectName() throws MalformedObjectNameException {
        return new ObjectName(Agent.class.getPackage().getName() + ":type="
            + Agent.class.getSimpleName());
    }

    /**
     * Set the value of multicastPort
     *
     * @param newmulticastPort new value of multicastPort
     */
    @Override
    public void setMulticastPort(int newmulticastPort) {
        this.multicastPort = newmulticastPort;
    }

    /**
     * Get the value of multicastPort
     *
     * @return the value of multicastPort
     */
    public int getMulticastPort() {
        return this.multicastPort;
    }

    public void addListener(AgentListener listener) {
        this.listeners.add(listener);
    }

    public void removeListener(AgentListener listener) {
        this.listeners.remove(listener);
    }

        byte[] serializeConfiguration(Configuration config) throws JAXBException, IOException {
        byte[] data = this.configurationIO.marshall(config);
        byte[] packet = new byte[data.length + 1];
        packet[0] = MESSAGE_CONFIG_UPDATE;
        System.arraycopy(data, 0, packet, 1, data.length);
        return packet;
    }

    public void sendConfiguration(Configuration config) {
        try {
            if (this.listener == null) {
                throw new RuntimeException("Agent not started.");
            }

            byte[] data = serializeConfiguration(config);

            DatagramPacket dp = new DatagramPacket(data, data.length,
                    listener.group, multicastPort);
            listener.socket.send(dp);
            LOG.info("Configuration sent.");
        } catch (Exception ex) {
            Logger.getLogger(Agent.class.getName()).log(Level.SEVERE, "Failed to send configuration update.", ex);
        }
    }

    byte[] serializeObject(Serializable object) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        baos.write(new byte[]{MESSAGE_HEARTBEAT});
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(object);
        oos.close();
        return baos.toByteArray();
    }

    public void sendHeartbeat(Serializable object) {
        try {
            byte[] data = serializeObject(object);
            DatagramPacket dp = new DatagramPacket(data, data.length,
                    listener.group, multicastPort);
            listener.socket.send(dp);
            LOG.log(Level.FINE, "Heartbeat sent. {0}", object.getClass().getName());

        } catch (IOException ex) {
            Logger.getLogger(Agent.class.getName())
                  .log(Level.SEVERE, "Failed to send heartbeat.", ex);
        }
    }


    @Override
    public void start() throws IOException {
        if ((this.listener != null) && this.listener.running) {
            throw new RuntimeException("Already started.");
        }

        this.listener = new ListenerThread();
        this.listener.running = true;
        this.listener.start();
    }

    @Override
    public void stop() {
        if ((this.listener == null) || !this.listener.running) {
            throw new RuntimeException("Already stopped.");
        }

        this.listener.running = false;
        this.listener.interrupt();
    }

    Configuration deserializeConfig(byte[] data) throws JAXBException, IOException {
        Configuration config = this.configurationIO.unmarshall(Arrays.copyOfRange(data, 1, data.length));
        return config;
    }

    Object deserializeObject(byte[] data) throws IOException, ClassNotFoundException {
        ObjectInputStream is = new ObjectInputStream(new ByteArrayInputStream(data, 1, data.length));
        return is.readObject();
    }

    
    private void processConfig(byte[] data) {
        try {
            Configuration config = this.deserializeConfig(data);
            LOG.info("Got configuration broadcast.");
            for (AgentListener l : listeners) {
                l.onConfiguration(config);
            }
        } catch (Exception ex) {
            Logger.getLogger(Agent.class.getName()).log(Level.SEVERE,
                    "Exception receiving configuration package \n"
                    + new String(data), ex);
        }
    }

    private void processHeartbeat(byte[] data) {
        try {
            Object o = this.deserializeObject(data);
            LOG.log(Level.FINE, "Got Message: {0}", o);
            for (AgentListener l : listeners) {
                l.onHeartbeat(o);
            }
        } catch (Exception ex) {
            LOG.log(Level.SEVERE, "Unable to deserialize heartbeat", ex);
        } 
    }

    
    public static interface AgentListener {

        void onConfiguration(Configuration config);

        void onHeartbeat(Object remote);
    }

    private class ListenerThread extends Thread {

        InetAddress group;
        MulticastSocket socket;
        ThreadPoolExecutor exec = new ThreadPoolExecutor(10, 20, 2,
                TimeUnit.MINUTES, new LinkedBlockingQueue(20));
        volatile boolean running;

        ListenerThread() throws IOException {
            socket = new MulticastSocket(multicastPort);
            group = InetAddress.getByName(broadcastAddress);
            LOG.log(Level.INFO, "Broadcast address {0}:{1}", new Object[]{broadcastAddress, multicastPort});
            if(networkInterface != null){
                LOG.log(Level.INFO, "Network interface {0}", networkInterface);
                socket.setNetworkInterface(NetworkInterface.getByName(networkInterface));
            }
        }

        @Override
        public void run() {
            try {
                //socket.joinGroup(group);
                while (running) {
                    byte[] buffer = new byte[1048576];
                    DatagramPacket packet = new DatagramPacket(buffer,
                            buffer.length);
                    LOG.finest("Waiting for broadcast");
                    socket.receive(packet);
                    LOG.finest("Got broadcast.");
                    final byte[] result = packet.getData();
                    Runnable r = new Runnable() {

                        @Override
                        public void run() {


                            switch (result[0]) {
                                case MESSAGE_CONFIG_UPDATE:
                                    processConfig(result);

                                    break;

                                case MESSAGE_HEARTBEAT:
                                    processHeartbeat(result);

                                    break;

                                default:
                                    LOG.log(Level.SEVERE,
                                            "Unknown Agent control message {0}",
                                            result[0]);
                            }
                        }
                    };

                    this.exec.execute(r);
                }
            } catch (Exception e) {
                LOG.log(Level.WARNING, "Exception in monitor thread.");

                if (running) {
                    run();
                }
            }

            try {
                socket.leaveGroup(group);
            } catch (IOException ex) {
                Logger.getLogger(Agent.class.getName()).log(Level.WARNING,
                        "Exception shutting down peering agent... (may not matter)",
                        ex);
            }

            socket.close();
        }
    }
}
