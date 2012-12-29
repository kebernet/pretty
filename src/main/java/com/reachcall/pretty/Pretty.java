package com.reachcall.pretty;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;

import com.reachcall.pretty.config.Configuration;
import com.reachcall.pretty.config.Configurator;
import com.reachcall.pretty.config.Configurator.ConfiguratorCallback;
import com.reachcall.pretty.config.Resolver;
import com.reachcall.pretty.http.ProxyServlet;
import com.reachcall.pretty.http.RateLimitFilter;
import com.reachcall.pretty.http.ResolutionFilter;
import com.reachcall.pretty.peering.Agent;
import com.reachcall.pretty.peering.Agent.AgentListener;
import com.reachcall.pretty.peering.FailHandler;
import com.reachcall.pretty.peering.FailWatcher;
import com.reachcall.pretty.peering.Heart;
import com.reachcall.pretty.peering.RemoteSessionWatcher;
import com.reachcall.pretty.shell.ShellServer;

import com.reachcall.util.SetUID;
import java.io.File;

import org.eclipse.jetty.http.ssl.SslContextFactory;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ssl.SslSocketConnector;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.resource.ResourceCollection;

import org.tuckey.web.filters.urlrewrite.UrlRewriteFilter;

import java.io.FileInputStream;
import java.io.IOException;

import java.lang.management.ManagementFactory;

import java.net.NetworkInterface;
import java.net.SocketException;

import java.util.EnumSet;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import javax.servlet.DispatcherType;

import javax.xml.bind.JAXBException;

/**
 *
 * @author kebernet
 */
public class Pretty {

    private static final Logger LOG = Logger.getLogger(Pretty.class.getCanonicalName());

    private static void listInterfaces() throws SocketException {
        Enumeration<NetworkInterface> ifs = NetworkInterface.getNetworkInterfaces();

        while (ifs.hasMoreElements()) {
            NetworkInterface iface = ifs.nextElement();
            System.out.println(iface.getName() + "\t" + iface.getDisplayName() + "\t" + iface.getHardwareAddress());
        }
    }

