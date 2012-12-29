/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.reachcall.pretty.peering.beats;

import com.reachcall.pretty.config.Destination;

import java.io.Serializable;

import java.util.ArrayList;


/**
 *
 * @author robert.cooper
 */
public class Session implements Serializable {
    public final ArrayList<Destination> affinities;
    public final String host;
    public final String sessionToken;
    public final byte[] configuration;

    public Session(String host, String sessionToken, byte[] configuration,
        ArrayList<Destination> affinities) {
        this.host = host;
        this.sessionToken = sessionToken;
        this.configuration = configuration;
        this.affinities = affinities;
    }
}
