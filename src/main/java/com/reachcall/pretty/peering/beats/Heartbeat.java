/**
 * Copyright 2013, Robert Cooper, Reach Health
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 */
package com.reachcall.pretty.peering.beats;

import java.io.Serializable;

/**
 *
 * @author kebernet
 */
public class Heartbeat implements Serializable {
    
    
    
    public final String instanceName;
    public final long timestamp;
    
    public Heartbeat(String instanceName){
        this.instanceName = instanceName;
        this.timestamp = System.currentTimeMillis();
    }

    @Override
    public String toString() {
        return "HB "+instanceName+" @ "+timestamp;
    }
    
}
