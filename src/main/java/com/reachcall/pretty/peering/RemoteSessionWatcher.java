/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.reachcall.pretty.peering;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import com.reachcall.pretty.config.Configuration;
import com.reachcall.pretty.config.Destination;
import com.reachcall.pretty.config.Match;
import com.reachcall.pretty.config.Resolver;
import com.reachcall.pretty.config.Resolver.ResolverListener;
import com.reachcall.pretty.peering.Agent.AgentListener;
import com.reachcall.pretty.peering.FailWatcher.FailListener;
import com.reachcall.pretty.peering.beats.Session;

import com.reachcall.util.CompressingJAXB;

import java.net.InetAddress;
import java.net.UnknownHostException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import javax.xml.bind.JAXBException;


/**
 *
 * @author robert.cooper
 */
public class RemoteSessionWatcher implements FailListener, AgentListener,
    ResolverListener {
    private static final Logger LOG = Logger.getLogger(RemoteSessionWatcher.class
            .getCanonicalName());
    private final Agent agent;
    private final Cache<String, Cache<String, Session>> cache;
    private final Callable<Cache<String, Session>> callable = new Callable<Cache<String, Session>>() {
            @Override
            public Cache<String, Session> call() throws Exception {
                return CacheBuilder.newBuilder()
                                   .expireAfterAccess(expirationTime,
                    TimeUnit.MILLISECONDS)
                                   .concurrencyLevel(128)
                                   .recordStats()
                                   .build();
            }
        };

    private final CompressingJAXB<Configuration> jaxb;
    private final Resolver resolver;
    private final String localhostname;
    private final ThreadPoolExecutor exec = new ThreadPoolExecutor(5, 20, 1,
            TimeUnit.MINUTES, new LinkedBlockingQueue(20));
    private long expirationTime;

    public RemoteSessionWatcher(String localhostname, Resolver resolver, Agent agent)
        throws JAXBException, UnknownHostException {
        this.resolver = resolver;
        this.localhostname = (localhostname == null) ? this.getHostname()
                                                     : localhostname;
        this.expirationTime = resolver.getExipirationTime();
        cache = CacheBuilder.newBuilder()
                            .expireAfterAccess(expirationTime,
                TimeUnit.MILLISECONDS)
                            .concurrencyLevel(128)
                            .recordStats()
                            .build();
        this.jaxb = new CompressingJAXB<Configuration>(Configuration.class);
        this.agent = agent;
    }

    public ObjectName getJMXObjectName() throws MalformedObjectNameException {
        return new ObjectName(RemoteSessionWatcher.class.getPackage().getName()
            + ":type=" + RemoteSessionWatcher.class.getSimpleName());
    }

    @Override
    public void onConfiguration(Configuration config) {
    }

    @Override
    public void onCreate(final String token, final Match m,
        final List<Destination> affinities) {
        Runnable r = new Runnable() {
                @Override
                public void run() {
                    try {
                        LOG.log(Level.INFO, "Replicating session {0}", token);

                        Session s = new Session(localhostname, token,
                                jaxb.marshall(m.rootConfiguration),
                                new ArrayList<Destination>(affinities));
                        agent.sendHeartbeat(s);
                    } catch (Exception ex) {
                        LOG.log(Level.SEVERE,
                            "EXCEPTION BROADCASTING SESSION AFFINITY INFORMATION",
                            ex);
                    }
                }
            };

        this.exec.execute(r);
    }

    @Override
    public void onFailure(String hostname) {
        LOG.log(Level.INFO, "Doing session affinity migrations for {0}",
            hostname);

        try {
            for (Entry<String, Session> entry : cache.get(hostname,
                    this.callable)
                                                     .asMap()
                                                     .entrySet()) {
                LOG.log(Level.INFO, "Migrating session {0} from host {1}",
                    new Object[] { entry.getValue().sessionToken, hostname });

                CopyOnWriteArrayList<Destination> dests = new CopyOnWriteArrayList<Destination>(entry
                        .getValue().affinities);
                resolver.insert(entry.getValue().sessionToken,
                    this.jaxb.unmarshall(entry.getValue().configuration), dests);
            }
        } catch (Exception ex) {
            LOG.log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public void onHeartbeat(Object remote) {
        if (remote instanceof Session) {
            try {
                //TODO store configuration hashes and check the current actives.
               
                Session inc = (Session) remote;
                LOG.log(Level.INFO, "Got session {0} {1}", new Object[]{inc.host, inc.sessionToken});
                Cache<String, Session> hostcache = cache.get(inc.host,
                        this.callable);

                if (inc.configuration != null) {
                    hostcache.put(inc.sessionToken, inc);
                } else {
                    Session old = hostcache.getIfPresent(inc.sessionToken);
                    if(old != null){
                        LOG.log(Level.FINER, "Hit on {0} session {1}",
                            new Object[] { old.host, old.sessionToken });
                    }
                }
            } catch (ExecutionException ex) {
                LOG.log(Level.SEVERE,
                    "Exception dealing with remote session hit", ex);
            }
        }
    }

    @Override
    public void onHit(String token) {
        Session s = new Session(this.localhostname, token, null, null);
        this.agent.sendHeartbeat(s);
    }

    public void start() {
        this.resolver.addListener(this);
        this.agent.addListener(this);
    }

    public void stop() {
        this.resolver.removeListener(this);
        this.agent.removeListener(this);
    }

    private String getHostname() throws UnknownHostException {
        InetAddress addr = InetAddress.getLocalHost();

        return addr.getHostName();
    }
}
