/**
 * Copyright 2013, Robert Cooper, Reach Health
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 */
package com.reachcall.util;

import java.util.Map.Entry;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * A simple unlimited object pool.
 */
public abstract class Pool<T> {
    protected static final Logger LOG = Logger.getLogger(Pool.class.getCanonicalName());
    protected final ConcurrentHashMap<T, Pool.CheckoutInfo> locked = new ConcurrentHashMap<T, Pool.CheckoutInfo>();
    protected final ConcurrentLinkedQueue<T> unlocked = new ConcurrentLinkedQueue<T>();
    private final Timer sweeper;
    private final int stackDepthLogging;

    public Pool(final long maxCheckoutTimeMillis) {
        this(maxCheckoutTimeMillis, null, 1);
    }

    protected Pool(final long maxCheckoutTimeMillis, final Pool.ExpiryListener listener, final int stackDepthLogging) {
        this.stackDepthLogging = stackDepthLogging;

        if (maxCheckoutTimeMillis > 0) {
            TimerTask t = new TimerTask() {
                    @Override
                    public void run() {
                        for (Entry<T, Pool.CheckoutInfo> entry : locked.entrySet()) {
                            if ((System.currentTimeMillis() - maxCheckoutTimeMillis) > entry.getValue().time) {
                                locked.remove(entry.getKey());

                                StackTraceElement e = entry.getValue()
                                                           .getStackTrace()[stackDepthLogging];
                                LOG.log(Level.WARNING, "{0} checked out too long: {1}ms. {2} checked out at {3}",
                                    new Object[] {
                                        entry.getKey()
                                             .getClass(), System.currentTimeMillis() - entry.getValue().time,
                                        entry.getKey(), e.toString()
                                    });

                                if (listener != null) {
                                    listener.onExpire(entry.getKey());
                                }
                            }
                        }
                    }
                };

            sweeper = new Timer(true);
            sweeper.scheduleAtFixedRate(t, maxCheckoutTimeMillis, maxCheckoutTimeMillis);
        } else {
            sweeper = null;
        }
    }

    public void checkin(T o) {
        assert o != null : "Checking in a null object";

        Pool.CheckoutInfo info = locked.remove(o);

        if (info != null) {
            unlocked.add(o);
            LOG.log(Level.FINE, "Checked in {0} after {1}ms.",
                new Object[] { o.toString(), System.currentTimeMillis() - info.time });
        } else {
            try {
                throw new RuntimeException();
            } catch (RuntimeException e) {
                LOG.log(Level.WARNING, "Checked in {1} ({0}) with unknown state at {2}",
                    new Object[] { o.hashCode(), o, e.getStackTrace()[stackDepthLogging].toString() });
                LOG.log(Level.FINE, "Unknown state checkin.", e);
            }
        }
    }

    public T checkout() {
        T o = unlocked.poll();

        if (o == null) {
            o = create();
        }

        try {
            throw new Pool.CheckoutInfo();
        } catch (Pool.CheckoutInfo info) {
            locked.put(o, info);
        }

        LOG.log(Level.FINE, "Checkout {0} {1}", new Object[] { o.hashCode(), o.toString() });

        return o;
    }

    protected abstract T create();

    public void invalidate(T value) {
        this.unlocked.remove(value);
        this.locked.remove(value);
        LOG.log(Level.WARNING, "Invalidated {0}", value);
    }
    
    public void invalidateAll(){
        int unlockedCount = this.unlocked.size();
        this.unlocked.clear();
        int lockedCount = this.locked.size();
        this.locked.clear();
        LOG.log(Level.WARNING, "Invalidated all pool items. Cheked out {0} Checked in {1}", new Object[]{lockedCount, unlockedCount});
    }

    public void shutdown() {
        if (this.sweeper != null) {
            sweeper.cancel();
        }
    }

    public static interface ExpiryListener<T> {
        void onExpire(T o);
    }

    private static class CheckoutInfo extends Exception {
        long time = System.currentTimeMillis();
    }
}
