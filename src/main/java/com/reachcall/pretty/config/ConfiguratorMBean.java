package com.reachcall.pretty.config;

import java.io.File;

import javax.xml.bind.JAXBException;


/**
 *
 * @author kebernet
 */
public interface ConfiguratorMBean {
    public void setConfigFile(File newconfigFile);

    public File getConfigFile();

    public void load() throws JAXBException;

    public void save() throws JAXBException;
    
    public void startWatching();
    
    public void stopWatching();
}
