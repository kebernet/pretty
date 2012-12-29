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

import com.reachcall.pretty.peering.FailWatcher.FailListener;
import groovy.lang.Binding;
import groovy.lang.GroovyShell;

import java.io.File;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 *
 * @author robert.cooper
 */
public class FailHandler implements FailListener{
    private static final Logger LOG = Logger.getLogger(FailHandler.class.getCanonicalName());
    private Map<String, Object> bindings = new HashMap<String, Object>();
    private File failScriptDirectory = new File("/etc/pretty/fail-scripts");

    /**
     * Get the value of failScriptDirectory
     *
     * @return the value of failScriptDirectory
     */
    public File getFailScriptDirectory() {
        return this.failScriptDirectory;
    }

    /**
     * Set the value of failScriptDirectory
     *
     * @param newfailScriptDirectory new value of failScriptDirectory
     */
    public void setFailScriptDirectory(File newfailScriptDirectory) {
        this.failScriptDirectory = newfailScriptDirectory;
    }

    /**
     * Get the value of bindings
     *
     * @return the value of bindings
     */
    public Map<String, Object> getBindings() {
        return this.bindings;
    }

    /**
     * Set the value of bindings
     *
     * @param newbindings new value of bindings
     */
    public void setBindings(Map<String, Object> newbindings) {
        this.bindings = newbindings;
    }

    public void runScript(File file) {
        LOG.log(Level.INFO, "Evaludating failover file {0}", file);
        try {
            Binding binding = new Binding();

            for (Entry<String, Object> entry : this.bindings.entrySet()) {
                binding.setProperty(entry.getKey(), entry.getValue());
            }

            GroovyShell shell = new GroovyShell(binding);
            shell.evaluate(file);
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Failure running script " + file, e);
        }
    }

    @Override
    public void onFailure(String hostname) {
        LOG.log(Level.INFO, "Starting failover for {0}", hostname);
        if(!this.failScriptDirectory.exists() || !this.failScriptDirectory.isDirectory()){
            LOG.log(Level.WARNING, "{0} doesn''t exist or is not a directory.", this.failScriptDirectory);
            return;
        }
        try{
            for(File f : this.failScriptDirectory.listFiles()){
                if(f.getName().equalsIgnoreCase(hostname+".groovy")){
                    runScript(f);
                    return;
                }
            }
            LOG.log(Level.WARNING, "NO FAILOVER SCRIPT AVAILABLE FOR {0}", hostname);
        } catch(Exception e){
            LOG.log(Level.SEVERE, "UNABLE TO HANDLE FAILOVER FOR "+hostname, e);
        }
    }
}
