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
import groovy.lang.Closure;

import org.codehaus.groovy.tools.shell.Command;
import org.codehaus.groovy.tools.shell.ExitNotification;
import org.codehaus.groovy.tools.shell.Groovysh;
import org.codehaus.groovy.tools.shell.IO;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;

import java.net.Socket;

import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 *
 * @author kebernet
 */
public class ShellTask implements Runnable {
    private static final Logger LOG = Logger.getLogger(ShellTask.class
            .getCanonicalName());
    private final Binding binding;
    private final List<String> defaultScripts;
    private final Socket socket;

    public ShellTask(Socket socket, Binding binding, List<String> defaultScripts) {
        this.socket = socket;
        this.binding = binding;
        this.defaultScripts = defaultScripts;
    }

    public void closeSocket() {
        closeQuietly(socket);
    }

    @Override
    @SuppressWarnings({"unchecked", "serial"})
    public void run() {
        PrintStream out = null;
        InputStream in = null;

        try {
            out = new PrintStream(socket.getOutputStream());
            in = socket.getInputStream();

            binding.setVariable("out", out);

            IO io = new IO(in, out, out);
            Groovysh shell = new Groovysh(binding, io);

            Exception cause = null;

            try {
                loadDefaultScripts(shell);
            } catch (Exception e) {
                LOG.log(Level.SEVERE, "Error while loading default script", e);
                cause = e;
            }

            final Closure<Groovysh> defaultErrorHook = shell.getErrorHook();
            shell.setErrorHook(new Closure<Groovysh>(this) {
                    @Override
                    public Groovysh call(Object... args) {
                        // If we see that the socket is closed, we ask the REPL loop to exit immediately
                        if (socket.isClosed()) {
                            throw new ExitNotification(0);
                        }

                        return defaultErrorHook.call(args);
                    }
                });

            try {
                if (cause != null) {
                    out.println("Unable to load default script: " + cause);
                }

                shell.run();
            } catch (Exception e) {
                LOG.log(Level.SEVERE, "Error while executing client command", e);
            }
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Exception in groovy shell client thread", e);
        } finally {
            closeQuietly(in);
            closeQuietly(out);
            closeQuietly(socket);
        }
    }

    private static void closeQuietly(Closeable object) {
        try {
            object.close();
        } catch (IOException e) {
            LOG.log(Level.WARNING, "Error while closing object", e);
        }
    }

    private static void closeQuietly(Socket socket) {
        try {
            socket.close();
        } catch (IOException e) {
            LOG.log(Level.WARNING, "Error while closing socket", e);
        }
    }

    @SuppressWarnings({"unchecked", "serial"})
    private void loadDefaultScripts(final Groovysh shell) {
        if (!defaultScripts.isEmpty()) {
            Closure<Groovysh> defaultResultHook = shell.getResultHook();

            try {
                // Set a "no-op closure so we don't get per-line value output when evaluating the default script
                shell.setResultHook(new Closure<Groovysh>(this) {
                        @Override
                        public Groovysh call(Object... args) {
                            return shell;
                        }
                    });

                Command cmd = shell.getRegistry()
                                   .find("load");

                for (String script : defaultScripts) {
                    cmd.execute(Arrays.asList(script));
                }
            } finally {
                // Restoring original result hook
                shell.setResultHook(defaultResultHook);
            }
        }
    }
}
