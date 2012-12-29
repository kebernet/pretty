/**
 * Copyright 2013, Robert Cooper, Reach Health
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 */
package com.reachcall.pretty;

import com.beust.jcommander.Parameter;

import java.io.File;


/**
 *
 * @author kebernet
 */
public class ExplainConfig {
    @Parameter(names =  {
        "--config", "-c"}
    , converter = FileConverter.class, description = "The configraiton xml to evaluate from.")
    private File configFile;
    @Parameter(names =  {
        "--url", "-u"}
    , description = "The URL to resolve in the new configuration file")
    private String url;

    /**
     * Set the value of configFile
     *
     * @param newconfigFile new value of configFile
     */
    public void setConfigFile(File newconfigFile) {
        this.configFile = newconfigFile;
    }

    /**
     * Get the value of configFile
     *
     * @return the value of configFile
     */
    public File getConfigFile() {
        return this.configFile;
    }

    /**
     * Set the value of url
     *
     * @param newurl new value of url
     */
    public void setUrl(String newurl) {
        this.url = newurl;
    }

    /**
     * Get the value of url
     *
     * @return the value of url
     */
    public String getUrl() {
        return this.url;
    }
}
