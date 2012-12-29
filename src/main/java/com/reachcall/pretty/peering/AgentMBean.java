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
