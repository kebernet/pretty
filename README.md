Introduction
============

Pretty is a Proxy server based on embedded Jetty, that does, or will meet all of these requirements. It supports runtime, graceful configuration changes that can be administered several ways; Changes can be re-read from the filesystem at runtime, they can be updated through an embedded Groovy command shell that runs on a localhost TCP socket, changes can also be pushed via the Java Management Extensions and JConsole.

There are two types of configuration in Pretty: server settings and resolution configuration (Settings vs Configuration). Settings can only be changed with a server restart. Configuration be changed at any time. When a configuration changes, Pretty keeps the previous configurations active until all users who have begun a session with a particular configuration have ended their session. It will then post a message indicating the configuration migration is complete. For example, User A begins a session with a configuration. His session is tied to Application Server A, from a pool that containers AS-A and AS-B. The administrator can then post a change to the configuration that takes AS-A out of the pool. User A will continue to talk to AS-A until his session expires. Any new users will only be sent to AS-B. Once all sessions to AS-A have ended, a message will be logged indicating it is safe to perform whatever administrative/deployment operations need to happen to AS-A since there are no more users accessing the system.

In the next couple of sections, we will look at the static Settings and the dynamic Configuration in turn.

Startup
=======

To successfully start Pretty, you need to include the npn-boot.jar on the boot classpath. An example of this is:

    java -Xbootclasspath/p:npn-boot.jar -jar target/pretty-1.0-SNAPSHOT-runnable.jar --help

This will give you the help text on the command line:

    Usage: pretty [options]
      Options:
        --config, -c     The configraiton xml to evaluate from.
                         Default: /etc/pretty/proxy.xml
        --help, /?       Show command help
                         Default: false
        --settings, -s   The settings properties file to configure the server.
                         Default: /etc/pretty/settings.properties

The default install will place /usr/bin/pretty as an available option. There is no current init.d script, but this is coming.

Settings
========

Settings are stored in a properties file. The properties file used can be passed in at the command line using "--settings [file]" or "-s [file]". If this argument is omitted, it will default to /etc/pretty/settings.properties. Below is a sample settings file:

    AffinityCookie = JSESSIONID
    MaxFileUploadSize = 52428800
    MaxSessionAge = 3600000
    MaxRequestsPerHostPerMinute = 1000
    EnableGroovyShell = true
    GroovyShellPort = 6363
    WatchConfigFile = true
    GroupBroadcastAddress = 224.0.0.1
    GroupBroadcastPort = 6464
    GroupNetworkInterfaceName = en1
    #GroupNetworkInterfaceName = eth0
    AutoStartHeart = true
    AutoStartFailWatcher = true
    AutoStartRemoteSessionWatcher = true
    HTTPPort=80
    HTTPSPort=443
    LocalServerName = proxy01
    ServiceUserId = 501
    KeyStorePassword=importkey
    KeyAlias=importkey

The first setting, "AffinityCookie" indicates the cookie value that will be used to attach a series of requests to a particular application server. "JSESSIONID" is the likely value for most Java-based application servers, but this can be changed to support whatever standard or custom cookies a user might have. It is important to note that an unknown AffinityCookie is treated the same way as no cookie. Also, a response from the app server that changes the affinity cookie ("Set-Cookie") will not change the server binding for the user. That is, abandoning a session in an app server and starting a new one (logging out) will not result in the user being assigned to a new application server.

Next is "MaxFileUploadSize". This is a limit, in bytes, of the maximum size a multipart-formdata request can contain. NB: This does not apply to other mime-types in HTTP PUT or HTTP POST requests. Any MPFD requests that come in are fully received at the Pretty leve before being passed to the application server to ensure integrity. Any requests that exceed the max size will be rejected.

MaxSessionAge is a critical value. This is the time, in milliseconds, that Pretty will keep an Affinity Cookie value in its internal cache. This should be a value that exceeds the application servers session by some amount. Checks for expired sessions that contain old configurations will occur at 1/4 this time.

MaxRequestsPerHostPerMinute is a default value to limit flooding/DOS attacks on the application server. This is a default value that can be changed per-configuration.

EnableGroovyShell and GroovyShellPort control the availability of the REPL Groovy shell on a TCP port. This port will only ever be available on localhost, and allows runtime changes to the server. While these options are configured in the Settings, which cannot generally be changed, toggling the on/off status of the Groovy shell can be controlled via JMX at runtime.

WatchConfigFile toggles whether or not there should be a thread looking for changes to the on-disk configuration information. When this is enabled, a check will be performed every 5 seconds against the current configuration file on the filesystem to look for a change to the Last Modified Time. A change will cause the server to re-load the configuration and begin transitioning new sessions to the new configuration file.

