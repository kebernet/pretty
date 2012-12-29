/**
 * Copyright 2013, Robert Cooper, Reach Health
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 */
/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.reachcall.pretty.peering;

import com.reachcall.pretty.peering.Agent.AgentListener;
import com.reachcall.pretty.peering.beats.Heartbeat;
import java.io.Serializable;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * @author robert.cooper
 */
public class FailWatcherTest {
    
    public FailWatcherTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    /**
     * Test of onHeartbeat method, of class FailWatcher.
     */
    @Test
    public void testOnHeartbeat() throws Exception {
        System.out.println("--------------------------------------watcher test");
        final AgentListener[] listener = new AgentListener[1];
        Agent a = new Agent(6767){

            @Override
            public void addListener(AgentListener l) {
                super.addListener(l);
                listener[0] = l;
            }

            @Override
            public void sendHeartbeat(Serializable object) {
                super.sendHeartbeat(object);
                System.out.println(object);
            }
            
            
        };
        a.start();
        FailWatcher watcher = new FailWatcher(a);
        watcher.setTimeToFailure(1000);
        watcher.start();
        a.sendHeartbeat(new Heartbeat("fake-host"));
        
        System.out.println("\n\n\n");
        Thread.sleep(500);
        a.sendHeartbeat(new Heartbeat("fake-host"));
        System.out.println("\n\n\n");
        Thread.sleep(1500);
        a.sendHeartbeat(new Heartbeat("fake-host"));
        System.out.println("\n\n\n");
        Thread.sleep(2000);
        watcher.stop();
        
    }
}
