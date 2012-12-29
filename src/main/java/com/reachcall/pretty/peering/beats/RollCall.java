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
