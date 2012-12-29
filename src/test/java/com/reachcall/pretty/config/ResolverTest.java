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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.bind.JAXBException;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 *
 * @author kebernet
 */
public class ResolverTest {
    
    public ResolverTest() {
    }

    
    
    @Test
    public void testGetActiveConfiguration() throws JAXBException, InterruptedException {
        final Resolver r = new Resolver(null, 500);
        Configurator configurator = new Configurator( new ConfiguratorCallback(){

            @Override
            public Configuration activeConfiguration() {
                return r.getActiveConfiguration();
            }

            @Override
            public void onLoad(Configuration configuration) {
                r.setActiveConfiguration(configuration);
            }
            
        });
        configurator.setConfigFile(new File("src/test/resources/simple-host-override.xml"));
        configurator.load();
        // Here we are going to make a couple of requests for a couple of different session IDs.
        
        Match m = r.resolve("123", "foo.com", "/somePath");
        m.path.getParent().getDestinations().get(0).getHost();
        m = r.resolve("234", "bar.com", "/somePath");
        m.path.getParent().getDestinations().get(0).getHost();
        m = r.resolve("keepalive", "otherhost.com", "/otherpath");
        m.path.getParent().getDestinations().get(0).getHost();
        
        // Sleep for a bit.
        Thread.sleep(250);
        
        // Load the Simple.xml that doesn't have "otherhost"
        configurator.setConfigFile(new File("src/test/resources/simple.xml"));
        configurator.load();
        
        // Now we expect the resolver has started the timer.
        // let's refresh otherhost
        m = r.resolve("keepalive", "otherhost.com", "/whateverpath");
        m.path.getParent().getDestinations().get(0).getHost();
        Thread.sleep(400); //Make sure things expire
        m = r.resolve("keepalive", "otherhost.com", "/some/new/path");
        
        //No Keepalive should be the new simple default path to localhost
        m.path.getParent().getDestinations().get(0).getHost();
        Match different = r.resolve("nokeepalive", "otherhost.com", "/whatevs");
        
        assertEquals("otherproxyhost", m.path.getParent().getDestinations().get(0).getHost());
        assertEquals("localhost", different.path.getParent().getDestinations().get(0).getHost());
        m = r.resolve("123", "foo.com", "/somePath");
        assertEquals("localhost", m.path.getParent().getDestinations().get(0).getHost());
        Thread.sleep(2100);
        
    }
    
    
    @Test
    public void testAffinity() throws Exception {
        System.out.println("----------------------- simple affinity test");
        final Resolver r = new Resolver(null, 500);
        Configurator configurator = new Configurator( new ConfiguratorCallback(){

            @Override
            public Configuration activeConfiguration() {
                return r.getActiveConfiguration();
            }

            @Override
            public void onLoad(Configuration configuration) {
                r.setActiveConfiguration(configuration);
            }
            
        });
        configurator.setConfigFile(new File("src/test/resources/two-destinations.xml"));
        configurator.load();
        ThreadPoolExecutor exec = new ThreadPoolExecutor(20, 20, 1, TimeUnit.MINUTES, new LinkedBlockingQueue(100));
        final List<Exception> failures = new ArrayList<Exception>();
        for(int i = 0; i < 100; i++){
            final int session = i;
            Runnable task = new Runnable(){

                @Override
                public void run() {
                    try {
                        String tok = "session"+session;
                        Match m = r.resolve(null, "testhost", "/");
                        r.bond(tok, m);
                        System.out.println(tok+" "+m.destination.getHost());
                        Match m2 = r.resolve(tok, "testhost", "/foo");
                        assert m2.destination.equals(m.destination);
                        
                        m2 = r.resolve(tok, "testhost", "/foo2");
                        assert m2.destination.equals(m.destination);
                        
                        m2 = r.resolve(tok, "testhost", "/something else");
                        assert m2.destination.equals(m.destination);
                    } catch (ExecutionException ex) {
                        failures.add(ex);
                    }
                    
                }
                
            };
            exec.execute(task);
            
        }
       
        exec.shutdown();
        exec.awaitTermination(2, TimeUnit.MINUTES);
        for(Exception e : failures){
            e.printStackTrace();
            throw e;
        }
        
        
        
    }
    

