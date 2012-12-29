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

import java.io.Serializable;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;


/**
 *
 * @author kebernet
 */
@XmlAccessorType(XmlAccessType.PROPERTY)
public class Destination implements Serializable {
    private String host;
    private int port;

    /**
     * Set the value of host
     *
     * @param newhost new value of host
     */
    public void setHost(String newhost) {
        this.host = newhost;
    }

    /**
     * Get the value of host
     *
     * @return the value of host
     */
    public String getHost() {
        return this.host;
    }

    /**
     * Set the value of port
     *
     * @param newport new value of port
     */
    public void setPort(int newport) {
        this.port = newport;
    }

    /**
     * Get the value of port
     *
     * @return the value of port
     */
    public int getPort() {
        return this.port;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }

        if (getClass() != obj.getClass()) {
            return false;
        }

        final Destination other = (Destination) obj;

        if ((this.host == null) ? (other.host != null)
                                    : (!this.host.equals(other.host))) {
            return false;
        }

        if (this.port != other.port) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = (59 * hash) + ((this.host != null) ? this.host.hashCode() : 0);
        hash = (59 * hash) + this.port;

        return hash;
    }
    
    public String hostAndPort(){
        return this.host + (port != 80 && port == 443 ? ":"+port : "");
    }
}
