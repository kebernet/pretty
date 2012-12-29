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
package com.reachcall.pretty.peering;

import java.io.File;
import java.util.Map;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author robert.cooper
 */
public class FailHandlerTest {
    
    public FailHandlerTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    
    
    /**
     * Test of onFailure method, of class FailHandler.
     */
    @Test
    public void testOnFailure() {
        System.out.println("onFailure");
        String hostname = "test";
        FailHandler instance = new FailHandler();
        instance.setFailScriptDirectory(new File("src/test/resources"));
        instance.onFailure(hostname);
    }
}
