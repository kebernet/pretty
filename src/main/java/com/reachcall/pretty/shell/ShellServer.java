/**
 * Copyright 2013, Robert Cooper, Reach Health
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 */
package com.reachcall.pretty.shell;

import groovy.lang.Binding;

import java.io.IOException;

import java.lang.management.ManagementFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.management.JMException;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;


/**
 *
 * @author kebernet
 */
public class ShellServer implements ShellServerMBean {
    private static final Logger LOG = Logger.getLogger(ShellServer.class
            .getCanonicalName());
    private Acceptor groovyShellAcceptor;
    private List<String> defaultScripts = new ArrayList<String>();
    private Map<String, Object> bindings;
    private Thread acceptorThread;
    private int port;

    public ShellServer(int port) {
        assert port > 0 && port <= 65535 : "Invalid port number.";
        this.port = port;
    }

    public void setBindings(Map<String, Object> bindings) {
        this.bindings = bindings;
    }

    public Map<String, Object> getBindings() {
        return bindings;
    }

    /**
     * Set the comma delimited list of default scripts
     *
     * @param scriptNames script names
     */
    public void setDefaultScripts(String scriptNames) {
        defaultScripts = Arrays.asList(scriptNames.split(","));
    }

    /**
     * @return complete List of scripts to be executed for each new client session
     */
    public List<String> getDefaultScripts() {
        return defaultScripts;
    }

    
    /**
     * Adds a groovy script to be executed for each new client session.
     *
     * @param script script
     */
    public void addDefaultScript(String script) {
        defaultScripts.add(script);
    }

    public synchronized void destroy() throws InterruptedException {
        if (acceptorThread != null) {
            acceptorThread.interrupt();
            acceptorThread.join();

            try {
                ManagementFactory.getPlatformMBeanServer()
                                 .unregisterMBean(getJMXObjectName());
            } catch (JMException e) {
                LOG.log(Level.WARNING,
                    "Failed to unregister GroovyShellService MBean", e);
            }

            acceptorThread = null;
        }
    }

    @Override
    public void killAllClients() {
        groovyShellAcceptor.killAllClients();
    }

    /**
     * Opens a server socket and starts a new Thread to accept client connections.
     *
     * @throws IOException thrown if socket cannot be opened
     */
    @Override
    public synchronized void start() throws IOException {
        if (acceptorThread == null) {
            groovyShellAcceptor = new Acceptor(port,
                    createBinding(bindings), defaultScripts);
            acceptorThread = new Thread(groovyShellAcceptor,
                    "GroovyShAcceptor-" + port);
            acceptorThread.start();
        }
    }
    
    @Override
    public synchronized void stop(){
        acceptorThread.stop();
        acceptorThread = null;
    }

    public ObjectName getJMXObjectName() throws MalformedObjectNameException {
        return new ObjectName(getClass().getName() + ":port=" + port);
    }

    private static Binding createBinding(Map<String, Object> objects) {
        Binding binding = new Binding();

        if (objects != null) {
            for (Map.Entry<String, Object> row : objects.entrySet()) {
                binding.setVariable(row.getKey(), row.getValue());
            }
        }

        return binding;
    }
}
