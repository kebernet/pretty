/**
 * Copyright 2013, Robert Cooper, Reach Health
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 */
package com.reachcall.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

/**
 *
 * @author kebernet
 */
public class CompressingJAXB<T> {
    
    private final JAXBContext context;
    
    public CompressingJAXB(Class<T> clazz) throws JAXBException{
        this.context = JAXBContext.newInstance(clazz);
    }
    
    public byte[] marshall(T object) throws JAXBException, IOException{
        Marshaller m = context.createMarshaller();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        GZIPOutputStream os = new GZIPOutputStream(baos);
        m.marshal(object, os);
        os.close();
        return baos.toByteArray();
    }
    
    public T unmarshall(byte[] input) throws JAXBException, IOException{
        Unmarshaller u = context.createUnmarshaller();
        ByteArrayInputStream bais = new ByteArrayInputStream(input);
        GZIPInputStream is = new GZIPInputStream(bais);
        return (T) u.unmarshal(is);
    }
    
}
