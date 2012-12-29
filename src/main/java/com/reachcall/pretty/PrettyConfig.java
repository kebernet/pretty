package com.reachcall.pretty;

import com.beust.jcommander.Parameter;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import java.util.Properties;

/**
 *
 * @author kebernet
 */
public class PrettyConfig {

    @Parameter(names = {
        "--config", "-c"}, converter = FileConverter.class, description = "The configraiton xml to evaluate from.")
    private File configurationFile = new File("/etc/pretty/proxy.xml");
    @Parameter(names = {
        "--keystore", "-ks"}, converter = FileConverter.class, description = "The Keystore file to use for SSL/TLS")
    private File keystoreFile = new File("/etc/pretty/keystore.jks");
    @Parameter(names = {
        "--logging", "-l"}, converter = FileConverter.class, description = "The logging properties file to configure the server.")
    private File loggingFile = new File("/etc/pretty/logging.properties");
    @Parameter(names = {
        "--settings", "-s"}, converter = FileConverter.class, description = "The settings properties file to configure the server.")
    private File settingsFile = new File("/etc/pretty/settings.properties");
    private Properties props = new Properties();
    @Parameter(names = {
        "--broadcasts", "/bc"}, description = "Show just listen for broadcasts and echo them.")
    private boolean broadcasts;
    @Parameter(names = {
        "--help", "-h", "/?"}, description = "Show command help.")
    private boolean help;
    @Parameter(names = {
        "--list", "-ln", "/l"}, description = "Show network interface list.")
    private boolean listInterfaces;

    public String getAffinityCookie() {
        return props.getProperty("AffinityCookie");
    }

    /**
     * Get the value of configurationFile
     *
     * @return the value of configurationFile
     */
    public File getConfigurationFile() {
        return this.configurationFile;
    }

    public int getGroovyShellPort() {
        return Integer.parseInt(props.getProperty("GroovyShellPort"));
    }

    public String getGroupBroadcastAddress() {
        return props.getProperty("GroupBroadcastAddress", "224.0.0.1");
    }

    public int getGroupBroadcastPort() {
        return Integer.parseInt(props.getProperty("GroupBroadcastPort", "6464"));
    }

    public String getGroupNetworkInterfaceName() {
        return props.getProperty("GroupNetworkInterfaceName");
    }

    public int getHTTPPort() {
        return Integer.parseInt(props.getProperty("HTTPPort", "80"));
    }

    public int getHTTPSPort() {
        return Integer.parseInt(props.getProperty("HTTPSPort", "-1"));
    }

    public File getKeystoreFile() {
        return this.keystoreFile;
    }

    public String getKeystorePassword() {
        return props.getProperty("KeyStorePassword");
    }

    public String getKeyAlias() {
        return props.getProperty("KeyAlias");
    }

    public String getLocalServerName() {
        return props.getProperty("LocalServerName");
    }

    /**
     * @return the loggingFile
     */
    public File getLoggingFile() {
        return loggingFile;
    }

    public long getMaxFileUploadSize() {
        return Long.parseLong(props.getProperty("MaxFileUploadSize"));
    }

    public int getMaxRequestsPerHostPerMinute() {
        return Integer.parseInt(props.getProperty("MaxRequestsPerHostPerMinute"));
    }

    public long getMaxSessionAge() {
        return Long.parseLong(props.getProperty("MaxSessionAge"));
    }

    /**
     * Get the value of settingsFile
     *
     * @return the value of settingsFile
     */
    public File getSettingsFile() {
        return this.settingsFile;
    }

    public int getTimeToFailure() {
        return Integer.parseInt(props.getProperty("timeToFailure"));
    }

    public String getUserId() {
        return props.getProperty("ServiceUserId");
    }

    public boolean isAutoStartFailWatcher() {
        return Boolean.parseBoolean(props.getProperty("AutoStartFailWatcher"));
    }

    public boolean isAutoStartHeart() {
        return Boolean.parseBoolean(props.getProperty("AutoStartHeart"));
    }

    public boolean isAutoStartRemoteSessionWatcher() {
        return Boolean.parseBoolean(props.getProperty("AutoStartRemoteSessionWatcher"));
    }

    /**
     * @return the broadcasts
     */
    public boolean isBroadcasts() {
        return broadcasts;
    }

    public boolean isEnableGroovyShell() {
        return Boolean.parseBoolean(props.getProperty("EnableGroovyShell"));
    }

    public boolean isHelp() {
        return help;
    }

    /**
     * @return the listInterfaces
     */
    public boolean isListInterfaces() {
        return listInterfaces;
    }

    public boolean isWatchConfigFile() {
        return Boolean.parseBoolean(props.getProperty("WatchConfigFile"));
    }

    public void loadSettings() throws IOException {
        props.load(new FileInputStream(this.settingsFile));
    }

    /**
     * Set the value of configurationFile
     *
     * @param newconfigurationFile new value of configurationFile
     */
    public void setConfigurationFile(File newconfigurationFile) {
        this.configurationFile = newconfigurationFile;
    }

    /**
     * Set the value of settingsFile
     *
     * @param newsettingsFile new value of settingsFile
     */
    public void setSettingsFile(File newsettingsFile) {
        this.settingsFile = newsettingsFile;
    }
}
