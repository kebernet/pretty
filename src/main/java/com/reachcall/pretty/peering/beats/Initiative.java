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
