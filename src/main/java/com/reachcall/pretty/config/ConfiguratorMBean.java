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