GroupBroadcastAddress is the IP Multicast address to broadcast presence and other stuff to. See also: http://www.networksorcery.com/enp/protocol/ip/multicast.htm

GroupBroadcastPort is the port number to broadcast to and listen on. If you wish to run multiple, separately configured clusters of Pretty on the same subnet, you can assign them differing port numbers.

GroupNetworkInterfaceName (optional) the hardware interface to send multicast packets over.

AutoStartHeart toggles whether to automatically start the heartbeat server. If you do not start it, other Pretty instances will not attempt to take over for this instance.

AutoStartFailWatcher toggles whether this server should watch for failure states on other servers and attempt to take over for them.

AutoStartRemoteSessionWatcher toggles whether this server should listen for remote session events. If you have the FailWatcher running, you likely want this running to. You can turn it off and maybe same some memory usage, but any sessions on servers you take over for might not resume.

HTTPPort/HTTPSPort are self explanatory. If HTTPSPort is not specified, TLS/SSL is disabled.

LocalServerName (optional) is the name the instance will use to identify itself with other instances. By default, it will use whatever the default name on the default interface is. This is usually OK, but if you want to run multiple instances of Pretty on the same server, be sure to change this to a unique value for each one.

ServiceUserId (optional) a numeric user id to switch to when the service has started. This makes it easy to not run as root on *nix machines. However, it does make running fail scripts problematic. See the runtime configuration section below.

KeyStorePassword the password for the Java Keystore containing the TLS/SSL information (if SSL is enabled)

KeyAlias the alias for the cert-signed key in the keystore to use.

Configuration
=============

Configuration works at, potentially, infinite levels, but we will start with the most basic. The configuration is initially loaded based on the "--config [file]" or "-c [file]" command line argument. If this is not provided, it is assumed to be "/etc/pretty/proxy.xml". Below is the most basic configuration.

    <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
    <configuration>
        <children/>
        <destinations>
            <destination>
                <host>localhost</host>
                <port>8080</port>
            </destination>
        </destinations>
        <host-pattern>.*</host-pattern>
        <paths>
            <path>
                <destination>/</destination>
                <source>/.*</source>
            </path>
        </paths>
    </configuration>

This is a configuration with one destination server, a simple host match, and a simple passthrough of request paths. Taking each element in turn:
<children> (empty) This element can contain a series or tree of <configurations> that override the default configuration when they match.
<destinations> contains 1..N <destination> elements that represent a host and port to pass requests to. If there are multiple <destination> elements declared, one will be chosen at random for each new session. Once a session is established, it will pass all requests matching the configuration to the same destination.
<host-pattern> is a Regular Expression that indicates whether the requested host name matches this configuration. As a rule, the top level configuration should always be ".*" or "Match any host".
<paths> contain multiple <path> elements that contains a <destination> and a <source>. The destination is a prefix on the path that will be requested to the back side server, while the <source> is a RegEx indicating whether the request matches the path. You can do some basic re-write functionality here by using the first RegEx group expression in the match. For instance, <destination>/bar/<destination> <source>/foo/(.*)</source> will cause requests to /foo/whatever to be mapped to /bar/whatever on the destination server. You can also use this as a simple access control mechanism. For example, having a <source>/allowed/.*</source> in stead of a wildcard first, followed by <source>/.*</source><destination>/forbidden</destination> will prevent the end user from requesting forbidden paths from the front end proxy.
Now that we have covered the basics, let's look at some more complex cases. A host match of *. matches any host, but say you want to map "beta.mysite.com" to a different endpoint, allowing you to test a new version without deploying it across all of the production application server. You might update the configuration to:

<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<configuration>
    <children>
        <configuration>
            <host-pattern>beta\..*</host-pattern>
            <destinations>
                <destination>
                    <host>appserver-b</host>
                    <port>8080</port>
                </destination>
            </destinations>
        </configuration>
    </children>
    <destinations>
        <destination>
            <host>appserver-a</host>
            <port>8080</port>
        </destination>
    </destinations>
    <host-pattern>.*</host-pattern>
    <paths>
        <path>
            <destination>/</destination>
            <source>/.*</source>
        </path>
    </paths>
</configuration>

