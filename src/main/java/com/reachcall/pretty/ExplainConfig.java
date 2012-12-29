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
