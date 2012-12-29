/**
 * Copyright 2013, Robert Cooper, Reach Health
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 */
/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.reachcall.pretty.peering.beats;

import java.io.Serializable;
import java.util.Date;


/**
 *
 * @author robert.cooper
 */
public class RollCall implements Serializable {
    public final String replacingHostname;
    public final long windowEnds;

    public RollCall(String replacingHostname, long windowEnds) {
        this.replacingHostname = replacingHostname;
        this.windowEnds = windowEnds;
    }

    @Override
    public String toString() {
        return "ROLL CALL TO REPLACE "+replacingHostname+" WINDOW ENDS "+new Date(windowEnds);
    }
    
    
}
