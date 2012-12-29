package com.reachcall.pretty.config;

import com.reachcall.pretty.config.Configurator.ConfiguratorCallback;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import javax.xml.bind.JAXBException;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import org.junit.BeforeClass;

/**
 *
 * @author kebernet
 */
public class ConfiguratorTest {
    
    public ConfiguratorTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }
    
    @Before
    public void setUp() {
    }
    
    @After
    public void tearDown() {
    }

    /**
     * Test of setConfigFile method, of class Configurator.
     */
    @Test
    public void testSimple() throws JAXBException {
        System.out.println("testSimple");
        File newconfigFile = new File("target/pretty-config.xml");
        
        final Configuration config = new Configuration();
        config.setHostPattern(".*");
        Path p = new Path();
        p.setSource("/*");
        p.setDestination("/");
        ArrayList<Path> paths = new ArrayList<Path>();
        paths.add(p);
        config.setPaths(paths);
        Configurator configurator = new Configurator(new ConfiguratorCallback(){
            @Override
            public Configuration activeConfiguration(){
                return config;
            }
            
            @Override
            public void onLoad(Configuration read){
                assert read.equals(config) : "Not equal configs";
                
            }
        });
        Destination dest = new Destination();
        dest.setHost("localhost");
        dest.setPort(8080);
        config.setDestinations(Arrays.asList(new Destination[]{ dest }));
        configurator.setConfigFile(newconfigFile);
        configurator.save();
        configurator.load();
        
        
    }

   
    
}
