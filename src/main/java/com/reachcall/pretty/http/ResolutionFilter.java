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

import com.reachcall.pretty.config.Match;
import com.reachcall.pretty.config.Resolver;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;


/**
 *
 * @author kebernet
 */
public class ResolutionFilter implements Filter {
    private String affinityCookie;
    private final Resolver resolver;
    
    public ResolutionFilter(Resolver resolver){
        this.resolver = resolver;
    }
    
    
    /**
     * Set the value of affinityCookie
     *
     * @param newaffinityCookie new value of affinityCookie
     */
    public void setAffinityCookie(String newaffinityCookie) {
        this.affinityCookie = newaffinityCookie;
    }

    /**
     * Get the value of affinityCookie
     *
     * @return the value of affinityCookie
     */
    public String getAffinityCookie() {
        return this.affinityCookie;
    }

    @Override
    public void destroy() {
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response,
        FilterChain chain) throws IOException, ServletException {
        Match m = this.getProxyURL((HttpServletRequest)request);
        request.setAttribute("match", m);
        chain.doFilter(request, response);
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    }

    private Match getProxyURL(HttpServletRequest req) {
        String path = req.getPathInfo();

        if (req.getQueryString() != null) {
            path += ("?" + req.getQueryString());
        }

        Match m = this.resolver.resolve(this.findAffinity(req),
                req.getServerName(), path);

        return m;
    }

    private String findAffinity(HttpServletRequest req) {
        if ((req.getCookies() == null) || (req.getCookies().length == 0)) {
            return null;
        }

        for (Cookie c : req.getCookies()) {
            if (this.affinityCookie.equals(c.getName())) {
                return c.getValue();
            }
        }

        return null;
    }
}
