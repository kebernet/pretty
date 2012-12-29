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

import com.reachcall.pretty.peering.beats.Heartbeat;

import java.net.InetAddress;
import java.net.UnknownHostException;

import java.util.Timer;
import java.util.TimerTask;


/**
 *
 * @author robert.cooper
 */
public class Heart {
    private String hostname;
    private Timer timer;
    private Agent agent;

    public Heart(Agent agent) throws UnknownHostException {
        this(null, agent);
    }

    public Heart(String hostname, Agent agent) throws UnknownHostException {
        this.hostname = (hostname == null) ? getHostname() : hostname;
        this.agent = agent;
    }

    private String getHostname() throws UnknownHostException {
        InetAddress addr = InetAddress.getLocalHost();

        return addr.getHostName();
    }

    public Heartbeat buildHeartbeat() {
        return new Heartbeat(this.hostname);
    }

    public void start() {
        TimerTask task = new TimerTask() {
                @Override
                public void run() {
                    agent.sendHeartbeat(buildHeartbeat());
                }
            };

        timer = new Timer();
        timer.scheduleAtFixedRate(task, 0, 500);
    }

    public void stop() {
        timer.cancel();
        timer = null;
    }
}
