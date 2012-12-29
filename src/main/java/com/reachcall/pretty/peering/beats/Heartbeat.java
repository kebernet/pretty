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
