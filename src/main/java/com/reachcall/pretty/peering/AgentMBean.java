/**
 * Copyright 2013, Robert Cooper, Reach Health
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 */
package com.reachcall.pretty.peering;

/**
 *
 * @author kebernet
 */
public interface AgentMBean {
    
    public void setBroadcastAddress(String newbroadcastAddress);
    public void setMulticastPort(int port);
    public void start() throws Exception;
    public void stop() throws Exception;
    
    
}
