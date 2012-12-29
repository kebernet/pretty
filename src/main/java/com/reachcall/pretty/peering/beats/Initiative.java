package com.reachcall.pretty.peering.beats;

import java.io.Serializable;
import java.util.Random;

/**
 *
 * @author kebernet
 */
public class Initiative implements Serializable {
    
    public final String instanceName;
    public final String toReplaceInstance;
    public final int roll;
    
    public Initiative(String instanceName, String toReplaceInstance){
        this.instanceName = instanceName;
        this.toReplaceInstance = toReplaceInstance;
        this.roll = new Random().nextInt();
    }

    @Override
    public String toString() {
        return instanceName+" rolls "+roll+" to replace "+toReplaceInstance;
    }
    
}
