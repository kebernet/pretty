/**
 * Copyright 2013, Robert Cooper, Reach Health
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 */
package com.reachcall.pretty;

import com.beust.jcommander.IStringConverter;
import java.io.File;

/**
 *
 * @author kebernet
 */
public class FileConverter implements IStringConverter<File>{

    @Override
    public File convert(String string) {
        return new File(string);
    }
    
}
