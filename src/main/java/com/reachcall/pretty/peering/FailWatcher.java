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

import com.reachcall.pretty.config.Configuration;
import com.reachcall.pretty.peering.Agent.AgentListener;
import com.reachcall.pretty.peering.beats.Heartbeat;
import com.reachcall.pretty.peering.beats.Initiative;
import com.reachcall.pretty.peering.beats.RollCall;

import java.net.InetAddress;
import java.net.UnknownHostException;

import java.util.Date;
import java.util.Map.Entry;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;


/**
 *
 * @author robert.cooper
 */
public class FailWatcher implements AgentListener, FailWatcherMBean {
    private static final Logger LOG = Logger.getLogger(FailWatcher.class.getCanonicalName());
    private ConcurrentHashMap<String, Long> lastBeats = new ConcurrentHashMap<String, Long>();
    private ConcurrentHashMap<String, RollCall> openRolls = new ConcurrentHashMap<String, RollCall>();
    private ConcurrentHashMap<String, Initiative> myRolls = new ConcurrentHashMap<String, Initiative>();
    private final String hostname;
    private final Agent agent;
    private Timer countBeats;
    private int timeToFailure = 1500;
    private final CopyOnWriteArrayList<FailListener> listeners = new CopyOnWriteArrayList<FailListener>();

    public FailWatcher(String localhostname, Agent agent)
        throws UnknownHostException {
        this.hostname = (localhostname == null) ? getHostname() : localhostname;
        this.agent = agent;
    }

    public FailWatcher(Agent agent) throws UnknownHostException {
        this(null, agent);
    }

    private String getHostname() throws UnknownHostException {
        InetAddress addr = InetAddress.getLocalHost();

        return addr.getHostName();
    }

    @Override
    public void start() {
        TimerTask task = new TimerTask() {
                @Override
                public void run() {
                    
                    for (Entry<String, Long> entry : lastBeats.entrySet()) {
                        
                        if ((System.currentTimeMillis() - entry.getValue()) > timeToFailure) {
                            LOG.log(Level.SEVERE,
                                "Last respone from {0} at {1} exceeds timeout {2}. Beginning failover.",
                                new Object[] {
                                    entry.getKey(), new Date(entry.getValue()),
                                    timeToFailure
                                });
                            lastBeats.remove(entry.getKey());
                            callForRoll(entry.getKey());
                        }
                    }
                }
            };

        this.countBeats = new Timer();
        this.countBeats.scheduleAtFixedRate(task, 0, 250);
        this.agent.addListener(this);
    }

    public ObjectName getJMXObjectName() throws MalformedObjectNameException {
        return new ObjectName(FailWatcher.class.getPackage().getName() + ":type="
            + FailWatcher.class.getSimpleName());
    }
    
    @Override
    public void stop() {
        this.countBeats.cancel();
        this.countBeats = null;
        agent.removeListener(this);
    }

    private void callForRoll(String hostname) {
        RollCall call = new RollCall(hostname,
                System.currentTimeMillis() + timeToFailure * 2);
        agent.sendHeartbeat(call);
    }

    private void handleCallForRoll(RollCall call) {
        if (System.currentTimeMillis() < call.windowEnds) {
            this.openRolls.put(call.replacingHostname, call);

            final Initiative roll = new Initiative(hostname,
                    call.replacingHostname);
            agent.sendHeartbeat(roll);
            this.myRolls.put(roll.toReplaceInstance, roll);
            long endDelta = (call.windowEnds + 100 - System.currentTimeMillis());
            LOG.log(Level.INFO, "CALL FOR ROLL {0} ENDS IN {1}", new Object[]{call.replacingHostname, endDelta});
            Timer t = new Timer();
            t.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        LOG.log(Level.INFO, "END OF ROLL FOR {0}", roll.toReplaceInstance);
                        if (!openRolls.containsKey(roll.toReplaceInstance)) {
                            LOG.log(Level.SEVERE,
                                "END OF ROLL FOR {0} BUT THE ROLL HAS RESOLVED.",
                                roll.toReplaceInstance);

                            return;
                        }

                        if (!myRolls.containsKey(roll.toReplaceInstance)) {
                            LOG.log(Level.SEVERE,
                                "END OF ROLL FOR {0} BUT I LOST.",
                                roll.toReplaceInstance);

                            return;
                        }

                        LOG.log(Level.SEVERE, "TAKING OVER FOR {0}",
                            roll.toReplaceInstance);
                        openRolls.remove(roll.toReplaceInstance);
                        myRolls.remove(roll.toReplaceInstance);
                        for(FailListener l : listeners){
                            l.onFailure(roll.toReplaceInstance);
                        }
                    }
                }, endDelta );
        }
    }

    private void handleRoll(Initiative roll) {
        if (roll.instanceName.equals(hostname)) {
            return;
        }

        Initiative myRoll = this.myRolls.get(roll.toReplaceInstance);

        if (myRoll == null) {
            LOG.log(Level.INFO, "Passe roll for {0}", roll.toReplaceInstance);
        }

        if (myRoll.roll < roll.roll) {
            LOG.log(Level.INFO, "{0} beats {1} for replacing lost host {2}",
                new Object[] { roll.instanceName, hostname, roll.toReplaceInstance });
            this.openRolls.remove(roll.toReplaceInstance);
            myRolls.remove(roll.toReplaceInstance);
        } else {
            LOG.log(Level.INFO, "I beat {0} for replacing lost host {1}",
                new Object[] { roll.instanceName, roll.toReplaceInstance });
        }
    }

    private void handleHeartbeat(Heartbeat h) {
        if ((System.currentTimeMillis() - h.timestamp) > (this.timeToFailure * 2)) {
            LOG.log(Level.SEVERE,
                "The time difference between this host and {0} seems to be too high!",
                h.instanceName);
        }
        LOG.log(Level.FINE, "Heartbeat from {0}", h.instanceName);
        if(!lastBeats.containsKey(h.instanceName)){
            LOG.log(Level.INFO, "Hearbeat received from new host {0}", h.instanceName);
        }
        this.lastBeats.put(h.instanceName, System.currentTimeMillis());

        if (this.openRolls.containsKey(h.instanceName)) {
            this.myRolls.remove(h.instanceName);
            this.openRolls.remove(h.instanceName);
            LOG.log(Level.WARNING,
                "CANCELLING OPEN ROLL FOR {0} as it has sent a heartbeat.",
                h.instanceName);
            
        }
    }

    /**
     * Get the value of timeToFailure
     *
     * @return the value of timeToFailure
     */
    public int getTimeToFailure() {
        return this.timeToFailure;
    }

    /**
     * Set the value of timeToFailure
     *
     * @param newtimeToFailure new value of timeToFailure
     */
    public void setTimeToFailure(int newtimeToFailure) {
        this.timeToFailure = newtimeToFailure;
    }

    @Override
    public void onConfiguration(Configuration config) {
    }

    @Override
    public void onHeartbeat(Object remote) {
        if (remote instanceof Heartbeat) {
            Heartbeat h = (Heartbeat) remote;
            handleHeartbeat(h);
        } else if (remote instanceof RollCall) {
            RollCall call = (RollCall) remote;
            handleCallForRoll(call);
        } else if (remote instanceof Initiative) {
            Initiative roll = (Initiative) remote;
            handleRoll(roll);
        }
    }

    public void addListener(FailListener listener) {
        this.listeners.add(listener);
    }

    public void removeListener(FailListener listener) {
        this.listeners.remove(listener);
    }

    public static interface FailListener {
        void onFailure(String hostname);
    }
}
