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


/**
 *
 * @author kebernet
 */
public class Match {
    public final Configuration rootConfiguration;
    public final Path path;
    public final String finalPath;
    public Destination destination;
    public String affinityId;
  
    public Match(final String affinityId, final Path path,
        final String finalPath, final Configuration rootConfiguration) {
        this.affinityId = affinityId;
        this.path = path;
        this.finalPath = finalPath;
        this.rootConfiguration = rootConfiguration;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }

        if (getClass() != obj.getClass()) {
            return false;
        }

        final Match other = (Match) obj;

        if ((this.finalPath == null) ? (other.finalPath != null)
                                         : (!this.finalPath.equals(
                    other.finalPath))) {
            return false;
        }

        if ((this.destination != other.destination)
                && ((this.destination == null)
                || !this.destination.equals(other.destination))) {
            return false;
        }

        if ((this.affinityId == null) ? (other.affinityId != null)
                                          : (!this.affinityId.equals(
                    other.affinityId))) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = (13 * hash)
            + ((this.finalPath != null) ? this.finalPath.hashCode() : 0);
        hash = (13 * hash)
            + ((this.destination != null) ? this.destination.hashCode() : 0);
        hash = (13 * hash)
            + ((this.affinityId != null) ? this.affinityId.hashCode() : 0);

        return hash;
    }
    
    public String toURL(){
        return "http://"+this.destination.getHost()+":"+this.destination.getPort()+ (finalPath.startsWith("/") ? finalPath : "/"+finalPath);
    }
}
