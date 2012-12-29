/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.reachcall.pretty;

import com.beust.jcommander.Parameter;

import java.io.File;

/**
 *
 * @author Robert
 */
public class TLSSetupConfig {

    @Parameter(names = {
        "--cert-pem", "-c"}, converter = FileConverter.class, description = "The PEM file for the certificate.")
    private File certificatePem;
    @Parameter(names = {
        "--key-pem", "-k"}, converter = FileConverter.class, description = "The PEM file for the key.")
    private File keyPem;
    @Parameter(names = {
        "--keystore", "-ks"}, converter = FileConverter.class, description = "The Keystore file to use for SSL/TLS")
    private File keystoreFile = new File("/etc/pretty/keystore.jks");
    @Parameter(names = {
        "--alias", "-a"}, description = "The new alias for the key in the keystore.")
    private String alias;
    @Parameter(names = {
        "--password", "-p"}, description = "The password for the keystore.")
    private String keystorePassword;
    @Parameter(names = {
        "--help", "-h", "/?"}, description = "Show command help.")
    private boolean help;

    public void allOK() throws Exception {
        if (!keystoreFile.exists()) {
            throw new Exception(keystoreFile + " does not exist.");
        }
        if (certificatePem == null) {
            throw new Exception("No certificate PEM specified.");
        }
        if (!certificatePem.exists()) {
            throw new Exception(certificatePem.getAbsolutePath() + " does not exist.");
        }
        if (keyPem == null) {
            throw new Exception("No key PEM specified.");
        }
        if (!keyPem.exists()) {
            throw new Exception(certificatePem.getAbsolutePath() + " does not exist.");
        }
        if (keystorePassword == null) {
            throw new Exception("Keystore password not specified.");
        }
        if (alias == null) {
            throw new Exception("Alias not specified.");
        }
    }

    /**
     * @return the certificatePem
     */
    public File getCertificatePem() {
        return certificatePem;
    }

    /**
     * @return the keyPem
     */
    public File getKeyPem() {
        return keyPem;
    }

    /**
     * @return the keystoreFile
     */
    public File getKeystoreFile() {
        return keystoreFile;
    }

    /**
     * @return the alias
     */
    public String getAlias() {
        return alias;
    }

    /**
     * @return the keystorePassword
     */
    public String getKeystorePassword() {
        return keystorePassword;
    }

    /**
     * @return the help
     */
    public boolean isHelp() {
        return help;
    }
}
