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

import com.reachcall.pretty.config.Configurator.ConfiguratorCallback;
import java.io.File;
import javax.xml.bind.JAXBException;
import org.junit.Test;
import static org.junit.Assert.assertEquals;

/**
 *
 * @author kebernet
 */
public class ConfigurationTest {
    
    public ConfigurationTest() {
    }

    

    /**
     * Test of setChildren method, of class Configuration.
     */
    @Test
    public void testSimple() throws JAXBException {
        System.out.println("testSimple");
        final Configuration[] holder = new Configuration[1];
        Configurator c = new Configurator(new ConfiguratorCallback(){

            @Override
            public Configuration activeConfiguration() {
               return holder[0];
            }

            @Override
            public void onLoad(Configuration configuration) {
                holder[0] = configuration;
            }
            
        });
        c.setConfigFile(new File("src/test/resources/simple.xml"));
        c.load();
        Match p = holder[0].apply(null, "rh.reachhealth.com", "/bar", holder[0]);
        assertEquals("/", p.path.getDestination());
        assertEquals("localhost", p.path.getParent().getDestinations().get(0).getHost());
        assertEquals(8080, p.path.getParent().getDestinations().get(0).getPort());
        assertEquals("/bar", p.finalPath);
    }
    
    /**
     * Test of setChildren method, of class Configuration.
     */
    @Test
    public void testHostOverride() throws JAXBException {
        System.out.println("testSimple");
        final Configuration[] holder = new Configuration[1];
        Configurator c = new Configurator(new ConfiguratorCallback(){

            @Override
            public Configuration activeConfiguration() {
               return holder[0];
            }

            @Override
            public void onLoad(Configuration configuration) {
                holder[0] = configuration;
            }
            
        });
        c.setConfigFile(new File("src/test/resources/simple-host-override.xml"));
        c.load();
        long incept = System.currentTimeMillis();
        Match p = holder[0].apply(null, "rh.reachhealth.com", "/", holder[0]);
        assertEquals("/", p.path.getDestination());
        assertEquals("localhost", p.path.getParent().getDestinations().get(0).getHost());
        assertEquals(8080, p.path.getParent().getDestinations().get(0).getPort());
        
        p = holder[0].apply(null, "otherhost.somewhere.com", "/frontend", holder[0]);
        assertEquals("/", p.path.getDestination());
        assertEquals("otherproxyhost", p.path.getParent().getDestinations().get(0).getHost());
        assertEquals(1025, p.path.getParent().getDestinations().get(0).getPort());
        assertEquals("/frontend", p.finalPath);
        
        System.out.println("Matched 2 host/paths in "+(System.currentTimeMillis() - incept) +"ms");
        
    }
    
    @Test
    public void testPathGroups() throws JAXBException {
        System.out.println("testPathGroup");
        final Configuration[] holder = new Configuration[1];
        Configurator c = new Configurator(new ConfiguratorCallback(){

            @Override
            public Configuration activeConfiguration() {
               return holder[0];
            }

            @Override
            public void onLoad(Configuration configuration) {
                holder[0] = configuration;
            }
            
        });
        c.setConfigFile(new File("src/test/resources/path-regex.xml"));
        c.load();
        Match p = holder[0].apply("test", "rh.reachhealth.com", "/frontend", holder[0]);
        assertEquals("/foo/", p.path.getDestination());
        assertEquals("/(.*)", p.path.getSource());
        assertEquals("localhost", p.path.getParent().getDestinations().get(0).getHost());
        assertEquals(8080, p.path.getParent().getDestinations().get(0).getPort());
        assertEquals("/foo/frontend", p.finalPath);
    }

}

    