In this case, we have added a new configuration under the <children> section that matches only "beta." host names -- note the escaping of the "dot" character; this is a regular expression, not a free text pattern -- and directs those sessions to "appserver-b" rather than the default of "appserver-a". If you were doing a migration, you might have replaced a configuration that had two destinations in the default configuration with this one. You would then wait for the "NO MORE ACTIVE SESSIONS ON OLD CONFIGURATIONS" message, and know you could safely redeploy to appserver-b and only affect the beta.mysite.com URLs. If instead, you wanted to keep beta.mysite.com running the entire time, you might change from a configuration with two appservers, to one with one appserver, then publish this configuration, with would then transition current beta.mysite.com sessions to the new application server.

If you are working entirely with the filesystem, and have enabled the "WatchConfigFile" setting, making the change to the configuration file, waiting 5 seconds to see the "INFO: Active configuration changed." message will indicate the server has been updated.

If the Agent service is running, any change to the configuration on one server will be broadcast to all the other servers in the cluster. They will then update their active configuration and write out the updated configuration to the config file they started up with.

Groovy Command Shell
====================

If you have enabled the Groovy Command Shell, you can connect to it with telnet by typing "telnet localhost 6363", or whatever port number you have configured for it. Remember, you can *only* connect from the local machine. Pretty will not bind the socket listener to any external interface, ever. Below you see an example of this.



Here you see a set of default commands. You can also, as seen above, call Configurator.load to re-load the configuration from disk. Other standard things you can do from the command shell:

Configurator.startWatching() -- begin watching the configuration file for changes.
Configurator.stopWatching() -- stop watching the configuration file for changes.
Configurator.load() -- load the current configuration file from disk
Configurator.save() -- save the current active configuration to disk
Resolver.getInactivesInCache() -- returns the current number of sessions on old configurations waiting to phase out
Resolver.stats() -- shows the status of the various caches in the resolution system
Server.stop() -- stops the proxy server.
Server.start() -- starts the proxy server.
Agent.start() -- starts the remote config synchronization agent.
(The services below this require this to be running to operate)
Agent.stop() -- stops the remote synchronization agent.
Heart.start() -- starts the heartbeat broadcasts.
Heart.stop() -- stops heartbeat broadcasts.
FailWatcher.start() -- starts the remote server failure watcher.
FailWatcher.stop() -- stops the fail watcher
RemoteSessionWatcher.start() -- starts the remote session replication.
RemoteSessionWatcher.stop() -- stops remote session replication.
Note, however, this is a full-featured Groovy shell. There is actually very little about the runtime state of the application that *can't* be done via this shell. The above list just shows some common tasks that you might want to perform.


You can also start and stop the Groovy Shell Listener using JConsole.

Java Management Extensions (JMX/JConsole)
=========================================

You can connect to the local process for Pretty using JConsole. The image below shows the operations available.



You can control the basic connection functions of the shell server. You can also control the load/save/startWatching/stopWatching values of the Configurator as you might do from the Groovy Shell.


Runtime Configurations
======================

There are a few ways you can run pretty in a Linux environment.

1. As "root" -- This might be a perfectly reasonable option if the machine is an isolated VM or you have jailed the user session
2. As a non-root user using the ServiceUserID parameter -- This works well, but if you need to re-bind the server to a new IP address it can be problematic.
3. As a non-root user using xinetd forwards -- This works well and should likely be the default.

To set up #3, configure the HTTPPort and HTTPSPort to be high range ports. Then edit the /etc/xinetd.d/pretty and pretty-ssl files to enabled the forwards.

Importing Crypto Files
======================

There is a facility to import SSL/TLS keys and certificates from the PEM format that is commonly used when getting certs from CAs. Running the pretty-import-key yeilds:

    Usage: pretty-import-key [options]
      Options:
        --alias, -a       The new alias for the key in the keystore.
        --cert-pem, -c    The PEM file for the certificate.
        --help, -h, /?    Show command help.
                          Default: false
        --key-pem, -k     The PEM file for the key.
        --keystore, -ks   The Keystore file to use for SSL/TLS
                          Default: \etc\pretty\keystore.jks
        --password, -p    The password for the keystore.

Rewrite Support
===============

Pretty also contains the URL Rewrite Filter, which is similar to mod_rewrite for Apache. The config file lives in [folder the settings file is in]/web/rewrite.xml. You can see extended documentation on how to use this here:

http://urlrewritefilter.googlecode.com/svn/trunk/src/doc/manual/4.0/index.html

A common example would be enforcing HTTPS connections to an application. This can be done with:

    <rule>
       <name>Ensure HTTPS</name>
       <condition type="scheme" operator="notequal">https</condition>
       <from>^(.*)$</from>
       <to type="redirect">https://%{server-name}$1</to>
    </rule>

