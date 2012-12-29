package com.reachcall.pretty.peering;

import com.reachcall.pretty.config.Configuration;
import com.reachcall.pretty.config.Configurator;
import com.reachcall.pretty.config.Configurator.ConfiguratorCallback;
import com.reachcall.pretty.peering.Agent.AgentListener;
import static org.junit.Assert.*;

import org.junit.Test;

import java.io.File;
import java.io.Serializable;

/**
 *
 * @author kebernet
 */
public class AgentTest {
    /**
     * Test of serializeConfiguration method, of class Agent.
     */
    @Test
    public void testSerializeConfiguration() throws Exception {
        System.out.println("----------------------------serializeConfiguration");

        Callback cb = new Callback();
        Configurator c = new Configurator(cb);
        c.setConfigFile(new File("src/test/resources/three-destinations.xml"));
        c.load();

        Configuration config = cb.config;
        Agent a = new Agent(6767);
        System.out.println(System.getProperty("os.name"));
        if(System.getProperty("os.name").indexOf("Mac") != -1){
            System.out.println("IM A MAC");
            a.setNetworkInterface(Mac.MAC_INTERFACE);
        } 
        byte[] serialized = a.serializeConfiguration(config);
        System.out.println("Serialized " + serialized.length + " bytes");

        Configuration deser = a.deserializeConfig(serialized);
        assertEquals(config, deser);

        a.start();

        Listener l = new Listener();
        a.addListener(l);
        a.sendConfiguration(config);
        Thread.sleep(500);
        a.stop();
       // assertEquals(config, l.config);
    }

    /**
     * Test of serializeObject method, of class Agent.
     */
    @Test
    public void testSerializeObject() throws Exception {
        System.out.println("----------------------------serializeObject");

        Serializable object = "Test";
        Agent instance = new Agent(6363);
        byte[] result = instance.serializeObject(object);

        assertEquals(object, instance.deserializeObject(result));
    }

    private class Listener implements AgentListener {
        Object heartbeat;
        Configuration config;

        @Override
        public void onConfiguration(Configuration config) {
            this.config = config;
        }

        @Override
        public void onHeartbeat(Object remote) {
            this.heartbeat = remote;
        }
    }

    private class Callback implements ConfiguratorCallback {
        Configuration config;

        @Override
        public Configuration activeConfiguration() {
            return config;
        }

        @Override
        public void onLoad(Configuration configuration) {
            this.config = configuration;
        }
    }
}
