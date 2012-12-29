/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.reachcall.pretty;

import com.beust.jcommander.JCommander;
import com.reachcall.util.ImportKey;
import java.io.File;
import java.io.IOException;

/**
 *
 * @author Robert
 */
public class TLSSetup {
    
    private static File keyPemToDer(File keyPem) throws IOException, InterruptedException{
        File result = File.createTempFile("keyFile", "der");
        result.deleteOnExit();
        //openssl pkcs8 -topk8 -nocrypt -in key.pem -inform PEM -out key.der -outform DER
        ProcessBuilder b = new ProcessBuilder("openssl",
                "pkcs8", "-topk8", "-nocypt", "-in",
                keyPem.getAbsolutePath(), "-inform",
                "PEM", "-out", result.getAbsolutePath(), 
                "-outform", "DER");
        b.inheritIO();
        Process proc = b.start();
       
        int exitCode = proc.waitFor();
        if(exitCode != 0){
            throw new RuntimeException("Conversion of key to der failed. Exit code: "+0);
        }
        return result;
    }
    
    private static File certPemToDer(File certPem) throws IOException, InterruptedException{
        File result = File.createTempFile("certFile", "der");
        result.deleteOnExit();
        //openssl x509 -in cert.pem -inform PEM -out cert.der -outform DE
        ProcessBuilder b = new ProcessBuilder("openssl",
                "x509", "-in", certPem.getAbsolutePath(),
                "-inform", "PEM", "-out", result.getAbsolutePath(),
                "-outform", "DER");
        b.inheritIO();
        Process proc = b.start();
       
        int exitCode = proc.waitFor();
        if(exitCode != 0){
            throw new RuntimeException("Conversion of certificate to der failed. Exit code: "+0);
        }
        return result;
    }
    
    private static void doImport(File keystoreFile, String keystorePassword, String alias, File keyFile, File certFile){
        ImportKey.importKey(keystorePassword, keystoreFile.getAbsolutePath(),keyFile.getAbsolutePath(), certFile.getAbsolutePath(), alias );
    }
    
    private static boolean checkOpenSsl() throws IOException, InterruptedException {
        ProcessBuilder b = new ProcessBuilder("openssl", "--help");
        int result = b.start().waitFor();
        return result == 0;
    }
    
    public static void main(String... args) throws IOException, InterruptedException{
        TLSSetupConfig config = new TLSSetupConfig();
        try {
            JCommander jCommander = new JCommander(config, args);
            jCommander.setProgramName("pretty-import-key");
            if (config.isHelp()) {
                jCommander.usage();

                return;
            }
            config.allOK();
        } catch (Exception ex) {
            System.err.println(ex.getMessage());
            System.err.println("Use --help for usage information.");
            return;
        } 
        
        File certDer = certPemToDer(config.getCertificatePem());
        File keyDer = keyPemToDer(config.getKeyPem());
        doImport(config.getKeystoreFile(), config.getKeystorePassword(), config.getAlias(), keyDer, certDer);
        System.out.println("Import successful.");
    }
    
}
