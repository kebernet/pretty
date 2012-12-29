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

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 *
 * @author kebernet
 */
public class Resolver {
    private static final Logger LOG = Logger.getLogger(Resolver.class
            .getCanonicalName());
    private Cache<String, CopyOnWriteArrayList<Destination>> affinities;
    private Cache<String, Configuration> configurations;
    private Configuration activeConfiguration;
    private final CopyOnWriteArrayList<ResolverListener> listeners = new CopyOnWriteArrayList<ResolverListener>();
    private final Random random = new Random();
    private final long expirationTime;
    private int inactivesInCache = 0;

    public Resolver(Configuration activeConfiguration, long expirationTime) {
        configurations = CacheBuilder.newBuilder()
                                     .expireAfterAccess(expirationTime,
                TimeUnit.MILLISECONDS)
                                     .concurrencyLevel(128)
                                     .recordStats()
                                     .build();
        affinities = CacheBuilder.newBuilder()
                                 .expireAfterAccess(expirationTime,
                TimeUnit.MILLISECONDS)
                                 .concurrencyLevel(128)
                                 .recordStats()
                                 .build();
        this.expirationTime = expirationTime;
    }

    /**
     * Set the value of activeConfiguration
     *
     * @param newactiveConfiguration new value of activeConfiguration
     */
    public void setActiveConfiguration(Configuration newactiveConfiguration) {
        if (this.activeConfiguration != null) {
            this.activeConfiguration.setInactive(true);
            doTimer();
        }

        this.activeConfiguration = newactiveConfiguration;
        LOG.info("Active configuration changed.");
    }

    /**
     * Get the value of activeConfiguration
     *
     * @return the value of activeConfiguration
     */
    public Configuration getActiveConfiguration() {
        return this.activeConfiguration;
    }

    public long getExipirationTime() {
        return this.expirationTime;
    }

    public int getInactivesInCache() {
        return this.inactivesInCache;
    }

    public void addListener(ResolverListener l) {
        listeners.add(l);
    }

    public void bond(String affinityId, Match serviced)
        throws ExecutionException {
        this.configurations.put(affinityId, serviced.rootConfiguration);

        CopyOnWriteArrayList<Destination> destinations = this.affinities.get(affinityId,
                new Callable<CopyOnWriteArrayList<Destination>>() {
                    @Override
                    public CopyOnWriteArrayList<Destination> call()
                        throws Exception {
                        return new CopyOnWriteArrayList<Destination>();
                    }
                });

        destinations.add(serviced.destination);

        for (ResolverListener l : this.listeners) {
            l.onCreate(affinityId, serviced, destinations);
        }
    }

    public void insert(String sessionToken, Configuration config,
        CopyOnWriteArrayList<Destination> affinities) {
        config.setInactive(true);
        this.configurations.put(sessionToken, config);
        this.affinities.put(sessionToken, affinities);
    }

    public void removeListener(ResolverListener l) {
        listeners.remove(l);
    }

    public Match resolve(String affinityId, final String host, final String path) {
        try {
            Configuration config = (affinityId == null)
                ? this.activeConfiguration
                : configurations.get(affinityId,
                    new Callable<Configuration>() {
                        @Override
                        public Configuration call() throws Exception {
                            return activeConfiguration;
                        }
                    });

            Match m = config.apply(affinityId, host, path, config);

            // This is a new host request
            if (affinityId == null) {
                m.destination = select(m.path.getParent().getDestinations());

                return m;
            }

            CopyOnWriteArrayList<Destination> destinations = this.affinities.get(affinityId,
                    new Callable<CopyOnWriteArrayList<Destination>>() {
                        @Override
                        public CopyOnWriteArrayList<Destination> call()
                            throws Exception {
                            return new CopyOnWriteArrayList<Destination>();
                        }
                    });

            //Search the affinity list
            for (Destination d : destinations) {
                for (Destination match : m.path.getParent()
                                               .getDestinations()) {
                    if (d.equals(match)) {
                        m.destination = d;

                        return m;
                    }
                }
            }

            //No match. Make a new selection.
            Destination d = select(m.path.getParent().getDestinations());
            destinations.add(d);
            m.destination = d;

            for (ResolverListener l : this.listeners) {
                l.onHit(affinityId);
            }

            return m;
        } catch (Exception e) {
            LOG.log(Level.WARNING,
                "Failed to get path " + affinityId + " host:" + host + " path:"
                + path, e);
        }

        throw new RuntimeException("No Match for " + host + " " + path);
    }

    /**
     * Select a destination machine.
     * @param list
     * @return 
     */
    public Destination select(List<Destination> list) {
        return list.get(random.nextInt(list.size()));
    }

    public String stats() {
        String s = new StringBuilder("Affinity Cache \n").append(this.affinities.stats().toString())
                                                         .append("\nCofiguration Cache \n")
                                                         .append(this.configurations.stats().toString())
                                                         .toString();
        LOG.info(s);

        return s;
    }

    private void doTimer() {
        final Timer t = new Timer();
        t.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    inactivesInCache = 0;

                    ConcurrentMap<String, Configuration> map = configurations
                        .asMap();

                    for (Configuration c : map.values()) {
                        if (c.isInactive()) {
                            inactivesInCache++;
                        }
                    }

                    if (inactivesInCache > 0) {
                        LOG.log(Level.WARNING,
                            "SESSIONS ON INACTIVE CONFIGS STILL ALIVE {0}",
                            inactivesInCache);
                    } else {
                        LOG.warning(
                            "NO MORE ACTIVE SESSIONS ON OLD CONFIGURATIONS");
                        t.cancel();
                    }
                }
            }, 0, expirationTime / 4);
    }

    public static interface ResolverListener {
        void onCreate(String token, Match m, List<Destination> affinities);

        void onHit(String token);
    }
}