    @Test
    public void testAffinity2() throws Exception {
        System.out.println("----------------------- three host affinity test");
        final Resolver r = new Resolver(null, 500);
        Configurator configurator = new Configurator( new ConfiguratorCallback(){

            @Override
            public Configuration activeConfiguration() {
                return r.getActiveConfiguration();
            }

            @Override
            public void onLoad(Configuration configuration) {
                r.setActiveConfiguration(configuration);
            }
            
        });
        configurator.setConfigFile(new File("src/test/resources/three-destinations.xml"));
        configurator.load();
        ThreadPoolExecutor exec = new ThreadPoolExecutor(20, 20, 1, TimeUnit.MINUTES, new LinkedBlockingQueue(100));
        final List<Exception> failures = new ArrayList<Exception>();
        for(int i = 0; i < 100; i++){
            final int session = i;
            Runnable task = new Runnable(){

                @Override
                public void run() {
                    try {
                        String tok = "session"+session;
                        Match m = r.resolve(null, "testhost", "/");
                        r.bond(tok, m);
                        System.out.println(tok+" "+m.destination.getHost());
                        Match m2 = r.resolve(tok, "testhost", "/foo");
                        assert m2.destination.equals(m.destination);
                        
                        m2 = r.resolve(tok, "testhost", "/foo2");
                        assert m2.destination.equals(m.destination);
                        
                        m2 = r.resolve(tok, "testhost", "/something else");
                        assert m2.destination.equals(m.destination);
                    } catch (ExecutionException ex) {
                        failures.add(ex);
                    }
                    
                }
                
            };
            exec.execute(task);
            
        }
       
        exec.shutdown();
        exec.awaitTermination(2, TimeUnit.MINUTES);
        for(Exception e : failures){
            e.printStackTrace();
            throw e;
        }
        
        
        
    }
    
    
    @Test
    public void testAffinity3() throws Exception {
        System.out.println("----------------------- affinity with migration test");
        final Resolver r = new Resolver(null, 500);
        final Configurator configurator = new Configurator( new ConfiguratorCallback(){

            @Override
            public Configuration activeConfiguration() {
                return r.getActiveConfiguration();
            }

            @Override
            public void onLoad(Configuration configuration) {
                r.setActiveConfiguration(configuration);
            }
            
        });
        configurator.setConfigFile(new File("src/test/resources/three-destinations.xml"));
        configurator.load();
        ThreadPoolExecutor exec = new ThreadPoolExecutor(20, 20, 1, TimeUnit.MINUTES, new LinkedBlockingQueue(200));
        final List<Exception> failures = new ArrayList<Exception>();
        for(int i = 0; i < 200; i++){
            final int session = i;
            Runnable task = new Runnable(){

                @Override
                public void run() {
                    try {
                        if(session == 50){
                            Thread.sleep(200);
                            configurator.setConfigFile(new File("src/test/resources/two-destinations.xml"));
                            configurator.load();
                        }
                        String tok = "session"+session;
                        Match m = r.resolve(null, "testhost", "/");
                        r.bond(tok, m);
                        System.out.println(tok+" "+m.destination.getHost());
                        Match m2 = r.resolve(tok, "testhost", "/foo");
                        assert m2.destination.equals(m.destination);
                        
                        m2 = r.resolve(tok, "testhost", "/foo2");
                        assert m2.destination.equals(m.destination);
                        Thread.sleep(200);
                        m2 = r.resolve(tok, "testhost", "/something else");
                        assert m2.destination.equals(m.destination);
                        
                         m2 = r.resolve(tok, "testhost", "/baz");
                        assert m2.destination.equals(m.destination);
                        
                        m2 = r.resolve(tok, "testhost", "/fak");
                        assert m2.destination.equals(m.destination);
                        Thread.sleep(200);
                        m2 = r.resolve(tok, "testhost", "/something/else");
                        assert m2.destination.equals(m.destination);
                    } catch (InterruptedException ex) {
                        Logger.getLogger(ResolverTest.class.getName()).log(Level.SEVERE, null, ex);
                    } catch (JAXBException ex) {
                        Logger.getLogger(ResolverTest.class.getName()).log(Level.SEVERE, null, ex);
                    } catch (ExecutionException ex) {
                        failures.add(ex);
                    }
                    
                }
                
            };
            exec.execute(task);
            
        }
       
        exec.shutdown();
        exec.awaitTermination(2, TimeUnit.MINUTES);
        for(Exception e : failures){
            e.printStackTrace();
            throw e;
        }
        
        
        
    }
    
}
