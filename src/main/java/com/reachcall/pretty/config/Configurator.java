/**
 * Copyright 2013, Robert Cooper, Reach Health
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 */
package com.reachcall.pretty.config;

import java.io.File;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;


/**
 *
 * @author kebernet
 */
public class Configurator implements ConfiguratorMBean {
    private static final Logger LOG = Logger.getLogger(Configurator.class
            .getCanonicalName());
    private final ConfiguratorCallback callback;
    private final JAXBContext context;
    private File configFile;
    private Watcher watcher;

    public Configurator(ConfiguratorCallback callback)
        throws JAXBException {
        this.callback = callback;
        this.context = JAXBContext.newInstance(Configuration.class);
    }

    /**
     * Set the value of configFile
     *
     * @param newconfigFile new value of configFile
     */
    @Override
    public void setConfigFile(File newconfigFile) {
        this.configFile = newconfigFile;
        LOG.log(Level.INFO, "Configuration file set: {0}", newconfigFile);
    }

    /**
     * Get the value of configFile
     *
     * @return the value of configFile
     */
    @Override
    public File getConfigFile() {
        return this.configFile;
    }

    @Override
    public void load() throws JAXBException {
        Unmarshaller u = this.context.createUnmarshaller();
        
        Configuration config = (Configuration) u.unmarshal(configFile);

        try {
            Match m = config.apply(null, "THIS-IS-AN-UNKNOWN-HOST", "/", config);
            LOG.info("New configuration loaded.");
            LOG.log(Level.INFO, "Likely default route {0}:{1} to {2}",
                new Object[] {
                    m.path.getParent()
                          .getDestinations()
                          .get(0)
                          .getHost(),
                    m.path.getParent()
                          .getDestinations()
                          .get(0)
                          .getPort(), m.finalPath
                });
        } catch (Exception e) {
            throw new RuntimeException("Likely missing a final path in config.");
        }

        this.callback.onLoad(config);
    }

    @Override
    public void save() throws JAXBException {
        Marshaller m = this.context.createMarshaller();
        m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
        m.marshal(this.callback.activeConfiguration(), this.configFile);
        LOG.log(Level.INFO, "Configuration saved to {0}", this.configFile);
    }

    @Override
    public void startWatching() {
        if ((watcher != null) && watcher.running) {
            return;
        }

        watcher = new Watcher();
        watcher.running = true;
        watcher.start();
        LOG.info("Config file watcher starting up.");
    }

    @Override
    public void stopWatching() {
        if ((watcher != null) && watcher.running) {
            watcher.running = false;
            watcher.interrupt();
            LOG.info("Config file watcher shutting down.");
        }
    }

    public static interface ConfiguratorCallback {
        Configuration activeConfiguration();

        void onLoad(Configuration configuration);
    }

    private class Watcher extends Thread {
        public volatile boolean running;
        private long configLastModified = System.currentTimeMillis();

        {
            setDaemon(true);
        }

        @Override
        public void run() {
            while (running) {
                try {
                    Thread.sleep(5000);

                    if (configFile.lastModified() > configLastModified) {
                        try {
                            load();
                            configLastModified = configFile.lastModified();
                        } catch (JAXBException ex) {
                            LOG.log(Level.SEVERE,
                                "Configuration watcher failed to reload the config file "
                                + configFile.getAbsolutePath(), ex);
                        }
                    }
                } catch (InterruptedException ex) {
                    LOG.log(Level.WARNING, null, ex);
                }
            }
        }
    }
}
