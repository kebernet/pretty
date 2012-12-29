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