    public static void main(String... args) throws Exception {
        PrettyConfig config = new PrettyConfig();
        try {
            JCommander jCommander = new JCommander(config, args);
            jCommander.setProgramName("pretty");
            if (config.isHelp()) {
                jCommander.usage();

                return;
            }
        } catch (ParameterException ex) {
            System.err.println(ex.getMessage());
            System.err.println("Use --help for usage information.");
            return;
        }


        if (config.isListInterfaces()) {
            listInterfaces();

            return;
        }

        if (config.isBroadcasts()) {
            showBroadcasts(config);

            return;
        }

        config.getLoggingFile()
                .getParentFile()
                .mkdirs();
        LogManager.getLogManager()
                .readConfiguration(new FileInputStream(config.getLoggingFile()));
        config.loadSettings();

        //
        // JMX setup
        //
        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();

        //
        // AGENT and Fail
        //
        final Agent agent = new Agent(config.getGroupBroadcastAddress(), config.getGroupBroadcastPort());
        agent.setNetworkInterface(config.getGroupNetworkInterfaceName());
        agent.start();
        mbs.registerMBean(agent, agent.getJMXObjectName());

        FailWatcher watcher = new FailWatcher(config.getLocalServerName(), agent);

        if (config.isAutoStartFailWatcher()) {
            watcher.start();
        }

        mbs.registerMBean(watcher, watcher.getJMXObjectName());

        Heart heart = new Heart(config.getLocalServerName(), agent);

        if (config.isAutoStartHeart()) {
            heart.start();
        }

        final Resolver resolver = new Resolver(null, config.getMaxSessionAge());
        final Configurator configurator = new Configurator(new ConfiguratorCallback() {
            @Override
            public Configuration activeConfiguration() {
                return resolver.getActiveConfiguration();
            }

            @Override
            public void onLoad(Configuration configuration) {
                resolver.setActiveConfiguration(configuration);
                agent.sendConfiguration(configuration);
            }
        });

        configurator.setConfigFile(config.getConfigurationFile());
        configurator.load();

        if (config.isWatchConfigFile()) {
            configurator.startWatching();
        }

        ObjectName name = new ObjectName("com.reachcall.pretty.config:type=Configurator");
        mbs.registerMBean(configurator, name);

        agent.addListener(new AgentListener() {
            @Override
            public void onConfiguration(Configuration config) {
                try {
                    LOG.info("Got updated configuration.");

                    if (!config.equals(resolver.getActiveConfiguration())) {
                        resolver.setActiveConfiguration(config);
                        configurator.save();
                    }
                } catch (JAXBException ex) {
                    LOG.log(Level.SEVERE, "Failed to save new remote configuration", ex);
                }
            }

            @Override
            public void onHeartbeat(Object remote) {
            }
        });

        //
        // Remote sessions
        //
        RemoteSessionWatcher rs = new RemoteSessionWatcher(config.getLocalServerName(), resolver, agent);

        if (config.isAutoStartRemoteSessionWatcher()) {
            rs.start();
        }

        // 
        // JETTY
        //
        Server server = new Server(config.getHTTPPort());

        if (config.getHTTPSPort() != -1) {
            SslContextFactory ctxFactory = new SslContextFactory();
            ctxFactory.setKeyStorePath(config.getKeystoreFile().getAbsolutePath());
            ctxFactory.setKeyStorePassword(config.getKeystorePassword());
            ctxFactory.setCertAlias(config.getKeyAlias());
            ctxFactory.setProtocol("TLSv1");

            //        HTTPSPDYServerConnector sCon = new HTTPSPDYServerConnector(ctxFactory);
            //        sCon.setPort(443);
            //        sCon.setResponseBufferSize(2048);
            //        server.addConnector(sCon);
            SslSocketConnector sCon = new SslSocketConnector(ctxFactory);
            sCon.setPort(config.getHTTPSPort());
            server.addConnector(sCon);
        } else {
            LOG.warning("HTTPSPort not configured. TLS did not start.");
        }

        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath("/");
        File webFolder = new File(config.getSettingsFile().getParentFile(), "web");
        if (webFolder.exists()) {
            context.setBaseResource(new ResourceCollection(new String[]{"/etc/pretty/web"}));
        }
        server.setHandler(context);

        //
        // Proxy Servlet
        //
        ProxyServlet servlet = new ProxyServlet();
        servlet.setResolver(resolver);
        servlet.setAffinityCookie(config.getAffinityCookie());

        RateLimitFilter filter = new RateLimitFilter();
        filter.setMaxRequestsPerMinute(config.getMaxRequestsPerHostPerMinute());
        context.addFilter(new FilterHolder(filter), "/*", EnumSet.of(DispatcherType.REQUEST));

        UrlRewriteFilter rewrite = new UrlRewriteFilter();
        FilterHolder fh = new FilterHolder(rewrite);
        fh.setInitParameter("confReloadCheckInterval", "20");
        fh.setInitParameter("confPath", "/rewrite.xml");
        context.addFilter(fh, "/*", EnumSet.of(DispatcherType.REQUEST, DispatcherType.FORWARD));

        ResolutionFilter resFilter = new ResolutionFilter(resolver);
        resFilter.setAffinityCookie(config.getAffinityCookie());
        context.addFilter(new FilterHolder(resFilter), "/*", EnumSet.of(DispatcherType.REQUEST));

        context.addServlet(new ServletHolder(servlet), "/*");

        server.start();

        //
        // GROOVY
        //
        ShellServer shell = new ShellServer(config.getGroovyShellPort());
        Map<String, Object> bindings = new HashMap<String, Object>();

        bindings.put("Configurator", configurator);
        bindings.put("Resolver", resolver);
        bindings.put("Server", server);
        bindings.put("Agent", agent);
        bindings.put("Heart", heart);
        bindings.put("FailWatcher", watcher);
        bindings.put("RemoteSessionWatcher", watcher);

        FailHandler failHandler = new FailHandler();
        failHandler.setBindings(bindings);
        watcher.addListener(failHandler);

        shell.setBindings(bindings);

        if (config.isEnableGroovyShell()) {
            shell.start();
        }

        ManagementFactory.getPlatformMBeanServer()
                .registerMBean(shell, shell.getJMXObjectName());

        if (config.getUserId() != null) {
            int result = SetUID.setuid(Integer.parseInt(config.getUserId()));

            if (result != SetUID.OK) {
                System.err.println("Failed to set runtime id to " + config.getUserId());
                System.exit(result);
            }
        }

        server.join();
    }

    private static void showBroadcasts(PrettyConfig config)
            throws JAXBException, IOException, InterruptedException {
        Agent agent = new Agent(config.getGroupBroadcastAddress(), config.getGroupBroadcastPort());
        agent.setNetworkInterface(config.getGroupNetworkInterfaceName());
        agent.addListener(new AgentListener() {
            @Override
            public void onConfiguration(Configuration config) {
                System.out.println("Configuration change..");
            }

            @Override
            public void onHeartbeat(Object remote) {
                System.out.println("Message: " + remote);
            }
        });
        agent.start();
        Thread.sleep(600000);
    }
}
