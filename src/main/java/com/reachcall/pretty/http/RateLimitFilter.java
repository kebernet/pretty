/**
 * Copyright 2013, Robert Cooper, Reach Health
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 */
package com.reachcall.pretty.http;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import com.reachcall.pretty.Pretty;

import java.io.IOException;

import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;


/**
 *
 * @author robert.cooper
 */
public class RateLimitFilter implements Filter {
    private static final Logger LOG = Logger.getLogger(Pretty.class
            .getCanonicalName());
    private static long MINUTE = 60 * 1000;
    Cache<String, RequestTimestamp> history = CacheBuilder.newBuilder()
                                                          .expireAfterAccess(2,
            TimeUnit.MINUTES)
                                                          .build();
    private int maxRequestsPerMinute;

    public RateLimitFilter() {
    }

    /**
     * Set the value of maxRequestsPerMinute
     *
     * @param newmaxRequestsPerMinute new value of maxRequestsPerMinute
     */
    public void setMaxRequestsPerMinute(int newmaxRequestsPerMinute) {
        this.maxRequestsPerMinute = newmaxRequestsPerMinute;
    }

    /**
     * Get the value of maxRequestsPerMinute
     *
     * @return the value of maxRequestsPerMinute
     */
    public int getMaxRequestsPerMinute() {
        return this.maxRequestsPerMinute;
    }

    @Override
    public void destroy() {
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response,
        FilterChain chain) throws IOException, ServletException {
        String host = request.getRemoteAddr();
        
        //Short circuit return
        if (maxRequestsPerMinute <= 0 || host == null) {
            chain.doFilter(request, response);

            return;
        }

        RequestTimestamp head = history.getIfPresent(host);
        long time = System.currentTimeMillis();
        RequestTimestamp rs = new RequestTimestamp();
        rs.timestamp = time;

        if (head == null) {
            history.put(host, rs);
        } else {
            while (((time - head.timestamp) > MINUTE) && (head.next != null)) {
                head = head.next;
            }

            RequestTimestamp tail = head;
            int count = 0;

            while (tail.next != null) {
                tail = tail.next;
                count++;
            }

            LOG.log(Level.FINE, "{0} requests this minute: {1}", new Object[]{host, count});
            tail.next = rs;
            history.put(host, head);

            if (count > 1000) {
                ((HttpServletResponse) response).setStatus(429);

                return;
            }
        }

        chain.doFilter(request, response);
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    }

    private class RequestTimestamp {
        RequestTimestamp next;
        long timestamp;
    }
}
