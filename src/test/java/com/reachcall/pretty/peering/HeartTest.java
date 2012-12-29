/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.reachcall.pretty.peering;

import java.io.IOException;
import java.net.SocketException;
import java.net.UnknownHostException;
import javax.xml.bind.JAXBException;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * @author robert.cooper
 */
public class HeartTest {
    
    public HeartTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    /**
     * Test of buildHeartbeat method, of class Heart.
     */
   
    /**
     * Test of start method, of class Heart.
     */
    @Test
    public void testStart() throws InterruptedException, UnknownHostException, SocketException, JAXBException, IOException {
        System.out.println("------------------------------ heartbeats");
        Agent agent = new Agent(6767);
        if(System.getProperty("os.name").indexOf("Mac") != -1){
            System.out.println("IM A MAC");
            agent.setNetworkInterface(Mac.MAC_INTERFACE);
        }
        agent.start();
        Heart instance = new Heart(agent);
        instance.start();
        Thread.sleep(3000);
        instance.stop();
        agent.stop();
    }

  
}
