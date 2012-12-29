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
